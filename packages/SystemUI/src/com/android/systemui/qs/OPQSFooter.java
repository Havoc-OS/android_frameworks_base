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

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;

import static com.android.systemui.util.InjectionInflationController.VIEW_CONTEXT;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.keyguard.CarrierText;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.TouchAnimator.Builder;
import com.android.systemui.statusbar.DataUsageView;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.statusbar.policy.UserInfoController.OnUserInfoChangedListener;
import com.android.systemui.tuner.TunerService;

import javax.inject.Inject;
import javax.inject.Named;

public class OPQSFooter extends LinearLayout {

    private Context mContext;
    private LinearLayout mMediaSpacer;
    private View mSettingsContainer;
    private SettingsButton mSettingsButton;
    private View mBrightnessView;
    private ImageView mBrightnessButton;
    private View mRunningServicesButton;
    protected View mEdit;
    protected TouchAnimator mFooterAnimator;
    protected TouchAnimator mCarrierTextAnimator;
    private Boolean mExpanded;
    private Boolean mIsLandscape = false;
    private FrameLayout mFooterActions;
    private DataUsageView mDataUsageView;
    private CarrierText mCarrierText;
    private boolean mIsQQSPanel = false;

    public OPQSFooter(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mBrightnessView = findViewById(R.id.brightness_view);
        mBrightnessButton = findViewById(R.id.auto_brightness_button);
        mMediaSpacer = findViewById(R.id.qs_footer_media_spacer);
        mEdit = findViewById(R.id.edit);
        mRunningServicesButton = findViewById(R.id.running_services_button);
        mSettingsButton = findViewById(R.id.settings_button);
        mSettingsContainer = findViewById(R.id.settings_button_container);
        mFooterActions = findViewById(R.id.op_qs_footer_actions);
        mCarrierText = findViewById(R.id.qs_carrier_text);
        mDataUsageView = findViewById(R.id.data_usage_view);
        mDataUsageView.setVisibility(View.GONE);
        mIsLandscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        mFooterAnimator = createFooterAnimator();
        mCarrierTextAnimator = createCarrierTextAnimator();
    }

    public void setExpansion(float headerExpansionFraction) {
        if (mFooterAnimator != null) {
            mFooterAnimator.setPosition(headerExpansionFraction);
        }
        if (mCarrierTextAnimator != null) {
            mCarrierTextAnimator.setPosition(headerExpansionFraction);
        }
    }

    public void setExpanded(boolean expanded) {
        mExpanded = expanded;
        if (mDataUsageView != null) {
            mDataUsageView.setVisibility(mExpanded ? View.VISIBLE : View.GONE);
            if (mExpanded) {
                mDataUsageView.updateUsage();
                mDataUsageView.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                    startDataUsageActivity();
                    }
                });
            }
        }
        if (mSettingsButton != null) {
            int visibility = isSettingsEnabled() ? View.VISIBLE : View.GONE;
            mSettingsButton.setVisibility(visibility);
        }
        if (mSettingsContainer != null) {
            int visibility = isSettingsEnabled() ? View.VISIBLE : View.GONE;
            mSettingsContainer.setVisibility(visibility);
        }
        if (mBrightnessButton != null) {
            int visibility = (mExpanded && isBrightnessEnabled()) ? View.VISIBLE : View.GONE;
            mBrightnessButton.setVisibility(visibility);
        }
        if (mEdit != null) {
            int visibility = (mExpanded && isEditEnabled()) ? View.VISIBLE : View.GONE;
            mEdit.setVisibility(visibility);
        }
        if (mRunningServicesButton != null) {
            int visibility = (mExpanded && isServicesEnabled()) ? View.VISIBLE : View.GONE;
            mRunningServicesButton.setVisibility(visibility);
        }
        mIsQQSPanel = !mExpanded;
        setOrientation(mIsLandscape);
    }

    public void setIsQQSPanel() {
        mIsQQSPanel = true;
        mBrightnessButton.setVisibility(View.GONE);
        mRunningServicesButton.setVisibility(View.GONE);
        mEdit.setVisibility(View.GONE);
        mDataUsageView.setVisibility(View.GONE);
        setOrientation(mIsLandscape);
    }

    @Nullable
    private TouchAnimator createFooterAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mBrightnessButton, "alpha", 0, 0, 1)
                .addFloat(mRunningServicesButton, "alpha", 0, 0, 1)
                .addFloat(mEdit, "alpha", 0, 0, 1);
        if (mIsLandscape) {
            builder = builder.addFloat(mBrightnessView, "alpha", 0, 0, 1)
                    .setStartDelay(0.5f);
            builder = builder.addFloat(mSettingsButton, "alpha", 0, 0, 1)
                    .setStartDelay(0.5f);
        }
        return builder.build();
    }

    @Nullable
    private TouchAnimator createCarrierTextAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mDataUsageView, "alpha", 0, 0, 1);
        if (mIsLandscape) {
            builder = builder.addFloat(mCarrierText, "alpha", 0, 0, 0)
                    .setStartDelay(0.5f);
        } else {
            builder = builder.addFloat(mCarrierText, "alpha", 1, 0, 0);
        }
        return builder.build();
    }

    public View getSettingsContainer() {
        return mSettingsContainer;
    }

    public View getSettingsButton() {
        return mSettingsButton;
    }

    public ImageView getBrightnessButton() {
        return mBrightnessButton;
    }

    public View getEditButton() {
        return mEdit;
    }

    public View getServicesButton() {
        return mRunningServicesButton;
    }

    public View getFooterActions() {
        return mFooterActions;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setOrientation(newConfig.orientation
                == Configuration.ORIENTATION_LANDSCAPE);
    }

    private void setOrientation(boolean isLandscape) {
        if (mIsLandscape != isLandscape) {
            mIsLandscape = isLandscape;
            mBrightnessView.setAlpha(1.0f);
            mSettingsButton.setAlpha(1.0f);
            mFooterAnimator = createFooterAnimator();
            mCarrierTextAnimator = createCarrierTextAnimator();
        }
        if (mIsLandscape && mIsQQSPanel) {
            mMediaSpacer.setVisibility(View.VISIBLE);
            mBrightnessView.setVisibility(View.GONE);
            mFooterActions.setVisibility(View.GONE);
        } else {
            mMediaSpacer.setVisibility(View.GONE);
            mBrightnessView.setVisibility(View.VISIBLE);
            mFooterActions.setVisibility(View.VISIBLE);
        }
    }

    public boolean isSettingsEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_SHOW_SETTINGS, 1) == 1;
    }

    public boolean isBrightnessEnabled() {
        boolean isAvailable = (getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available) &&
                Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_SHOW_BRIGHTNESS_ICON, 1) == 1);
        return isAvailable;
    }

    public boolean isServicesEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_SHOW_SERVICES, 0) == 1;
    }

    public boolean isEditEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QS_FOOTER_SHOW_EDIT, 1) == 1;
    }

    private void startDataUsageActivity() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",
                "com.android.settings.Settings$DataUsageSummaryActivity");
        Dependency.get(ActivityStarter.class).startActivity(intent, true /* dismissShade */);
    }
}
