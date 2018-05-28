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
import android.telecom.TelecomManager;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

import com.android.systemui.Dependency;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.GlobalSetting;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import android.content.ComponentName;

import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CallTile extends QSTileImpl<BooleanState> {
    private boolean mListening;
    private final ActivityStarter mActivityStarter;
    static final int REQUEST_CALL = 1;

    public CallTile(QSHost host) {
        super(host);
        mActivityStarter = Dependency.get(ActivityStarter.class);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.HAVOC_SETTINGS;
    }

    private void dispatchCallIntent(Context context) {
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        if (callIntent.resolveActivity(context.getPackageManager()) != null) {
            mActivityStarter.postStartActivityDismissingKeyguard(callIntent, REQUEST_CALL);
        }
    }


    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.call_title_tile);
    }

    @Override
    protected void handleClick() {
        mHost.collapsePanels();
        dispatchCallIntent(mContext);
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }


    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_call);
        state.label = mContext.getString(R.string.call_title_tile);

    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }
}