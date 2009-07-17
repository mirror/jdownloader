package jd.gui.swing.laf;

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
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

import java.awt.Insets;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDUtilities;

import com.jtattoo.plaf.AbstractLookAndFeel;
import com.jtattoo.plaf.BaseTheme;

public class LookAndFeelController {

    /**
     *Config parameter to store the users laf selection
     */
    public static final String PARAM_PLAF = "PLAF4";
    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean uiInitated = false;
    /**
     * Stores mac default LAF if found
     */
    private static LookAndFeelWrapper MAC_DEFAULT;

    /**
     * Collects all supported LAFs for the current system
     * 
     * @return
     */
    public static LookAndFeelWrapper[] getSupportedLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();

        ArrayList<LookAndFeelWrapper> ret = new ArrayList<LookAndFeelWrapper>();
        for (int i = 0; i < lafis.length; i++) {
            String clname = lafis[i].getClassName();

            if (clname.contains("Substance") && JDUtilities.getJavaVersion() >= 1.6) {
                ret.add(new LookAndFeelWrapper(lafis[i]));
            } else if (clname.contains("Synthetica")) {
                ret.add(new LookAndFeelWrapper(lafis[i]));
            } else if (clname.contains("goodie")) {
                // if (OSDetector.isLinux() || JDInitFlags.SWITCH_DEBUG) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName());
                ret.add(lafm);
                // }
            } else if (clname.startsWith("apple.laf")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Apple Aqua");
                MAC_DEFAULT = lafm;
                ret.add(lafm);
            } else if (clname.endsWith("WindowsLookAndFeel")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Windows Style");
                ret.add(lafm);
            } else if (clname.endsWith("MetalLookAndFeel") && OSDetector.isLinux()) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName("Light(Metal)");
                ret.add(lafm);
            } else if (clname.startsWith("com.jtattoo")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName());
                ret.add(lafm);

            } else if (JDInitFlags.SWITCH_DEBUG) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName() + "(debug)");
                ret.add(lafm);
            }

        }
        return ret.toArray(new LookAndFeelWrapper[] {});
    }

    /**
     * Returns the configured LAF and makes sure that this LAF is supported by
     * the system
     * 
     * @return
     */
    public static LookAndFeelWrapper getPlaf() {
        LookAndFeelWrapper ret = getPlaf0();
        for (LookAndFeelWrapper laf : getSupportedLookAndFeels()) {
            if (ret.getClassName().equals(laf.getClassName())) return ret;
        }

        return getDefaultLAFM();

    }

    public static LookAndFeelWrapper getPlaf0() {
        SubConfiguration config = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        Object plaf = config.getProperty(PARAM_PLAF, null);
        if (plaf == null) { return getDefaultLAFM(); }
        if (plaf instanceof LookAndFeelWrapper) {
            if (((LookAndFeelWrapper) plaf).getName() != null) {
                return (LookAndFeelWrapper) plaf;
            } else {
                plaf = getDefaultLAFM();
                config.setProperty(PARAM_PLAF, plaf);
                config.save();
                return (LookAndFeelWrapper) plaf;
            }
        } else if (plaf instanceof String) {
            for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
                if (lafi.getName().equals(plaf) || lafi.getName().equals("Substance" + plaf)) {
                    plaf = new LookAndFeelWrapper(lafi);
                    config.setProperty(PARAM_PLAF, plaf);
                    config.save();
                    return (LookAndFeelWrapper) plaf;
                }
            }
        }
        return getDefaultLAFM();
    }

    /**
     * Returns the default Look And Feel... may be os dependend
     * 
     * @return
     */
    private static LookAndFeelWrapper getDefaultLAFM() {
        return new LookAndFeelWrapper("com.jtattoo.plaf.acryl.AcrylLookAndFeel");

        // LookAndFeelWrapper[] sup = getSupportedLookAndFeels();
        // if (sup.length == 0) return new
        // LookAndFeelWrapper(UIManager.getSystemLookAndFeelClassName());
        // if (MAC_DEFAULT != null) return MAC_DEFAULT;
        // return sup[0];

    }

    /**
     * INstalls all Substance LookAndFeels
     */
    public static void installSubstance() {

        String pkg = "org/jvnet/substance/skin/";
        URL res = JDUtilities.getJDClassLoader().getResource(pkg);
        String url = new Regex(res, "(.*)\\!.*").getMatch(0);
        url = url.substring(4);
        try {
            File file = new File(new URL(url).toURI());

            JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
            JarEntry e;
            while ((e = jarFile.getNextJarEntry()) != null) {
                if (e.getName().startsWith(pkg)) {
                    String laf = new Regex(e.getName(), "org/jvnet/substance/skin/(.*?)LookAndFeel\\.class").getMatch(0);
                    if (laf != null) {

                        UIManager.installLookAndFeel(laf, "org.jvnet.substance.skin." + laf + "LookAndFeel");
                    }
                }

            }

        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    /**
     * INstalls all Synthetica Look and Feels (if found)
     */
    public static void installSynthetica() {
        // de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel
        String pkg = "de/javasoft/plaf/synthetica";
        URL res = JDUtilities.getJDClassLoader().getResource(pkg);
        String url = new Regex(res, "(.*)\\!.*").getMatch(0);
        url = url.substring(4);
        try {
            File file = new File(new URL(url).toURI());

            JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
            JarEntry e;
            while ((e = jarFile.getNextJarEntry()) != null) {
                if (e.getName().startsWith(pkg)) {
                    String laf = new Regex(e.getName(), "de/javasoft/plaf/synthetica/(.*?)LookAndFeel\\.class").getMatch(0);

                    if (laf != null) {

                        UIManager.installLookAndFeel(laf, "de.javasoft.plaf.synthetica." + laf + "LookAndFeel");
                    }
                }

            }
            // de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel
            // de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel

            UIManager.installLookAndFeel("SkyMetallic", "de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel");
            UIManager.installLookAndFeel("WhiteVision", "de.javasoft.plaf.synthetica.SyntheticaWhiteVisionLookAndFeel");
            UIManager.installLookAndFeel("SyntheticaBlackMoon", "de.javasoft.plaf.synthetica.SyntheticaBlackMoonLookAndFeel");
        } catch (Exception e) {
            JDLogger.exception(e);
        }
    }

    /**
     * setups the correct Look and Feel
     */
    public static void setUIManager() {
        if (uiInitated) return;

        uiInitated = true;

        // installJGoodies();
        installJTattoo();
        if (JDUtilities.getJavaVersion() >= 1.6) installSubstance();
        // installSynthetica();

        try {
            JDLogger.getLogger().info("Use Look & Feel: " + getPlaf().getClassName());

            preSetup(getPlaf().getClassName());
            UIManager.setLookAndFeel(getPlaf().getClassName());
            // overwrite defaults
            SubConfiguration cfg = SubConfiguration.getConfig(DEFAULT_PREFIX + "." + LookAndFeelController.getPlaf().getClassName());

            postSetup(getPlaf().getClassName());

            for (Iterator<Entry<String, Object>> it = cfg.getProperties().entrySet().iterator(); it.hasNext();) {
                Entry<String, Object> next = it.next();
                JDLogger.getLogger().info("Use special LAF Property: " + next.getKey() + " = " + next.getValue());
                UIManager.put(next.getKey(), next.getValue());
            }

        } catch (Exception e) {
            JDLogger.exception(e);
        }

    }

    /**
     * Executes laf dependend commands AFTER setting the laf
     * 
     * @param className
     */
    private static void postSetup(String className) {
        if (className.equals("com.jtattoo.plaf.acryl.AcrylLookAndFeel")) {
            AbstractLookAndFeel.setTheme(new jd.gui.swing.laf.ext.jtattoo.acryl.themes.AcrylJDTheme());

            // jd.gui.swing.laf.ext.jattoo.ui.BluredPopupUI
            // set own uis
            UIDefaults defaults = UIManager.getDefaults();
            defaults.put("PopupMenu.blurParameter", new int[] { 2, 2, 3 });
            defaults.put("PopupMenuAlpha", 0.7f);
            defaults.put("PopupMenuUI", "jd.gui.swing.laf.ext.jattoo.ui.BluredPopupUI");
            defaults.put("RootPaneUI", "jd.gui.swing.laf.ext.jtattoo.acryl.ui.AcrylRootPaneUI");
            defaults.put("CheckBoxUI", "jd.gui.swing.laf.ext.jattoo.ui.BaseJDCheckBoxUI");
            defaults.put("ButtonUI", "jd.gui.swing.laf.ext.jattoo.ui.BaseJDButtonUI");
            defaults.put("TabbedPane.tabInsets", new Insets(0, 5, 0, 5));
//            defaults.put("ProgressBar.selectionForeground", new Color(100, 100, 100));
            Properties props = new Properties();
            props.put("dynamicLayout", "on");
            props.put("logoString", "");
            props.put("textAntiAliasingMode", "GRAY");
            props.put("windowDecoration", "off");
            props.put("dynamicLayout", "on");
            props.put("textAntiAliasing", "off");
            BaseTheme.setProperties(props);
        }

        //  
        // JTattooUtils.setJTattooRootPane(this);

    }

    /*
     * Execvutes LAF dependen commands BEFORE initializing the LAF
     */
    private static void preSetup(String className) {
        if (className.equals("com.jtattoo.plaf.acryl.AcrylLookAndFeel")) {
            Properties props = new Properties();
            props.put("textAntiAliasingMode", "GRAY");
            props.put("windowDecoration", "off");
            props.put("dynamicLayout", "on");
            props.put("textAntiAliasing", "on");
            props.put("logoString", "JDownloader");
            com.jtattoo.plaf.acryl.AcrylLookAndFeel.setCurrentTheme(props);

        }
    }

    private static void installJTattoo() {

        UIManager.installLookAndFeel("AluminiumLookAndFeel", "com.jtattoo.plaf.aluminium.AluminiumLookAndFeel");
        UIManager.installLookAndFeel("AcrylLookAndFeel", "com.jtattoo.plaf.acryl.AcrylLookAndFeel");
        UIManager.installLookAndFeel("AeroLookAndFeel", "com.jtattoo.plaf.aero.AeroLookAndFeel");
        UIManager.installLookAndFeel("NoireLookAndFeel", "com.jtattoo.plaf.noire.NoireLookAndFeel");
        UIManager.installLookAndFeel("MintLookAndFeel", "com.jtattoo.plaf.mint.MintLookAndFeel");
        UIManager.installLookAndFeel("McWinLookAndFeel", "com.jtattoo.plaf.mcwin.McWinLookAndFeel");
        UIManager.installLookAndFeel("LunaLookAndFeel", "com.jtattoo.plaf.luna.LunaLookAndFeel");
        UIManager.installLookAndFeel("HiFiLookAndFeel", "com.jtattoo.plaf.hifi.HiFiLookAndFeel");
        UIManager.installLookAndFeel("FastLookAndFeel", "com.jtattoo.plaf.fast.FastLookAndFeel");
        UIManager.installLookAndFeel("BernsteinLookAndFeel", "com.jtattoo.plaf.bernstein.BernsteinLookAndFeel");
        UIManager.installLookAndFeel("SmartLookAndFeel", "com.jtattoo.plaf.smart.SmartLookAndFeel");
    }

    // private static void installJGoodies() {
    // // com.jgoodies.plaf.plastic.PlasticXPLookAndFeel
    //
    // UIManager.installLookAndFeel("JDownloaderDefault",
    // "com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
    // UIManager.installLookAndFeel("JDownloaderWindows",
    // "com.jgoodies.looks.windows.WindowsLookAndFeel");
    // }

}
