/*
 * Copyright (C) 2018 Havoc-OS
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

package com.android.systemui.havoc.logo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.Dependency;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.tuner.TunerService;

public class LogoImageViewRight extends ImageView implements
        TunerService.Tunable {

    private Context mContext;

    private boolean mAttached;
    private boolean mHavocLogo;
    private int mHavocLogoColor;
    private int mHavocLogoPosition;
    private int mHavocLogoStyle;
    private int mTintColor = Color.WHITE;

    private static final String STATUS_BAR_HAVOC_LOGO =
            "system:" + Settings.System.STATUS_BAR_HAVOC_LOGO;
    private static final String STATUS_BAR_HAVOC_LOGO_COLOR =
            "system:" + Settings.System.STATUS_BAR_HAVOC_LOGO_COLOR;
    private static final String STATUS_BAR_HAVOC_LOGO_POSITION =
            "system:" + Settings.System.STATUS_BAR_HAVOC_LOGO_POSITION;
    private static final String STATUS_BAR_HAVOC_LOGO_STYLE =
            "system:" + Settings.System.STATUS_BAR_HAVOC_LOGO_STYLE;

    public LogoImageViewRight(Context context) {
        this(context, null);
    }

    public LogoImageViewRight(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoImageViewRight(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final Resources resources = getResources();
        mContext = context;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached)
            return;

        mAttached = true;

        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);

        Dependency.get(TunerService.class).addTunable(this,
                STATUS_BAR_HAVOC_LOGO,
                STATUS_BAR_HAVOC_LOGO_COLOR,
                STATUS_BAR_HAVOC_LOGO_POSITION,
                STATUS_BAR_HAVOC_LOGO_STYLE);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached)
            return;

        mAttached = false;
        Dependency.get(TunerService.class).removeTunable(this);
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mTintColor = DarkIconDispatcher.getTint(area, this, tint);
        if (mHavocLogo && mHavocLogoPosition == 1 && 
                mHavocLogoColor == 0xFFFFFFFF) {
            updateHavocLogo();
        }
    }

    public void updateHavocLogo() {
        Drawable drawable = null;

        if (!mHavocLogo || mHavocLogoPosition == 0) {
            setImageDrawable(null);
            setVisibility(View.GONE);
            return;
        } else {
            setVisibility(View.VISIBLE);
        }

        if (mHavocLogoStyle == 0) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_havoc_logo);
        } else if (mHavocLogoStyle == 1) {
           drawable = mContext.getResources().getDrawable(R.drawable.ic_android_logo);
        } else if (mHavocLogoStyle == 2) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_apple_logo);
        } else if (mHavocLogoStyle == 3) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_ios_logo);
        } else if (mHavocLogoStyle == 4) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon);
        } else if (mHavocLogoStyle == 5) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_cool);
        } else if (mHavocLogoStyle == 6) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_dead);
        } else if (mHavocLogoStyle == 7) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_devil);
        } else if (mHavocLogoStyle == 8) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_happy);
        } else if (mHavocLogoStyle == 9) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_neutral);
        } else if (mHavocLogoStyle == 10) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_poop);
        } else if (mHavocLogoStyle == 11) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_sad);
        } else if (mHavocLogoStyle == 12) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_emoticon_tongue);
        } else if (mHavocLogoStyle == 13) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_blackberry);
        } else if (mHavocLogoStyle == 14) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_cake);
        } else if (mHavocLogoStyle == 15) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_blogger);
        } else if (mHavocLogoStyle == 16) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_biohazard);
        } else if (mHavocLogoStyle == 17) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_linux);
        } else if (mHavocLogoStyle == 18) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_yin_yang);
        } else if (mHavocLogoStyle == 19) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_windows);
        } else if (mHavocLogoStyle == 20) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_robot);
        } else if (mHavocLogoStyle == 21) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_ninja);
        } else if (mHavocLogoStyle == 22) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_heart);
        } else if (mHavocLogoStyle == 23) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_flower);
        } else if (mHavocLogoStyle == 24) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_ghost);
        } else if (mHavocLogoStyle == 25) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_google);
        } else if (mHavocLogoStyle == 26) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_human_male);
        } else if (mHavocLogoStyle == 27) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_human_female);
        } else if (mHavocLogoStyle == 28) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_human_male_female);
        } else if (mHavocLogoStyle == 29) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_gender_male);
        } else if (mHavocLogoStyle == 30) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_gender_female);
        } else if (mHavocLogoStyle == 31) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_gender_male_female);
        } else if (mHavocLogoStyle == 32) {
            drawable = mContext.getResources().getDrawable(R.drawable.ic_guitar_electric);
        } else if (mHavocLogoStyle == 33) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_batman); 
        } else if (mHavocLogoStyle == 34) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_deadpool); 
        } else if (mHavocLogoStyle == 35) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_decepticons); 
        } else if (mHavocLogoStyle == 36) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_ironman); 
        } else if (mHavocLogoStyle == 37) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_minions); 
        } else if (mHavocLogoStyle == 38) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_spiderman); 
        } else if (mHavocLogoStyle == 39) { 
            drawable = mContext.getResources().getDrawable(R.drawable.ic_superman); 
        } 

        setImageDrawable(null);

        clearColorFilter();

        if (mHavocLogoColor == 0xFFFFFFFF) {
            drawable.setTint(mTintColor);
        } else {
            setColorFilter(mHavocLogoColor, PorterDuff.Mode.SRC_IN);
        }
        setImageDrawable(drawable);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        switch (key) {
            case STATUS_BAR_HAVOC_LOGO:
                mHavocLogo = newValue != null && Integer.parseInt(newValue) == 1;
                break;
            case STATUS_BAR_HAVOC_LOGO_COLOR:
                mHavocLogoColor =
                        newValue == null ? 0xFFFFFFFF : Integer.parseInt(newValue);
                break;
            case STATUS_BAR_HAVOC_LOGO_POSITION:
                mHavocLogoPosition =
                        newValue == null ? 0 : Integer.parseInt(newValue);
                break;
            case STATUS_BAR_HAVOC_LOGO_STYLE:
                mHavocLogoStyle =
                        newValue == null ? 0 : Integer.parseInt(newValue);
                break;
            default:
                break;
        }
        updateHavocLogo();
    }
}
