/*
 * Copyright (C) 2018 ABC ROM
 *               2023 the RisingOS android Project
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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.Nullable
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.nano.MetricsProto.MetricsEvent
import com.android.internal.util.custom.Utils
import com.android.systemui.Dependency
import com.android.systemui.R
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.plugins.ActivityStarter
import com.android.systemui.plugins.FalsingManager
import com.android.systemui.plugins.qs.QSIconView
import com.android.systemui.plugins.qs.QSTile.BooleanState
import com.android.systemui.plugins.statusbar.StatusBarStateController
import com.android.systemui.qs.QSHost
import com.android.systemui.qs.logging.QSLogger
import com.android.systemui.qs.tileimpl.QSTileImpl
import javax.inject.Inject

class SoundSearchTile @Inject constructor(
    host: QSHost,
    @Background backgroundLooper: Looper,
    @Main mainHandler: Handler,
    falsingManager: FalsingManager,
    metricsLogger: MetricsLogger,
    statusBarStateController: StatusBarStateController,
    activityStarter: ActivityStarter,
    qsLogger: QSLogger
) : QSTileImpl<BooleanState>(host, backgroundLooper, mainHandler, falsingManager, metricsLogger,
    statusBarStateController, activityStarter, qsLogger) {

    companion object {
        const val TILE_SPEC = "soundsearch"
    }

    override fun getMetricsCategory(): Int {
        return MetricsEvent.HAVOC_SETTINGS
    }

    override fun handleClick(@Nullable view: View?) {
        host.collapsePanels()
        val context: Context = mContext

        // Shazam
        if (Utils.isPackageInstalled(context, "com.shazam.android") ||
                Utils.isPackageInstalled(context, "com.shazam.encore.android")) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.action = "com.shazam.android.intent.actions.START_TAGGING"
            context.startActivity(intent)
        }
        // Soundhound
        else if (Utils.isPackageInstalled(context, "com.melodis.midomiMusicIdentifier.freemium") ||
                Utils.isPackageInstalled(context, "com.melodis.midomiMusicIdentifier")) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.action = "com.soundhound.android.ID_NOW_EXTERNAL"
            context.startActivity(intent)
        }
        // Google Search Music
        else if (Utils.isPackageInstalled(context, "com.google.android.googlequicksearchbox")) {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.action = "com.google.android.googlequicksearchbox.MUSIC_SEARCH"
            context.startActivity(intent)
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.quick_settings_sound_search_no_app),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun getLongClickIntent(): Intent? {
        return null
    }

    override fun getTileLabel(): CharSequence {
        return mContext.getString(R.string.quick_settings_sound_search)
    }

    override fun handleUpdateState(state: BooleanState, arg: Any?) {
        state.label = mContext.getString(R.string.quick_settings_sound_search)
        state.contentDescription = mContext.getString(R.string.quick_settings_sound_search)
        state.icon = ResourceIcon.get(R.drawable.ic_qs_sound_search)
        state.state = Tile.STATE_INACTIVE
    }

    override fun newTileState(): BooleanState {
        val state = BooleanState()
        state.handlesLongClick = false
        return state
    }

    override fun handleSetListening(listening: Boolean) {
    }
}
