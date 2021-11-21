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

package com.android.systemui.custom;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.content.Context;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import java.util.List;

public class BackgroundProcessManager {

    private static final Handler mHandler = new Handler();
    private static Context mContext;
    private static ActivityManager mActivityManager;

    public static void init(Context context) {
        mContext = context;
        mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
    }

    private static final Runnable mStartRunnable = BackgroundProcessManager::killProcesses;

    private static final Runnable mStopRunnable = BackgroundProcessManager::stop;

    public static void start() {
        AlarmClockInfo alarmClockInfo =
                ((AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE)).getNextAlarmClock();
        boolean sleepModeEnabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SLEEP_MODE_ENABLED, 0, UserHandle.USER_CURRENT) == 1;
        long timeUntilAlarm = 0;

        if (alarmClockInfo != null) {
            timeUntilAlarm = alarmClockInfo.getTriggerTime() - System.currentTimeMillis();
        }

        if (sleepModeEnabled || (timeUntilAlarm != 0 && timeUntilAlarm < 3600000)) {
            mHandler.postDelayed(mStartRunnable, 100);
        } else {
            mHandler.postDelayed(mStartRunnable, 3600000);
        }

        if (timeUntilAlarm != 0) {
            mHandler.postDelayed(mStopRunnable, (timeUntilAlarm - 900000));
        }
    }

    public static void stop() {
        mHandler.removeCallbacks(mStartRunnable);
        mHandler.removeCallbacks(mStopRunnable);
    }

    private static void killProcesses() {
        List<RunningAppProcessInfo> processes = mActivityManager.getRunningAppProcesses();

        for (int i = 0; i < processes.size(); i++) {
            if (!processes.get(i).pkgList[0].equals("android") &&
                    !processes.get(i).pkgList[0].contains("com.android.") &&
                    !processes.get(i).pkgList[0].contains("launcher") &&
                    !processes.get(i).pkgList[0].contains("ims")) {
                mActivityManager.killBackgroundProcesses(processes.get(i).pkgList[0]);
            }
        }
    }
}
