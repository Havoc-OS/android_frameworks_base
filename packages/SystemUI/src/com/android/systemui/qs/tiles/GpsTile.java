/*
 * Copyright (C) 2019 Havoc-OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.systemui.qs.tiles;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.widget.Switch;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.R.drawable;
import com.android.systemui.qs.QSHost;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.qs.QSTile.BooleanState;
import com.android.systemui.qs.tileimpl.QSTileImpl;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationChangeCallback;

public class GpsTile extends QSTileImpl<BooleanState> {
    private final Icon mIcon = ResourceIcon.get(R.drawable.ic_qs_gps);

    private static final Intent LOCATION_SETTINGS_INTENT =
            new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    private static final Map<Integer, Integer> SECONDARY_LABEL_FOR_LOCATION_STATES = initSecondaryLabelForLocationStates();

    private final LocationController mController;
    private final KeyguardMonitor mKeyguard;
    private final Callback mCallback = new Callback();

    public GpsTile(QSHost host) {
        super(host);
        mController = Dependency.get(LocationController.class);
        mKeyguard = Dependency.get(KeyguardMonitor.class);
    }
    
    private static final Map<Integer, Integer> initSecondaryLabelForLocationStates() {
        final HashMap<Integer, Integer> secondaryLabelForLocationStates = new HashMap<Integer, Integer>();
        
        secondaryLabelForLocationStates.put(Settings.Secure.LOCATION_MODE_BATTERY_SAVING,
            R.string.quick_settings_location_secondary_battery_saving);
        secondaryLabelForLocationStates.put(Settings.Secure.LOCATION_MODE_SENSORS_ONLY,
            R.string.quick_settings_location_secondary_gps_only);
        secondaryLabelForLocationStates.put(Settings.Secure.LOCATION_MODE_HIGH_ACCURACY,
            R.string.quick_settings_location_secondary_high_accuracy);
        secondaryLabelForLocationStates.put(Settings.Secure.LOCATION_MODE_OFF,
            R.string.quick_settings_secondary_location_off);
        
        return secondaryLabelForLocationStates;
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    private void handleClickInner() {
        final boolean wasEnabled = mState.value;
        mController.setGpsEnabled(!wasEnabled);
    }

    @Override
    protected void handleClick() {
        if (mKeyguard.isSecure() && mKeyguard.isShowing()) {
            Dependency.get(ActivityStarter.class).postQSRunnableDismissingKeyguard(() -> {
                mHost.openPanels();
                handleClickInner();
            });
            return;
        }

	handleClickInner();
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        if (mController == null) {
            return;
        }

        // Work around for bug 15916487: don't show location tile on top of lock screen. After the
        // bug is fixed, this should be reverted to only hiding it on secure lock screens:
        // state.visible = !(mKeyguard.isSecure() && mKeyguard.isShowing());
        checkIfRestrictionEnforcedByAdminOnly(state, UserManager.DISALLOW_SHARE_LOCATION);
        if (state.disabledByPolicy == false) {
            checkIfRestrictionEnforcedByAdminOnly(state, UserManager.DISALLOW_CONFIG_LOCATION);
        }
        
        final boolean gpsEnabled = mController.isGpsEnabled();
        final int currentMode = mController.getCurrentMode();
        
        state.value = gpsEnabled;
        state.label = mContext.getString(R.string.quick_settings_gps_label);
        state.secondaryLabel = mContext.getString(SECONDARY_LABEL_FOR_LOCATION_STATES.get(currentMode));
        state.icon = mIcon;
        state.state = state.value ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE;
        state.contentDescription = state.value
            ? mContext.getString(R.string.quick_settings_gps_on)
            : mContext.getString(R.string.quick_settings_gps_off);
        state.expandedAccessibilityClassName = Switch.class.getName();
    }
    
    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_gps_label);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_LOCATION;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_gps_changed_off);
        }
    }

    @Override
    public void handleSetListening(boolean listening) {
        if (mController == null) {
            return;
        }

        if (listening) {
            mController.addCallback(mCallback);
            mKeyguard.addCallback(mCallback);
        } else {
            mController.removeCallback(mCallback);
            mKeyguard.removeCallback(mCallback);
        }        
    }

    private final class Callback implements LocationChangeCallback, KeyguardMonitor.Callback {
        @Override
        public void onLocationSettingsChanged(boolean enabled) {
            refreshState();
        }

        @Override
        public void onKeyguardShowingChanged() {
            refreshState();
        }
    }
}
