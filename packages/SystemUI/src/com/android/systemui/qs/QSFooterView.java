/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.systemui.qs;

import static android.app.StatusBarManager.DISABLE2_QUICK_SETTINGS;

import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.BidiFormatter;
import android.text.format.Formatter;
import android.text.format.Formatter.BytesResult;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.settingslib.development.DevelopmentSettingsEnabler;
import com.android.settingslib.net.DataUsageController;
import com.android.systemui.R;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Footer of expanded Quick Settings, tiles page indicator, (optionally) build number and
 * {@link FooterActionsView}
 */
public class QSFooterView extends FrameLayout {
    private PageIndicator mPageIndicator;
    private TextView mUsageText;
    private View mEditButton;

    @Nullable
    protected TouchAnimator mFooterAnimator;

    private boolean mQsDisabled;
    private boolean mExpanded;
    private float mExpansionAmount;

    @Nullable
    private OnClickListener mExpandClickListener;

    private DataUsageController mDataController;
    private ConnectivityManager mConnectivityManager;
    private WifiManager mWifiManager;
    private SubscriptionManager mSubManager;
    private TelephonyManager telephonyManager;

    public QSFooterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDataController = new DataUsageController(context);
        mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mSubManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPageIndicator = findViewById(R.id.footer_page_indicator);
        mUsageText = findViewById(R.id.build);
        mEditButton = findViewById(android.R.id.edit);

        updateResources();
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setUsageText(false);
    }

    private void setUsageText(boolean expanded) {
        if (!isExpanded(expanded)) return;
        if (!isWifiConnected()) {
            mDataController.setSubscriptionId(SubscriptionManager.getDefaultDataSubscriptionId());
        }

        DataUsageController.DataUsageInfo info = isWifiConnected() ? mDataController.getWifiDailyDataUsageInfo() : mDataController.getDailyDataUsageInfo();
        boolean showData = info != null && info.usageLevel >= 0 && (isWifiConnected() || isMobileDataEnabled());
        if (showData) {
            String suffix = mContext.getResources().getString(isWifiConnected() ? R.string.usage_wifi_default_suffix : R.string.usage_data_default_suffix);
            String dataUsage = formatDataUsage(info.usageLevel) + " " + mContext.getResources().getString(R.string.usage_data) + " (" + suffix + ")";
            mUsageText.setText(dataUsage);
        } else {
            mUsageText.setVisibility(View.INVISIBLE);
        }
    }

    private boolean isExpanded(boolean expanded) {
        if (mUsageText != null) {
            mUsageText.setVisibility(expanded ? View.VISIBLE : View.INVISIBLE);
        }
        return expanded;
    }

    private boolean isMobileDataEnabled() {
        try {
            Method method = telephonyManager.getClass().getDeclaredMethod("getDataEnabled");
            return (boolean) method.invoke(telephonyManager);
        } catch (Exception e) {}
        return false;
    }

    private CharSequence formatDataUsage(long byteValue) {
        final BytesResult res = Formatter.formatBytes(mContext.getResources(), byteValue,
                Formatter.FLAG_IEC_UNITS);
        return BidiFormatter.getInstance().unicodeWrap(mContext.getString(
                com.android.internal.R.string.fileSizeSuffix, res.value, res.units));
    }

    private boolean isWifiConnected() {
        final Network network = mConnectivityManager.getActiveNetwork();
        if (network != null) {
            NetworkCapabilities capabilities = mConnectivityManager.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        } else {
            return false;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
        MarginLayoutParams lp = (MarginLayoutParams) getLayoutParams();
        lp.bottomMargin = getResources().getDimensionPixelSize(R.dimen.qs_footers_margin_bottom);
        setLayoutParams(lp);
    }

    private void updateFooterAnimator() {
        mFooterAnimator = createFooterAnimator();
    }

    @Nullable
    private TouchAnimator createFooterAnimator() {
        TouchAnimator.Builder builder = new TouchAnimator.Builder()
                .addFloat(mPageIndicator, "alpha", 0, 1)
                .addFloat(mUsageText, "alpha", 0, 1)
                .addFloat(mEditButton, "alpha", 0, 1)
                .setStartDelay(0.9f);
        return builder.build();
    }

    /** */
    public void setKeyguardShowing() {
        setExpansion(mExpansionAmount);
    }

    public void setExpandClickListener(OnClickListener onClickListener) {
        mExpandClickListener = onClickListener;
    }

    void setExpanded(boolean expanded) {
        if (mExpanded == expanded) return;
        mExpanded = expanded;
        updateEverything();
    }

    /** */
    public void setExpansion(float headerExpansionFraction) {
        mExpansionAmount = headerExpansionFraction;
        if (mFooterAnimator != null) {
            mFooterAnimator.setPosition(headerExpansionFraction);
        }

        if (mUsageText == null) return;
        if (headerExpansionFraction == 1.0f) {
            mUsageText.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mUsageText.setSelected(true);
                }
            }, 1000);
        } else {
            mUsageText.setSelected(false);
        }
    }

    void disable(int state2) {
        final boolean disabled = (state2 & DISABLE2_QUICK_SETTINGS) != 0;
        if (disabled == mQsDisabled) return;
        mQsDisabled = disabled;
        updateEverything();
    }

    void updateEverything() {
        post(() -> {
            setUsageText(mExpanded);
            setClickable(false);
        });
    }
}
