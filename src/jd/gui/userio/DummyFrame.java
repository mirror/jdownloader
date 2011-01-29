//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.userio;

import java.awt.Image;
import java.util.ArrayList;

import javax.swing.JFrame;

import jd.gui.swing.SwingGui;
import jd.nutils.JDImage;

import org.appwork.utils.Application;

/**
 * Dumme JFRame from which dialogs can inherit the icon. workaround for 1.5
 * 
 * @author Coalado
 */
public class DummyFrame extends JFrame {

    public static JFrame PARENT;

    public static JFrame getDialogParent() {
        if (SwingGui.getInstance() != null) return SwingGui.getInstance().getMainFrame();
        if (PARENT == null) PARENT = new DummyFrame();
        return PARENT;
    }

    private static final long serialVersionUID = 5729536627803588177L;

    private DummyFrame() {
        super();

        if (Application.getJavaVersion() >= 16000000) {
            ArrayList<Image> list = new ArrayList<Image>();
            list.add(JDImage.getImage("logo/logo_14_14"));
            list.add(JDImage.getImage("logo/logo_15_15"));
            list.add(JDImage.getImage("logo/logo_16_16"));
            list.add(JDImage.getImage("logo/logo_17_17"));
            list.add(JDImage.getImage("logo/logo_18_18"));
            list.add(JDImage.getImage("logo/logo_19_19"));
            list.add(JDImage.getImage("logo/logo_20_20"));
            list.add(JDImage.getImage("logo/jd_logo_64_64"));
            this.setIconImages(list);
        } else {
            this.setIconImage(JDImage.getImage("logo/logo_17_17"));
        }
    }

}
