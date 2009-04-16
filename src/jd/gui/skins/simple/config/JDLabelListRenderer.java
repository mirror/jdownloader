package jd.gui.skins.simple.config;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.config.container.JDLabelContainer;

public class JDLabelListRenderer extends JLabel implements ListCellRenderer {

    public JDLabelListRenderer() {
        setOpaque(false);
        // setHorizontalAlignment();
        // setVerticalAlignment(CENTER);
//       setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.RIGHT);

    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        setIcon(((JDLabelContainer) value).getIcon());
        setText(((JDLabelContainer) value).getLabel());
        setFont(list.getFont());

        return this;
    }
}
