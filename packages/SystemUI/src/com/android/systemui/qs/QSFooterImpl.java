/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.qs;

import static com.android.internal.logging.nano.MetricsProto.MetricsEvent.ACTION_QS_DATE;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.UserManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.CalendarContract; 
import android.net.Uri; 
import com.android.keyguard.CarrierText;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.android.keyguard.KeyguardStatusView;
import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.R.dimen;
import com.android.systemui.R.id;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.TouchAnimator.Builder;
import com.android.systemui.qs.TouchAnimator.Listener;
import com.android.systemui.qs.TouchAnimator.ListenerAdapter;
import com.android.systemui.statusbar.phone.ExpandableIndicator;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.NetworkController.EmergencyListener;
import com.android.systemui.statusbar.policy.NetworkController.SignalCallback;
import com.android.systemui.statusbar.policy.NextAlarmController;
import com.android.systemui.statusbar.policy.NextAlarmController.NextAlarmChangeCallback;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerService.Tunable;
import com.android.systemui.havoc.carrierlabel.CarrierLabelQS;

public class QSFooterImpl extends FrameLayout implements Tunable, QSFooter,
        OnClickListener, OnLongClickListener, OnUserInfoChangedListener, EmergencyListener,SignalCallback  {
    private static final float EXPAND_INDICATOR_THRESHOLD = .93f;

    private static final String QS_FOOTER_SHOW_SERVICES =
            "system:" + Settings.System.QSFOOTER_SHOW_SERVICES;

    private ActivityStarter mActivityStarter;

    private UserInfoController mUserInfoController;
    private SettingsButton mSettingsButton;
    protected View mSettingsContainer;
    private View mRunningServicesButton;

    private QSPanel mQsPanel;

    private boolean mExpanded;
    private boolean mServicesButtonVisible = true;

    protected ExpandableIndicator mExpandIndicator;

    private boolean mListening;

    private boolean mShowEmergencyCallsOnly;
    protected MultiUserSwitch mMultiUserSwitch;
    private ImageView mMultiUserAvatar;

    protected TouchAnimator mSettingsAlpha;
    private float mExpansionAmount;

    protected View mEdit;
    private TouchAnimator mAnimator;
    private boolean mKeyguardShowing;
    private boolean hasEdit;
    private CarrierLabelQS mCarrierText;

    public QSFooterImpl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Resources res = getResources();

        mEdit = findViewById(android.R.id.edit);
        mEdit.setOnClickListener(view ->
                Dependency.get(ActivityStarter.class).postQSRunnableDismissingKeyguard(() ->
                        mQsPanel.showEdit(view)));

        mExpandIndicator = findViewById(R.id.expand_indicator);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsContainer = findViewById(R.id.settings_button_container);
        mSettingsButton.setOnClickListener(this);
        mSettingsButton.setOnLongClickListener(this);

        mCarrierText = findViewById(R.id.qs_carrier_text);

        mRunningServicesButton = findViewById(R.id.running_services_button);
        mRunningServicesButton.setOnClickListener(this);

        mMultiUserSwitch = findViewById(R.id.multi_user_switch);
        mMultiUserAvatar = mMultiUserSwitch.findViewById(R.id.multi_user_avatar);

        // RenderThread is doing more harm than good when touching the header (to expand quick
        // settings), so disable it for this view
        ((RippleDrawable) mSettingsButton.getBackground()).setForceSoftware(true);
        ((RippleDrawable) mExpandIndicator.getBackground()).setForceSoftware(true);
        ((RippleDrawable) mRunningServicesButton.getBackground()).setForceSoftware(true);

        updateSettingsAnimator();

        mUserInfoController = Dependency.get(UserInfoController.class);
        mActivityStarter = Dependency.get(ActivityStarter.class);
        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight,
                oldBottom) -> updateAnimator(right - left));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(TunerService.class).addTunable(this,
                        QS_FOOTER_SHOW_SERVICES);
        updateVisibilities();
    }

    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case QS_FOOTER_SHOW_SERVICES:
                mServicesButtonVisible =
                          newValue != null && Integer.parseInt(newValue) != 0;
                break;
            default:
                break;
        }

        updateVisibilities();
    }

    private void updateAnimator(int width) {
        int numTiles = QuickQSPanel.getNumQuickTiles(mContext);
        int size = mContext.getResources().getDimensionPixelSize(R.dimen.qs_quick_tile_size)
                - mContext.getResources().getDimensionPixelSize(dimen.qs_quick_tile_padding);
        int remaining = (width - numTiles * size) / (numTiles - 1);
        int defSpace = mContext.getResources().getDimensionPixelOffset(R.dimen.default_gear_space);

        mAnimator = new Builder()
                //.addFloat(mSettingsContainer, "translationX", -(remaining - defSpace), 0)
				.addFloat(mSettingsContainer, "alpha", 0, 1)
                .addFloat(mSettingsButton, "rotation", -120, 0)
                .build();

        setExpansion(mExpansionAmount);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateSettingsAnimator();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateSettingsAnimator();
    }

    private void updateSettingsAnimator() {
        mSettingsAlpha = createSettingsAlphaAnimator();
    }

    @Nullable
    private TouchAnimator createSettingsAlphaAnimator() {
        return new TouchAnimator.Builder()
                .addFloat(mCarrierText, "alpha", 0, 1)
                .addFloat(mEdit, "alpha", 0, 1)
                .addFloat(mMultiUserSwitch, "alpha", 0, 1)
                .addFloat(mRunningServicesButton, "alpha", 0, 1)
                .build();
    }

    @Override
    public void setKeyguardShowing(boolean keyguardShowing) {
        mKeyguardShowing = keyguardShowing;
        setExpansion(mExpansionAmount);
    }

    @Override
    public void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        updateEverything();
    }

    @Override
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        if (mAnimator != null) mAnimator.setPosition(headerExpansionFraction);

        if (mSettingsAlpha != null) {
            mSettingsAlpha.setPosition(headerExpansionFraction);
        }

        mExpandIndicator.setExpanded(headerExpansionFraction > EXPAND_INDICATOR_THRESHOLD);
    }

    @Override
    @VisibleForTesting
    public void onDetachedFromWindow() {
        setListening(false);
        Dependency.get(TunerService.class).removeTunable(this);
        super.onDetachedFromWindow();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening == mListening) {
            return;
        }
        mListening = listening;
        updateListeners();
    }

    @Override
    public View getExpandView() {
        return findViewById(R.id.expand_indicator);
    }

    public void updateEverything() {
        post(() -> {
            updateVisibilities();
            setClickable(false);
        });
    }

    private void updateVisibilities() {
        mSettingsContainer.findViewById(R.id.tuner_icon).setVisibility(View.INVISIBLE);
        final boolean isDemo = UserManager.isDeviceInDemoMode(mContext);
        hasEdit = !isEditDisabled();

        mMultiUserSwitch.setVisibility(mExpanded && mMultiUserSwitch.hasMultipleUsers() && !isDemo
                ? View.VISIBLE : View.INVISIBLE);

        mEdit.setVisibility(hasEdit ?  isDemo || !mExpanded ? View.INVISIBLE : View.VISIBLE : View.GONE);

        mCarrierText.setVisibility(mExpanded ? View.VISIBLE : View.INVISIBLE);
       
        mRunningServicesButton.setVisibility(mServicesButtonVisible ? (!isDemo && mExpanded ? View.VISIBLE : View.INVISIBLE) : View.GONE);
    }

    private void updateListeners() {
        if (mListening) {
            mUserInfoController.addCallback(this);
            if (Dependency.get(NetworkController.class).hasVoiceCallingFeature()) {
                Dependency.get(NetworkController.class).addEmergencyListener(this);
                Dependency.get(NetworkController.class).addCallback(this);
            }
        } else {
            mUserInfoController.removeCallback(this);
            Dependency.get(NetworkController.class).removeEmergencyListener(this);
            Dependency.get(NetworkController.class).removeCallback(this);
        }
    }

    public boolean isEditDisabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
            Settings.System.QS_EDIT_TOGGLE, 0) == 1;
    }
  
    @Override
    public void setQSPanel(final QSPanel qsPanel) {
        mQsPanel = qsPanel;
        if (mQsPanel != null) {
            mMultiUserSwitch.setQsPanel(qsPanel);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mSettingsButton) {
            if (!Dependency.get(DeviceProvisionedController.class).isCurrentUserSetup()) {
                // If user isn't setup just unlock the device and dump them back at SUW.
                mActivityStarter.postQSRunnableDismissingKeyguard(() -> { });
                return;
            }
            MetricsLogger.action(mContext,
                    mExpanded ? MetricsProto.MetricsEvent.ACTION_QS_EXPANDED_SETTINGS_LAUNCH
                            : MetricsProto.MetricsEvent.ACTION_QS_COLLAPSED_SETTINGS_LAUNCH);
                startSettingsActivity();
        } else if (v == mRunningServicesButton) {
            MetricsLogger.action(mContext,
                    mExpanded ? MetricsProto.MetricsEvent.ACTION_QS_EXPANDED_SETTINGS_LAUNCH
                            : MetricsProto.MetricsEvent.ACTION_QS_COLLAPSED_SETTINGS_LAUNCH);
            startRunningServicesActivity();
        }
    }

    private void startRunningServicesActivity() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.Settings$DevRunningServicesActivity");
        mActivityStarter.startActivity(intent, true /* dismissShade */);
    }

    @Override
    public boolean onLongClick(View v) {
        if (v == mSettingsButton) {
            HavocSettingsActivity();
        }
        return false;
    }

    private void HavocSettingsActivity() {
        Intent nIntent = new Intent(Intent.ACTION_MAIN);
        nIntent.setClassName("com.android.settings",
            "com.android.settings.Settings$HavocSettingsActivity");
        mActivityStarter.startActivity(nIntent, true /* dismissShade */);
    }

    private void startSettingsActivity() {
        mActivityStarter.startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS),
                true /* dismissShade */);
    }

    @Override
    public void setEmergencyCallsOnly(boolean show) {
        boolean changed = show != mShowEmergencyCallsOnly;
        if (changed) {
            mShowEmergencyCallsOnly = show;
            if (mExpanded) {
                updateEverything();
            }
        }
    }

    @Override
    public void onUserInfoChanged(String name, Drawable picture, String userAccount) {
        if (picture != null &&
                UserManager.get(mContext).isGuestUser(ActivityManager.getCurrentUser())) {
            picture = picture.getConstantState().newDrawable().mutate();
            picture.setColorFilter(
                    Utils.getColorAttr(mContext, android.R.attr.colorForeground),
                    Mode.SRC_IN);
        }
        mMultiUserAvatar.setImageDrawable(picture);
    }
}
