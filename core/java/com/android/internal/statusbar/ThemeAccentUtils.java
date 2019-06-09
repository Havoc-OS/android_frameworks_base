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

    // Dark themes
    private static final String[] DARK_THEMES = {
        "com.android.system.overlay.dark", // 0
        "com.android.systemui.overlay.dark", // 1
        "com.android.settings.overlay.dark", // 2
        "com.android.settings.intelligence.overlay.dark", // 3
        "com.android.wellbeing.overlay.dark", // 4
    };

    // Shady themes
    private static final String[] SHADY_THEMES = {
        "com.android.system.overlay.shady", // 0
        "com.android.systemui.overlay.shady", // 1
        "com.android.settings.overlay.shady", // 2
        "com.android.settings.intelligence.overlay.shady", // 3
        "com.android.wellbeing.overlay.shady", // 4
    };

    // Glassy themes
    private static final String[] GLASSY_THEMES = {
        "com.android.system.overlay.glassy", // 0
        "com.android.systemui.overlay.glassy", // 1
        "com.android.settings.overlay.glassy", // 2
        "com.android.settings.intelligence.overlay.glassy", // 3
        "com.android.wellbeing.overlay.glassy", // 4
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
        "com.accents.oneplusred", // 57
        "com.accents.usereight", // 58
        "com.accents.usernine", // 59
    };

    // Themes
    private static final String[] THEMES = {
        "default_theme", // 0
        "com.havoc.themes.vermilion", // 1
        "com.havoc.themes.firebrick", // 2
        "com.havoc.themes.americanrose", // 3
        "com.havoc.themes.cerise", // 4
        "com.havoc.themes.mauve", // 5
        "com.havoc.themes.amaranth", // 6
        "com.havoc.themes.violet", // 7
        "com.havoc.themes.indigo", // 8
        "com.havoc.themes.fuchsia", // 9
        "com.havoc.themes.amethyst", // 10
        "com.havoc.themes.ultramarine", // 11
        "com.havoc.themes.persianblue", // 12
        "com.havoc.themes.cobalt", // 13
        "com.havoc.themes.darkviolet", // 14
        "com.havoc.themes.royalblue", // 15
        "com.havoc.themes.unblue", // 16
        "com.havoc.themes.denim", // 17
        "com.havoc.themes.azure", // 18
        "com.havoc.themes.pinegreen", // 19
        "com.havoc.themes.darkturquoise", // 20
        "com.havoc.themes.aquamarine", // 21
        "com.havoc.themes.selectiveyellow", // 22
        "com.havoc.themes.ubuntuorange", // 23
        "com.havoc.themes.scarlet", // 24
        "com.havoc.themes.russet", // 25
        "com.havoc.themes.bistre", // 26
        "com.havoc.themes.slategrey", // 27
        "com.havoc.themes.arsenic", // 28
        "com.havoc.themes.grey", // 29
        "com.havoc.themes.darkaf", // 30
        "com.havoc.themes.almostblack", // 31
        "com.havoc.themes.black", // 32
        "com.havoc.themes.blackpearl", // 33
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
        "com.android.systemui.qstile.neonlike", // 20
        "com.android.systemui.qstile.oos", // 21
        "com.android.systemui.qstile.triangles", // 22
        "com.android.systemui.qstile.divided", // 23
        "com.android.systemui.qstile.cosmos" // 24
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

    // Check for the shady system theme
    public static boolean isUsingShadyTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(SHADY_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Check for the glassy system theme
    public static boolean isUsingGlassyTheme(IOverlayManager om, int userId) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(GLASSY_THEMES[0],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Set dark theme
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

    // Set shady theme
    public static void setShadyTheme(IOverlayManager om, int userId, boolean useShadyTheme) {
        for (String theme : SHADY_THEMES) {
                try {
                    om.setEnabled(theme,
                        useShadyTheme, userId);
                    unfuckBlackWhiteAccent(om, userId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Can't change theme", e);
                }
        }
    }

    // Set glassy theme
    public static void setGlassyTheme(IOverlayManager om, int userId, boolean useGlassyTheme) {
        for (String theme : GLASSY_THEMES) {
                try {
                    om.setEnabled(theme,
                        useGlassyTheme, userId);
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
            if (isUsingDarkTheme(om, userId) || isUsingShadyTheme(om, userId) || isUsingGlassyTheme(om, userId)) {
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
                // If using a dark theme we use the white accent, otherwise use the black accent
                if (isUsingDarkTheme(om, userId) || isUsingShadyTheme(om, userId)|| isUsingGlassyTheme(om, userId)) {
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

    // Check for any theme overlay
    public static boolean isUsingTheme(IOverlayManager om, int userId, int theme) {
        OverlayInfo themeInfo = null;
        try {
            themeInfo = om.getOverlayInfo(THEMES[theme],
                    userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return themeInfo != null && themeInfo.isEnabled();
    }

    // Switches theme from to another or back to stock
    public static void updateThemes(IOverlayManager om, int userId, int themeSetting) {
        if (themeSetting == 0) {
            unloadThemes(om, userId);
        } else {
            try {
                om.setEnabled(THEMES[themeSetting],
                        true, userId);
            } catch (RemoteException e) {
                Log.w(TAG, "Can't change theme", e);
            }
        }
    }

    // Unload all the themes
    public static void unloadThemes(IOverlayManager om, int userId) {
        // skip index 0
        for (int i = 1; i < THEMES.length; i++) {
            String theme = THEMES[i];
            try {
                om.setEnabled(theme,
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
