package com.android.systemui.qs.tiles;

import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemProperties;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.provider.Settings.System;
import android.widget.Switch;
import android.service.quicksettings.Tile;
import android.text.TextUtils;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.plugins.qs.QSIconView;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.QSHost;
import com.android.systemui.qs.tileimpl.QSTileImpl;

/** Quick settings tile: Enable/Disable ScreenStabilization **/
public class ScreenStabilizationTile extends QSTileImpl<BooleanState> {
    private ScreenStabilizationObserver mScreenStabilizationObserver;

    private boolean mListening;
    private boolean mScbStabEnable;

    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_screen_stabilization_enabled);

    public ScreenStabilizationTile(QSHost host) {
        super(host);
        mScreenStabilizationObserver = new ScreenStabilizationObserver(new Handler());
        mScreenStabilizationObserver.register();
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.SCREEN_STABILIZATION_SETTINGS);
    }

    @Override
    protected void handleClick() {
        ContentResolver resolver = mContext.getContentResolver();
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        Settings.System.putInt(resolver, Settings.System.STABILIZATION_ENABLE, (Settings.System.getInt(resolver, Settings.System.STABILIZATION_ENABLE, 0) == 1) ? 0:1);
    }

    @Override
    protected void handleSecondaryClick() {
        handleClick();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_stabilization_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        /*final Drawable mEnable = mContext.getDrawable(R.drawable.ic_screen_stabilization_enabled);
        final Drawable mDisable = mContext.getDrawable(R.drawable.ic_screen_stabilization_disabled);*/
        mScbStabEnable = (Settings.System.getIntForUser(
                mContext.getContentResolver(), Settings.System.STABILIZATION_ENABLE,
                          0, UserHandle.USER_CURRENT) == 1);
        if (state.slash == null) {
            state.slash = new SlashState();
        }
        state.label = mContext.getString(R.string.quick_settings_stabilization_label);
        state.icon = mIcon;
        state.expandedAccessibilityClassName = Switch.class.getName();
        state.contentDescription = state.label;
        if (mScbStabEnable) {
            state.value = true;
        } else {
            state.value = false;
        }
        state.slash.isSlashed = !state.value;
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.HAVOC_SETTINGS;
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
    }
	
    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.quick_settings_stabilization_on);
        } else {
            return mContext.getString(R.string.quick_settings_stabilization_off);
        }
    }
    
    private class ScreenStabilizationObserver extends ContentObserver {
	private ContentResolver mResolver;
        ScreenStabilizationObserver(Handler handler) {
            super(handler);
	    mResolver = mContext.getContentResolver();
        }

        public void register() {
            mResolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.STABILIZATION_ENABLE),
                    false, this, UserHandle.USER_ALL);
            refreshState();
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshState();
        }
    }
}