/**
 * Copyright (C) 2019 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;
import com.android.systemui.R;

public class FODAnimation extends ImageView {

    private final WindowManager.LayoutParams mAnimParams = new WindowManager.LayoutParams();

    private boolean mShowing = false;
    private Context mContext;
    private int mAnimationSize;
    private int mAnimationOffset;
    private int mAnimationPositionY;
    private AnimationDrawable recognizingAnim;
    private WindowManager mWindowManager;
    private boolean mIsDreaming;
    private boolean mIsPulsing;
    private boolean mIsKeyguard;

    private KeyguardUpdateMonitor mUpdateMonitor;

    private KeyguardUpdateMonitorCallback mMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onDreamingStateChanged(boolean dreaming) {
            mIsDreaming = dreaming;
        }

        @Override
        public void onPulsing(boolean pulsing) {
            super.onPulsing(pulsing);
            mIsPulsing = pulsing;
	        if (mIsPulsing) {
                mIsDreaming = false;
            }
	    }
    };

    public FODAnimation(Context context, int mPositionX, int mPositionY) {
        super(context);

        mContext = context;
        mWindowManager = mContext.getSystemService(WindowManager.class);

        mUpdateMonitor = KeyguardUpdateMonitor.getInstance(context);
        mUpdateMonitor.registerCallback(mMonitorCallback);

        mAnimationSize = mContext.getResources().getDimensionPixelSize(R.dimen.fod_animation_size);
        mAnimationOffset = mContext.getResources().getDimensionPixelSize(R.dimen.fod_animation_offset);
        mAnimParams.height = mAnimationSize;
        mAnimParams.width = mAnimationSize;

        mAnimParams.format = PixelFormat.TRANSLUCENT;
        mAnimParams.type = WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY; // it must be behind FOD icon
        mAnimParams.flags =  WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mAnimParams.gravity = Gravity.TOP | Gravity.CENTER;
        mAnimParams.y = mPositionY - (mAnimationSize / 2) + mAnimationOffset;

        setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        setBackgroundResource(R.drawable.fod_default_touch_animation);
        recognizingAnim = (AnimationDrawable) getBackground();
    }

    public void updateParams(int positionY) {
        mAnimParams.y = positionY - (mAnimationSize / 2) + mAnimationOffset;
    }

    public void setAnimationKeyguard(boolean state) {
        mIsKeyguard = state;
    }

    public void showFODanimation() {
        if (mAnimParams != null && !mShowing && mIsKeyguard && !mIsDreaming) {
            mShowing = true;
            if (this.getWindowToken() == null) {
                mWindowManager.addView(this, mAnimParams);
                mWindowManager.updateViewLayout(this, mAnimParams);
            }
            recognizingAnim.start();
        }
    }

    public void hideFODanimation() {
        if (mShowing) {
            mShowing = false;

            if (recognizingAnim != null) {
                clearAnimation();
                recognizingAnim.stop();
                recognizingAnim.selectDrawable(0);
            }

            if (this.getWindowToken() != null) {
                mWindowManager.removeView(this);
            }
        }
    }
}
