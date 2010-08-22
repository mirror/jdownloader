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

package jd.gui.swing;

import java.awt.Cursor;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.config.ConfigGroup;
import jd.gui.swing.components.JDUnderlinedText;
import net.miginfocom.swing.MigLayout;

public final class Factory {

    /**
     * Don't let anyone instantiate this class.
     */
    private Factory() {
    }

    public static JPanel createHeader(final ConfigGroup group) {
        return createHeader(group.getName(), group.getIcon());
    }

    public static JPanel createHeader(final String name, final ImageIcon icon) {
        JLabel label = new JLabel("<html><u><b>" + name + "</b></u></html>");
        label.setIcon(icon);
        label.setIconTextGap(5);
        label.setBorder(null);

        final JPanel ret = new JPanel(new MigLayout("ins 0", "[]10[grow,fill]"));
        ret.add(label);
        ret.add(new JSeparator());
        ret.setOpaque(false);
        return ret;
    }

    public static JButton createButton(final String string, final Icon i) {
        return createButton(string, i, null);
    }

    public static JButton createButton(final String string, final Icon i, final ActionListener listener) {
        final JButton bt = new JButton(string);
        if (i != null) bt.setIcon(i);
        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        bt.setIconTextGap(5);
        if (listener != null) bt.addActionListener(listener);
        bt.addMouseListener(new JDUnderlinedText(bt));
        return bt;
    }

}
