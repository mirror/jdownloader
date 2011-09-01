//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.laf;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import jd.JDInitFlags;
import jd.controlling.JDLogger;
import jd.crypt.JDCrypt;
import jd.nutils.OSDetector;
import jd.utils.JDHexUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.tooltips.ExtTooltip;
import org.appwork.utils.Application;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LookAndFeelController {
    private static final String                DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL = "de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel";
    private static final LookAndFeelController INSTANCE                                                      = new LookAndFeelController();

    /**
     * get the only existing instance of LookAndFeelController. This is a
     * singleton
     * 
     * @return
     */
    public static LookAndFeelController getInstance() {
        return LookAndFeelController.INSTANCE;
    }

    private LookAndFeelWrapper[]           supportedLookAndFeels;
    private boolean                        defaultLAF;
    private LAFOptions                     lafOptions;
    private GraphicalUserInterfaceSettings config;

    /**
     * Create a new instance of LookAndFeelController. This is a singleton
     * class. Access the only existing instance by using {@link #getInstance()}.
     */
    private LookAndFeelController() {
        config = JsonConfig.create(GraphicalUserInterfaceSettings.class);
        this.supportedLookAndFeels = collectSupportedLookAndFeels();

    }

    public LookAndFeelWrapper[] getSupportedLookAndFeels() {
        return supportedLookAndFeels;
    }

    public LookAndFeelWrapper getPlaf() {
        String ret = config.getLookAndFeel();
        if (ret == null || "".equals(ret)) {
            ret = LookAndFeelController.DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL;
        }
        return new LookAndFeelWrapper(ret);
    }

    /**
     * Config parameter to store the users laf selection
     */

    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean     uiInitated     = false;

    /**
     * Collects all supported LAFs for the current system
     * 
     * @return
     */
    private LookAndFeelWrapper[] collectSupportedLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();

        ArrayList<LookAndFeelWrapper> ret = new ArrayList<LookAndFeelWrapper>();

        if (Application.getJavaVersion() >= 16000000 && LookAndFeelController.class.getResource("/org/pushingpixels/substance") != null) {
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Autumn", "org.pushingpixels.substance.api.skin.SubstanceAutumnLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Business Black Steel", "org.pushingpixels.substance.api.skin.SubstanceBusinessBlackSteelLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Business Blue Steel", "org.pushingpixels.substance.api.skin.SubstanceBusinessBlueSteelLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Business", "org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Challenger Deep", "org.pushingpixels.substance.api.skin.SubstanceChallengerDeepLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Creme Coffee", "org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Creme", "org.pushingpixels.substance.api.skin.SubstanceCremeLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Dust Coffee", "org.pushingpixels.substance.api.skin.SubstanceDustCoffeeLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Dust", "org.pushingpixels.substance.api.skin.SubstanceDustLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Emerald", "org.pushingpixels.substance.api.skin.SubstanceEmeraldDuskLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Gemini", "org.pushingpixels.substance.api.skin.SubstanceGeminiLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Graphite Aqua", "org.pushingpixels.substance.api.skin.SubstanceGraphiteAquaLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Graphite Glass", "org.pushingpixels.substance.api.skin.SubstanceGraphiteGlassLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Graphite", "org.pushingpixels.substance.api.skin.SubstanceGraphiteLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Magellan", "org.pushingpixels.substance.api.skin.SubstanceMagellanLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Mist Aqua", "org.pushingpixels.substance.api.skin.SubstanceMistAquaLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Mist Silver", "org.pushingpixels.substance.api.skin.SubstanceMistSilverLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Moderate", "org.pushingpixels.substance.api.skin.SubstanceModerateLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Nebula Brick Waöö", "org.pushingpixels.substance.api.skin.SubstanceNebulaBrickWallLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Nebula", "org.pushingpixels.substance.api.skin.SubstanceNebulaLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Office Blue", "org.pushingpixels.substance.api.skin.SubstanceOfficeBlue2007LookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Office Silver", "org.pushingpixels.substance.api.skin.SubstanceOfficeSilver2007LookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Raven", "org.pushingpixels.substance.api.skin.SubstanceRavenLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Sahara", "org.pushingpixels.substance.api.skin.SubstanceSaharaLookAndFeel")));
            ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Substance Twilight", "org.pushingpixels.substance.api.skin.SubstanceTwilightLookAndFeel")));

        }

        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Classic", "de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Black Moon", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Black Star", "de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Blue Ice", "de.javasoft.plaf.synthetica.SyntheticaBlueIceLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Blue Moon", "de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Blue Steel", "de.javasoft.plaf.synthetica.SyntheticaBlueSteelLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Mauve Metallic", "de.javasoft.plaf.synthetica.SyntheticaMauveMetallicLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Orange Metallic", "de.javasoft.plaf.synthetica.SyntheticaOrangeMetallicLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Silver Moon", "de.javasoft.plaf.synthetica.SyntheticaSilverMoonLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica JDownloader", LookAndFeelController.DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL)));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Sky Metallic", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica White Vision", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Black Eye", "de.javasoft.plaf.synthetica.SyntheticaBlackEyeLookAndFeel")));
        ret.add(new LookAndFeelWrapper(new LookAndFeelInfo("Synthetica Green Dream", "de.javasoft.plaf.synthetica.SyntheticaGreenDreamLookAndFeel")));

        for (int i = 0; i < lafis.length; i++) {
            String clname = lafis[i].getClassName();

            if ((clname.startsWith("apple.laf")) || (clname.startsWith("com.apple.laf"))) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Apple Aqua");
                ret.add(lafm);
            } else if (clname.endsWith("WindowsLookAndFeel")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Windows Style");
                ret.add(lafm);
            } else if (clname.endsWith("MetalLookAndFeel") && OSDetector.isLinux()) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Light(Metal)");
                ret.add(lafm);
            } else if (clname.endsWith("GTKLookAndFeel") && OSDetector.isLinux()) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Light(GTK)");
                ret.add(lafm);
            } else if (JDInitFlags.SWITCH_DEBUG) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName() + " [Debug]");
                ret.add(lafm);
            }

        }
        return ret.toArray(new LookAndFeelWrapper[] {});
    }

    /**
     * setups the correct Look and Feel
     */
    public void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;
        long t = System.currentTimeMillis();
        try {
            String laf = getPlaf().getClassName();
            Log.L.info("Use Look & Feel: " + laf);
            preSetup(laf);
            String str = null;
            try {
                str = NewTheme.I().getText("lafoptions/" + laf + ".json");
            } catch (final Throwable e) {
                Log.exception(e);
            }
            if (str != null) {
                lafOptions = JSonStorage.restoreFromString(str, new TypeRef<LAFOptions>() {
                }, new LAFOptions());
            } else {
                Log.L.warning("Not LAF Options found: " + laf + ".json");
                lafOptions = new LAFOptions();
            }
            defaultLAF = laf.equals(DE_JAVASOFT_PLAF_SYNTHETICA_SYNTHETICA_SIMPLE2D_LOOK_AND_FEEL);
            if (laf.contains("Synthetica")) {
                try {
                    /* special init for Synthetica */
                    de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setLookAndFeel(laf);
                    de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setExtendedFileChooserEnabled(false);
                } catch (Throwable e) {
                    Log.exception(e);
                }
            } else {
                /* init for all other laf */
                UIManager.setLookAndFeel(laf);
            }
            postSetup(laf);
        } catch (Throwable e) {
            JDLogger.exception(e);
        } finally {
            System.out.println("LAF init: " + (System.currentTimeMillis() - t));
        }
    }

    public LAFOptions getLAFOptions() {
        return lafOptions;
    }

    public boolean isDefaultLAF() {
        return defaultLAF;
    }

    /**
     * Returns if currently a substance look and feel is selected. Not very
     * fast.. do not use this in often used methods
     * 
     * @return
     */
    public static boolean isSubstance() {
        return UIManager.getLookAndFeel().getName().toLowerCase().contains("substance");
    }

    public static boolean isSynthetica() {
        return UIManager.getLookAndFeel().getName().toLowerCase().contains("synthetica");
    }

    /**
     * Executes laf dependend commands AFTER setting the laf
     * 
     * @param className
     */
    private void postSetup(String className) {
        int fontSize = config.getFontScaleFactor();
        String fontName = config.getFontName();

        ExtTooltip.createConfig(ExtTooltip.DEFAULT).setForegroundColor(getLAFOptions().getTooltipForegroundColor());
        if (isSynthetica()) {
            try {
                if ("default".equalsIgnoreCase(fontName)) fontName = de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFontName();
                de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.setFont(fontName, (de.javasoft.plaf.synthetica.SyntheticaLookAndFeel.getFontSize() * fontSize) / 100);
            } catch (final Throwable e) {
                Log.exception(e);
            }

        } else if (isSubstance()) {
            try {
                final JDSubstanceFontPolicy fp = new JDSubstanceFontPolicy(org.pushingpixels.substance.api.SubstanceLookAndFeel.getFontPolicy().getFontSet("substance", null), fontName, fontSize);
                org.pushingpixels.substance.api.SubstanceLookAndFeel.setFontPolicy(fp);
            } catch (Throwable e) {
                Log.exception(e);
            }
        } else {
            try {
                Font font = Font.getFont(fontName);
                for (Enumeration<Object> e = UIManager.getDefaults().keys(); e.hasMoreElements();) {
                    Object key = e.nextElement();
                    Object value = UIManager.get(key);
                    if (value instanceof Font) {
                        Font f = null;
                        if (font != null) {
                            f = font;
                        } else {
                            f = (Font) value;
                        }
                        UIManager.put(key, new FontUIResource(f.getName(), f.getStyle(), (f.getSize() * fontSize) / 100));
                    }
                }
            } catch (Throwable e) {
                Log.exception(e);
            }
        }
    }

    /**
     * Executes LAF dependend commands BEFORE initializing the LAF
     */
    private void preSetup(String className) {
        UIManager.put("Synthetica.window.decoration", false);
        UIManager.put("Synthetica.text.antialias", config.isTextAntiAliasEnabled());
        UIManager.put("Synthetica.font.respectSystemDPI", config.isFontRespectsSystemDPI());
        UIManager.put("Synthetica.font.scaleFactor", config.getFontScaleFactor());
        UIManager.put("Synthetica.animation.enabled", config.isAnimationEnabled());
        if (CrossSystem.isWindows()) {
            /* only windows opaque works fine */
            UIManager.put("Synthetica.window.opaque", config.isWindowOpaque());
        } else {
            /* must be true to disable it..strange world ;) */
            UIManager.put("Synthetica.window.opaque", true);
        }
        /*
         * NOTE: This Licensee Information may only be used by AppWork UG. If
         * you like to create derived creation based on this sourcecode, you
         * have to remove this license key. Instead you may use the FREE Version
         * of synthetica found on javasoft.de
         */
        try {
            String key = JDCrypt.decrypt(JDHexUtils.getByteArray("4a94286634a203ada63b87c54662227252490d6f10e421b7239c610138c53e4c51fc7c0a2a8a18a0a2c0a40191b1186f"), new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 12, 13, 14, 15, 16 });
            if (key != null) {
                String[] li = { "Licensee=AppWork UG", "LicenseRegistrationNumber=289416475", "Product=Synthetica", "LicenseType=Small Business License", "ExpireDate=--.--.----", "MaxVersion=2.999.999" };
                UIManager.put("Synthetica.license.info", li);
                UIManager.put("Synthetica.license.key", key);
            }
        } catch (final Throwable e) {
            JDLogger.exception(e);
        }
    }
}
