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

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;

import com.android.internal.util.custom.OnTheGoUtils;
import com.android.systemui.R;

public class OnTheGoDialog extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        // Inflate the decor view, so the attributes below are not overwritten by the theme.
        window.getDecorView();
        window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.addPrivateFlags(WindowManager.LayoutParams.SYSTEM_FLAG_SHOW_FOR_ALL_USERS);
        window.setGravity(Gravity.BOTTOM);
        setTitle(R.string.onthego_label);

        setContentView(R.layout.onthego_dialog);

        Button cancelBtn = findViewById(R.id.button_cancel);
        cancelBtn.setOnClickListener(v -> {
            finish();
        });

        Button startBtn = findViewById(R.id.button_start);
        startBtn.setOnClickListener(v -> {
            startService();
            finish();
        });

        final ContentResolver resolver = this.getContentResolver();

        final SeekBar mSlider = (SeekBar) findViewById(R.id.alpha_slider);
        int value = Settings.System.getInt(resolver,
                Settings.System.ON_THE_GO_ALPHA, 50);
        mSlider.setProgress(value);
        mSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Settings.System.putInt(resolver,
                        Settings.System.ON_THE_GO_ALPHA, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        if (!OnTheGoUtils.hasFrontCamera(this)) {
            findViewById(R.id.onthego_front_camera).setVisibility(View.GONE);
        } else {
            final Switch mCamSwitch = (Switch) findViewById(R.id.onthego_camera_toggle);
            boolean useFrontCam = (Settings.System.getInt(resolver,
                    Settings.System.ON_THE_GO_CAMERA, 0) == 1);
            mCamSwitch.setChecked(useFrontCam);
            mCamSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    Settings.System.putInt(resolver,
                            Settings.System.ON_THE_GO_CAMERA, isChecked ? 1 : 0);
                }
            });
        }
    }

    private void startService() {
        ComponentName cn = new ComponentName("com.android.systemui",
                "com.android.systemui.custom.onthego.OnTheGoService");
        Intent startIntent = new Intent();
        startIntent.setComponent(cn);
        startIntent.setAction("start");
        this.startService(startIntent);
    }
}
