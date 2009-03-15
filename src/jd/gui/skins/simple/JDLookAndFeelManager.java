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

package jd.gui.skins.simple;

import java.io.Serializable;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import jd.config.SubConfiguration;
import jd.utils.JDUtilities;
import de.javasoft.plaf.synthetica.SyntheticaBlueMoonLookAndFeel;

public class JDLookAndFeelManager implements Serializable {

    private static final long serialVersionUID = -8056003135389551814L;
    public static final String PARAM_PLAF = "PLAF";
    private static boolean uiInitated = false;
    private String className;

    public static JDLookAndFeelManager[] getInstalledLookAndFeels() {
        LookAndFeelInfo[] lafis = UIManager.getInstalledLookAndFeels();
        JDLookAndFeelManager[] ret = new JDLookAndFeelManager[lafis.length];
        for (int i = 0; i < lafis.length; i++) {
            ret[i] = new JDLookAndFeelManager(lafis[i]);
        }
        return ret;
    }

    public static JDLookAndFeelManager getPlaf() {
        SubConfiguration config = JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME);
        Object plaf = config.getProperty(PARAM_PLAF, null);
        if (plaf == null) return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
        if (plaf instanceof JDLookAndFeelManager) {
            if (((JDLookAndFeelManager) plaf).className != null) {
                return (JDLookAndFeelManager) plaf;
            } else {
                plaf = new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
                config.setProperty(PARAM_PLAF, plaf);
                config.save();
                return (JDLookAndFeelManager) plaf;
            }
        } else if (plaf instanceof String) {
            for (LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels()) {
                if (lafi.getName().equals(plaf)) {
                    plaf = new JDLookAndFeelManager(lafi);
                    config.setProperty(PARAM_PLAF, plaf);
                    config.save();
                    return (JDLookAndFeelManager) plaf;
                }
            }
        }
        return new JDLookAndFeelManager(UIManager.getSystemLookAndFeelClassName());
    }

    public static void setUIManager() {
        if (uiInitated) return;
        uiInitated = true;
        try {
            // UIManager.setLookAndFeel(new SyntheticaStandardLookAndFeel());
            // UIManager.setLookAndFeel(new SyntheticaSilverMoonLookAndFeel());
           // UIManager.setLookAndFeel(new de.javasoft.plaf.synthetica.SyntheticaSimple2DLookAndFeel());
            //UIManager.setLookAndFeel(new SyntheticaBlueMoonLookAndFeel());
            // UIManager.setLookAndFeel( new SubstanceCremeLookAndFeel());
             UIManager.setLookAndFeel(getPlaf().getClassName());

        } catch (Exception e) {
            e.printStackTrace();
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
        return className.substring(className.lastIndexOf(".") + 1, className.length() - 11);
    }

}
