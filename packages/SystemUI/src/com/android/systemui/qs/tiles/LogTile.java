/*
 * Copyright (C) 2018 MiracleDROID
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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.internal.util.aicp.SuShell;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

public class LogTile extends QSTileImpl<BooleanState> {

    private int mLog = 0;
	private boolean mLogcat;
	private boolean mLogcatRadio;
	private boolean mKmsg;
	private boolean mDmsg;

    public LogTile(QSHost host) {
        super(host);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        if (mLog == 0) {
            mLog = 1;
        } else if (mLog == 1) {
            mLog = 2;
        } else if (mLog == 2) {
            mLog = 3;
        } else {
            mLog = 0;
        }
        refreshState();
    }

    @Override
    protected void handleLongClick() {
        if (mLog == 1) {
            mLogcat = false;
            mLogcatRadio = true;
            mKmsg = false;
            mDmsg = false;
        } else if (mLog == 2) {
            mLogcat = false;
            mLogcatRadio = false;
            mKmsg = true;
            mDmsg = false;
        } else if (mLog == 3) {
            mLogcat = false;
            mLogcatRadio = false;
            mKmsg = false;
            mDmsg = true;
        } else {
            mLogcat = true;
            mLogcatRadio = false;
            mKmsg = false;
            mDmsg = false;
        }
        new CreateLogTask().execute(mLogcat, mLogcatRadio, mKmsg, mDmsg);
    }

    public void makeLogcat() throws SuShell.SuDeniedException, IOException {
        final String LOGCAT_FILE = new File(Environment
            .getExternalStorageDirectory(), "LogCat.txt").getAbsolutePath();
        String command = "logcat -d";
        command += " > " + LOGCAT_FILE;
        SuShell.runWithSuCheck(command);
    }

    public void makeLogcatRadio() throws SuShell.SuDeniedException, IOException {
        final String LOGCAT_RADIO_FILE = new File(Environment
            .getExternalStorageDirectory(), "LogcatRadio.txt").getAbsolutePath();
        String command = "logcat -d -b radio";
        command += " > " + LOGCAT_RADIO_FILE;
        SuShell.runWithSuCheck(command);
    }

    public void makeKmsg() throws SuShell.SuDeniedException, IOException {
        final String KMSG_FILE = new File(Environment
            .getExternalStorageDirectory(), "KernelLog.txt").getAbsolutePath();
        String command = "cat /proc/last_kmsg";
        command += " > " + KMSG_FILE;
        SuShell.runWithSuCheck(command);
    }

    public void makeDmesg() throws SuShell.SuDeniedException, IOException {
        final String DMESG_FILE = new File(Environment
            .getExternalStorageDirectory(), "Dmesg.txt").getAbsolutePath();
        String command = "dmesg";
        command += " > " + DMESG_FILE;
        SuShell.runWithSuCheck(command);
    }

    private class CreateLogTask extends AsyncTask<Boolean, Void, Void> {

        private Exception mException = null;

        @Override
        protected Void doInBackground(Boolean... params) {
            try {
                if (params[0]) {
                    makeLogcat();
                }
                if (params[1]) {
                    makeLogcatRadio();
                }
                if (params[2]) {
                    makeKmsg();
                }
                if (params[3]) {
                    makeDmesg();
                }
            } catch (SuShell.SuDeniedException e) {
                mException = e;
            } catch (IOException e) {
                e.printStackTrace();
                mException = e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (mException instanceof SuShell.SuDeniedException) {
                Toast.makeText(mContext, mContext.getString(R.string.cannot_get_su),
                        Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    @Override
    public Intent getLongClickIntent() {
        return null;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_logcat_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.HAVOC_SETTINGS;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mLog == 1) {
            state.label = mContext.getString(R.string.quick_settings_logcatradio_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_log);
        } else if (mLog == 2) {
            state.label = mContext.getString(R.string.quick_settings_kmsg_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_log);
        } else if (mLog == 3) {
            state.label = mContext.getString(R.string.quick_settings_dmesg_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_log);
        } else {
            state.label = mContext.getString(R.string.quick_settings_logcat_label);
            state.icon = ResourceIcon.get(R.drawable.ic_qs_log);
        }
    }

    @Override
    public void handleSetListening(boolean listening) {
    }
}

