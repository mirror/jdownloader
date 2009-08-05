package jd.gui.swing.components;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;

import jd.HostPluginWrapper;

public class IconListRenderer extends DefaultListCellRenderer {

    /**
     * 
     */
    private static final long serialVersionUID = -7406509479266128054L;

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setIcon(((HostPluginWrapper) value).getPlugin().getHosterIcon());
        return label;
    }
}
