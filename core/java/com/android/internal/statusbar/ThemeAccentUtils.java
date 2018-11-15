/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.internal.statusbar;

import android.content.om.IOverlayManager;
import android.content.om.OverlayInfo;
import android.os.RemoteException;
import android.util.Log;

public class ThemeAccentUtils {
    public static final String TAG = "ThemeAccentUtils";

    // Stock dark theme package
    private static final String STOCK_DARK_THEME = "com.android.systemui.theme.dark";

    private static final String[] DARK_THEMES = {
        "com.android.system.theme.dark.havoc", // 0
        "com.android.settings.theme.dark.havoc", // 1
        "com.android.systemui.theme.dark.havoc", // 2
    };

    // Dark themes
    private static final String[] BLACK_THEMES = {
        "com.android.system.theme.black.havoc", // 0
        "com.android.settings.theme.black.havoc", // 1
        "com.android.systemui.theme.black.havoc", // 2
    };

    // Notification themes
    private static final String[] NOTIFICATION_THEMES = {
        "com.android.system.notification.light", // 0
        "com.android.system.notification.dark", // 1
        "com.android.system.notification.black", // 2
    };

    // Accents
    private static final String[] ACCENTS = {
        "default_accent", // 0
        "com.accents.red", // 1
        "com.accents.pink", // 2
        "com.accents.purple", // 3
        "com.accents.deeppurple", // 4
        "com.accents.indigo", // 5
        "com.accents.blue", // 6
        "com.accents.lightblue", // 7
        "com.accents.cyan", // 8
        "com.accents.teal", // 9
        "com.accents.green", // 10
        "com.accents.lightgreen", // 11
        "com.accents.lime", // 12
        "com.accents.yellow", // 13
        "com.accents.amber", // 14
        "com.accents.orange", // 15
        "com.accents.deeporange", // 16
        "com.accents.brown", // 17
        "com.accents.grey", // 18
        "com.accents.bluegrey", // 19
        "com.accents.black", // 20
        "com.accents.white", // 21
        "com.accents.userone", // 22
        "com.accents.usertwo", // 23
        "com.accents.userthree", // 24
        "com.accents.userfour", // 25
        "com.accents.userfive", // 26
        "com.accents.usersix", // 27
        "com.accents.userseven", // 28
    };

    private static final String[] QS_TILE_THEMES = {
        "com.android.systemui.qstile.default", // 0
        "com.android.systemui.qstile.circlegradient", // 1
        "com.android.systemui.qstile.circletrim", // 2
        "com.android.systemui.qstile.dottedcircle", // 3
        "com.android.systemui.qstile.dualtonecircle", // 4
        "com.android.systemui.qstile.dualtonecircletrim", // 5
        "com.android.systemui.qstile.ink", // 6
        "com.android.systemui.qstile.inkdrop", // 7
        "com.android.systemui.qstile.mountain", // 8
        "com.android.systemui.qstile.ninja", // 9
        "com.android.systemui.qstile.oreo", // 10
        "com.android.systemui.qstile.oreocircletrim", // 11
        "com.android.systemui.qstile.oreosquircletrim", // 12
        "com.android.systemui.qstile.pokesign", // 13
        "com.android.systemui.qstile.squaremedo", // 14
        "com.android.systemui.qstile.squircle", // 15
        "com.android.systemui.qstile.squircletrim", // 16
        "com.android.systemui.qstile.teardrop", // 17
        "com.android.systemui.qstile.wavey", // 18
    };

    // QS header themes
    private static final String[] QS_HEADER_THEMES = {
        "com.android.systemui.qsheader.black", // 0
        "com.android.systemui.qsheader.grey", // 1
        "com.android.systemui.qsheader.lightgrey", // 2
        "com.android.systemui.qsheader.accent", // 3
        "com.android.systemui.qsheader.transparent", // 4
    };

