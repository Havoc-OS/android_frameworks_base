/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.keyguard;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContentResolver;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.text.Html;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.widget.CustomAnalogClock;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.support.v4.graphics.ColorUtils;
import android.widget.RelativeLayout;
import android.database.ContentObserver;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import com.android.internal.util.ArrayUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.ChargingView;
import com.android.systemui.havoc.omnijaws.OmniJawsClient;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.policy.DateView;

import java.util.Locale;

public class KeyguardStatusView extends GridLayout implements
        OmniJawsClient.OmniJawsObserver {
    private static final boolean DEBUG = KeyguardConstants.DEBUG;
    private static final String TAG = "KeyguardStatusView";
    private static final int MARQUEE_DELAY_MS = 2000;
    private static final String FONT_FAMILY_LIGHT = "sans-serif-light";
    private static final String FONT_FAMILY_MEDIUM = "sans-serif-medium";

    private final LockPatternUtils mLockPatternUtils;
    private final AlarmManager mAlarmManager;

    private TextView mAlarmStatusView;
    private DateView mDateView;
    private TextClock mClockView;
    private TextView mOwnerInfo;
    private ViewGroup mClockContainer;
    // private ChargingView mBatteryDoze;
    private View mKeyguardStatusArea;
    private Runnable mPendingMarqueeStart;
    private Handler mHandler;

    private CustomAnalogClock mAnalogClockView;
    private boolean mShowAlarm;
    private boolean mAvailableAlarm;
    private boolean mShowClock;
    private boolean mShowDate;
    private int mClockSelection;
    private int mDateSelection;

    private View[] mVisibleInDoze;
    private boolean mPulsing;
    private float mDarkAmount = 0;
    private int mTextColor;
    private int mDateTextColor;
    private int mAlarmTextColor;
    private int mLockClockFontSize;
    private int mLockDateFontSize;
    private int mLockAlarmFontSize;
    private int mLockTempFontSize;
    private int mLockCityFontSize;
    private int mLockConditionFontSize;
    private int dateFont;
    private int tempFont;
    private int cityFont;
    private int conditionFont;

    private boolean mForcedMediaDoze;

    private int mTempColor;
    private int mConditionColor;
    private int mCityColor;
    private int alarmColor;
    private int ownerInfoColor;
    private int mLockColor;
    private int mDateColor;
    private int mLockOwnerInfoFontSize; 
    private int ownerinfoFont; 
    private int alarmFont; 
    private boolean mWeatherEnabled;
    private View mWeatherView;
    private View mShortcut;
    private View weatherPanel;
    private TextView noWeatherInfo;
    private TextView mWeatherCity;
    private ImageView mWeatherConditionImage;
    private TextView mWeatherCurrentTemp;
    private TextView mWeatherConditionText;
    private OmniJawsClient mWeatherClient;
    private OmniJawsClient.WeatherInfo mWeatherData;

    private boolean mShowWeather;
    private boolean mShowConditionIcon;
    private boolean mShowLocation;
    private boolean mShowAmbientBattery;

    private SettingsObserver mSettingsObserver;

    //On the first boot, keygard will start to receiver TIME_TICK intent.
    //And onScreenTurnedOff will not get called if power off when keyguard is not started.
    //Set initial value to false to skip the above case.
    private boolean mEnableRefresh = false;

    private KeyguardUpdateMonitorCallback mInfoCallback = new KeyguardUpdateMonitorCallback() {

        @Override
        public void onTimeChanged() {
            if (mEnableRefresh) {
                refresh();
            }
        }

        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            if (showing) {
                if (DEBUG) Slog.v(TAG, "refresh statusview showing:" + showing);
                refresh();
                updateOwnerInfo();
                updateClockColor();
                updateClockDateColor();
                refreshOwnerInfoSize(); 
            }
        }

        @Override
        public void onStartedWakingUp() {
            setEnableMarquee(true);
            mEnableRefresh = true;
            refresh();
        }

        @Override
        public void onFinishedGoingToSleep(int why) {
            setEnableMarquee(false);
            mEnableRefresh = false;
        }

        @Override
        public void onUserSwitchComplete(int userId) {
            refresh();
            updateOwnerInfo();
            updateClockColor();
            updateClockDateColor();
            refreshLockFont();
            refreshOwnerInfoSize(); 
        }
    };

    public KeyguardStatusView(Context context) {
        this(context, null, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyguardStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        mLockPatternUtils = new LockPatternUtils(getContext());
        mHandler = new Handler(Looper.myLooper());
        mWeatherClient = new OmniJawsClient(mContext);
        updateClockColor();
        updateClockDateColor();
    }

    private void setEnableMarquee(boolean enabled) {
        if (DEBUG) Log.v(TAG, "Schedule setEnableMarquee: " + (enabled ? "Enable" : "Disable"));
        if (enabled) {
            if (mPendingMarqueeStart == null) {
                mPendingMarqueeStart = () -> {
                    setEnableMarqueeImpl(true);
                    mPendingMarqueeStart = null;
                };
                mHandler.postDelayed(mPendingMarqueeStart, MARQUEE_DELAY_MS);
            }
        } else {
            if (mPendingMarqueeStart != null) {
                mHandler.removeCallbacks(mPendingMarqueeStart);
                mPendingMarqueeStart = null;
            }
            setEnableMarqueeImpl(false);
        }
    }

    private void setEnableMarqueeImpl(boolean enabled) {
        if (DEBUG) Log.v(TAG, (enabled ? "Enable" : "Disable") + " transport text marquee");
        if (mAlarmStatusView != null) mAlarmStatusView.setSelected(enabled);
        if (mOwnerInfo != null) mOwnerInfo.setSelected(enabled);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mClockContainer = findViewById(R.id.keyguard_clock_container);
        mAlarmStatusView = findViewById(R.id.alarm_status);
        mDateView = findViewById(R.id.date_view);
        mClockView = findViewById(R.id.clock_view);
        mAnalogClockView = findViewById(R.id.analog_clock_view);
        mClockView.setShowCurrentUserTime(true);
        mOwnerInfo = findViewById(R.id.owner_info);
        // mBatteryDoze = findViewById(R.id.battery_doze);
        mKeyguardStatusArea = findViewById(R.id.keyguard_status_area);
        mVisibleInDoze = new View[]{/*mBatteryDoze*/ mClockView, mAnalogClockView, mKeyguardStatusArea};
        mTextColor = mClockView.getCurrentTextColor();
        mDateTextColor = mDateView.getCurrentTextColor();
        mAlarmTextColor = mAlarmStatusView.getCurrentTextColor();
       
        updateSettings(); 
 
        boolean shouldMarquee = KeyguardUpdateMonitor.getInstance(mContext).isDeviceInteractive(); 
        setEnableMarquee(shouldMarquee); 
        refresh(); 
        updateOwnerInfo(); 
        updateClockColor(); 
        updateClockDateColor(); 
        refreshLockFont(); 

        mWeatherView = findViewById(R.id.keyguard_weather_view);
        mWeatherContainer = findViewById(R.id.weather_container);
        weatherPanel = findViewById(R.id.weather_panel);
        noWeatherInfo = (TextView) findViewById(R.id.no_weather_info_text);
        mWeatherCity = (TextView) findViewById(R.id.city);
        mWeatherConditionImage = (ImageView) findViewById(R.id.ls_weather_image);
        mWeatherCurrentTemp = (TextView) findViewById(R.id.current_temp);
        mWeatherConditionText = (TextView) findViewById(R.id.condition);
        mShortcut = findViewById(R.id.keyguard_shortcuts);
        mSettingsObserver = new SettingsObserver(new Handler());
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Typeface tf = Typeface.create(FONT_FAMILY_MEDIUM, Typeface.NORMAL);

        // Some layouts like burmese have a different margin for the clock
        MarginLayoutParams layoutParams = (MarginLayoutParams) mClockView.getLayoutParams();
        layoutParams.bottomMargin = getResources().getDimensionPixelSize(
                R.dimen.bottom_text_spacing_digital);
        mClockView.setLayoutParams(layoutParams);
        // mClockView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        // Custom analog clock
       MarginLayoutParams customlayoutParams = (MarginLayoutParams) mAnalogClockView.getLayoutParams();
       customlayoutParams.bottomMargin = getResources().getDimensionPixelSize(
               R.dimen.bottom_text_spacing_digital);
       mAnalogClockView.setLayoutParams(customlayoutParams);

        if (mOwnerInfo != null) {
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                 getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21)); 
        }
    }

    private int getLockClockFont() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.LOCK_CLOCK_FONTS, 29);
    }

    public void refreshTime() {
        mDateView.setDatePattern(Patterns.dateViewSkel);

        if (mClockSelection == 0) {
            mClockView.setFormat12Hour(Patterns.clockView12);
            mClockView.setFormat24Hour(Patterns.clockView24);
        } else if (mClockSelection == 1) {
            mClockView.setFormat12Hour(Html.fromHtml("<strong>h</strong>mm"));
            mClockView.setFormat24Hour(Html.fromHtml("<strong>kk</strong>mm"));
        } else if (mClockSelection == 3) {
            mClockView.setFormat12Hour(Html.fromHtml("<strong>hh</strong><br>mm"));
            mClockView.setFormat24Hour(Html.fromHtml("<strong>kk</strong><br>mm"));
        } else {
            mClockView.setFormat12Hour("hh\nmm");
            mClockView.setFormat24Hour("kk\nmm");
        }
    }

    private void refresh() {
        AlarmManager.AlarmClockInfo nextAlarm =
                mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT);
            Patterns.update(mContext, nextAlarm != null && mShowAlarm);

        refreshTime();
        refreshAlarmStatus(nextAlarm);
        refreshLockFont();
    }

    void refreshAlarmStatus(AlarmManager.AlarmClockInfo nextAlarm) {
        if (nextAlarm != null) {
            String alarm = formatNextAlarm(mContext, nextAlarm);
            mAlarmStatusView.setText(alarm);
	    mAlarmStatusView.setContentDescription(
                    getResources().getString(R.string.keyguard_accessibility_next_alarm, alarm));
                    mAvailableAlarm = true;
        } else {
            mAvailableAlarm = false;
        }
        mAlarmStatusView.setVisibility(mDarkAmount != 1 ? (mShowAlarm && mAvailableAlarm ? View.VISIBLE : View.GONE)
                : mAvailableAlarm ? View.VISIBLE : View.GONE);
	        if(mAvailableAlarm && mShowAlarm ){ 
               mAlarmStatusView.setTextColor(alarmColor); 
            } 
    }

    public int getClockBottom() {
        return mKeyguardStatusArea.getBottom();
    }

    public int getClockSelection() {
               return mClockSelection;
    }

    public float getClockTextSize() {
        return mClockView.getTextSize();
    }

    public static String formatNextAlarm(Context context, AlarmManager.AlarmClockInfo info) {
        if (info == null) {
            return "";
        }
        String skeleton = DateFormat.is24HourFormat(context, ActivityManager.getCurrentUser())
                ? "EHm"
                : "Ehma";
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton);
        return DateFormat.format(pattern, info.getTriggerTime()).toString();
    }

    private void updateOwnerInfo() {
        if (mOwnerInfo == null) return;
        String ownerInfo = getOwnerInfo();
        if (!TextUtils.isEmpty(ownerInfo)) {
            mOwnerInfo.setVisibility(View.VISIBLE);
            mOwnerInfo.setText(ownerInfo);
            mOwnerInfo.setTextColor(ownerInfoColor);
        } else {
            mOwnerInfo.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mInfoCallback);
        mWeatherClient.addObserver(this);
        mSettingsObserver.observe();
        updateSettings(); 
        updateWeather();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        KeyguardUpdateMonitor.getInstance(mContext).removeCallback(mInfoCallback);
        mWeatherClient.removeObserver(this);
        mWeatherClient.cleanupObserver();
        mSettingsObserver.unobserve();
    }

    private String getOwnerInfo() {
        String info = null;
        if (mLockPatternUtils.isDeviceOwnerInfoEnabled()) {
            // Use the device owner information set by device policy client via
            // device policy manager.
            info = mLockPatternUtils.getDeviceOwnerInfo();
        } else {
            // Use the current user owner information if enabled.
            final boolean ownerInfoEnabled = mLockPatternUtils.isOwnerInfoEnabled(
                    KeyguardUpdateMonitor.getCurrentUser());
            if (ownerInfoEnabled) {
                info = mLockPatternUtils.getOwnerInfo(KeyguardUpdateMonitor.getCurrentUser());
            }
        }
        return info;
    }

    @Override
    public void weatherUpdated() {
        queryAndUpdateWeather();
    }

    public void queryAndUpdateWeather() {
        try {
            mWeatherClient.queryWeather();
            mWeatherData = mWeatherClient.getWeatherInfo();
            // Weather data not available. Try later.
            if (mWeatherData == null) {
                updateSettings();
                return;
            }
            mWeatherCity.setText(mWeatherData.city);
            mWeatherCurrentTemp.setText(mWeatherData.temp + mWeatherData.tempUnits);
            mWeatherConditionText.setText(mWeatherData.condition);
            updateSettings();
       } catch(Exception e) {
          // Do nothing
       }
    }

    public void updateWeather() {
        try {
            // Check for latest cached info
            if (mWeatherClient.getWeatherInfo() != null)
                mWeatherData = mWeatherClient.getWeatherInfo();
            if (mWeatherData == null) {
                // No weather data available. Try querying.
                queryAndUpdateWeather();
                return;
            }
            mWeatherCity.setText(mWeatherData.city);
            mWeatherCurrentTemp.setText(mWeatherData.temp + mWeatherData.tempUnits);
            mWeatherConditionText.setText(mWeatherData.condition);
            updateSettings();
       } catch(Exception e) {
          // Do nothing
       }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    private void updateVisibilities() {
        switch (mClockSelection) {
        case 0: // default digital 
            mClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mAnalogClockView.setVisibility(View.GONE); 
            break; 
        case 1: // digital (bold) 
            mClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mAnalogClockView.setVisibility(View.GONE); 
            break; 
        case 2: // sammy 
            mClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mAnalogClockView.setVisibility(View.GONE); 
            break; 
        case 3: // sammy (bold) 
            mClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mAnalogClockView.setVisibility(View.GONE); 
            break; 
        case 4: // analog 
            mAnalogClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mClockView.setVisibility(View.GONE); 
            break; 
        default: // custom analog styles (int > 4) 
            mAnalogClockView.setVisibility(mDarkAmount != 1 ? (mShowClock ? View.VISIBLE : View.GONE) : View.VISIBLE); 
            mClockView.setVisibility(View.GONE); 
            break; 
        }
    
        mDateView.setVisibility(mDarkAmount != 1 ? (mShowDate ? View.VISIBLE : View.GONE) : View.VISIBLE);

        mAlarmStatusView.setVisibility(mDarkAmount != 1 ? (mShowAlarm && mAvailableAlarm ? View.VISIBLE : View.GONE)
                : mAvailableAlarm ? View.VISIBLE : View.GONE);
    }

    public void refreshOwnerInfoSize() { 
        int size = mLockOwnerInfoFontSize; 
            if (size == 0) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1)); 
            } else if (size == 1) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1)); 
            } else if (size == 2) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2)); 
            } else if (size == 3) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3)); 
            } else if (size == 4) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4)); 
            } else if (size == 5) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5)); 
            } else if (size == 6) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6)); 
            } else if (size == 7) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7)); 
            } else if (size == 8) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8)); 
            } else if (size == 9) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9)); 
            } else if (size == 10) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10)); 
            } else if (size == 11) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11)); 
            } else if (size == 12) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12)); 
            } else if (size == 13) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13)); 
            } else if (size == 14) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14)); 
            }  else if (size == 15) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15)); 
            } else if (size == 16) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16)); 
            } else if (size == 17) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17)); 
            } else if (size == 18) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18)); 
            } else if (size == 19) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19)); 
            } else if (size == 20) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20)); 
            } else if (size == 21) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21)); 
            } else if (size == 22) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22)); 
            } else if (size == 23) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23)); 
            } else if (size == 24) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24)); 
            } else if (size == 25) { 
            mOwnerInfo.setTextSize(TypedValue.COMPLEX_UNIT_PX, 
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25)); 
            } 
        } 

    public void updateclocksize() {
        int size = mLockClockFontSize;
        if (size == 50) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_50));
        } else if (size == 51) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_51));
        } else if (size == 52) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_52));
        } else if (size == 53) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_53));
        } else if (size == 54) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_54));
        } else if (size == 55) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_55));
        } else if (size == 56) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_56));
        } else if (size == 57) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_57));
        } else if (size == 58) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_58));
        } else if (size == 59) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_59));
        } else if (size == 60) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_60));
        } else if (size == 61) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_61));
        } else if (size == 62) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_62));
        } else if (size == 63) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_63));
        } else if (size == 64) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_64));
        } else if (size == 65) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_65));
        } else if (size == 66) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_66));
        } else if (size == 66) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_67));
        } else if (size == 68) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_68));
        } else if (size == 69) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_69));
        } else if (size == 70) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_70));
        } else if (size == 71) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_71));
        } else if (size == 72) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_72));
        } else if (size == 73) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_73));
        } else if (size == 74) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_74));
        } else if (size == 75) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_75));
        } else if (size == 76) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_76));
        } else if (size == 77) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_77));
        } else if (size == 78) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_78));
        } else if (size == 79) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_79));
        } else if (size == 80) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_80));
        } else if (size == 81) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_81));
        } else if (size == 82) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_82));
        } else if (size == 83) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_83));
        } else if (size == 84) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_84));
        }  else if (size == 85) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_85));
        } else if (size == 86) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_86));
        } else if (size == 87) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_87));
         } else if (size == 88) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_88));
        } else if (size == 89) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_89));
         } else if (size == 90) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_90));
        } else if (size == 91) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_91));
        } else if (size == 92) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_92));
        }  else if (size == 93) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_93));
        } else if (size == 94) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_94));
        } else if (size == 95) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_95));
         } else if (size == 96) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_96));
        } else if (size == 97) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_97));
         } else if (size == 98) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_98));
        } else if (size == 99) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_99));
         } else if (size == 100) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_100));
        } else if (size == 101) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_101));
        } else if (size == 102) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_102));
        }  else if (size == 103) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_103));
        } else if (size == 104) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_104));
        } else if (size == 105) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_105));
         } else if (size == 106) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_106));
        } else if (size == 107) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_107));
         } else if (size == 108) {
        mClockView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_108));
         }
    }

    public void refreshdatesize() {
    int size = mLockDateFontSize;
        if (size == 0) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
        } else if (size == 1) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
        } else if (size == 2) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2));
        } else if (size == 3) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3));
        } else if (size == 4) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4));
        } else if (size == 5) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5));
        } else if (size == 6) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6));
        } else if (size == 7) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7));
        } else if (size == 8) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8));
        } else if (size == 9) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9));
        } else if (size == 10) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
        } else if (size == 11) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
        } else if (size == 12) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
        } else if (size == 13) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
        } else if (size == 14) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
        }  else if (size == 15) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
        } else if (size == 16) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
        } else if (size == 17) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
        } else if (size == 18) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
        } else if (size == 19) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
        } else if (size == 20) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
        } else if (size == 21) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
        } else if (size == 22) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
        } else if (size == 23) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
        } else if (size == 24) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
        } else if (size == 25) {
        mDateView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
        }
    }

    public void refreshtempsize() {
        int size = mLockTempFontSize;
            if (size == 0) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
            } else if (size == 1) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
            } else if (size == 2) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2));
            } else if (size == 3) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3));
            } else if (size == 4) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4));
            } else if (size == 5) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5));
            } else if (size == 6) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6));
            } else if (size == 7) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7));
            } else if (size == 8) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8));
            } else if (size == 9) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9));
            } else if (size == 10) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
            } else if (size == 11) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
            } else if (size == 12) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
            } else if (size == 13) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
            } else if (size == 14) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
            }  else if (size == 15) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
            } else if (size == 16) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
            } else if (size == 17) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
            } else if (size == 18) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
            } else if (size == 19) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
            } else if (size == 20) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
            } else if (size == 21) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
            } else if (size == 22) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
            } else if (size == 23) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
            } else if (size == 24) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
            } else if (size == 25) {
                mWeatherCurrentTemp.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
            }
        }

    
        public void refreshalarmsize() {
        int size = mLockAlarmFontSize;
            if (size == 0) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
            } else if (size == 1) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
            } else if (size == 2) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2));
            } else if (size == 3) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3));
            } else if (size == 4) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4));
            } else if (size == 5) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5));
            } else if (size == 6) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6));
            } else if (size == 7) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7));
            } else if (size == 8) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8));
            } else if (size == 9) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9));
            } else if (size == 10) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
            } else if (size == 11) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
            } else if (size == 12) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
            } else if (size == 13) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
            } else if (size == 14) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
            }  else if (size == 15) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
            } else if (size == 16) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
            } else if (size == 17) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
            } else if (size == 18) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
            } else if (size == 19) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
            } else if (size == 20) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
            } else if (size == 21) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
            } else if (size == 22) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
            } else if (size == 23) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
            } else if (size == 24) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
            } else if (size == 25) {
                mAlarmStatusView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                    getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
            }
        }

        public void refreshcitysize() {
            int size = mLockCityFontSize;
                if (size == 0) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
                } else if (size == 1) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
                } else if (size == 2) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2));
                } else if (size == 3) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3));
                } else if (size == 4) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4));
                } else if (size == 5) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5));
                } else if (size == 6) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6));
                } else if (size == 7) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7));
                } else if (size == 8) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8));
                } else if (size == 9) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9));
                } else if (size == 10) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
                } else if (size == 11) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
                } else if (size == 12) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
                } else if (size == 13) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
                } else if (size == 14) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
                }  else if (size == 15) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
                } else if (size == 16) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
                } else if (size == 17) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
                } else if (size == 18) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
                } else if (size == 19) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
                } else if (size == 20) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
                } else if (size == 21) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
                } else if (size == 22) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
                } else if (size == 23) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
                } else if (size == 24) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
                } else if (size == 25) {
                    mWeatherCity.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
                }
            }

            public void refreshconditionsize() {
                int size = mLockConditionFontSize;
                    if (size == 0) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
                    } else if (size == 1) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_1));
                    } else if (size == 2) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_2));
                    } else if (size == 3) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_3));
                    } else if (size == 4) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_4));
                    } else if (size == 5) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_5));
                    } else if (size == 6) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_6));
                    } else if (size == 7) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_7));
                    } else if (size == 8) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_8));
                    } else if (size == 9) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_9));
                    } else if (size == 10) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_10));
                    } else if (size == 11) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_11));
                    } else if (size == 12) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_12));
                    } else if (size == 13) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_13));
                    } else if (size == 14) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14));
                    }  else if (size == 15) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_15));
                    } else if (size == 16) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_16));
                    } else if (size == 17) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_17));
                    } else if (size == 18) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_18));
                    } else if (size == 19) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_19));
                    } else if (size == 20) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_20));
                    } else if (size == 21) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_21));
                    } else if (size == 22) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_22));
                    } else if (size == 23) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_23));
                    } else if (size == 24) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_24));
                    } else if (size == 25) {
                        mWeatherConditionText.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                            getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_25));
                    }
                }
    
    
        private void updateSettings() {
        final ContentResolver resolver = getContext().getContentResolver();

        if (mWeatherData != null) {
        mWeatherEnabled = mWeatherClient.isOmniJawsEnabled();

        
        if (mWeatherView != null) {
            mWeatherView.setVisibility(mShowWeather ? View.VISIBLE : View.GONE);
        }

        if (noWeatherInfo != null) {
            noWeatherInfo.setVisibility(mShowWeather && !mWeatherClient.isOmniJawsEnabled() ?
                View.VISIBLE : View.GONE);
        }
        if (mWeatherEnabled && mShowWeather && mWeatherConditionText != null &&
                mWeatherConditionText.getText() != null) {
            mWeatherConditionText.setVisibility(View.VISIBLE);
        } else if (mWeatherConditionText != null) {
            mWeatherConditionText.setVisibility(View.GONE);
        }
        if (mWeatherEnabled && mShowWeather && mShowConditionIcon &&
                mWeatherConditionImage != null &&
                mWeatherClient.getWeatherConditionImage(mWeatherData.conditionCode) != null) {
            mWeatherConditionImage.setImageDrawable(overlay(mWeatherClient
                .getWeatherConditionImage(mWeatherData.conditionCode)));
            mWeatherConditionImage.setVisibility(View.VISIBLE);
        } else if (!mWeatherEnabled && mShowWeather && mShowConditionIcon &&
                mWeatherConditionImage != null) {
            mWeatherConditionImage.setImageDrawable(overlay(mContext
                .getResources().getDrawable(R.drawable.keyguard_weather_default_off)));
            mWeatherConditionImage.setVisibility(View.VISIBLE);
        } else if (mWeatherConditionImage != null) {
            mWeatherConditionImage.setVisibility(View.GONE);
        }
        if (mWeatherEnabled && mShowWeather && mWeatherCurrentTemp != null &&
                mWeatherCurrentTemp.getText() != null) {
            mWeatherCurrentTemp.setVisibility(View.VISIBLE);
        } else if (mWeatherCurrentTemp != null) {
            mWeatherCurrentTemp.setVisibility(View.GONE);
        }
        if (mWeatherEnabled && mShowWeather && mShowLocation &&
                mWeatherCity != null && mWeatherCity.getText() != null) {
            mWeatherCity.setVisibility(View.VISIBLE);
        } else if (mWeatherCity != null) {
            mWeatherCity.setVisibility(View.INVISIBLE);
        }
        if (mWeatherConditionText.getVisibility() != View.VISIBLE &&
                mWeatherConditionImage.getVisibility() != View.VISIBLE &&
                mWeatherCurrentTemp.getVisibility() != View.VISIBLE) {
            weatherPanel.setVisibility(View.GONE);
            noWeatherInfo.setVisibility(mShowWeather ?
                View.VISIBLE : View.GONE);
        } else {
            weatherPanel.setVisibility(mShowWeather ?
                View.VISIBLE : View.GONE);
        }

        // LS Temp Font
        if (tempFont == 0) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (tempFont == 1) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (tempFont == 2) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (tempFont == 3) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (tempFont == 4) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (tempFont == 5) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (tempFont == 6) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (tempFont == 7) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (tempFont == 8) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (tempFont == 9) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (tempFont == 10) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (tempFont == 11) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (tempFont == 12) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (tempFont == 13) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
        if (tempFont == 14) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        }
        if (tempFont == 15) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
        }
        if (tempFont == 16) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        }
        if (tempFont == 17) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
        }
        if (tempFont == 18) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        }
        if (tempFont == 19) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("cursive", Typeface.BOLD));
        }
        if (tempFont == 20) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("casual", Typeface.NORMAL));
        }
        if (tempFont == 21) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        }
        if (tempFont == 22) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        }
        if (tempFont == 23) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("serif", Typeface.BOLD));
        }
        if (tempFont == 24) {
            mWeatherCurrentTemp.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        }

        // LS City font
        
        if (cityFont == 0) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (cityFont == 1) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (cityFont == 2) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (cityFont == 3) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (cityFont == 4) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (cityFont == 5) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (cityFont == 6) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (cityFont == 7) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (cityFont == 8) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (cityFont == 9) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (cityFont == 10) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (cityFont == 11) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (cityFont == 12) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (cityFont == 13) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
        if (cityFont == 14) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        }
        if (cityFont == 15) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
        }
        if (cityFont == 16) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        }
        if (cityFont == 17) {
            mWeatherCity.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
        }
        if (cityFont == 18) {
            mWeatherCity.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        }
        if (cityFont == 19) {
            mWeatherCity.setTypeface(Typeface.create("cursive", Typeface.BOLD));
        }
        if (cityFont == 20) {
            mWeatherCity.setTypeface(Typeface.create("casual", Typeface.NORMAL));
        }
        if (cityFont == 21) {
            mWeatherCity.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        }
        if (cityFont == 22) {
            mWeatherCity.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        }
        if (cityFont == 23) {
            mWeatherCity.setTypeface(Typeface.create("serif", Typeface.BOLD));
        }
        if (cityFont == 24) {
            mWeatherCity.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        }

        // LS Contiton font
        
        if (conditionFont == 0) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (conditionFont == 1) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (conditionFont == 2) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (conditionFont == 3) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (conditionFont == 4) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (conditionFont == 5) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (conditionFont == 6) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (conditionFont == 7) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (conditionFont == 8) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (conditionFont == 9) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (conditionFont == 10) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (conditionFont == 11) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (conditionFont == 12) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (conditionFont == 13) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
        if (conditionFont == 14) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        }
        if (conditionFont == 15) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
        }
        if (conditionFont == 16) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        }
        if (conditionFont == 17) {
            mWeatherConditionText.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
        }
        if (conditionFont == 18) {
            mWeatherConditionText.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        }
        if (conditionFont == 19) {
            mWeatherConditionText.setTypeface(Typeface.create("cursive", Typeface.BOLD));
        }
        if (conditionFont == 20) {
            mWeatherConditionText.setTypeface(Typeface.create("casual", Typeface.NORMAL));
        }
        if (conditionFont == 21) {
            mWeatherConditionText.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        }
        if (conditionFont == 22) {
            mWeatherConditionText.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        }
        if (conditionFont == 23) {
            mWeatherConditionText.setTypeface(Typeface.create("serif", Typeface.BOLD));
        }
        if (conditionFont == 24) {
            mWeatherConditionText.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        }

    }

        // LS Date Font
        if (dateFont == 0) {
            mDateView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (dateFont == 1) {
            mDateView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (dateFont == 2) {
            mDateView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (dateFont == 3) {
            mDateView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (dateFont == 4) {
            mDateView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (dateFont == 5) {
                mDateView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        }
        if (dateFont == 6) {
            mDateView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (dateFont == 7) {
                mDateView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        }
        if (dateFont == 8) {
            mDateView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (dateFont == 9) {
            mDateView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (dateFont == 10) {
            mDateView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (dateFont == 11) {
            mDateView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (dateFont == 12) {
            mDateView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (dateFont == 13) {
            mDateView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
        if (dateFont == 14) {
                mDateView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));
        }
        if (dateFont == 15) {
                mDateView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));
        }
        if (dateFont == 16) {
                mDateView.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        }
        if (dateFont == 17) {
                mDateView.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));
        }
        if (dateFont == 18) {
                mDateView.setTypeface(Typeface.create("cursive", Typeface.NORMAL));
        }
        if (dateFont == 19) {
                mDateView.setTypeface(Typeface.create("cursive", Typeface.BOLD));
        }
        if (dateFont == 20) {
                mDateView.setTypeface(Typeface.create("casual", Typeface.NORMAL));
        }
        if (dateFont == 21) {
                mDateView.setTypeface(Typeface.create("serif", Typeface.NORMAL));
        }
        if (dateFont == 22) {
                mDateView.setTypeface(Typeface.create("serif", Typeface.ITALIC));
        }
        if (dateFont == 23) {
                mDateView.setTypeface(Typeface.create("serif", Typeface.BOLD));
        }
        if (dateFont == 24) {
                mDateView.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));
        }

        

        if (ownerinfoFont == 0) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 1) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD)); 
        } 
        if (ownerinfoFont == 2) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 3) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC)); 
        } 
        if (ownerinfoFont == 4) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 5) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 6) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 7) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 8) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 9) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 10) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD)); 
        } 
        if (ownerinfoFont == 11) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC)); 
        } 
        if (ownerinfoFont == 12) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 13) { 
            mOwnerInfo.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 14) { 
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 15) { 
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 16) { 
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 17) { 
                mOwnerInfo.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 18) { 
                mOwnerInfo.setTypeface(Typeface.create("cursive", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 19) { 
                mOwnerInfo.setTypeface(Typeface.create("cursive", Typeface.BOLD)); 
        } 
        if (ownerinfoFont == 20) { 
                mOwnerInfo.setTypeface(Typeface.create("casual", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 21) { 
                mOwnerInfo.setTypeface(Typeface.create("serif", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 22) { 
                mOwnerInfo.setTypeface(Typeface.create("serif", Typeface.ITALIC)); 
        } 
        if (ownerinfoFont == 23) { 
                mOwnerInfo.setTypeface(Typeface.create("serif", Typeface.BOLD)); 
        } 
        if (ownerinfoFont == 24) { 
                mOwnerInfo.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC)); 
        } 
        if (ownerinfoFont == 25) { 
            mOwnerInfo.setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 26) { 
            mOwnerInfo.setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 27) { 
            mOwnerInfo.setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 28) { 
            mOwnerInfo.setTypeface(Typeface.create("googlesans-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 29) { 
            mOwnerInfo.setTypeface(Typeface.create("neoneon-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 30) { 
            mOwnerInfo.setTypeface(Typeface.create("themeable-sys", Typeface.NORMAL)); 
        } 
        if (ownerinfoFont == 31) { 
            mOwnerInfo.setTypeface(Typeface.create("samsung-sys", Typeface.NORMAL)); 
        } 

        // alarm font

        if (alarmFont == 0) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));  
        }  
        if (alarmFont == 1) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));  
        }  
        if (alarmFont == 2) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));  
        }  
        if (alarmFont == 3) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));  
        }  
        if (alarmFont == 4) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));  
        }  
        if (alarmFont == 5) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));  
        }  
        if (alarmFont == 6) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));  
        }  
        if (alarmFont == 7) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));  
        }  
        if (alarmFont == 8) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));  
        }  
        if (alarmFont == 9) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));  
        }  
        if (alarmFont == 10) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));  
        }  
        if (alarmFont == 11) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));  
        }  
        if (alarmFont == 12) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));  
        }  
        if (alarmFont == 13) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));  
        }  
        if (alarmFont == 14) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.NORMAL));  
        }  
        if (alarmFont == 15) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-condensed-light", Typeface.ITALIC));  
        }  
        if (alarmFont == 16) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));  
        }  
        if (alarmFont == 17) {  
            mAlarmStatusView.setTypeface(Typeface.create("sans-serif-black", Typeface.ITALIC));  
        }  
        if (alarmFont == 18) {  
            mAlarmStatusView.setTypeface(Typeface.create("cursive", Typeface.NORMAL));  
        }  
        if (alarmFont == 19) {  
            mAlarmStatusView.setTypeface(Typeface.create("cursive", Typeface.BOLD));  
        }  
        if (alarmFont == 20) {  
            mAlarmStatusView.setTypeface(Typeface.create("casual", Typeface.NORMAL));  
        }  
        if (alarmFont == 21) {  
            mAlarmStatusView.setTypeface(Typeface.create("serif", Typeface.NORMAL));  
        }  
        if (alarmFont == 22) {  
            mAlarmStatusView.setTypeface(Typeface.create("serif", Typeface.ITALIC));  
        }  
        if (alarmFont == 23) {  
            mAlarmStatusView.setTypeface(Typeface.create("serif", Typeface.BOLD));  
        }  
        if (alarmFont == 24) {  
            mAlarmStatusView.setTypeface(Typeface.create("serif", Typeface.BOLD_ITALIC));  
        }  
        if (alarmFont == 25) {  
            mAlarmStatusView.setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 26) {  
            mAlarmStatusView.setTypeface(Typeface.create("roadrage-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 27) {  
            mAlarmStatusView.setTypeface(Typeface.create("snowstorm-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 28) {  
            mAlarmStatusView.setTypeface(Typeface.create("googlesans-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 29) {  
            mOwnerInfo.setTypeface(Typeface.create("neoneon-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 30) {  
            mAlarmStatusView.setTypeface(Typeface.create("themeable-sys", Typeface.NORMAL));  
        }  
        if (alarmFont == 31) {  
            mAlarmStatusView.setTypeface(Typeface.create("samsung-sys", Typeface.NORMAL));  
        }  

        AlarmManager.AlarmClockInfo nextAlarm = 
        mAlarmManager.getNextAlarmClock(UserHandle.USER_CURRENT); 

        mShowAlarm = Settings.System.getIntForUser(resolver,
                Settings.System.HIDE_LOCKSCREEN_ALARM, 1, UserHandle.USER_CURRENT) == 1;
        mShowClock = Settings.System.getIntForUser(resolver,
                Settings.System.HIDE_LOCKSCREEN_CLOCK, 1, UserHandle.USER_CURRENT) == 1;
        mShowDate = Settings.System.getIntForUser(resolver,
                Settings.System.HIDE_LOCKSCREEN_DATE, 1, UserHandle.USER_CURRENT) == 1;
        
        mClockSelection = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_CLOCK_SELECTION, 0, UserHandle.USER_CURRENT);
        mDateSelection = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKSCREEN_DATE_SELECTION, 0, UserHandle.USER_CURRENT);

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mKeyguardStatusArea.getLayoutParams();
        switch (mClockSelection) {
            case 0: // default digital 
                params.addRule(RelativeLayout.BELOW, R.id.clock_view); 
                mClockView.setSingleLine(true); 
                mClockView.setGravity(Gravity.CENTER); 
                mAnalogClockView.unregisterReceiver(); 
                break; 
            case 1: // digital (bold) 
                params.addRule(RelativeLayout.BELOW, R.id.clock_view); 
                mClockView.setSingleLine(true); 
                mClockView.setGravity(Gravity.CENTER); 
                mAnalogClockView.unregisterReceiver(); 
                break; 
            case 2: // sammy 
                params.addRule(RelativeLayout.BELOW, R.id.clock_view); 
                mClockView.setSingleLine(false); 
                mClockView.setGravity(Gravity.CENTER); 
                mAnalogClockView.unregisterReceiver(); 
                break; 
            case 3: // sammy (bold) 
                params.addRule(RelativeLayout.BELOW, R.id.clock_view); 
                mClockView.setSingleLine(false); 
                mClockView.setGravity(Gravity.CENTER); 
                mAnalogClockView.unregisterReceiver(); 
                break; 
            case 4: // analog 
                params.addRule(RelativeLayout.BELOW, R.id.analog_clock_view); 
                mClockView.setGravity(Gravity.CENTER);
                mAnalogClockView.registerReceiver(); 
                break; 
            default: // custom analog styles (int > 4) 
                params.addRule(RelativeLayout.BELOW, R.id.analog_clock_view); 
                mClockView.setGravity(Gravity.CENTER);
                mAnalogClockView.registerReceiver(); 
                break; 
        }

        switch (mDateSelection) {
            case 0: // default
            default:
                mDateView.setBackgroundResource(0);
                mDateView.setPadding(0,0,0,0);
                break;
            case 1: // semi-transparent box
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_box_str_border));
                mDateView.setPadding(40,20,40,20);
                break;
            case 2: // semi-transparent box (round)
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_str_border));
                mDateView.setPadding(40,20,40,20);
                break;
            case 3: // white outline
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_box_border_outline));
                mDateView.setPadding(40,20,40,20);
                break;
            case 4: // white outline (round)
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_border_outline));
                mDateView.setPadding(40,20,40,20);
                break;
            case 5: // black outline
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_box_border_outline_black));
                mDateView.setPadding(40,20,40,20);
                break;
            case 6: // black outline (round)
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_border_outline_black));
                mDateView.setPadding(40,20,40,20);
                break;
            case 7: // accent outline
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_box_border_outline_accent));
                mDateView.setPadding(40,20,40,20);
                break;
            case 8: // accent outline (round)
                mDateView.setBackground(getResources().getDrawable(R.drawable.date_border_outline_accent));
                mDateView.setPadding(40,20,40,20);
                break;
        }

        updateVisibilities();  
        updateDozeVisibleViews();
    }

    public void updateAll() {
        updateSettings();
        refresh();
    }

    private Drawable overlay(Drawable image) {
        Resources resources = mContext.getResources(); 
        if (image instanceof VectorDrawable) {
            image = applyTint(image);
        }
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));
        final int imageWidth = image.getIntrinsicWidth();
        final int imageHeight = image.getIntrinsicHeight();

        final Bitmap bmp = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bmp);
        image.setBounds(0, 0, imageWidth, imageHeight);
        image.draw(canvas);

        BitmapDrawable d = shadow(resources, bmp);

        return d;
    }

    public static BitmapDrawable shadow(Resources resources, Bitmap b) {
        final Canvas canvas = new Canvas();
        canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.ANTI_ALIAS_FLAG,
                Paint.FILTER_BITMAP_FLAG));

        BlurMaskFilter blurFilter = new BlurMaskFilter(5, BlurMaskFilter.Blur.OUTER);
        Paint shadowPaint = new Paint();
        shadowPaint.setColor(resources.getColor(R.color.default_shadow_color_no_alpha));
        shadowPaint.setMaskFilter(blurFilter);

        int[] offsetXY = new int[2];
        Bitmap b2 = b.extractAlpha(shadowPaint, offsetXY);

        Bitmap bmResult = Bitmap.createBitmap(b.getWidth(), b.getHeight(),
                Bitmap.Config.ARGB_8888);

        canvas.setBitmap(bmResult);
        canvas.drawBitmap(b2, offsetXY[0], offsetXY[1], null);
        canvas.drawBitmap(b, 0, 0, null);

        return new BitmapDrawable(resources, bmResult);
    }

    private Drawable applyTint(Drawable icon) {
        icon = icon.mutate();
        icon.setTint(getTintColor());
        return icon;
    }
    
    private void refreshLockFont() {
        final Resources res = getContext().getResources();
        boolean isPrimary = UserHandle.getCallingUserId() == UserHandle.USER_OWNER;
        int lockClockFont = isPrimary ? getLockClockFont() : 29;

        if (lockClockFont == 0) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        }
        if (lockClockFont == 1) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        }
        if (lockClockFont == 2) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
        }
        if (lockClockFont == 3) {
            mClockView.setTypeface(Typeface.create("sans-serif", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 4) {
            mClockView.setTypeface(Typeface.create("sans-serif-light", Typeface.ITALIC));
        }
        if (lockClockFont == 5) {
            mClockView.setTypeface(Typeface.create("sans-serif-thin", Typeface.ITALIC));
        }
        if (lockClockFont == 6) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));
        }
        if (lockClockFont == 7) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.ITALIC));
        }
        if (lockClockFont == 8) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        }
        if (lockClockFont == 9) {
            mClockView.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC));
        }
        if (lockClockFont == 10) {
            mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        if (lockClockFont == 11) {
            mClockView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));
        }
    }

    private int getTintColor() {
        return Color.WHITE;
        /*TypedArray array = mContext.obtainStyledAttributes(new int[]{android.R.attr.colorControlNormal});
        int color = array.getColor(0, 0);
        array.recycle();
        return color;*/
    }

    @Override
    public void weatherError(int errorReason) {
        if (DEBUG) Log.d(TAG, "weatherError " + errorReason);
        if (mShowWeather && !mWeatherClient.isOmniJawsEnabled()) {
            updateSettings();
        }
    }

    private void updateClockColor() {
        ContentResolver resolver = getContext().getContentResolver();
        int color = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CLOCK_COLOR, 0xFFFFFFFF);

        if (mClockView != null) {
            mClockView.setTextColor(color);
        }
    }

    private void updateClockDateColor() {
        ContentResolver resolver = getContext().getContentResolver();
        int color = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, 0xFFFFFFFF);

        if (mDateView != null) {
            mDateView.setTextColor(color);
        }
    }

     // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // DateFormat.getBestDateTimePattern is extremely expensive, and refresh is called often.
    // This is an optimization to ensure we only recompute the patterns when the inputs change.
    private static final class Patterns {
        static String dateViewSkel;
        static String clockView12;
        static String clockView24;
        static String cacheKey;

        static void update(Context context, boolean hasAlarm) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();

            final ContentResolver resolver = context.getContentResolver();
   
            dateViewSkel = res.getString(hasAlarm
                    ? R.string.abbrev_wday_month_day_no_year_alarm
                    : R.string.abbrev_wday_month_day_no_year);
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);
            final String key = locale.toString() + dateViewSkel + clockView12Skel + clockView24Skel;
            if (key.equals(cacheKey)) return;

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);
            // CLDR insists on adding an AM/PM indicator even though it wasn't in the skeleton
            // format.  The following code removes the AM/PM indicator if we didn't want it.
            if (!clockView12Skel.contains("a")) {
                clockView12 = clockView12.replaceAll("a", "").trim();
            }

            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            cacheKey = key;
        }
    }

    public void setDark(float darkAmount) {
        if (mDarkAmount == darkAmount) {
            updateVisibilities();
            return;
        }
        mDarkAmount = darkAmount;

        boolean dark = darkAmount == 1;
        final int N = mClockContainer.getChildCount();
        for (int i = 0; i < N; i++) {
            View child = mClockContainer.getChildAt(i);
            if (!mForcedMediaDoze && ArrayUtils.contains(mVisibleInDoze, child)) {
                continue;
            }
            child.setAlpha(dark ? 0 : 1);
        }

        if(mOwnerInfo != null) {
            mOwnerInfo.setAlpha(dark ? 0 : 1);
        }

        if (mWeatherCity != null ) {
            mWeatherCity.setAlpha(dark ? 0 :1);
        }

        if (mWeatherCurrentTemp != null ) {
            mWeatherCurrentTemp.setAlpha(dark ? 0 :1);
        }

        if (mWeatherConditionText != null ) {
            mWeatherConditionText.setAlpha(dark ? 0 :1);
        }     

        if (mShortcut != null ) {
            mShortcut.setAlpha(dark ? 0 :1);
        }     


        updateDozeVisibleViews();
        mAnalogClockView.setDark(dark); 
        updateVisibilities();
    }

    public void setPulsing(boolean pulsing) {
        mPulsing = pulsing;
    }

    public void setForcedMediaPulse(int reason) {
        mForcedMediaDoze =
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION;
        updateDozeVisibleViews();
    }

    public void setCleanLayout(boolean force) {
        mForcedMediaDoze = force;
        updateDozeVisibleViews();
        refresh();
    }

    private void updateDozeVisibleViews() {
        for (View child : mVisibleInDoze) {
            if (!mForcedMediaDoze) {
                child.setAlpha(mDarkAmount == 1 && mPulsing ? 0.8f : 1);
            } else {
                child.setAlpha(mDarkAmount == 1 ? 0 : 1);
            }
        }
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();

            // enable if not working
            // int showAmbientBottomInfo = Settings.System.getIntForUser(mContext.getContentResolver(), 
            // Settings.System.AMBIENT_BOTTOM_DISPLAY, 0, UserHandle.USER_CURRENT); 

            resolver.registerContentObserver(Settings.System.getUriFor(
                   Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                   Settings.System.LOCK_SCREEN_SHOW_WEATHER), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                  Settings.System.OMNIJAWS_WEATHER_ICON_PACK), false, this, UserHandle.USER_ALL);
            resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_CLOCK_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_DATE_FONTS), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_ALARM_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_OWNER_INFO_COLOR), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKCLOCK_FONT_SIZE), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor(
                     Settings.System.LOCKDATE_FONT_SIZE), false, this, UserHandle.USER_ALL);
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCKALARM_FONT_SIZE), false, this, UserHandle.USER_ALL); 
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCKTEMP_FONT_SIZE), false, this, UserHandle.USER_ALL); 
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCKCITY_FONT_SIZE), false, this, UserHandle.USER_ALL); 
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCKCONDITION_FONT_SIZE), false, this, UserHandle.USER_ALL);    
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCK_OWNERINFO_FONTS), false, this, UserHandle.USER_ALL); 
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCKOWNER_FONT_SIZE), false, this, UserHandle.USER_ALL);   
             resolver.registerContentObserver(Settings.System.getUriFor( 
                        Settings.System.LOCK_ALARM_FONTS), false, this, UserHandle.USER_ALL);  

            if (mWeatherData != null ) {
                mShowConditionIcon = Settings.System.getIntForUser(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 1, UserHandle.USER_CURRENT) == 1;
            }
            queryAndUpdateWeather();
            update();                        
        }
 
        void unobserve() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = mContext.getContentResolver();

            if (uri.equals(Settings.System.getUriFor(
                      Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR))) {
                  updateSettings();
              } else if (uri.equals(Settings.System.getUriFor(
                      Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR))) {
                  updateSettings();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR))) {
                 updateClockDateColor();
             } else if (uri.equals(Settings.System.getUriFor(
                     Settings.System.LOCKSCREEN_CLOCK_COLOR))) {
                        updateClockColor();
             } else  if (uri.equals(Settings.System.getUriFor(
                Settings.System.LOCK_SCREEN_SHOW_WEATHER))) {
                    updateWeather();
                } else if (uri.equals(Settings.System.getUriFor(
                        Settings.System.OMNIJAWS_WEATHER_ICON_PACK))) {
                            updateWeather();
                }  else if (uri.equals(Settings.System.getUriFor(
                        Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION))) {
                            updateWeather();
              } else if (uri.equals(Settings.System.getUriFor(
                      Settings.System.LOCK_DATE_FONTS))) {
                    updateSettings();
              } else if (uri.equals(Settings.System.getUriFor(
                      Settings.System.LOCKCLOCK_FONT_SIZE))) {
                  updateclocksize();
              } else if (uri.equals(Settings.System.getUriFor(
                      Settings.System.LOCKDATE_FONT_SIZE))) {
                        refreshdatesize();
               } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCKALARM_FONT_SIZE))) {
                        refreshalarmsize();
               } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCKTEMP_FONT_SIZE))) {
                        refreshtempsize();
               } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCKCITY_FONT_SIZE))) {
                        refreshcitysize();
               } else if (uri.equals(Settings.System.getUriFor(
                    Settings.System.LOCKCONDITION_FONT_SIZE))) {
                        refreshconditionsize();
                    } else if (uri.equals(Settings.System.getUriFor( 
                        Settings.System.LOCK_OWNERINFO_FONTS))) { 
                   refreshOwnerInfoSize(); 
                } else if (uri.equals(Settings.System.getUriFor( 
                    Settings.System.LOCKOWNER_FONT_SIZE))) { 
                    updateSettings(); 
                } else if (uri.equals(Settings.System.getUriFor( 
                    Settings.System.LOCK_ALARM_FONTS))) { 
                    updateSettings(); 
                }  else if (uri.equals(Settings.System.getUriFor( 
                    Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON))) { 
                    if(mWeatherData != null) {
                        mShowConditionIcon = Settings.System.getIntForUser(resolver,
                        Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 1, UserHandle.USER_CURRENT) == 1;
                        updateWeather(); 
                    }  
                }
             update();
         }

       public void update() {
           ContentResolver resolver = mContext.getContentResolver();
           int currentUserId = ActivityManager.getCurrentUser();

            mTempColor = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_TEMP_COLOR, 0xFFFFFFFF);
           mCityColor = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_CITY_COLOR, 0xFFFFFFFF);
           alarmColor = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_ALARM_COLOR, 0xFFFFFFFF);
           ownerInfoColor = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_OWNER_INFO_COLOR, 0xFFFFFFFF);
           mLockColor = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CLOCK_COLOR, 0xFFFFFFFF);
           mDateColor = Settings.System.getInt(resolver,
                Settings.System.LOCKSCREEN_CLOCK_DATE_COLOR, 0xFFFFFFFF);
           dateFont = Settings.System.getIntForUser(resolver,
                Settings.System.LOCK_DATE_FONTS, 8, UserHandle.USER_CURRENT);
           tempFont = Settings.System.getIntForUser(resolver,
                Settings.System.LOCK_TEMP_FONTS, 8, UserHandle.USER_CURRENT);
           cityFont = Settings.System.getIntForUser(resolver,
                Settings.System.LOCK_CITY_FONTS, 8, UserHandle.USER_CURRENT);
           conditionFont = Settings.System.getIntForUser(resolver,
                Settings.System.LOCK_CONDITION_FONTS, 8, UserHandle.USER_CURRENT);
           mLockClockFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKCLOCK_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_clock_font_size_78),
                UserHandle.USER_CURRENT);
           mLockDateFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKDATE_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14),
                UserHandle.USER_CURRENT);
           mLockAlarmFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKALARM_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14),
                UserHandle.USER_CURRENT);
           mLockTempFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKTEMP_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14),
                UserHandle.USER_CURRENT);
           mLockCityFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKCITY_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14),
                UserHandle.USER_CURRENT);
           mLockConditionFontSize = Settings.System.getIntForUser(resolver,
                Settings.System.LOCKCONDITION_FONT_SIZE,
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14),
                UserHandle.USER_CURRENT);      
           ownerinfoFont = Settings.System.getIntForUser(resolver, 
                Settings.System.LOCK_OWNERINFO_FONTS, 26, UserHandle.USER_CURRENT); 
           alarmFont = Settings.System.getIntForUser(resolver, 
                Settings.System.LOCK_ALARM_FONTS, 26, UserHandle.USER_CURRENT); 
           mLockOwnerInfoFontSize = Settings.System.getIntForUser(resolver, 
                Settings.System.LOCKOWNER_FONT_SIZE, 
                getResources().getDimensionPixelSize(R.dimen.lock_date_font_size_14), 
                UserHandle.USER_CURRENT); 
                refreshOwnerInfoSize(); 
        
            if(mWeatherData != null) {
                mShowWeather = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER, 0) == 1;
                mShowConditionIcon = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_WEATHER_CONDITION_ICON, 0) == 1;
                mShowLocation = Settings.System.getInt(resolver,
                Settings.System.LOCK_SCREEN_SHOW_WEATHER_LOCATION, 1) == 1;
                mWeatherEnabled = mWeatherClient.isOmniJawsEnabled();
                 queryAndUpdateWeather();
            }
            queryAndUpdateWeather();
           updateSettings();


         }
    }
}
