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
//     along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.swing.laf;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.crypt.JDCrypt;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.nutils.OSDetector;
import jd.utils.JDHexUtils;
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
                ret.add(new LookAndFeelWrapper(lafis[i]).setName(lafis[i].getName().replaceAll("([A-Z0-9]\\d*)", " $0").trim()));
            } else if (clname.contains("Synthetica")) {
                ret.add(new LookAndFeelWrapper(lafis[i]).setName(lafis[i].getName().replaceAll("([A-Z0-9]\\d*)", " $0").trim()));
            } else if (clname.contains("goodie")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName());
                ret.add(lafm);
                // Before Java 1.6 || with Java 1.6
            } else if ((clname.startsWith("apple.laf")) || (clname.startsWith("com.apple.laf"))) {
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
            } else if (clname.startsWith("com.jtattoo")) {
                LookAndFeelWrapper lafm = new LookAndFeelWrapper(lafis[i]);
                lafm.setName(lafis[i].getName());
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
        return new LookAndFeelWrapper("de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel");
    }

    /**
     * setups the correct Look and Feel
     */
    public static void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;

        install();
        try {
            String laf = getPlaf().getClassName();

            JDLogger.getLogger().info("Use Look & Feel: " + laf);

            preSetup(laf);

            UIManager.put("ClassLoader", JDUtilities.getJDClassLoader());
            if (laf.contains("Synthetica")) {

                // Sets the Synthetica Look and feel and avoids errors if the
                // synth laf is not loaded (no imports)
                try {
                    // LookAndFeelController.setUIManager();

                    Class<?> slaf = Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel");

                    Method method = slaf.getMethod("setLookAndFeel", new Class[] { String.class, boolean.class, boolean.class });

                    method.invoke(null, new Object[] { laf, false, false });

                    // disable extended filechooser. jd cares itself for setting
                    // the latestlocation
                    slaf.getMethod("setExtendedFileChooserEnabled", new Class[] { boolean.class }).invoke(null, false);

                } catch (InvocationTargetException e) {

                    // ON some systems (turkish) sntheticy throws bugs when
                    // inited for the SPlashscreen. this workaroudn disables the
                    // spashscreen and
                    // this the synthetica lafs work
                    JDLogger.exception(e);
                    try {
                        UIManager.setLookAndFeel(laf);
                    } catch (Exception e2) {
                        GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_SHOW_SPLASH, false);
                        GUIUtils.getConfig().save();
                        JDLogger.warning("Disabled Splashscreen cause it cases LAF errors");
                        JDLogger.exception(e2);
                        uiInitated = false;
                        return;
                    }
                }

                // SyntheticaLookAndFeel#setLookAndFeel(String className),
            } else {
                UIManager.setLookAndFeel(laf);
            }

            UIManager.put("ClassLoader", JDUtilities.getJDClassLoader());

            // UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());

            // overwrite defaults
            SubConfiguration cfg = SubConfiguration.getConfig(DEFAULT_PREFIX + "." + laf);
            postSetup(laf);
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
            } catch (Throwable e) {
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
        int fontsize = GUIUtils.getConfig().getIntegerProperty(JDGuiConstants.PARAM_GENERAL_FONT_SIZE, 100);
        if (isSynthetica()) {
            try {
                /*
                 * set default font to Dialog, so we can show japanese chars,
                 * note that java itself must have correct font mappings
                 */
                Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel").getMethod("setFont", new Class[] { String.class, int.class }).invoke(null, new Object[] { GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_GENERAL_FONT_NAME, "Dialog"), 12 });
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
            try {
                /* dynamic fontsize */
                String font = "" + Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel").getMethod("getFontName", new Class[] {}).invoke(null, new Object[] {});
                int fonts = (Integer) Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel").getMethod("getFontSize", new Class[] {}).invoke(null, new Object[] {});
                Class.forName("de.javasoft.plaf.synthetica.SyntheticaLookAndFeel").getMethod("setFont", new Class[] { String.class, int.class }).invoke(null, new Object[] { font, (fonts * fontsize) / 100 });
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
        } else if (isSubstance()) {
            try {
                /* set default Font to Dialog and change dynamic fontsize */
                Class.forName("jd.gui.swing.laf.SubstanceFontSet").getMethod("postSetup", new Class[] {}).invoke(null);
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
        } else {
            try {
                Font font = Font.getFont(GUIUtils.getConfig().getStringProperty(JDGuiConstants.PARAM_GENERAL_FONT_NAME, "Dialog"));
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
                        UIManager.put(key, new FontUIResource(f.getName(), f.getStyle(), (f.getSize() * fontsize) / 100));
                    }
                }
            } catch (Throwable e) {
                JDLogger.exception(e);
            }
        }
        //
        // JTattooUtils.setJTattooRootPane(this);
    }

    /**
     * Executes LAF dependend commands BEFORE initializing the LAF
     */
    private static void preSetup(String className) {
        Boolean windowDeco = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.DECORATION_ENABLED, true);
        UIManager.put("Synthetica.window.decoration", windowDeco);
        JFrame.setDefaultLookAndFeelDecorated(windowDeco);
        JDialog.setDefaultLookAndFeelDecorated(windowDeco);
        /*
         * NOTE: This Licensee Information may only be used by AppWork UG. If
         * you like to create derived creation based on this sourcecode, you
         * have to remove this license key. Instead you may use the FREE Version
         * of synthetica found on javasoft.de
         */
        String[] li = { "Licensee=AppWork UG", "LicenseRegistrationNumber=289416475", "Product=Synthetica", "LicenseType=Small Business License", "ExpireDate=--.--.----", "MaxVersion=2.999.999" };
        UIManager.put("Synthetica.license.info", li);
        UIManager.put("Synthetica.license.key", JDCrypt.decrypt(JDHexUtils.getByteArray("4a94286634a203ada63b87c54662227252490d6f10e421b7239c610138c53e4c51fc7c0a2a8a18a0a2c0a40191b1186f"), new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 11, 12, 13, 14, 15, 16 }));

        // if
        // (className.equalsIgnoreCase("de.javasoft.plaf.synthetica.SyntheticaStandardLookAndFeel"))
        // {
        // UIManager.put("Synthetica.window.decoration", false);
        // }
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

}
