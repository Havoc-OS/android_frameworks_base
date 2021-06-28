/**
 * Copyright (C) 2019-2020 The LineageOS Project
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

package com.android.systemui.biometrics;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_ASSISTANT;
import static android.app.WindowConfiguration.WINDOWING_MODE_UNDEFINED;

import android.annotation.SuppressLint;
import android.app.ActivityManager.StackInfo;
import android.app.ActivityTaskManager;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.hardware.biometrics.BiometricConstants;
import android.hardware.biometrics.BiometricSourceType;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.settingslib.utils.ThreadUtils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.shared.system.TaskStackChangeListener;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener;

import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreen;
import vendor.lineage.biometrics.fingerprint.inscreen.V1_0.IFingerprintInscreenCallback;

import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

public class FODCircleView extends ImageView implements ConfigurationListener {
    private static final int FADE_ANIM_DURATION = 125;

    private final int mPositionX;
    private final int mPositionY;
    private final int mSize;
    private final int mDreamingMaxOffset;
    private final int mNavigationBarSize;
    private final boolean mShouldBoostBrightness;
    private final Paint mPaintFingerprintBackground = new Paint();
    private final Paint mPaintFingerprint = new Paint();
    private final WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();
    private final WindowManager.LayoutParams mPressedParams = new WindowManager.LayoutParams();
    private final WindowManager mWindowManager;
    private final FODAnimation mFODAnimation;

    private IFingerprintInscreen mFingerprintInscreenDaemon;
    private Context mContext;

    private int mDreamingOffsetY;

    private boolean mFading;
    private boolean mIsBouncer;
    private boolean mIsBiometricRunning;
    private boolean mIsCircleShowing;
    private boolean mIsDreaming;
    private boolean mIsKeyguard;
    private boolean mTouchedOutside;
    private boolean mCanUnlockWithFp;
    private boolean mFpDisabled;
    private boolean mShouldRemoveIconOnAOD;
    private boolean mScreenOffFodEnabled;
    private boolean mScreenOffFodIconEnabled;
    private boolean mIsAssistantVisible = false;
    private boolean mIsRecognizingAnimEnabled;

    private Handler mHandler;

    private final ImageView mPressedView;

    private LockPatternUtils mLockPatternUtils;

    private Timer mBurnInProtectionTimer;

    private IFingerprintInscreenCallback mFingerprintInscreenCallback =
            new IFingerprintInscreenCallback.Stub() {
        @Override
        public void onFingerDown() {
            mHandler.post(() -> showCircle());
        }

        @Override
        public void onFingerUp() {
            mHandler.post(() -> hideCircle());
        }
    };

    private KeyguardUpdateMonitor mUpdateMonitor;

    private KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onBiometricAuthenticated(int userId, BiometricSourceType biometricSourceType,
                boolean isStrongBiometric) {
            // We assume that if biometricSourceType matches Fingerprint it will be
            // handled here, so we hide only when other biometric types authenticate
            if (biometricSourceType != BiometricSourceType.FINGERPRINT) {
                hide();
            }
        }

        @Override
        public void onBiometricRunningStateChanged(boolean running,
                BiometricSourceType biometricSourceType) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT) {
                mIsBiometricRunning = running;
                if (running) {
                    mHandler.postDelayed(() -> {
                        mFpDisabled = false;
                        if (mIsKeyguard) {
                            show();
                        }
                    }, mFpDisabled ? 50 : 0);
                }
            }
        }

        @Override
        public void onDreamingStateChanged(boolean dreaming) {
            mIsDreaming = dreaming;

            if (mIsKeyguard && mUpdateMonitor.isFingerprintDetectionRunning()) {
                show();
            } else {
                hide();
            }

            if (dreaming) {
                mBurnInProtectionTimer = new Timer();
                mBurnInProtectionTimer.schedule(new BurnInProtectionTask(), 0, 60 * 1000);
                if (mShouldRemoveIconOnAOD) {
                    resetFODIcon(false);
                }
            } else if (mBurnInProtectionTimer != null) {
                mBurnInProtectionTimer.cancel();
                mBurnInProtectionTimer = null;
                updatePosition();
            }

            if (mShouldRemoveIconOnAOD && !dreaming) {
                resetFODIcon(true);
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            mIsKeyguard = showing;
            if (!showing) {
                hide();
            } else {
                setAlpha(getFODAlpha());
            }
            if (mFODAnimation != null && mIsRecognizingAnimEnabled) {
                mFODAnimation.setAnimationKeyguard(mIsKeyguard);
            }

            if (mFODAnimation != null) {
                mFODAnimation.setAnimationKeyguard(showing);
            }
        }

        @Override
        public void onKeyguardBouncerChanged(boolean isBouncer) {
            mIsBouncer = isBouncer;
            if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                if (isPinOrPattern(mUpdateMonitor.getCurrentUser()) || !isBouncer) {
                    mIsAssistantVisible = false;
                    show();
                } else {
                    hide();
                }
            } else {
                hide();
            }
        }

        @Override
        public void onScreenTurnedOff() {
            hide();
        }

        @Override
        public void onStartedWakingUp() {
            if (mUpdateMonitor.isFingerprintDetectionRunning() && mIsKeyguard) {
                show();
            }
        }

        @Override
        public void onScreenTurnedOn() {
            if (mUpdateMonitor.isFingerprintDetectionRunning() && mIsKeyguard) {
                show();
                if (!mShouldRemoveIconOnAOD) triggerFodIconAnimation();
            }
        }

        @Override
        public void onBiometricError(int msgId, String helpString,
                BiometricSourceType biometricSourceType) {
            if (biometricSourceType == BiometricSourceType.FINGERPRINT
                    && (msgId == BiometricConstants.BIOMETRIC_ERROR_LOCKOUT
                    || msgId == BiometricConstants.BIOMETRIC_ERROR_LOCKOUT_PERMANENT)) {
                mHandler.removeCallbacksAndMessages(null);
                mFpDisabled = true;
                // animate to new alpha value
                show();
            }
        }

        @Override
        public void onStrongAuthStateChanged(int userId) {
            setAlpha(getFODAlpha());
        }

        @Override
        public void onBiometricHelp(int msgId, String helpString,
                                    BiometricSourceType biometricSourceType) {
            if (msgId == KeyguardUpdateMonitor.BIOMETRIC_HELP_FINGERPRINT_NOT_RECOGNIZED) {
                hideCircle();
                if (mFODAnimation != null && mIsRecognizingAnimEnabled)
                    mHandler.post(() -> mFODAnimation.hideFODAnimation());
            }
        }
    };

    private final TaskStackChangeListener
            mTaskStackChangeListener = new TaskStackChangeListener() {
        @Override
        public void onTaskStackChangedBackground() {
            try {
                StackInfo stackInfo = ActivityTaskManager.getService().getStackInfo(
                        WINDOWING_MODE_UNDEFINED, ACTIVITY_TYPE_ASSISTANT);
                if (stackInfo == null && mIsAssistantVisible) {
                        mIsAssistantVisible = false;
                        if (mUpdateMonitor.isFingerprintDetectionRunning()) {
                            mHandler.post(() -> show());
                    }
                    return;
                }
                if (stackInfo != null) mIsAssistantVisible = stackInfo.visible;
                if (mIsAssistantVisible) {
                    mHandler.post(() -> hide());
                }
            } catch (RemoteException ignored) { }
        }
    };

    private boolean mCutoutMasked;
    private int mStatusbarHeight;
    private class CustomSettingsObserver extends ContentObserver {
        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_OFF_FOD),
                    false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.SCREEN_OFF_FOD_ICON),
                    false, this, UserHandle.USER_ALL);
        }

        void unobserve() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        void update() {
            mScreenOffFodEnabled = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_OFF_FOD, 0) != 0;
            mScreenOffFodIconEnabled = Settings.System.getInt(mContext.getContentResolver(),
                    Settings.System.SCREEN_OFF_FOD_ICON, 1) != 0;
            mShouldRemoveIconOnAOD = mScreenOffFodEnabled && !mScreenOffFodIconEnabled;
        }
    }

    private CustomSettingsObserver mCustomSettingsObserver;

    @SuppressLint("RtlHardcoded")
    public FODCircleView(Context context) {
        super(context);
        mContext = context;

        setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        if (daemon == null) {
            throw new RuntimeException("Unable to get IFingerprintInscreen");
        }

        try {
            mShouldBoostBrightness = daemon.shouldBoostBrightness();
            mPositionX = daemon.getPositionX();
            mPositionY = daemon.getPositionY();
            mSize = daemon.getSize();
        } catch (RemoteException e) {
            throw new RuntimeException("Failed to retrieve FOD circle position or size");
        }

        Resources res = context.getResources();

        mPaintFingerprint.setColor(res.getColor(R.color.config_fodColor, null));
        mPaintFingerprint.setAntiAlias(true);

        mPaintFingerprintBackground.setColor(res.getColor(R.color.config_fodColorBackground, null));
        mPaintFingerprintBackground.setAntiAlias(true);

        mWindowManager = context.getSystemService(WindowManager.class);

        mNavigationBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);

        mDreamingMaxOffset = (int) (mSize * 0.1f);

        mHandler = new Handler(Looper.getMainLooper());

        mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
        mCustomSettingsObserver.update();

        mParams.height = mSize;
        mParams.width = mSize;
        mParams.format = PixelFormat.TRANSLUCENT;

        mParams.packageName = "android";
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_FINGERPRINT;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;

        mPressedParams.copyFrom(mParams);
        mPressedParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_FINGERPRINT_HIGH_LIGHT;
        mPressedParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH;

        mParams.setTitle("Fingerprint on display");
        mPressedParams.setTitle("Fingerprint on display.touched");

        mPressedView = new ImageView(context)  {
            @Override
            protected void onDraw(Canvas canvas) {
                if (mIsCircleShowing) {
                    canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2.0f, mPaintFingerprint);
                }
                super.onDraw(canvas);
            }
        };
        mPressedView.setImageResource(R.drawable.fod_icon_pressed);

        mWindowManager.addView(this, mParams);

        updateSettings();
        updatePosition();
        hide();

        mLockPatternUtils = new LockPatternUtils(mContext);

        mUpdateMonitor = Dependency.get(KeyguardUpdateMonitor.class);
        mUpdateMonitor.registerCallback(mMonitorCallback);
    
        updateCutoutFlags();
        Dependency.get(ConfigurationController.class).addCallback(this);

        mFODAnimation = new FODAnimation(context, mPositionY);
    }

    @Override
    protected void onAttachedToWindow() {
        ActivityManagerWrapper.getInstance().registerTaskStackListener(
                mTaskStackChangeListener);
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        ActivityManagerWrapper.getInstance().unregisterTaskStackListener(
                mTaskStackChangeListener);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!mIsCircleShowing) {
            canvas.drawCircle(mSize / 2, mSize / 2, mSize / 2.0f, mPaintFingerprintBackground);
        }
        super.onDraw(canvas);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getAxisValue(MotionEvent.AXIS_X);
        float y = event.getAxisValue(MotionEvent.AXIS_Y);

        boolean newIsInside = (x > 0 && x < mSize) && (y > 0 && y < mSize);
        mTouchedOutside = false;

        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
            mTouchedOutside = true;
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN && newIsInside) {
            showCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            hideCircle();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return true;
        }

        if (mFODAnimation != null && mIsRecognizingAnimEnabled) {
            mHandler.post(() -> mFODAnimation.hideFODAnimation());
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        updateSettings();
        updatePosition();
    }

    public IFingerprintInscreen getFingerprintInScreenDaemon() {
        if (mFingerprintInscreenDaemon == null) {
            try {
                mFingerprintInscreenDaemon = IFingerprintInscreen.getService();
                if (mFingerprintInscreenDaemon != null) {
                    mFingerprintInscreenDaemon.setCallback(mFingerprintInscreenCallback);
                    mFingerprintInscreenDaemon.asBinder().linkToDeath((cookie) -> {
                        mFingerprintInscreenDaemon = null;
                    }, 0);
                }
            } catch (NoSuchElementException | RemoteException e) {
                // do nothing
            }
        }
        return mFingerprintInscreenDaemon;
    }

    public void dispatchPress() {
        if (mFading) return;
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onPress();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchRelease() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onRelease();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchShow() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onShowFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void dispatchHide() {
        IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
        try {
            daemon.onHideFODView();
        } catch (RemoteException e) {
            // do nothing
        }
    }

    public void showCircle() {
        if (mFading || mTouchedOutside || !mCanUnlockWithFp) return;
        mIsCircleShowing = true;

        setKeepScreenOn(true);

        setDim(true);
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchPress();
        });

        if (mFODAnimation != null && mIsRecognizingAnimEnabled) {
            mHandler.post(() -> mFODAnimation.showFODAnimation());
        }

        setImageDrawable(null);
        updatePosition();
        invalidate();
    }

    public void hideCircle() {
        mIsCircleShowing = false;

        setFODIcon();
        invalidate();

        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchRelease();
        });
        setDim(false);

        if (mFODAnimation != null && mIsRecognizingAnimEnabled) {
            mHandler.post(() -> mFODAnimation.hideFODAnimation());
        }

        setKeepScreenOn(false);
    }

    private void resetFODIcon(boolean show) {
        if (show) {
            setFODIcon();
        } else {
            this.setImageResource(0);
        }
    }

    private void setFODIcon() {
        if (mIsDreaming && mShouldRemoveIconOnAOD) {
            return;
        }

        this.setImageResource(R.drawable.fod_icon_default);
    }

    public void show() {
        if (!mUpdateMonitor.isScreenOn()) {
            // Keyguard is shown just after screen turning off
            return;
        }

        if (mIsBouncer && !isPinOrPattern(mUpdateMonitor.getCurrentUser())) {
            // Ignore show calls when Keyguard password screen is being shown
            return;
        }

        if (mIsKeyguard && mUpdateMonitor.getUserCanSkipBouncer(mUpdateMonitor.getCurrentUser())) {
            // Ignore show calls if user can skip bouncer
            return;
        }

        if (mIsAssistantVisible) {
            // Don't show when assistant UI is visible
            return;
        }

        updatePosition();
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();

        setVisibility(View.VISIBLE);
        animate().withStartAction(() -> mFading = true)
                .alpha(getFODAlpha())
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> mFading = false)
                .start();
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchShow();
        });
    }

    public void hide() {
        animate().withStartAction(() -> mFading = true)
                .alpha(0)
                .setDuration(FADE_ANIM_DURATION)
                .withEndAction(() -> {
                    setVisibility(View.GONE);
                    mFading = false;
                })
                .start();
        mCustomSettingsObserver.unobserve();
        hideCircle();
        ThreadUtils.postOnBackgroundThread(() -> {
            dispatchHide();
        });
    }

    private float getFODAlpha() {
        canUnlockWithFp();
        float alpha = (mIsDreaming ? 0.5f : 1f) *
                (mCanUnlockWithFp ? 1f : 0.4f);
        return alpha;
    }

    private void canUnlockWithFp() {
        boolean biometrics = mUpdateMonitor.isUnlockWithFingerprintPossible();
        KeyguardUpdateMonitor.StrongAuthTracker strongAuthTracker =
                mUpdateMonitor.getStrongAuthTracker();
        mCanUnlockWithFp = (biometrics && strongAuthTracker.isUnlockingWithBiometricAllowed(true)
                || !biometrics) && !mFpDisabled;
        mFODAnimation.setCanUnlock(mCanUnlockWithFp);
    }

    @SuppressWarnings("SuspiciousNameCombination")
    private void updatePosition() {
        Display defaultDisplay = mWindowManager.getDefaultDisplay();

        Point size = new Point();
        defaultDisplay.getRealSize(size);

        int rotation = defaultDisplay.getRotation();
        int cutoutMaskedExtra = mCutoutMasked ? mStatusbarHeight : 0;
        int x, y;
        switch (rotation) {
            case Surface.ROTATION_0:
                x = mPositionX;
                y = mPositionY - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_90:
                x = mPositionY;
                y = mPositionX - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_180:
                x = mPositionX;
                y = size.y - mPositionY - mSize - cutoutMaskedExtra;
                break;
            case Surface.ROTATION_270:
                x = size.x - mPositionY - mSize - mNavigationBarSize - cutoutMaskedExtra;
                y = mPositionX;
                break;
            default:
                throw new IllegalArgumentException("Unknown rotation: " + rotation);
        }

        mPressedParams.x = mParams.x = x;
        mPressedParams.y = mParams.y = y;

        if (mIsDreaming && !mIsCircleShowing) {
            mParams.y += mDreamingOffsetY;
        }

        mWindowManager.updateViewLayout(this, mParams);

        if (mPressedView.getParent() != null) {
            mWindowManager.updateViewLayout(mPressedView, mPressedParams);
        }
    }

    private void setDim(boolean dim) {
        if (dim) {
            int curBrightness = Settings.System.getInt(getContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, 100);
            int dimAmount = 0;

            IFingerprintInscreen daemon = getFingerprintInScreenDaemon();
            try {
                dimAmount = daemon.getDimAmount(curBrightness);
            } catch (RemoteException e) {
                // do nothing
            }

            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 1.0f;
            }

            mPressedParams.dimAmount = dimAmount / 255.0f;
            if (mPressedView.getParent() == null) {
                mWindowManager.addView(mPressedView, mPressedParams);
            } else {
                mWindowManager.updateViewLayout(mPressedView, mPressedParams);
            }
        } else {
            if (mShouldBoostBrightness) {
                mPressedParams.screenBrightness = 0.0f;
            }
            mPressedParams.dimAmount = 0.0f;
            if (mPressedView.getParent() != null) {
                mWindowManager.removeView(mPressedView);
            }
        }
    }

    private boolean isPinOrPattern(int userId) {
        int passwordQuality = mLockPatternUtils.getActivePasswordQuality(userId);
        switch (passwordQuality) {
            // PIN
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            // Pattern
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return true;
        }

        return false;
    }

    private void updateSettings() {
        mIsRecognizingAnimEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.FOD_RECOGNIZING_ANIMATION, 1) != 0;
        if (mFODAnimation != null) {
            mFODAnimation.update(mIsRecognizingAnimEnabled);
        }
    }

    private class BurnInProtectionTask extends TimerTask {
        @Override
        public void run() {
            long now = System.currentTimeMillis() / 1000 / 60;
            // Let y to be not synchronized with x, so that we get maximum movement
            mDreamingOffsetY = (int) ((now + mDreamingMaxOffset / 3) % (mDreamingMaxOffset * 2));
            mDreamingOffsetY -= mDreamingMaxOffset;

            mHandler.post(() -> updatePosition());
        }
    };

    @Override
    public void onOverlayChanged() {
        updateCutoutFlags();
    }

    private void updateCutoutFlags() {
        mStatusbarHeight = getContext().getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height_portrait);
        boolean cutoutMasked = getContext().getResources().getBoolean(
                com.android.internal.R.bool.config_maskMainBuiltInDisplayCutout);
        if (mCutoutMasked != cutoutMasked){
            mCutoutMasked = cutoutMasked;
            updatePosition();
        }
    }

    public void triggerFodIconAnimation() {
        AnimatedVectorDrawable fodIconAnimatedVectorDrawable =
                (AnimatedVectorDrawable) mContext.getDrawable(R.drawable.fod_icon_default_animated);
        setImageDrawable(fodIconAnimatedVectorDrawable);
        fodIconAnimatedVectorDrawable.start();
    }
}
