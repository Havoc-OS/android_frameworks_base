/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.keyguard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.View;
import android.widget.LinearLayout;

import androidx.constraintlayout.helper.widget.Flow;

import com.android.internal.util.LatencyTracker;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.statusbar.policy.DevicePostureController;
import com.android.keyguard.PasswordTextView.QuickUnlockListener;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;

public class KeyguardPinViewController
        extends KeyguardPinBasedInputViewController<KeyguardPINView> {
    private final KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    private final DevicePostureController mPostureController;
    private final DevicePostureController.Callback mPostureCallback = posture ->
            mView.onDevicePostureChanged(posture);
    private final LockPatternUtils mLockPatternUtils;
    private final View mDeleteButton;
    private boolean mDeleteButtonShowing = true;

    protected KeyguardPinViewController(KeyguardPINView view,
            KeyguardUpdateMonitor keyguardUpdateMonitor,
            SecurityMode securityMode, LockPatternUtils lockPatternUtils,
            KeyguardSecurityCallback keyguardSecurityCallback,
            KeyguardMessageAreaController.Factory messageAreaControllerFactory,
            LatencyTracker latencyTracker, LiftToActivateListener liftToActivateListener,
            EmergencyButtonController emergencyButtonController,
            FalsingCollector falsingCollector,
            DevicePostureController postureController) {
        super(view, keyguardUpdateMonitor, securityMode, lockPatternUtils, keyguardSecurityCallback,
                messageAreaControllerFactory, latencyTracker, liftToActivateListener,
                emergencyButtonController, falsingCollector);
        mKeyguardUpdateMonitor = keyguardUpdateMonitor;
        mPostureController = postureController;
        mLockPatternUtils = lockPatternUtils;
        mDeleteButton = mView.findViewById(R.id.delete_button);
    }

    @Override
    protected void onViewAttached() {
        super.onViewAttached();

        int passwordLength = mLockPatternUtils.getPinPasswordLength(
                KeyguardUpdateMonitor.getCurrentUser());

        mPasswordEntry.setQuickUnlockListener(new QuickUnlockListener() {
            public void onValidateQuickUnlock(String password) {
                if (password != null) {
                    int length = password.length();
                    if (length > 0) {
                        showDeleteButton(true, true);
                    } else if (length == 0) {
                        showDeleteButton(false, true);
                    }
                    if (length == passwordLength) {
                        verifyPasswordAndUnlock();
                    }
                }
            }
        });

        showDeleteButton(false, false);

        View okButton = mView.findViewById(R.id.key_enter);
        if (okButton != null) {
            /* show okButton only if password length is unset
               because quick unlock won't work */
            if (passwordLength != -1) {
                okButton.setVisibility(View.INVISIBLE);
                Flow flow = (Flow) mView.findViewById(R.id.flow1);
                if (flow != null) {
                    List<Integer> ids = Arrays.stream(flow.getReferencedIds())
                                            .boxed().collect(Collectors.toList());
                    Collections.swap(ids, 9 /* delete_button */, 11 /* key_enter */);
                    flow.setReferencedIds(ids.stream().mapToInt(i -> i).toArray());
                }
            }
        }

        View cancelBtn = mView.findViewById(R.id.cancel_button);
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(view -> {
                getKeyguardSecurityCallback().reset();
                getKeyguardSecurityCallback().onCancelClicked();
            });
        }

        mPostureController.addCallback(mPostureCallback);
    }

    @Override
    protected void onViewDetached() {
        super.onViewDetached();
        mPostureController.removeCallback(mPostureCallback);
    }

    @Override
    public boolean startDisappearAnimation(Runnable finishRunnable) {
        return mView.startDisappearAnimation(
                mKeyguardUpdateMonitor.needsSlowUnlockTransition(), finishRunnable);
    }

    private void showDeleteButton(boolean show, boolean animate) {
        int visibility = show ? View.VISIBLE : View.INVISIBLE;
        if (mDeleteButton != null && mDeleteButtonShowing != show) {
            mDeleteButtonShowing = show;
            if (animate) {
                mDeleteButton.setAlpha(show ? 0.0f : 1.0f);
                mDeleteButton.animate()
                    .alpha(show ? 1.0f : 0.0f)
                    .setDuration(show ? 250 : 450)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            if (show) mDeleteButton.setVisibility(visibility);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            if (!show) mDeleteButton.setVisibility(visibility);
                        }
                    });
            } else {
                mDeleteButton.setVisibility(visibility);
            }
        }
    }
}
