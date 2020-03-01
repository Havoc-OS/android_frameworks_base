/*
 * Copyright (C) 2020 ExtendedUI
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

package com.android.systemui.statusbar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.View;

public class AODDimView extends View {

    private static int ANIMATION_DURATION = 200;
    private boolean mIsEnabled;

    public AODDimView(Context context) {
        this(context, null);
        init();
    }

    public AODDimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public AODDimView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
        init();
    }

    public AODDimView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        this.setBackgroundColor(Color.parseColor("#000000"));
        this.setVisibility(View.GONE);
    }

    public void setEnabled(boolean enabled) {
        mIsEnabled = enabled;
    }

    public void setVisible(boolean visible) {
        setVisible(visible, false);
    }

    public void setVisible(boolean visible, boolean animate) {
        if (!mIsEnabled) {
            return;
        }

        if (visible && animate) {
            AlphaAnimation AODDimAnimation = new AlphaAnimation(0.0f, 1.0f);
            AODDimAnimation.setDuration(ANIMATION_DURATION);
            AODDimAnimation.setFillAfter(false);
            this.startAnimation(AODDimAnimation);
            this.setVisibility(View.VISIBLE);
        } else if (visible) {
            this.setVisibility(View.VISIBLE);
        }

        if (!visible && animate) {
            AlphaAnimation AODDimAnimation = new AlphaAnimation(1.0f, 0.0f);
            AODDimAnimation.setDuration(ANIMATION_DURATION);
            AODDimAnimation.setFillAfter(false);
            this.startAnimation(AODDimAnimation);
            AODDimAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation arg0) {
                }
                @Override
                public void onAnimationRepeat(Animation arg0) {
                }
                @Override
                public void onAnimationEnd(Animation arg0) {
                    setHidden();
                }
            });
        } else if (!visible && !animate) {
            this.setVisibility(View.GONE);
        }
    }

    private void setHidden() {
        this.setVisibility(View.GONE);
    }
}
