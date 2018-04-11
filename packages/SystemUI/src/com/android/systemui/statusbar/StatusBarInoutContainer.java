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

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.settingslib.Utils;
import com.android.systemui.R;

public class StatusBarInoutContainer extends FrameLayout {
    private static final String TAG = "StatusBarInoutContainer";

    private ImageView mIn;
    private ImageView mOut;
    private boolean mActivityIn;
    private boolean mActivityOut;
    private float mDarkIntensity = 0;

    private ContextThemeWrapper mDarkContext;
    private ContextThemeWrapper mLightContext;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;
    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

    public StatusBarInoutContainer(Context context) {
        this(context, null);
    }

    public StatusBarInoutContainer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StatusBarInoutContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public StatusBarInoutContainer(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        int dualToneLightTheme = Utils.getThemeAttr(mContext, R.attr.lightIconTheme);
        int dualToneDarkTheme = Utils.getThemeAttr(mContext, R.attr.darkIconTheme);
        mLightContext = new ContextThemeWrapper(mContext, dualToneLightTheme);
        mDarkContext = new ContextThemeWrapper(mContext, dualToneDarkTheme);

        mDarkModeBackgroundColor = Utils.getColorAttrDefaultColor(mDarkContext, R.attr.backgroundColor);
        mDarkModeFillColor = Utils.getColorAttrDefaultColor(mDarkContext, R.attr.fillColor);
        mLightModeBackgroundColor = Utils.getColorAttrDefaultColor(mLightContext, R.attr.backgroundColor);
        mLightModeFillColor = Utils.getColorAttrDefaultColor(mLightContext, R.attr.fillColor);
    }

    @Override
    public void onFinishInflate() {
        mIn = findViewById(R.id.activity_in);
        mOut = findViewById(R.id.activity_out);
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (changedView == this) {
            updateColors();
        }
    }

    public void setState(boolean activityIn, boolean activityOut) {
        if (activityIn == mActivityIn && activityOut == mActivityOut) {
            return;
        }
        mActivityIn = activityIn;
        mActivityOut = activityOut;

        updateColors();
    }

    public void setDarkIntensity(float darkIntensity) {
        if (darkIntensity == mDarkIntensity) {
            return;
        }
        mDarkIntensity = darkIntensity;

        updateColors();
    }

    private void updateColors() {
        if (getVisibility() != View.VISIBLE) {
            return;
        }

        mIn.setImageTintList(ColorStateList.valueOf(getTintColor(mActivityIn)));
        mOut.setImageTintList(ColorStateList.valueOf(getTintColor(mActivityOut)));
    }

    private int getTintColor(boolean isActive) {
        if (isActive) {
            return getFillColor(mDarkIntensity);
        } else {
            return getBackgroundColor(mDarkIntensity);
        }
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeBackgroundColor, mDarkModeBackgroundColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }
}