    // Check for the dark system theme
    public static boolean isUsingDarkTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(DARK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the black system theme
    public static boolean isUsingBlackTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(BLACK_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Set light / dark theme
    public static void setLightDarkTheme(IOverlayManager om, int userId, boolean useDarkTheme) {
        for (String theme : DARK_THEMES) {
                try {
                    om.setEnabled(theme,
                        useDarkTheme, userId);
                    unfuckBlackWhiteAccent(om, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
        }
    }

    // Set light / black theme
    public static void setLightBlackTheme(IOverlayManager om, int userId, boolean useBlackTheme) {
        for (String theme : BLACK_THEMES) {
                try {
                    om.setEnabled(theme,
                        useBlackTheme, userId);
                    unfuckBlackWhiteAccent(om, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
        }
    }

    // Check for black and white accent overlays
    public static void unfuckBlackWhiteAccent(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            if (isUsingDarkTheme(om, userId) || isUsingBlackTheme(om, userId)) {
                themeInfo = om.getOverlayInfo(ACCENTS[20],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[20],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                }
            } else {
                themeInfo = om.getOverlayInfo(ACCENTS[21],
                        userId);
                if (themeInfo != null && themeInfo.isEnabled()) {
                    om.setEnabled(ACCENTS[21],
                            false /*disable*/, userId);
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // Check for any accent overlay
    public static boolean isUsingAccent(IOverlayManager om, int userId, int accent) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(ACCENTS[accent],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Switches theme accent from to another or back to stock
    public static void updateAccents(IOverlayManager om, int userId, int accentSetting) {
        if (accentSetting == 0) {
            unloadAccents(om, userId);
        } else if (accentSetting < 20) {
            try {
                om.setEnabled(ACCENTS[accentSetting],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        } else if (accentSetting > 21) {
            try {
                om.setEnabled(ACCENTS[accentSetting],
                        true, userId);
            } catch (RemoteException e) {
            }
        } else if (accentSetting == 20) {
            try {
                // If using a dark, black theme we use the white accent, otherwise use the black accent
                if (isUsingDarkTheme(om, userId) || isUsingBlackTheme(om, userId)) {
                    om.setEnabled(ACCENTS[21],
                            true, userId);
                } else {
                    om.setEnabled(ACCENTS[20],
                            true, userId);
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }

    // Unload all the theme accents
    public static void unloadAccents(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < ACCENTS.length; i++) {
            String accent = ACCENTS[i];
            try {
                om.setEnabled(accent,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs tile style to user selected.
    public static void updateTileStyle(IOverlayManager om, int userId, int qsTileStyle) {
        if (qsTileStyle == 0) {
            stockTileStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_TILE_THEMES[qsTileStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs tile icon", e);
            }
        }
    }

    // Switches qs tile style back to stock.
    public static void stockTileStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < QS_TILE_THEMES.length; i++) {
            String qstiletheme = QS_TILE_THEMES[i];
            try {
                om.setEnabled(qstiletheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches notification style to user selected.
    public static void updateNotificationStyle(IOverlayManager om, int userId, int notificationStyle) {
        if (notificationStyle == 0) {
            stockNotificationStyle(om, userId);
        } else {
            try {
                om.setEnabled(NOTIFICATION_THEMES[notificationStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change notification theme", e);
            }
        }
    }

    // Switches notification style back to stock.
    public static void stockNotificationStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < NOTIFICATION_THEMES.length; i++) {
            String notificationtheme = NOTIFICATION_THEMES[i];
            try {
                om.setEnabled(notificationtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    // Switches qs header style to user selected.
    public static void updateQSHeaderStyle(IOverlayManager om, int userId, int qsHeaderStyle) {
        if (qsHeaderStyle == 0) {
            stockQSHeaderStyle(om, userId);
        } else {
            try {
                om.setEnabled(QS_HEADER_THEMES[qsHeaderStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change qs header theme", e);
            }
        }
    }

    // Switches qs header style back to stock.
    public static void stockQSHeaderStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < QS_HEADER_THEMES.length; i++) {
            String qsheadertheme = QS_HEADER_THEMES[i];
            try {
                om.setEnabled(qsheadertheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
