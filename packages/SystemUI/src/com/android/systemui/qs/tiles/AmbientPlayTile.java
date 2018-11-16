/*
 * Copyright (C) 2018 Havoc-OS
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

package com.android.systemui.qs.tiles;

import android.content.ComponentName;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Settings.System;
import android.service.quicksettings.Tile;
import com.android.systemui.qs.SystemSetting; 

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

import com.android.systemui.R;

public class AmbientPlayTile extends QSTileImpl<BooleanState> {
    private boolean mListening; 
    private final SystemSetting mSetting; 
	
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_music_note_24dp);
       
    public AmbientPlayTile(QSHost host) {
        super(host);
        mSetting = new SystemSetting(mContext, mHandler, System.AMBIENT_RECOGNITION) { 
            @Override 
            protected void handleValueChanged(int value, boolean observedChange) { 
                handleRefreshState(value); 
            } 
        }; 
    }
    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }

    @Override 
    public BooleanState newTileState() { 
        return new BooleanState(); 
    } 
 
    @Override 
    public void handleClick() { 
        mSetting.setValue(mState.value ? 0 : 1); 
        refreshState(); 
    } 
 
    @Override 
    public Intent getLongClickIntent() { 
        return new Intent().setComponent(new ComponentName(
            "com.android.settings", "com.android.settings.Settings$AmbientPlayHistoryActivity"));
    } 
 
    @Override 
    public CharSequence getTileLabel() { 
        return mContext.getString(com.android.internal.R.string.ambient_recognition_notification); 
    } 
 
    @Override 
    protected void handleUpdateState(BooleanState state, Object arg) { 
        final int value = arg instanceof Integer ? (Integer)arg : mSetting.getValue(); 
        final boolean play = value != 0; 
        if (state.slash == null) {
            state.slash = new SlashState();
        }
        state.icon = mIcon;
        state.value = play;
        state.slash.isSlashed = !state.value;
        state.label = mContext.getString(com.android.internal.R.string.ambient_recognition_notification);
        if (play) {
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.state = Tile.STATE_INACTIVE;
        }
    }
 
    @Override 
    public int getMetricsCategory() { 
        return MetricsEvent.HAVOC_SETTINGS; 
    }
}
