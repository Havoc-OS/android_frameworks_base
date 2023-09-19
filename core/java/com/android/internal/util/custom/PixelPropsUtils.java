/*
 * Copyright (C) 2020 The Pixel Experience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.internal.util.custom;

import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PixelPropsUtils {

    public static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String DEVICE = "ro.product.device";
    private static final String TAG = PixelPropsUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final Map<String, Object> propsToChangePixel2;

    private static final Map<String, Object> propsToChangePixel5;
    private static final String[] packagesToChangePixel5 = {
        "com.google.android.tts",
        "com.google.android.googlequicksearchbox",
        "com.google.android.apps.recorder"
    };

    // Packages to Spoof as Pixel XL
    private static final Map<String, Object> propsToChangePixelXL;
    private static final String[] packagesToChangePixelXL = {
        "com.google.android.apps.photos"
    };

    private static final Map<String, ArrayList<String>> propsToKeep;
    private static final String[] extraPackagesToChange = {
        "com.android.chrome",
        "com.breel.wallpapers20",
        "com.google.android.apps.wallpaper",
        "com.google.android.gm"
    };

    private static final String[] packagesToKeep = {
        "com.google.android.apps.cameralite",
        "com.google.android.apps.youtube.kids",
        "com.google.android.apps.youtube.music",
        "com.google.android.dialer",
        "com.google.android.GoogleCamera",
        "com.google.android.GoogleCamera.Cameight",
        "com.google.android.GoogleCamera.Go",
        "com.google.android.GoogleCamera.Urnyx",
        "com.google.android.GoogleCameraAsp",
        "com.google.android.GoogleCameraCVM",
        "com.google.android.GoogleCameraEng",
        "com.google.android.GoogleCameraEng2",
        "com.google.android.MTCL83",
        "com.google.android.UltraCVM",
        "com.google.android.youtube"
    };

    private static final String[] streamingPackagesToChange = {
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.netflix.mediaclient",
        "in.startv.hotstar"
    };

    // Packages to Spoof as Asus ROG Phone 1
    private static final Map<String, Object> propsToChangeROG1;
    private static final String[] packagesToChangeROG1 = {
        "com.dts.freefireth",
        "com.dts.freefiremax",
        "com.madfingergames.legends"
    };

    // Packages to Spoof as Xperia 5
    private static final Map<String, Object> propsToChangeXP5;
    private static final String[] packagesToChangeXP5 = {
        "com.activision.callofduty.shooter",
        "com.tencent.tmgp.kr.codm",
        "com.garena.game.codm",
        "com.vng.codmvn"
    };

    // Packages to Spoof as OnePlus 8 Pro
    private static final Map<String, Object> propsToChangeOP8P;
    private static final String[] packagesToChangeOP8P = {
        "com.riotgames.league.wildrift",
        "com.riotgames.league.wildrifttw",
        "com.riotgames.league.wildriftvn",
        "com.netease.lztgglobal",
        "com.pubg.imobile",
        "com.pubg.krmobile",
        "com.pubg.newstate",
        "com.rekoo.pubgm",
        "com.tencent.ig",
        "com.tencent.tmgp.pubgmhd",
        "com.vng.pubgmobile"
    };

    // Packages to Spoof as Mi 11 Ultra
    private static final Map<String, Object> propsToChangeMI11;
    private static final String[] packagesToChangeMI11 = {
        "com.mobile.legends",
        "com.tencent.tmgp.sgame"
    };

    // Codenames for currently supported Pixels by Google
    private static final String[] pixelCodenames = {
        "cheetah",
        "panther",
        "bluejay",
        "oriole",
        "raven",
        "barbet",
        "redfin",
        "bramble",
        "sunfish",
        "coral",
        "flame",
        "bonito",
        "sargo"
    };

    private static volatile boolean sIsGms = false;

    static {
        propsToKeep = new HashMap<>();
        propsToKeep.put("com.google.android.settings.intelligence", new ArrayList<>(Collections.singletonList("FINGERPRINT")));
        propsToChangePixel2 = new HashMap<>();
        propsToChangePixel2.put("BRAND", "google");
        propsToChangePixel2.put("MANUFACTURER", "Google");
        propsToChangePixel2.put("DEVICE", "walleye");
        propsToChangePixel2.put("PRODUCT", "walleye");
        propsToChangePixel2.put("HARDWARE", "walleye");
        propsToChangePixel2.put("MODEL", "Pixel 2");
        propsToChangePixel2.put("FINGERPRINT", "google/walleye/walleye:8.1.0/OPM1.171019.011/4448085:user/release-keys");
        propsToChangePixel5 = new HashMap<>();
        propsToChangePixel5.put("BRAND", "google");
        propsToChangePixel5.put("MANUFACTURER", "Google");
        propsToChangePixel5.put("DEVICE", "redfin");
        propsToChangePixel5.put("PRODUCT", "redfin");
        propsToChangePixel5.put("MODEL", "Pixel 5");
        propsToChangePixel5.put("FINGERPRINT", "google/redfin/redfin:13/TQ3A.230901.001/10750268:user/release-keys");
        propsToChangePixelXL = new HashMap<>();
        propsToChangePixelXL.put("BRAND", "google");
        propsToChangePixelXL.put("MANUFACTURER", "Google");
        propsToChangePixelXL.put("DEVICE", "marlin");
        propsToChangePixelXL.put("PRODUCT", "marlin");
        propsToChangePixelXL.put("MODEL", "Pixel XL");
        propsToChangePixelXL.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
        propsToChangeROG1 = new HashMap<>();
        propsToChangeROG1.put("MODEL", "ASUS_Z01QD");
        propsToChangeROG1.put("MANUFACTURER", "asus");
        propsToChangeXP5 = new HashMap<>();
        propsToChangeXP5.put("MODEL", "SO-52A");
        propsToChangeOP8P = new HashMap<>();
        propsToChangeOP8P.put("MODEL", "IN2020");
        propsToChangeOP8P.put("MANUFACTURER", "OnePlus");
        propsToChangeMI11 = new HashMap<>();
        propsToChangeMI11.put("BRAND", "Xiaomi");
        propsToChangeMI11.put("MANUFACTURER", "Xiaomi");
        propsToChangeMI11.put("DEVICE", "cmi");
        propsToChangeMI11.put("PRODUCT", "cmi");
        propsToChangeMI11.put("MODEL", "Mi 10 Pro");
    }

    public static void setProps(String packageName) {
        if (packageName == null) {
            return;
        }
        if (packageName.equals(PACKAGE_GMS)) {
            sIsGms = true;
        }
        boolean isPixelDevice = Arrays.asList(pixelCodenames).contains(SystemProperties.get(DEVICE));
        if (!isPixelDevice &&
            ((packageName.startsWith("com.google.") && !Arrays.asList(packagesToKeep).contains(packageName))
                || Arrays.asList(extraPackagesToChange).contains(packageName)
                || Arrays.asList(streamingPackagesToChange).contains(packageName))) {

            Map<String, Object> propsToChange = propsToChangePixel2;

            if (Arrays.asList(packagesToChangePixel5).contains(packageName)) {
                propsToChange = propsToChangePixel5;
            }

            if (Arrays.asList(packagesToChangePixelXL).contains(packageName)) {
                propsToChange = propsToChangePixelXL;
            }

            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            for (Map.Entry<String, Object> prop : propsToChange.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (propsToKeep.containsKey(packageName) && propsToKeep.get(packageName).contains(key)) {
                    if (DEBUG) Log.d(TAG, "Not defining " + key + " prop for: " + packageName);
                    continue;
                }
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
            // Set proper indexing fingerprint
            if (packageName.equals("com.google.android.settings.intelligence")) {
                setPropValue("FINGERPRINT", Build.VERSION.INCREMENTAL);
            }
        } else {

            if (Arrays.asList(packagesToChangeROG1).contains(packageName)) {
                if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
                for (Map.Entry<String, Object> prop : propsToChangeROG1.entrySet()) {
                    String key = prop.getKey();
                    Object value = prop.getValue();
                    setPropValue(key, value);
                }
            } else if (Arrays.asList(packagesToChangeXP5).contains(packageName)) {
                if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
                for (Map.Entry<String, Object> prop : propsToChangeXP5.entrySet()) {
                    String key = prop.getKey();
                    Object value = prop.getValue();
                    setPropValue(key, value);
                }
            } else if (Arrays.asList(packagesToChangeOP8P).contains(packageName)) {
                if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
                for (Map.Entry<String, Object> prop : propsToChangeOP8P.entrySet()) {
                    String key = prop.getKey();
                    Object value = prop.getValue();
                    setPropValue(key, value);
                }
            } else if (Arrays.asList(packagesToChangeMI11).contains(packageName)) {
                if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
                for (Map.Entry<String, Object> prop : propsToChangeMI11.entrySet()) {
                    String key = prop.getKey();
                    Object value = prop.getValue();
                    setPropValue(key, value);
                }
            }
        }
    }

    private static void setPropValue(String key, Object value) {
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (sIsGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}