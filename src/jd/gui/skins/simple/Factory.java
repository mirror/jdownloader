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
import jd.http.Encoding;
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
            ret.add(label = new JLinkButton("<html><u><b>" + name + "</b></u></html>", icon, new URL("http://wiki.jdownloader.org/?do=search&id=" + Encoding.urlEncode(name))));
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
