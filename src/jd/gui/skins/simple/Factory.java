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

import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.config.ConfigGroup;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.components.JDUnderlinedText;
import jd.gui.skins.simple.components.JLinkButton;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class Factory {

    public static JPanel createHeader(ConfigGroup group) {
        return createHeader(group.getName(), group.getIcon());
    }

    public static JPanel createHeader(String name, ImageIcon icon) {
        JPanel ret = new JPanel(new MigLayout("ins 0", "[]10[grow,fill]3[]"));
        JLinkButton label;
        try {
            ret.add(label = new JLinkButton("<html><u><b>" + name + "</b></u></html>", icon, new URL("http://wiki.jdownloader.org/quickhelp/" + name.replace(" ", "-"))));
            label.setIconTextGap(8);
            label.setBorder(null);
        } catch (MalformedURLException e) {
            JDLogger.getLogger().log(Level.SEVERE, "Exception occured", e);
        }
        ret.add(new JSeparator());
        ret.add(new JLabel(JDTheme.II("gui.images.config.tip", 16, 16)));
        ret.setOpaque(false);
        ret.setBackground(null);
        return ret;
    }

    public static JButton createButton(String string, Icon i) {
        return createButton(string, i, null);
    }

    public static JButton createButton(String string, Icon i, ActionListener listener) {
        JButton bt;
        if (i != null) {
            bt = new JButton(string, i);
        } else {
            bt = new JButton(string);
        }

        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        if (listener != null) bt.addActionListener(listener);
        bt.addMouseListener(new JDUnderlinedText());
        return bt;
    }
}
