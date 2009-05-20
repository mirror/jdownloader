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

package jd.gui.skins.simple;

import java.awt.Image;

import javax.swing.JComponent;
import javax.swing.JRootPane;

import org.jvnet.substance.SubstanceRootPaneUI;

public class JDSubstanceUI extends SubstanceRootPaneUI {

    private JDSubstanceTitlePane titlePane;
    private Image logo;

    public JDSubstanceUI(Image mainMenuIcon) {
        logo = mainMenuIcon;
    }

    protected JComponent createTitlePane(JRootPane root) {
        return titlePane = new JDSubstanceTitlePane(root, this, logo);
    }

    public void setMainMenuIcon(Image mainMenuIcon) {
        logo = mainMenuIcon;
        if (titlePane != null)

        titlePane.setLogo(logo);
    }

    public void setToolTipText(String string) {
        if (titlePane != null) titlePane.setToolTipText(string);
    }

}
