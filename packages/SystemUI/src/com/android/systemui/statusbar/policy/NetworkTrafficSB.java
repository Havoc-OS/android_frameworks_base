package com.android.systemui.statusbar.policy;

import static com.android.systemui.statusbar.StatusBarIconView.STATE_DOT;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_HIDDEN;
import static com.android.systemui.statusbar.StatusBarIconView.STATE_ICON;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import android.view.View;
import android.view.ViewGroup;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.StatusIconDisplayable;

public class NetworkTrafficSB extends NetworkTraffic implements StatusIconDisplayable {

    public static final String SLOT = "networktraffic";

    private int mVisibleState = -1;
    private boolean mSystemIconVisible = true;
    private boolean mKeyguardShowing;

    public NetworkTrafficSB(Context context) {
        this(context, null);
    }

    public NetworkTrafficSB(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NetworkTrafficSB(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ));
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        if (!mIsEnabled || mLocation == 1) return;
        mTintColor = DarkIconDispatcher.getTint(area, this, tint);
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public String getSlot() {
        return SLOT;
    }

    @Override
    public boolean isIconVisible() {
        return mIsEnabled && mLocation == 0 && !mKeyguardShowing;
    }

    @Override
    public int getVisibleState() {
        return mVisibleState;
    }

    @Override
    public void setVisibleState(int state, boolean animate) {
        if (state == mVisibleState || !mIsEnabled || !mAttached) {
            return;
        }
        mVisibleState = state;

        switch (state) {
            case STATE_ICON:
                mSystemIconVisible = true;
                break;
            case STATE_DOT:
            case STATE_HIDDEN:
            default:
                mSystemIconVisible = false;
                break;
        }
        update();
    }

    @Override
    public void setStaticDrawableColor(int color) {
        mTintColor = color;
        setTextColor(mTintColor);
        updateTrafficDrawable();
    }

    @Override
    public void setDecorColor(int color) {}

    public void setKeyguardShowing(boolean showing) {
        mKeyguardShowing = showing;
        if (showing) {
            setText("");
            setVisibility(View.GONE);
        }
        maybeRestoreVisibility();
    }

    private void maybeRestoreVisibility() {
        if (mSystemIconVisible && getVisibility() == View.GONE && isIconVisible() && restoreViewQuickly()) {
            setVisibility(View.VISIBLE);
            update();
        }
    }

    @Override
    boolean isDisabled() {
        return !mIsEnabled || mLocation != 0 || !mSystemIconVisible;
    }
}
