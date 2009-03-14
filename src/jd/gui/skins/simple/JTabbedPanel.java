package jd.gui.skins.simple;

import java.awt.LayoutManager;

import javax.swing.JPanel;

public abstract class JTabbedPanel extends JPanel {

    public JTabbedPanel(LayoutManager layout) {
        super(layout);
    }

    public JTabbedPanel() {
        super();
    }

    abstract public void onDisplay(int i);

    abstract public void onHide();
}
