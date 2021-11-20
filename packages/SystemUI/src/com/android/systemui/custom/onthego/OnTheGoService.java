/*
 * Copyright (C) 2014 The NamelessRom Project
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

package com.android.systemui.custom.onthego;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.IBinder;
import android.provider.Settings;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.internal.util.custom.OnTheGoUtils;
import com.android.systemui.R;

import java.io.IOException;

public class OnTheGoService extends Service {

    private static final int ONTHEGO_NOTIFICATION_ID = 81333378;
    private static final String ONTHEGO_CHANNEL_ID = "onthego_notif";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private static final int CAMERA_BACK = 0;
    private static final int CAMERA_FRONT = 1;

    private static final int NOTIFICATION_STARTED = 0;
    private static final int NOTIFICATION_ERROR = 1;

    private FrameLayout mOverlay;
    private Camera mCamera;
    private NotificationManager mNotificationManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetViews();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !OnTheGoUtils.hasCamera(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final String action = intent.getAction();

        if (action != null && !action.isEmpty()) {
            if (action.equals(ACTION_START)) {
                startOnTheGo();
            } else if (action.equals(ACTION_STOP)) {
                stopOnTheGo();
            }
        } else {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startOnTheGo() {
        if (mNotificationManager != null) {
            stopOnTheGo();
            return;
        }

        resetViews();
        setupViews();

        createNotification(NOTIFICATION_STARTED);
    }

    private void stopOnTheGo() {
        resetViews();

        // Cancel notification
        if (mNotificationManager != null) {
            mNotificationManager.cancel(ONTHEGO_NOTIFICATION_ID);
            mNotificationManager.deleteNotificationChannel(ONTHEGO_CHANNEL_ID);
            mNotificationManager = null;
        }

        stopSelf();
    }

    private void toggleOnTheGoAlpha() {
        int alpha = Settings.System.getInt(getContentResolver(),
                Settings.System.ON_THE_GO_ALPHA, 50);
        toggleOnTheGoAlpha(alpha);
    }

    private void toggleOnTheGoAlpha(int alpha) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.ON_THE_GO_ALPHA, alpha);

        if (mOverlay != null) {
            mOverlay.setAlpha(alpha / 100f);
        }
    }

    private void getCameraInstance(int type) throws RuntimeException {
        releaseCamera();

        if (!OnTheGoUtils.hasFrontCamera(this)) {
            mCamera = Camera.open();
            return;
        }

        switch (type) {
            // Get hold of the back facing camera
            default:
            case CAMERA_BACK:
                mCamera = Camera.open(0);
                break;
            // Get hold of the front facing camera
            case CAMERA_FRONT:
                final Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                final int cameraCount = Camera.getNumberOfCameras();

                for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
                    Camera.getCameraInfo(camIdx, cameraInfo);
                    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera = Camera.open(camIdx);
                    }
                }
                break;
        }
    }

    private void setupViews() {
        int cameraType = Settings.System.getInt(getContentResolver(),
                Settings.System.ON_THE_GO_CAMERA, 0);

        try {
            getCameraInstance(cameraType);
        } catch (Exception exc) {
            createNotification(NOTIFICATION_ERROR);
            stopOnTheGo();
        }

        final TextureView mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i2) {
                try {
                    if (mCamera != null) {
                        mCamera.setDisplayOrientation(90);
                        mCamera.setPreviewTexture(surfaceTexture);
                        mCamera.startPreview();
                    }
                } catch (IOException io) {}
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i2) {}

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                releaseCamera();
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {}
        });

        mOverlay = new FrameLayout(this);
        mOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mOverlay.addView(mTextureView);

        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION |
                        WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                PixelFormat.TRANSLUCENT
        );
        wm.addView(mOverlay, params);

        toggleOnTheGoAlpha();
    }

    private void resetViews() {
        releaseCamera();
        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (mOverlay != null) {
            mOverlay.removeAllViews();
            wm.removeView(mOverlay);
            mOverlay = null;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void createNotification(final int type) {
        final Resources r = getResources();
        final Notification.Builder builder = new Notification.Builder(this, ONTHEGO_CHANNEL_ID)
                .setTicker(r.getString(type == NOTIFICATION_ERROR ? R.string.onthego_notif_error : R.string.onthego_notif_ticker))
                .setContentTitle(r.getString(type == NOTIFICATION_ERROR ? R.string.onthego_notif_error : R.string.onthego_notif_title))
                .setSmallIcon(com.android.systemui.R.drawable.ic_onthego)
                .setWhen(System.currentTimeMillis())
                .setOngoing(type != NOTIFICATION_ERROR);

        if (type == NOTIFICATION_ERROR) {
            final ComponentName cn = new ComponentName("com.android.systemui",
                    "com.android.systemui.custom.onthego.OnTheGoService");
            final Intent startIntent = new Intent();
            startIntent.setComponent(cn);
            startIntent.setAction(ACTION_START);
            final PendingIntent startPendIntent = PendingIntent.getService(this, 0, startIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentText(getResources().getString(R.string.onthego_restart_text))
                    .setContentIntent(startPendIntent);
        } else {
            final Intent stopIntent = new Intent(this, OnTheGoService.class)
                    .setAction(OnTheGoService.ACTION_STOP);
            final PendingIntent stopPendIntent = PendingIntent.getService(this, 0, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentText(getResources().getString(R.string.screenrecord_stop_text))
                    .setContentIntent(stopPendIntent);
        }

        final Notification notif = builder.build();

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mNotificationChannel = new NotificationChannel(ONTHEGO_CHANNEL_ID,
                r.getString(R.string.onthego_label), NotificationManager.IMPORTANCE_LOW);
        mNotificationChannel.enableVibration(true);
        mNotificationManager.createNotificationChannel(mNotificationChannel);
        mNotificationManager.notify(ONTHEGO_NOTIFICATION_ID, notif);
    }
}
