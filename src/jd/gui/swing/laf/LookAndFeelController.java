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

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class LookAndFeelController {

    /**
     *Config parameter to store the users laf selection
     */
    public static final String PARAM_PLAF = "PLAF5";
    public static final String DEFAULT_PREFIX = "LAF_CFG";
    private static boolean uiInitated = false;

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
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName());
                ret.add(lafm);
            } else if (clname.startsWith("apple.laf")) {
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
        SubConfiguration config = GUIUtils.getConfig();
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
        // de.javasoft.plaf.synthetica.SyntheticaSkyMetallicLookAndFeel
        return new LookAndFeelWrapper("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel");
        // return new
        // LookAndFeelWrapper("de.javasoft.plaf.synthetica.SyntheticaBlackStarLookAndFeel");

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
     * setups the correct Look and Feel
     */
    public static void setUIManager() {
        if (uiInitated) return;

        uiInitated = true;

        install();
        try {

            JDLogger.getLogger().info("Use Look & Feel: " + getPlaf().getClassName());
            Thread.currentThread().setContextClassLoader(JDUtilities.getJDClassLoader());

            preSetup(getPlaf().getClassName());

            UIManager.put("ClassLoader", JDUtilities.getJDClassLoader());
            UIManager.setLookAndFeel(getPlaf().getClassName());
            UIManager.put("ClassLoader", JDUtilities.getJDClassLoader());

            // UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());

            // overwrite defaults
            SubConfiguration cfg = SubConfiguration.getConfig(DEFAULT_PREFIX + "." + LookAndFeelController.getPlaf().getClassName());

            postSetup(getPlaf().getClassName());

            for (Iterator<Entry<String, Object>> it = cfg.getProperties().entrySet().iterator(); it.hasNext();) {
                Entry<String, Object> next = it.next();
                JDLogger.getLogger().info("Use special LAF Property: " + next.getKey() + " = " + next.getValue());
                UIManager.put(next.getKey(), next.getValue());
            }

        } catch (Throwable e) {

            JDLogger.exception(e);
        }

    }

    /**
     * INstalls all Look and feels founmd in libs/laf/
     */
    private static void install() {
        for (File file : JDUtilities.getJDClassLoader().getLafs()) {
            try {
                JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
                JarEntry e;
                String cl;
                ArrayList<String> names = new ArrayList<String>();
                ArrayList<String> classes = new ArrayList<String>();
                while ((e = jarFile.getNextJarEntry()) != null) {
                    if (!e.getName().endsWith(".class") || e.getName().contains("$")) continue;
                    cl = e.getName().replace("/", ".");
                    cl = cl.substring(0, cl.length() - 6);
                    if (!cl.toLowerCase().endsWith("lookandfeel")) continue;
                    Class<?> clazz = JDUtilities.getJDClassLoader().loadClass(cl);
                    try {

                        if (LookAndFeel.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {

                            String name = clazz.getSimpleName().replace("LookAndFeel", "");
                            names.add(name);
                            classes.add(cl);
                            UIManager.installLookAndFeel(name, cl);

                        }
                    } catch (Throwable t) {

                        t.printStackTrace();
                    }

                }
                // first collect all. Of the jar contaisn errors, an exception
                // gets thrown and no laf is added (e.gh. substance for 1.5
                for (int i = 0; i < names.size(); i++) {
                    UIManager.installLookAndFeel(names.get(i), classes.get(i));
                }
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }

    }

    /**
     * Executes laf dependend commands AFTER setting the laf
     * 
     * @param className
     */
    private static void postSetup(String className) {

        UIManager.put("Synthetica​.rootPane​.titlePane​.menuButton​.useOriginalImageSize", Boolean.TRUE);

        //
        // JTattooUtils.setJTattooRootPane(this);

    }

    /*
     * Execvutes LAF dependen commands BEFORE initializing the LAF
     */
    private static void preSetup(String className) {
        Boolean windowDeco = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.DECORATION_ENABLED, true);
        UIManager.put("Synthetica.window.decoration", windowDeco);
        JFrame.setDefaultLookAndFeelDecorated(windowDeco);
        JDialog.setDefaultLookAndFeelDecorated(windowDeco);
        if (className.equalsIgnoreCase("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel")) {
            UIManager.put("Synthetica.window.decoration", false);
        }
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

}
