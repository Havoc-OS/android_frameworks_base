/*
 * Copyright (C) 2018 CarbonROM
 * Copyright (C) 2018 Adin Kwok (adinkwok)
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

package com.android.systemui.qs.tiles

import android.content.Intent
import android.os.Looper
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.service.quicksettings.Tile
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.systemui.R
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.plugins.qs.QSTile.BooleanState
import com.android.systemui.plugins.qs.QSTile.SlashState
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.qs.QSHost
import com.android.systemui.qs.logging.QSLogger
import com.android.systemui.qs.tileimpl.QSTileImpl
import com.android.systemui.statusbar.policy.BatteryController
import javax.inject.Inject

class SmartPixelsTile @Inject constructor(
    host: QSHost,
    @Background backgroundLooper: Looper,
    @Main mainHandler: android.os.Handler,
    falsingManager: FalsingManager,
    metricsLogger: MetricsLogger,
    statusBarStateController: StatusBarStateController,
    activityStarter: ActivityStarter,
    qsLogger: QSLogger,
    private val mBatteryController: BatteryController
) : QSTileImpl<BooleanState>(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
        statusBarStateController, activityStarter, qsLogger), BatteryController.BatteryStateChangeCallback {

    companion object {
        const val TILE_SPEC = "smartpixels"
        val SMART_PIXELS_SETTINGS = Intent("android.settings.SMART_PIXELS_SETTINGS")
    }

    private var mSmartPixelsEnable = false
    private var mSmartPixelsOnPowerSave = false
    private var mLowPowerMode = false
    private var mListening = false

    override fun newTileState() = BooleanState()

    override protected fun handleSetListening(listening: Boolean) {
        if (listening) mBatteryController.addCallback(this) 
        else mBatteryController.removeCallback(this)
    }

    override fun isAvailable() = true

    override fun handleClick(v: View?) {
        val resolver = mContext.contentResolver
        val currentUserId = UserHandle.USER_CURRENT
        mSmartPixelsEnable = Settings.System.getIntForUser(resolver, Settings.System.SMART_PIXELS_ENABLE, 0, currentUserId) == 1
        mSmartPixelsOnPowerSave = Settings.System.getIntForUser(resolver, Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, currentUserId) == 1
        when {
            mLowPowerMode && mSmartPixelsOnPowerSave -> {
                Settings.System.putIntForUser(resolver, Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, currentUserId)
                Settings.System.putIntForUser(resolver, Settings.System.SMART_PIXELS_ENABLE, 0, currentUserId)
            }
            !mSmartPixelsEnable -> Settings.System.putIntForUser(resolver, Settings.System.SMART_PIXELS_ENABLE, 1, currentUserId)
            else -> Settings.System.putIntForUser(resolver, Settings.System.SMART_PIXELS_ENABLE, 0, currentUserId)
        }
        refreshState()
    }

    override fun getLongClickIntent() = SMART_PIXELS_SETTINGS

    override protected fun handleUpdateState(state: BooleanState, arg: Any?) {
        val resolver = mContext.contentResolver
        val currentUserId = UserHandle.USER_CURRENT
        mSmartPixelsEnable = Settings.System.getIntForUser(resolver, Settings.System.SMART_PIXELS_ENABLE, 0, currentUserId) == 1
        mSmartPixelsOnPowerSave = Settings.System.getIntForUser(resolver, Settings.System.SMART_PIXELS_ON_POWER_SAVE, 0, currentUserId) == 1
        state.icon = ResourceIcon.get(R.drawable.ic_qs_smart_pixels)
        state.slash = state.slash ?: SlashState()
        state.label = mContext.getString(R.string.quick_settings_smart_pixels)
        state.value = when {
            mLowPowerMode && mSmartPixelsOnPowerSave -> true
            mSmartPixelsEnable -> true
            else -> false
        }
        state.slash.isSlashed = !state.value
        state.state = if (state.value) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    }

    override fun getTileLabel() = mContext.getString(R.string.quick_settings_smart_pixels)

    override fun getMetricsCategory() = MetricsEvent.HAVOC_SETTINGS

    override fun onBatteryLevelChanged(level: Int, plugged: Boolean, charging: Boolean) {}

    override fun onPowerSaveChanged(isActive: Boolean) {
        mLowPowerMode = isActive
        refreshState()
    }
}

