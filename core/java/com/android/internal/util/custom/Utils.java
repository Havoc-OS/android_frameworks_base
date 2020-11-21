/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.internal.util.custom;

import android.app.NotificationManager;
import android.app.ActivityManager;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.widget.Toast;

import com.android.internal.R;
import com.android.internal.statusbar.IStatusBarService;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.VIBRATOR_SERVICE;

public class Utils {

    public static final String INTENT_SCREENSHOT = "action_take_screenshot";
    public static final String INTENT_REGION_SCREENSHOT = "action_take_region_screenshot";

    // Check to see if device is WiFi only
    public static boolean isWifiOnly(Context context) {
        ConnectivityManager cm = (ConnectivityManager)context.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        return (cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE) == false);
    }

    // Check to see if device supports the Fingerprint scanner
    public static boolean hasFingerprintSupport(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected());
    }

    // Check to see if device not only supports the Fingerprint scanner but also if is enrolled
    public static boolean hasFingerprintEnrolled(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
        return context.getApplicationContext().checkSelfPermission(Manifest.permission.USE_FINGERPRINT) == PackageManager.PERMISSION_GRANTED &&
                (fingerprintManager != null && fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints());
    }

    // Check to see if device has a camera
    public static boolean hasCamera(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    // Check to see if device supports NFC
    public static boolean hasNFC(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    // Check to see if device supports Wifi
    public static boolean hasWiFi(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI);
    }

    // Check to see if device supports Bluetooth
    public static boolean hasBluetooth(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
    }

    // Check to see if device supports an alterative ambient display package
    /*public static boolean hasAltAmbientDisplay(Context context) {
        return context.getResources().getBoolean(com.android.internal.R.bool.config_alt_ambient_display);
    }*/

    // Check to see if device supports A/B (seamless) system updates
    public static boolean isABdevice(Context context) {
        return SystemProperties.getBoolean("ro.build.ab_update", false);
    }

    // Screen on
    public static void switchScreenOn(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) return;
        pm.wakeUp(SystemClock.uptimeMillis(), "com.android.systemui:CAMERA_GESTURE_PREVENT_LOCK");
    }

    public static boolean deviceHasFlashlight(Context ctx) {
        return ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public static void toggleCameraFlash(boolean proximityCheck) {
        FireActions.toggleCameraFlash(proximityCheck);
    }

    public static void killForegroundApp() {
        FireActions.killForegroundApp();
    }

    private static final class FireActions {
        private static IStatusBarService mStatusBarService = null;

        private static IStatusBarService getStatusBarService() {
            synchronized (FireActions.class) {
                if (mStatusBarService == null) {
                    mStatusBarService = IStatusBarService.Stub.asInterface(
                            ServiceManager.getService("statusbar"));
                }
                return mStatusBarService;
            }
        }

        public static void toggleCameraFlash(boolean proximityCheck) {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleCameraFlash(proximityCheck);
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }

        public static void killForegroundApp() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.killForegroundApp();
                } catch (RemoteException e) {
                    // do nothing.
                }
            }
        }

        // Toggle notifications panel
        public static void toggleNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.togglePanel();
                } catch (RemoteException e) {}
            }
        }

        // Toggle qs panel
        public static void toggleQsPanel() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.toggleSettingsPanel();
                } catch (RemoteException e) {}
            }
        }

        // Clear notifications
        public static void clearAllNotifications() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.onClearAllNotifications(ActivityManager.getCurrentUser());
                } catch (RemoteException e) {}
            }
        }

        // Clear notifications
        public static void startAssist() {
            IStatusBarService service = getStatusBarService();
            if (service != null) {
                try {
                    service.startAssist(new Bundle());
                } catch (RemoteException e) {}
            }
        }
    }

    public static void clearAllNotifications() {
        FireActions.clearAllNotifications();
    }

    public static void toggleQsPanel() {
        FireActions.toggleQsPanel();
    }

    public static void toggleNotifications() {
        FireActions.toggleNotifications();
    }

    // Method to turn off the screen
    public static void switchScreenOff(Context ctx) {
        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        if (pm!= null && pm.isScreenOn()) {
            pm.goToSleep(SystemClock.uptimeMillis());
        }
    }

    public static void takeScreenshot(boolean full) {
        final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.sendCustomAction(new Intent(full? INTENT_SCREENSHOT : INTENT_REGION_SCREENSHOT));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Cycle ringer modes
    public static void toggleRingerModes (Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        Vibrator mVibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);

        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_NORMAL:
                if (mVibrator.hasVibrator()) {
                    am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                NotificationManager notificationManager =
                        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                break;
            case AudioManager.RINGER_MODE_SILENT:
                am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                break;
        }
    }

    // Volume panel
    public static void toggleVolumePanel(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        am.adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI);
    }

    public static void launchCamera(Context context) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    public static void launchVoiceSearch(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEARCH_LONG_PRESS);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void triggerHushMute(Context context) {
        // We can't call AudioService#silenceRingerModeInternal from here, so this is a partial copy of it
        int silenceRingerSetting = Settings.Secure.getIntForUser(context.getContentResolver(),
                Settings.Secure.VOLUME_HUSH_GESTURE, Settings.Secure.VOLUME_HUSH_OFF,
                UserHandle.USER_CURRENT);

        int ringerMode;
        int toastText;
        if (silenceRingerSetting == Settings.Secure.VOLUME_HUSH_VIBRATE) {
            ringerMode = AudioManager.RINGER_MODE_VIBRATE;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_vibrate;
        } else {
            // VOLUME_HUSH_MUTE and VOLUME_HUSH_OFF
            ringerMode = AudioManager.RINGER_MODE_SILENT;
            toastText = com.android.internal.R.string.volume_dialog_ringer_guidance_silent;
        }
        AudioManager audioMan = (AudioManager)
                context.getSystemService(Context.AUDIO_SERVICE);
        audioMan.setRingerModeInternal(ringerMode);
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }

    public static void showPowerMenu() {
        final IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            wm.showGlobalActions();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void startAssist() {
        FireActions.startAssist();
    }

    // Check to see if a package is installed
    public static boolean isPackageInstalled(Context context, String pkg, boolean ignoreState) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled && !ignoreState) {
                    return false;
                }
            } catch (NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        return isPackageInstalled(context, pkg, true);
    }
}
