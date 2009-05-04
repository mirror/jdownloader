package jd.gui.skins.simple.config;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.config.container.JDLabelContainer;
import jd.gui.skins.simple.SimpleGUI;

/**
 * Cellrenderer für Copmboboxen mit Bildern
 * 
 * @author coalado
 */
public class JDLabelListRenderer extends JLabel implements ListCellRenderer {

    private static final long serialVersionUID = 3607383089555373774L;

    public JDLabelListRenderer() {
        if (SimpleGUI.isSubstance()) {
            setOpaque(false);
        } else {
            setOpaque(true);
        }

        setHorizontalTextPosition(JLabel.RIGHT);
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        if (SimpleGUI.isSubstance()) {
            if (isSelected) {
                setOpaque(true);
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setOpaque(false);
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        } else {

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
        }

        if (index == -1) {
            setIcon(null);
        } else {
            setIcon(((JDLabelContainer) value).getIcon());
        }
        setText(((JDLabelContainer) value).getLabel());
        setFont(list.getFont());
        setToolTipText(((JDLabelContainer) value).getLabel());

        return this;
    }
}
