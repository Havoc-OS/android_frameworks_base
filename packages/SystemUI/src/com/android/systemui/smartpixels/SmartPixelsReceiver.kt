/*
 * Copyright (C) 2018 CarbonROM
 * Copyright (C) 2023 the RisingOS Android Project
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

/*
 * Copyright (C) 2018 CarbonROM
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

package com.android.systemui.smartpixels

import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.PowerManager
import android.os.UserHandle
import android.provider.Settings
import android.util.Log

import com.android.systemui.CoreStartable
import com.android.systemui.dagger.SysUISingleton

import javax.inject.Inject

@SysUISingleton
class SmartPixelsReceiver @Inject constructor(private val mContext: Context) : BroadcastReceiver(), CoreStartable {

    private val TAG = "SmartPixelsReceiver"

    private val mHandler = Handler()
    private lateinit var mResolver: ContentResolver
    private lateinit var mPowerManager: PowerManager
    private lateinit var mSettingsObserver: SettingsObserver
    private lateinit var mSmartPixelsService: Intent
    private lateinit var mFilter: IntentFilter

    private var mEnabled = false
    private var mOnPowerSave = false
    private var mPowerSave = false
    private var mServiceRunning = false
    private var mRegisteredReceiver = false

    override fun start() {
        mSmartPixelsService = Intent(mContext, SmartPixelsService::class.java)

        mFilter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(Intent.ACTION_USER_FOREGROUND)
        }

        initiateSettingsObserver()
    }

    private fun registerReceiver() {
        mContext.registerReceiver(this, mFilter)
        mRegisteredReceiver = true
    }

    private fun unregisterReceiver() {
        mContext.unregisterReceiver(this)
        mRegisteredReceiver = false
    }

    private fun initiateSettingsObserver() {
        mResolver = mContext.contentResolver
        mSettingsObserver = SettingsObserver(mHandler).apply {
            observe()
            update()
        }
    }

    private inner class SettingsObserver(handler: Handler) : ContentObserver(handler) {

        init {
            mPowerManager = mContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        }

        fun observe() {
            mResolver.apply {
                registerContentObserver(Settings.System.getUriFor(Settings.System.SMART_PIXELS_ENABLE), false, this@SettingsObserver, UserHandle.USER_ALL)
                registerContentObserver(Settings.System.getUriFor(Settings.System.SMART_PIXELS_ON_POWER_SAVE), false, this@SettingsObserver, UserHandle.USER_ALL)
                registerContentObserver(Settings.System.getUriFor(Settings.System.SMART_PIXELS_PATTERN), false, this@SettingsObserver, UserHandle.USER_ALL)
                registerContentObserver(Settings.System.getUriFor(Settings.System.SMART_PIXELS_SHIFT_TIMEOUT), false, this@SettingsObserver, UserHandle.USER_ALL)
            }
        }

        override fun onChange(selfChange: Boolean) {
            update()
        }

        fun update() {
            mEnabled = Settings.System.getIntForUser(mResolver, Settings.System.SMART_PIXELS_ENABLE, 0, UserHandle.USER_CURRENT) == 1
            mOnPowerSave = Settings.System.getIntForUser(mResolver, Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, UserHandle.USER_CURRENT) == 1
            mPowerSave = mPowerManager.isPowerSaveMode

            if (mEnabled || mOnPowerSave) {
                if (!mRegisteredReceiver) registerReceiver()
            } else if (mRegisteredReceiver) {
                unregisterReceiver()
            }

            if (!mEnabled && mOnPowerSave) {
                when {
                    mPowerSave && !mServiceRunning -> {
                        mContext.startService(mSmartPixelsService)
                        mServiceRunning = true
                        Log.d(TAG, "Started Smart Pixels Service by Power Save enable")
                    }
                    !mPowerSave && mServiceRunning -> {
                        mContext.stopService(mSmartPixelsService)
                        mServiceRunning = false
                        Log.d(TAG, "Stopped Smart Pixels Service by Power Save disable")
                    }
                    mPowerSave && mServiceRunning -> {
                        mContext.stopService(mSmartPixelsService)
                        mContext.startService(mSmartPixelsService)
                        Log.d(TAG, "Restarted Smart Pixels Service by Power Save enable")
                    }
                }
            } else if (mEnabled && !mServiceRunning) {
                mContext.startService(mSmartPixelsService)
                mServiceRunning = true
                Log.d(TAG, "Started Smart Pixels Service by enable")
            } else if (!mEnabled && mServiceRunning) {
                mContext.stopService(mSmartPixelsService)
                mServiceRunning = false
                Log.d(TAG, "Stopped Smart Pixels Service by disable")
            } else if (mEnabled && mServiceRunning) {
                mContext.stopService(mSmartPixelsService)
                mContext.startService(mSmartPixelsService)
                Log.d(TAG, "Restarted Smart Pixels Service")
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        mSettingsObserver.update()
    }
}
