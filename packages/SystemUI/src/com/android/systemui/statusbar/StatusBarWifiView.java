/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import static com.android.systemui.statusbar.StatusBarIconView.STATE_DOT;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_ICON;
import static com.android.systemui.statusbar.policy.DarkIconDispatcher.getTint;
import static com.android.systemui.statusbar.policy.DarkIconDispatcher.getDarkIntensity;
import static com.android.systemui.statusbar.policy.DarkIconDispatcher.isInArea;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;

import android.widget.LinearLayout;
import com.android.keyguard.AlphaOptimizedLinearLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy.WifiIconState;
import com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver;

/**
 * Start small: StatusBarWifiView will be able to layout from a WifiIconState
 */
public class StatusBarWifiView extends FrameLayout implements DarkReceiver,
        StatusIconDisplayable {
    private static final String TAG = "StatusBarWifiView";

    /// Used to show etc dots
    private StatusBarIconView mDotView;
    /// Contains the main icon layout
    private LinearLayout mWifiGroup;
    private ImageView mWifiIcon;
    private StatusBarInoutContainer mInoutContainer;
    private View mSignalSpacer;
    private View mAirplaneSpacer;
    private WifiIconState mState;
    private String mSlot;
    private int mVisibleState = -1;
    private boolean mActivityEnabled;

    private boolean mShowWifiActivity;
    private final Handler mHandler = new Handler();

     private class SettingsObserver extends ContentObserver {
         SettingsObserver(Handler handler) {
             super(handler);
         }

         void observe() {
             getContext().getContentResolver().registerContentObserver(Settings.System.getUriFor(
                 Settings.System.STATUSBAR_SHOW_WIFI_ACTIVITY), false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
    private SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);

    public static StatusBarWifiView fromContext(Context context, String slot) {
        LayoutInflater inflater = LayoutInflater.from(context);
        StatusBarWifiView v = (StatusBarWifiView) inflater.inflate(R.layout.status_bar_wifi_group, null);
        v.setSlot(slot);
        v.init();
        v.setVisibleState(STATE_ICON);
        return v;
    }

    public StatusBarWifiView(Context context) {
        super(context);
    }

    public StatusBarWifiView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StatusBarWifiView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StatusBarWifiView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setSlot(String slot) {
        mSlot = slot;
    }

    @Override
    public void setStaticDrawableColor(int color) {
        ColorStateList list = ColorStateList.valueOf(color);
        float intensity = color == Color.WHITE ? 0 : 1;
        mWifiIcon.setImageTintList(list);
        mInoutContainer.setDarkIntensity(intensity);
        mDotView.setDecorColor(color);
    }

    @Override
    public void setDecorColor(int color) {
        mDotView.setDecorColor(color);
    }

    @Override
    public String getSlot() {
        return mSlot;
    }

    @Override
    public boolean isIconVisible() {
        return mState != null && mState.visible;
    }

    @Override
    public void setVisibleState(int state) {
        if (state == mVisibleState) {
            return;
        }
        mVisibleState = state;

        switch (state) {
            case STATE_ICON:
                mWifiGroup.setVisibility(View.VISIBLE);
                mDotView.setVisibility(View.GONE);
                break;
            case STATE_DOT:
                mWifiGroup.setVisibility(View.GONE);
                mDotView.setVisibility(View.VISIBLE);
                break;
            case STATE_HIDDEN:
            default:
                mWifiGroup.setVisibility(View.GONE);
                mDotView.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getVisibleState() {
        return mVisibleState;
    }

    @Override
    public void getDrawingRect(Rect outRect) {
        super.getDrawingRect(outRect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        outRect.left += translationX;
        outRect.right += translationX;
        outRect.top += translationY;
        outRect.bottom += translationY;
    }

    private void init() {
        mWifiGroup = findViewById(R.id.wifi_group);
        mWifiIcon = findViewById(R.id.wifi_signal);
        mSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        mAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        mInoutContainer = findViewById(R.id.inout_container);

        mSettingsObserver.observe();
        updateSettings();
        initDotView();
    }

    private void initDotView() {
        mDotView = new StatusBarIconView(mContext, mSlot, null);
        mDotView.setVisibleState(STATE_DOT);

        int width = mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
        LayoutParams lp = new LayoutParams(width, width);
        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.START;
        addView(mDotView, lp);
    }

    public void applyWifiState(WifiIconState state) {
        if (state == null) {
            setVisibility(View.GONE);
            mState = null;
            return;
        }

        if (mState == null) {
            mState = state.copy();
            initViewState();
        }

        if (!mState.equals(state)) {
            updateState(state.copy());
        }
    }

    private void updateState(WifiIconState state) {
        setContentDescription(state.contentDescription);
        if (mState.resId != state.resId && state.resId >= 0) {
            mWifiIcon.setImageDrawable(mContext.getDrawable(mState.resId));
        }

        mInoutContainer.setVisibility(mShowWifiActivity && state.visible ? View.VISIBLE : View.GONE);
        mInoutContainer.setState(state.activityIn, state.activityOut);
        mAirplaneSpacer.setVisibility(state.airplaneSpacerVisible ? View.VISIBLE : View.GONE);
        mSignalSpacer.setVisibility(state.signalSpacerVisible ? View.VISIBLE : View.GONE);
        if (mState.visible != state.visible) {
            setVisibility(state.visible ? View.VISIBLE : View.GONE);
        }

        mState = state;
    }

    private void initViewState() {
        setContentDescription(mState.contentDescription);
        if (mState.resId >= 0) {
            mWifiIcon.setImageDrawable(mContext.getDrawable(mState.resId));
        }

        mInoutContainer.setVisibility(
                mShowWifiActivity && mState.visible ? View.VISIBLE : View.GONE);
        mInoutContainer.setState(mState.activityIn, mState.activityOut);
        mAirplaneSpacer.setVisibility(mState.airplaneSpacerVisible ? View.VISIBLE : View.GONE);
        mSignalSpacer.setVisibility(mState.signalSpacerVisible ? View.VISIBLE : View.GONE);
        setVisibility(mState.visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        if (!isInArea(area, this)) {
            return;
        }
        mWifiIcon.setImageTintList(ColorStateList.valueOf(getTint(area, this, tint)));
        mInoutContainer.setDarkIntensity(getDarkIntensity(area, this, darkIntensity));
        mDotView.setDecorColor(tint);
        mDotView.setIconColor(tint, false);
    }


    @Override
    public String toString() {
        return "StatusBarWifiView(slot=" + mSlot + " state=" + mState + ")";
    }

    public void updateSettings() {
        mShowWifiActivity = Settings.System.getIntForUser(getContext().getContentResolver(),
            Settings.System.STATUSBAR_SHOW_WIFI_ACTIVITY, 0, UserHandle.USER_CURRENT) == 1;
    }
}
