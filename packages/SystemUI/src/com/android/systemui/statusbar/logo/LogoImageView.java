/*
 * Copyright (C) 2021 Havoc-OS
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

package com.android.systemui.statusbar.logo;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;

public class LogoImageView extends ImageView {

    private final Context mContext;

    protected boolean mLogoEnabled;
    protected int mLogoPosition;
    private int mLogoStyle;
    private boolean mAttached;
    private final ContentResolver mContentResolver;

    private class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_LOGO), false, this);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_LOGO_POSITION), false, this);
            mContentResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STATUS_BAR_LOGO_STYLE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }

    public LogoImageView(Context context) {
        this(context, null);
    }

    public LogoImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LogoImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        mContentResolver = context.getContentResolver();
        Handler mHandler = new Handler();
        SettingsObserver mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();
        updateSettings();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mAttached) {
            return;
        }
        mAttached = true;
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
        updateSettings();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (!mAttached) {
            return;
        }
        mAttached = false;
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
    }

    private Drawable getLogoDrawable() {
        Drawable drawable;
        switch (mLogoStyle) {
            default:
            case 0:
                drawable = mContext.getDrawable(R.drawable.ic_havoc_logo);
                break;
            case 1:
                drawable = mContext.getDrawable(R.drawable.ic_android_logo);
                break;
            case 2:
                drawable = mContext.getDrawable(R.drawable.ic_apple_logo);
                break;
            case 3:
                drawable = mContext.getDrawable(R.drawable.ic_beats);
                break;
            case 4:
                drawable = mContext.getDrawable(R.drawable.ic_biohazard);
                break;
            case 5:
                drawable = mContext.getDrawable(R.drawable.ic_blackberry);
                break;
            case 6:
                drawable = mContext.getDrawable(R.drawable.ic_blogger);
                break;
            case 7:
                drawable = mContext.getDrawable(R.drawable.ic_bomb);
                break;
            case 8:
                drawable = mContext.getDrawable(R.drawable.ic_brain);
                break;
            case 9:
                drawable = mContext.getDrawable(R.drawable.ic_cake);
                break;
            case 10:
                drawable = mContext.getDrawable(R.drawable.ic_cannabis);
                break;
            case 11:
                drawable = mContext.getDrawable(R.drawable.ic_death_star);
                break;
            case 12:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon);
                break;
            case 13:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_cool);
                break;
            case 14:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_dead);
                break;
            case 15:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_devil);
                break;
            case 16:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_happy);
                break;
            case 17:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_neutral);
                break;
            case 18:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_poop);
                break;
            case 19:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_sad);
                break;
            case 20:
                drawable = mContext.getDrawable(R.drawable.ic_emoticon_tongue);
                break;
            case 21:
                drawable = mContext.getDrawable(R.drawable.ic_fire);
                break;
            case 22:
                drawable = mContext.getDrawable(R.drawable.ic_flask);
                break;
            case 23:
                drawable = mContext.getDrawable(R.drawable.ic_gender_female);
                break;
            case 24:
                drawable = mContext.getDrawable(R.drawable.ic_gender_male);
                break;
            case 25:
                drawable = mContext.getDrawable(R.drawable.ic_gender_male_female);
                break;
            case 26:
                drawable = mContext.getDrawable(R.drawable.ic_ghost);
                break;
            case 27:
                drawable = mContext.getDrawable(R.drawable.ic_google);
                break;
            case 28:
                drawable = mContext.getDrawable(R.drawable.ic_guitar_acoustic);
                break;
            case 29:
                drawable = mContext.getDrawable(R.drawable.ic_guitar_electric);
                break;
            case 30:
                drawable = mContext.getDrawable(R.drawable.ic_heart);
                break;
            case 31:
                drawable = mContext.getDrawable(R.drawable.ic_human_female);
                break;
            case 32:
                drawable = mContext.getDrawable(R.drawable.ic_human_male);
                break;
            case 33:
                drawable = mContext.getDrawable(R.drawable.ic_human_male_female);
                break;
            case 34:
                drawable = mContext.getDrawable(R.drawable.ic_incognito);
                break;
            case 35:
                drawable = mContext.getDrawable(R.drawable.ic_ios_logo);
                break;
            case 36:
                drawable = mContext.getDrawable(R.drawable.ic_linux);
                break;
            case 37:
                drawable = mContext.getDrawable(R.drawable.ic_lock);
                break;
            case 38:
                drawable = mContext.getDrawable(R.drawable.ic_music);
                break;
            case 39:
                drawable = mContext.getDrawable(R.drawable.ic_ninja);
                break;
            case 40:
                drawable = mContext.getDrawable(R.drawable.ic_pac_man);
                break;
            case 41:
                drawable = mContext.getDrawable(R.drawable.ic_peace);
                break;
            case 42:
                drawable = mContext.getDrawable(R.drawable.ic_robot);
                break;
            case 43:
                drawable = mContext.getDrawable(R.drawable.ic_skull);
                break;
            case 44:
                drawable = mContext.getDrawable(R.drawable.ic_smoking);
                break;
            case 45:
                drawable = mContext.getDrawable(R.drawable.ic_wallet);
                break;
            case 46:
                drawable = mContext.getDrawable(R.drawable.ic_windows);
                break;
            case 47:
                drawable = mContext.getDrawable(R.drawable.ic_xbox);
                break;
            case 48:
                drawable = mContext.getDrawable(R.drawable.ic_xbox_controller);
                break;
            case 49:
                drawable = mContext.getDrawable(R.drawable.ic_yin_yang);
                break;
        }
        return drawable;
    }

    public void updateSettings() {
        mLogoEnabled = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_LOGO, 1) == 1;
        mLogoPosition = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_LOGO_POSITION, 0);
        mLogoStyle = Settings.System.getInt(mContentResolver,
                Settings.System.STATUS_BAR_LOGO_STYLE, 0);
        updateVisibility();
    }

    public void updateVisibility() {
        if (isEnabled()) {
            setVisibility(View.VISIBLE);
            setImageDrawable(getLogoDrawable());
        } else {
            setImageDrawable(null);
            setVisibility(View.GONE);
        }
    }

    public boolean isEnabled() {
        return mLogoEnabled && mLogoPosition == 0;
    }
}
