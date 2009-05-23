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

package jd.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.JDInitFlags;
import jd.config.SubConfiguration;
import jd.config.container.JDLabelContainer;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class JDLookAndFeelManager implements Serializable, JDLabelContainer {

    private static final long serialVersionUID = -8056003135389551814L;
    public static final String PARAM_PLAF = "PLAF";
    private static boolean uiInitated = false;
    private static JDLookAndFeelManager MACDEFAULT;
    private String className;
    private String staticName;

    public static JDLookAndFeelManager[] getSupportedLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();

        ArrayList<JDLookAndFeelManager> ret = new ArrayList<JDLookAndFeelManager>();
        for (int i = 0; i < lafis.length; i++) {
            String clname = lafis[i].getClassName();
            // if (clname.endsWith("MetalLookAndFeel")) continue;
            // if (clname.endsWith("NimbusLookAndFeel")) continue;
            // if (clname.endsWith("MotifLookAndFeel")) continue;
            // if (clname.contains("GTK")) continue;
            // if (clname.endsWith("WindowsLookAndFeel")) continue;
            // if (clname.endsWith("WindowsClassicLookAndFeel")) continue;
            //       

            if (clname.contains("Substance") && JDUtilities.getJavaVersion() >= 1.6) {

                ret.add(new JDLookAndFeelManager(lafis[i]));
            } else if (clname.contains("Synthetica")) {
                ret.add(new JDLookAndFeelManager(lafis[i]));
            } else if (clname.contains("goodie")) {
                if (OSDetector.isLinux()) {
                    JDLookAndFeelManager lafm = new JDLookAndFeelManager(lafis[i]);
                    lafm.setName("JGoodies");
                    ret.add(lafm);
                    
                }
            } else if (clname.startsWith("apple.laf")) {

                JDLookAndFeelManager lafm = new JDLookAndFeelManager(lafis[i]);
                lafm.setName("Apple Aqua");
                MACDEFAULT = lafm;
                ret.add(lafm);
            } else if (clname.endsWith("WindowsLookAndFeel")) {
                JDLookAndFeelManager lafm = new JDLookAndFeelManager(lafis[i]);
                lafm.setName("Windows Style");
                ret.add(lafm);
            } else if (clname.endsWith("MetalLookAndFeel") && OSDetector.isLinux()) {
                JDLookAndFeelManager lafm = new JDLookAndFeelManager(lafis[i]);
                lafm.setName("Light(Metal)");
                ret.add(lafm);
            } else if (JDInitFlags.SWITCH_DEBUG) {
                JDLookAndFeelManager lafm = new JDLookAndFeelManager(lafis[i]);
                lafm.setName(lafis[i].getName() + "(debug)");
                ret.add(lafm);
            }

        }
        return ret.toArray(new JDLookAndFeelManager[] {});
    }

    private void setName(String string) {
        this.staticName = string;

    }

    /**
     * Returns the configured LAF and makes sure that this LAF is supported by
     * the system
     * 
     * @return
     */
    public static JDLookAndFeelManager getPlaf() {
        JDLookAndFeelManager ret = getPlaf0();
        for (JDLookAndFeelManager laf : getSupportedLookAndFeels()) {
            if (ret.getClassName().equals(laf.getClassName())) return ret;
        }

        return getDefaultLAFM();

    }

    public static JDLookAndFeelManager getPlaf0() {
        SubConfiguration config = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        Object plaf = config.getProperty(PARAM_PLAF, null);
        if (plaf == null) { return getDefaultLAFM(); }
        if (plaf instanceof JDLookAndFeelManager) {
            if (((JDLookAndFeelManager) plaf).className != null) {
                return (JDLookAndFeelManager) plaf;
            } else {
                plaf = getDefaultLAFM();
                config.setProperty(PARAM_PLAF, plaf);
                config.save();
                return (JDLookAndFeelManager) plaf;
            }
        } else if (plaf instanceof String) {
            for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
                if (lafi.getName().equals(plaf) || lafi.getName().equals("Substance" + plaf)) {
                    plaf = new JDLookAndFeelManager(lafi);
                    config.setProperty(PARAM_PLAF, plaf);
                    config.save();
                    return (JDLookAndFeelManager) plaf;
                }
            }
        }
        return getDefaultLAFM();
    }

    private static JDLookAndFeelManager getDefaultLAFM() {
        if (JDUtilities.getJavaVersion() >= 1.6) return new JDLookAndFeelManager("org.jvnet.substance.skin.SubstanceBusinessBlackSteelLookAndFeel");

        // return new
        // JDLookAndFeelManager("com.jgoodies.looks.windows.WindowsLookAndFeel"
        // );
        JDLookAndFeelManager[] sup = getSupportedLookAndFeels();
        if (sup.length == 0) return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
        if(MACDEFAULT!=null)return MACDEFAULT;
        return sup[0];

    }

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
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }
    }

    public static void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;

        installJGoodies();
        if (JDUtilities.getJavaVersion() >= 1.6) installSubstance();

        // installSynthetica();

        // System.setProperty(key, "Windows XP"); /*dieser eintrag sorgt dafür
        // das JD unter linux nicht mehr startet*/

        try {
            JDLogger.getLogger().info("Use Look & Feel: " + getPlaf().getClassName());
            UIManager.setLookAndFeel(getPlaf().getClassName());
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }

        // try {
        // UIManager.setLookAndFeel(
        // "com.jgoodies.looks.windows.WindowsLookAndFeel");
        // } catch (Exception e){
        //             
        // }

        // com.incors.plaf.alloy.AlloyLookAndFeel.setProperty("alloy.licenseCode"
        // , "2009/05/16#rechenmacher@appwork.org#19lvyj#157lsi");
        // try {
        // javax.swing.LookAndFeel alloyLnF = new
        // com.incors.plaf.alloy.AlloyLookAndFeel();
        // javax.swing.UIManager.setLookAndFeel(alloyLnF);
        // } catch (javax.swing.UnsupportedLookAndFeelException ex) {
        // // You may handle the exception here
        // }
    }

    private static void installJGoodies() {
        UIManager.installLookAndFeel("JGoodie", "com.jgoodies.looks.windows.WindowsLookAndFeel");
        // UIManager.installLookAndFeel("JGoodie Plastic",
        // "com.jgoodies.looks.plastic.PlasticLookAndFeel");
        // UIManager.installLookAndFeel("JGoodie Plastic 3D",
        // "com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
        // UIManager.installLookAndFeel("JGoodie Plastic XP",
        // "com.jgoodies.looks.plastic.PlasticXPLookAndFeel");

    }

    // @SuppressWarnings("unused")
    // private static void installSynthetica() {
    // try {
    // UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    //
    // String pkg = "de/javasoft/plaf/synthetica/";
    // Enumeration<URL> ress = null;
    // try {
    // ress = JDUtilities.getJDClassLoader().getResources(pkg);
    // } catch (IOException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    //
    // while (ress.hasMoreElements()) {
    // URL res = ress.nextElement();
    //
    // String url = new Regex(res, "(.*)\\!.*").getMatch(0);
    // url = url.substring(4);
    // try {
    // File file = new File(new URL(url).toURI());
    //
    // JarInputStream jarFile = new JarInputStream(new FileInputStream(file));
    // JarEntry e;
    // while ((e = jarFile.getNextJarEntry()) != null) {
    // System.out.println(e.getName());
    // if (e.getName().startsWith(pkg)) {
    //
    // String laf = new Regex(e.getName(),
    // "de/javasoft/plaf/synthetica/Synthetica(.*?)LookAndFeel\\.class").getMatch
    // (0);
    //
    // if (laf != null) {
    //
    // UIManager.installLookAndFeel(laf,
    // "de.javasoft.plaf.synthetica.Synthetica" + laf + "LookAndFeel");
    // }
    // }
    //
    // }
    //
    // } catch (Exception e) {
    // jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,
    // "Exception occured", e);
    // }
    // }
    //
    // }

    public JDLookAndFeelManager(LookAndFeelInfo lafi) {

        this.className = lafi.getClassName();
    }

    public JDLookAndFeelManager(String className) {
        this.className = className;
    }

    // @Override
    public boolean equals(Object obj) {
        return (obj instanceof JDLookAndFeelManager) && ((JDLookAndFeelManager) obj).getClassName() != null && ((JDLookAndFeelManager) obj).getClassName().equals(className);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    // @Override
    public String toString() {
        if (className == null) return null;
        if (staticName != null) return staticName;
        return className.substring(className.lastIndexOf(".") + 1, className.length() - 11).replace("Substance", "");
    }

    public ImageIcon getIcon() {
        try {

            ImageIcon img = JDImage.getImageIcon(JDTheme.V("plaf.screenshot." + this.getClassName()));
            // if(img!=null)img=JDImage.getScaledImageIcon(img,100,100);
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public String getLabel() {

        return toString();
    }

    public boolean isJGoodies() {
        // TODO Auto-generated method stub
        return this.className.contains("jgoodie");
    }

    public boolean isSubstance() {
        return this.className.contains("substance");

    }

}
