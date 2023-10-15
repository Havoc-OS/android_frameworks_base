/*
 * Copyright (C) 2023 The Pixel Experience Project
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

import android.content.ContentResolver;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.Log;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.android.internal.util.ArrayUtils;

public class DeviceConfigUtils {

    private static final String TAG = DeviceConfigUtils.class.getSimpleName();

    private static String[] getDeviceConfigsOverride(){
        String[] globalDeviceConfigs =
            Resources.getSystem().getStringArray(com.android.internal.R.array.global_device_configs_override);
        String[] deviceConfigs =
            Resources.getSystem().getStringArray(com.android.internal.R.array.device_configs_override);
        String[] allDeviceConfigs = Arrays.copyOf(globalDeviceConfigs, globalDeviceConfigs.length + deviceConfigs.length);
        System.arraycopy(deviceConfigs, 0, allDeviceConfigs, globalDeviceConfigs.length, deviceConfigs.length);
        return allDeviceConfigs;
    }

    public static boolean shouldDenyDeviceConfigControl(String namespace, String property) {
        Log.d(TAG, "shouldAllowDeviceConfigControl, namespace=" + namespace + ", property=" + property);
        for (String p : getDeviceConfigsOverride()) {
            String[] kv = p.split("=");
            String fullKey = kv[0];
            String[] nsKey = fullKey.split("/");
            if (nsKey[0] == namespace && nsKey[1] == property){
                Log.d(TAG, "shouldAllowDeviceConfigControl, deny, namespace=" + namespace + ", property=" + property);
                return true;
            }
        }
        Log.d(TAG, "shouldAllowDeviceConfigControl, allow, namespace=" + namespace + ", property=" + property);
        return false;
    }

    public static Map<String, String> filterDeviceConfigs(String namespace, Map<String, String> keyValues) {
        Log.d(TAG, "shouldAllowDeviceConfigControl, namespace=" + namespace + ", properties=[" + String.join(",", keyValues.keySet()) + "]");
        Map<String, String> keyValuesNew = new HashMap();
        for (Map.Entry<String, String> entry : keyValues.entrySet()) {
            keyValuesNew.put(entry.getKey(), entry.getValue());
        }
        for (String p : getDeviceConfigsOverride()) {
            String[] kv = p.split("=");
            String fullKey = kv[0];
            String[] nsKey = fullKey.split("/");
            if (nsKey[0] == namespace){
                String key = nsKey[1];
                String value = "";
                if (kv.length > 1) {
                    value = kv[1];
                }
                keyValuesNew.put(key, value);
            }
        }
        return keyValuesNew;
    }

    public static void setDefaultProperties(ContentResolver contentResolver) {
        Log.d(TAG, "setDefaultProperties");
        for (String p : getDeviceConfigsOverride()) {
            String[] kv = p.split("=");
            String fullKey = kv[0];
            String[] nsKey = fullKey.split("/");

            String namespace = nsKey[0];
            String key = nsKey[1];
            String value = "";
            if (kv.length > 1) {
                value = kv[1];
            }
            Settings.Config.putString(contentResolver, namespace, key, value, false);
        }
    }
}
