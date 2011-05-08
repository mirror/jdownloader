package jd.gui.swing.jdgui.views.settings.sidebar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.ColorUtils;
import org.jdownloader.extensions.AbstractExtensionWrapper;

public class ExtensionPanelListRenderer extends JPanel implements ListCellRenderer {

    private JLabel    lbl;
    private Font      orgFont;
    private Font      boldFont;
    private Color     f;
    private Color     b2;
    private Color     a;
    private JCheckBox cb;

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        AbstractExtensionWrapper ext = (AbstractExtensionWrapper) value;
        // AddonConfig.getInstance(plg.getSettings(), "", true)

        setText(ext.getName());
        setIcon(ext._getIcon(32));
        cb.setSelected(ext._isEnabled());
        lbl.setEnabled(cb.isSelected());

        if (isSelected) {
            lbl.setFont(boldFont);
            // lbl.setBorder(brd);
            setBackground(b2);
            // lbl.setForeground(b);
            setOpaque(true);
            // lbl.setForeground(b);

        } else {
            lbl.setFont(orgFont);
            if (index % 2 == 0) {
                setBackground(a);
                setOpaque(true);
            } else {
                setOpaque(false);
                setBackground(null);
            }
        }
        return this;
    }

    private void setIcon(Icon icon) {
        lbl.setIcon(icon);
    }

    private void setText(String name) {
        lbl.setText(name);
    }

    @Override
    public Dimension getPreferredSize() {
        return TreeRenderer.DIMENSION;
    }

    public ExtensionPanelListRenderer() {
        super(new MigLayout("ins 0 ,wrap 3", "[][grow,fill][]", "[]"));

        lbl = new JLabel();
        cb = new JCheckBox();
        add(cb, "aligny top,width 20!,sg border");
        add(lbl, "");
        add(Box.createGlue(), "sg border");

        lbl.setVerticalTextPosition(JLabel.BOTTOM);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        lbl.setHorizontalAlignment(JLabel.CENTER);
        orgFont = lbl.getFont();
        boldFont = lbl.getFont().deriveFont(Font.BOLD);

        // this.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        f = lbl.getForeground();
        b2 = ColorUtils.getAlphaInstance(f, 60);

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelHeaderColor();
        if (c >= 0) {
            b2 = ColorUtils.getAlphaInstance((new Color(c)), 230);
        }
        a = ColorUtils.getAlphaInstance(lbl.getForeground(), 4);
        lbl.setOpaque(false);
        setOpaque(false);
        setBackground(null);
        lbl.setBackground(null);

    }

}
