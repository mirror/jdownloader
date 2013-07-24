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

import jd.gui.swing.jdgui.JDGui;

import org.appwork.swing.ExtJFrame;
import org.appwork.utils.Application;
import org.jdownloader.images.NewTheme;

/**
 * Dumme JFRame from which dialogs can inherit the icon. workaround for 1.5
 * 
 * @author Coalado
 */
public class DummyFrame extends ExtJFrame {

    public static JFrame PARENT;

    public static JFrame getDialogParent() {
        if (JDGui.getInstance() != null) return JDGui.getInstance().getMainFrame();
        if (PARENT == null) PARENT = new DummyFrame();
        return PARENT;
    }

    private static final long serialVersionUID = 5729536627803588177L;

    private DummyFrame() {
        super();

        if (Application.getJavaVersion() >= 16000000) {
            java.util.List<Image> list = new ArrayList<Image>();

            list.add(NewTheme.I().getImage("logo/logo_14_14", -1));
            list.add(NewTheme.I().getImage("logo/logo_15_15", -1));
            list.add(NewTheme.I().getImage("logo/logo_16_16", -1));
            list.add(NewTheme.I().getImage("logo/logo_17_17", -1));
            list.add(NewTheme.I().getImage("logo/logo_18_18", -1));
            list.add(NewTheme.I().getImage("logo/logo_19_19", -1));
            list.add(NewTheme.I().getImage("logo/logo_20_20", -1));
            list.add(NewTheme.I().getImage("logo/jd_logo_64_64", -1));
            this.setIconImages(list);
        } else {
            this.setIconImage(NewTheme.I().getImage("logo/logo_17_17", -1));
        }
    }

}
