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
