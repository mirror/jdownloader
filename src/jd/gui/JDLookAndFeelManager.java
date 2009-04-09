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

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.config.SubConfiguration;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class JDLookAndFeelManager implements Serializable {

    private static final long serialVersionUID = -8056003135389551814L;
    public static final String PARAM_PLAF = "PLAF";
    private static boolean uiInitated = false;
    private String className;

    public static JDLookAndFeelManager[] getSupportedLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();

        ArrayList<JDLookAndFeelManager> ret = new ArrayList<JDLookAndFeelManager>();
        for (int i = 0; i < lafis.length; i++) {
            String clname = lafis[i].getClassName();
            if (lafis[i].getClassName().endsWith("MetalLookAndFeel")) continue;
            if (lafis[i].getClassName().endsWith("NimbusLookAndFeel")) continue;
            if (lafis[i].getClassName().endsWith("MotifLookAndFeel")) continue;
            if (lafis[i].getClassName().contains("GTK")) continue;
            if (lafis[i].getClassName().endsWith("WindowsLookAndFeel")) continue;
            if (lafis[i].getClassName().endsWith("WindowsClassicLookAndFeel")) continue;
            if (lafis[i].getClassName().endsWith("WindowsLookAndFeel")) continue;
            if (lafis[i].getClassName().endsWith("WindowsLookAndFeel")) continue;
            if (lafis[i].getClassName().contains("Substance") && JDUtilities.getJavaVersion() >= 1.6) {
                ret.add(new JDLookAndFeelManager(lafis[i]));
            }

        }
        return ret.toArray(new JDLookAndFeelManager[] {});
    }
/**
 * Returns the configured LAF and makes sure that this LAF is supported by the system
 * @return
 */
    public static JDLookAndFeelManager getPlaf() {
        JDLookAndFeelManager ret = getPlaf0();
        for (JDLookAndFeelManager laf : getSupportedLookAndFeels()) {
            if (ret.getClassName().equals(ret.getClassName())) return ret;
        }

        return getDefaultLAFM();

    }

    public static JDLookAndFeelManager getPlaf0() {
        SubConfiguration config = JDUtilities.getSubConfig(SimpleGuiConstants.GUICONFIGNAME);
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
                if (lafi.getName().equals(plaf)||lafi.getName().equals("Substance"+plaf)) {
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

        return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
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

        installSubstance();
        try {
            UIManager.setLookAndFeel(getPlaf().getClassName());
        } catch (Exception e) {
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
        }
    }

    public JDLookAndFeelManager(LookAndFeelInfo lafi) {
     
        this.className = lafi.getClassName();
    }

    public JDLookAndFeelManager(String className) {
        this.className = className;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof JDLookAndFeelManager) && ((JDLookAndFeelManager) obj).getClassName() != null && ((JDLookAndFeelManager) obj).getClassName().equals(className);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        if (className == null) return null;
        return className.substring(className.lastIndexOf(".") + 1, className.length() - 11).replace("Substance", "");
    }

}
