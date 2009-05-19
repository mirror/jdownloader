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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JToggleButton;

import jd.utils.JDLocale;
import jd.utils.JDTheme;

public class JDSeparator extends JToggleButton implements ActionListener {

    private static final long serialVersionUID = 3007033193590223026L;

    private String leftToolTip;
    private String rightToolTip;

    private ImageIcon left;
    private ImageIcon right;

    public JDSeparator() {
        leftToolTip = JDLocale.L("gui.tooltips.jdseparator", "Close sidebar");
        rightToolTip = JDLocale.L("gui.tooltips.jdseparator", "Open sidebar");

        left = JDTheme.II("gui.images.minimize.left", 5, 10);
        right = JDTheme.II("gui.images.minimize.right", 5, 10);

        setIcon(left);
        setSelectedIcon(right);

        setRolloverIcon(left);
        setRolloverSelectedIcon(right);

        setFocusable(false);
        setMinimized(false);

        addActionListener(this);
    }

    public void setMinimized(boolean b) {
        setSelected(b);
        setToolTipText(b ? rightToolTip : leftToolTip);
    }

    public void actionPerformed(ActionEvent e) {
        SimpleGUI.CURRENTGUI.hideSideBar(isSelected());
    }

}
