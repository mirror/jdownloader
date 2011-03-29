package org.jdownloader.extensions;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

public class Header extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public Header(String name, ImageIcon icon) {
        super(new MigLayout("ins 0", "[]10[grow,fill]"));
        JLabel label = new JLabel("<html><u><b>" + name + "</b></u></html>");
        label.setIcon(icon);
        label.setIconTextGap(5);
        label.setBorder(null);

        add(label);
        add(new JSeparator());
        setOpaque(false);
    }

}
