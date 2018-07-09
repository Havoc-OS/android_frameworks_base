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

import android.app.ActivityManager;
import android.content.ComponentName;
import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Secure; 
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.content.Intent;
import android.provider.MediaStore;
import android.service.quicksettings.Tile;
import com.android.systemui.qs.SecureSetting; 

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import android.content.ComponentName;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EdgeGestureTile extends QSTileImpl<BooleanState> {
    private boolean mListening; 
    private final SecureSetting mSetting; 
	
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_edgegesture);
       
    public EdgeGestureTile(QSHost host) {
        super(host);
        mSetting = new SecureSetting(mContext, mHandler, Secure.EDGE_GESTURES_ENABLED ) { 
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
       return null;
    } 
 
    @Override 
    public CharSequence getTileLabel() { 
        return mContext.getString(R.string.edgegesture_title_tile); 
    } 
 
    @Override 
    protected void handleUpdateState(BooleanState state, Object arg) { 
        final int value = arg instanceof Integer ? (Integer)arg : mSetting.getValue(); 
        final boolean suspend = value != 0; 
        if (state.slash == null) {
            state.slash = new SlashState();
        }
        state.icon = mIcon;
        state.value = suspend;
        state.slash.isSlashed = !state.value;
        state.label = mContext.getString(R.string.edgegesture_title_tile);
        if (suspend) {
            state.contentDescription =  mContext.getString(
                    R.string.edgegesture_title_tile);
            state.state = Tile.STATE_ACTIVE;
        } else {
            state.contentDescription =  mContext.getString(
                    R.string.edgegesture_title_tile);
            state.state = Tile.STATE_INACTIVE;
        }
    }
 
    @Override 
    protected String composeChangeAnnouncement() { 
        if (mState.value) { 
            return mContext.getString( 
                    R.string.edgegesture_title_tile); 
        } else { 
            return mContext.getString( 
                    R.string.edgegesture_title_tile); 
        } 
    } 
 
    @Override 
    public int getMetricsCategory() { 
        return MetricsEvent.HAVOC_SETTINGS; 
    } 
 
}