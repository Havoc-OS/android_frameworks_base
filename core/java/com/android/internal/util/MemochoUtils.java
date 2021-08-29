/*
 * Copyright (C) 2018 Potato Open Sauce Project
 * Copyright (C) 2021 Jyotiraditya Panda <jyotiraditya@aospa.co>
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

package com.android.internal.util;

import android.os.Handler;
import android.os.HandlerThread;

import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * Helper functions for uploading to Memochō (https://bin.kv2.dev/).
 */
public final class MemochoUtils {

    private static final String binUrl = "https://bin.kv2.dev";
    private static Handler mHandler;

    /**
     * Uploads {@code content} to Memochō
     *
     * @param content  the content to upload to Memochō
     * @param callback the callback to call on success / failure
     */
    public static void upload(String content, UploadResultCallback callback) {
        getHandler().post(() -> {
            try {
                URL url = new URL(binUrl);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Content-Type", "text/plain");
                urlConnection.setInstanceFollowRedirects(false);
                urlConnection.setDoOutput(true);

                try (OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream())) {
                    writer.write(content);
                    writer.flush();
                }

                String urlPath = "";
                if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_MOVED_TEMP) {
                    urlPath = urlConnection.getHeaderField("Location");
                }

                if (!urlPath.isEmpty()) {
                    callback.onSuccess(binUrl + urlPath);
                } else {
                    String msg = "Failed to upload to Memochō: No id retrieved";
                    callback.onFail(msg, new Exception(msg));
                }
                urlConnection.disconnect();
            } catch (Exception e) {
                String msg = "Failed to upload to Memochō";
                callback.onFail(msg, e);
            }
        });
    }

    private static Handler getHandler() {
        if (mHandler == null) {
            HandlerThread memochoThread = new HandlerThread("MemochoThread");
            if (!memochoThread.isAlive()) {
                memochoThread.start();
            }
            mHandler = new Handler(memochoThread.getLooper());
        }
        return mHandler;
    }

    public interface UploadResultCallback {
        void onSuccess(String url);

        void onFail(String message, Exception e);
    }
}
