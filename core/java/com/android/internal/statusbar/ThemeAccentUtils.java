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

    // Vendor overlays to ignore
    public static final String[] BLACKLIST_VENDOR_OVERLAYS = {
        "SysuiDarkTheme",
        "Pixel",
        "DisplayCutoutEmulationCorner",
        "DisplayCutoutEmulationDouble",
        "DisplayCutoutEmulationNarrow",
        "DisplayCutoutEmulationWide",
    };

    // Stock dark theme package
    private static final String STOCK_DARK_THEME = "com.android.systemui.theme.dark";

    // Dark themes
    private static final String[] DARK_THEMES = {
        "com.android.system.theme.dark.havoc", // 0
        "com.android.settings.theme.dark.havoc", // 1
        "com.android.systemui.theme.dark.havoc", // 2
        "com.android.wellbeing.theme.dark.havoc", // 3
    };

    // Black themes
    private static final String[] BLACK_THEMES = {
        "com.android.system.theme.black.havoc", // 0
        "com.android.settings.theme.black.havoc", // 1
        "com.android.systemui.theme.black.havoc", // 2
        "com.android.wellbeing.theme.black.havoc", // 3
    };

    // Notification themes
    private static final String NOTIFICATION_DARK_THEME = "com.android.system.notification.dark";
    private static final String NOTIFICATION_BLACK_THEME = "com.android.system.notification.black";

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
        "com.accents.maniaamber", // 29
        "com.accents.coldyellow", // 30
        "com.accents.newhouseorange", // 31
        "com.accents.warmthorange", // 32
        "com.accents.burningred", // 33
        "com.accents.candyred", // 34
        "com.accents.palered", // 35
        "com.accents.hazedpink", // 36
        "com.accents.bubblegumpink", // 37
        "com.accents.trufilpink", // 38
        "com.accents.duskpurple", // 39
        "com.accents.illusionspurple", // 40
        "com.accents.spookedpurple", // 41
        "com.accents.notimppurple", // 42
        "com.accents.dreamypurple", // 43
        "com.accents.footprintpurple", // 44
        "com.accents.obfusbleu", // 45
        "com.accents.frenchbleu", // 46
        "com.accents.coldbleu", // 47
        "com.accents.heirloombleu", // 48
        "com.accents.paleblue", // 49
        "com.accents.holillusion", // 50
        "com.accents.stock", // 51
        "com.accents.seasidemint", // 52
        "com.accents.movemint", // 53
        "com.accents.extendedgreen", // 54
        "com.accents.diffdaygreen", // 55
        "com.accents.jadegreen", // 56
    };

    private static final String[] QS_TILE_THEMES = {
        "com.android.systemui.qstile.default", // 0
        "com.android.systemui.qstile.circlegradient", // 1
        "com.android.systemui.qstile.circletrim", // 2
        "com.android.systemui.qstile.cookie", // 3
        "com.android.systemui.qstile.dottedcircle", // 4
        "com.android.systemui.qstile.dualtonecircle", // 5
        "com.android.systemui.qstile.dualtonecircletrim", // 6
        "com.android.systemui.qstile.ink", // 7
        "com.android.systemui.qstile.inkdrop", // 8
        "com.android.systemui.qstile.mountain", // 9
        "com.android.systemui.qstile.ninja", // 10
        "com.android.systemui.qstile.oreo", // 11
        "com.android.systemui.qstile.oreocircletrim", // 12
        "com.android.systemui.qstile.oreosquircletrim", // 13
        "com.android.systemui.qstile.pokesign", // 14
        "com.android.systemui.qstile.squaremedo", // 15
        "com.android.systemui.qstile.squircle", // 16
        "com.android.systemui.qstile.squircletrim", // 17
        "com.android.systemui.qstile.teardrop", // 18
        "com.android.systemui.qstile.wavey", // 19
    };

    // QS header themes
    private static final String[] QS_HEADER_THEMES = {
        "com.android.systemui.qsheader.black", // 0
        "com.android.systemui.qsheader.grey", // 1
        "com.android.systemui.qsheader.lightgrey", // 2
        "com.android.systemui.qsheader.accent", // 3
        "com.android.systemui.qsheader.transparent", // 4
    };

    // UI themes
    private static final String[] UI_THEMES = {
        "com.android.system.pixel", // 0
        "com.android.system.aosp", // 1
    };

    // Switch themes
    private static final String[] SWITCH_THEMES = {
        "com.android.system.switch.stock", // 0
        "com.android.system.switch.md2", // 1
        "com.android.system.switch.oneplus", // 2
    };

    // Unloads dark notification theme
    private static void unloadDarkNotificationTheme(IOverlayManager om, int userId) {
        try {
            om.setEnabled(NOTIFICATION_DARK_THEME, false, userId);
        } catch (RemoteException e) {
        }
    }

    // Unloads black notification theme
    private static void unloadBlackNotificationTheme(IOverlayManager om, int userId) {
        try {
            om.setEnabled(NOTIFICATION_BLACK_THEME, false, userId);
        } catch (RemoteException e) {
        }
    }

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

    // Check for the dark notification theme
    public static boolean isUsingDarkNotificationTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(NOTIFICATION_DARK_THEME, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the black notification theme
    public static boolean isUsingBlackNotificationTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(NOTIFICATION_BLACK_THEME, userId);
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

    // Set light / dark notification theme
    public static void setNotificationTheme(IOverlayManager om, int userId,
                boolean useDarkTheme, boolean useBlackTheme, int notificationStyle) {
        if (notificationStyle == 1 || (notificationStyle == 0 && !useDarkTheme && !useBlackTheme)) {
            unloadDarkNotificationTheme(om, userId);
            unloadBlackNotificationTheme(om, userId);
        } else if (notificationStyle == 2 || (notificationStyle == 0 && useDarkTheme && !useBlackTheme)) {
            unloadBlackNotificationTheme(om, userId);
            try {
                om.setEnabled(NOTIFICATION_DARK_THEME, true, userId);
            } catch (RemoteException e) {
            }
        } else if (notificationStyle == 3 || (notificationStyle == 0 && !useDarkTheme && useBlackTheme)) {
            unloadDarkNotificationTheme(om, userId);
            try {
                om.setEnabled(NOTIFICATION_BLACK_THEME, true, userId);
            } catch (RemoteException e) {
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

    // Check for any QS tile styles overlay
    public static boolean isUsingQsTileStyles(IOverlayManager om, int userId, int qsstyle) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(QS_TILE_THEMES[qsstyle],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
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

    // Switches ui style to user selected.
    public static void updateUIStyle(IOverlayManager om, int userId, int uiStyle) {
        if (uiStyle == 0) {
            stockUIStyle(om, userId);
        } else {
            try {
                om.setEnabled(UI_THEMES[uiStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change ui theme", e);
            }
        }
    }

    // Switches ui style back to stock.
    public static void stockUIStyle(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < UI_THEMES.length; i++) {
            String uitheme = UI_THEMES[i];
            try {
                om.setEnabled(uitheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateSwitchStyle(IOverlayManager om, int userId, int switchStyle) {
        if (switchStyle == 2) {
            stockSwitchStyle(om, userId);
        } else {
            try {
                om.setEnabled(SWITCH_THEMES[switchStyle],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change switch theme", e);
            }
        }
    }

    public static void stockSwitchStyle(IOverlayManager om, int userId) {
        for (int i = 0; i < SWITCH_THEMES.length; i++) {
            String switchtheme = SWITCH_THEMES[i];
            try {
                om.setEnabled(switchtheme,
                        false /*disable*/, userId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
