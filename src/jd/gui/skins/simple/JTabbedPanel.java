package jd.gui.skins.simple;

import java.awt.LayoutManager;

import javax.swing.JPanel;

public abstract class JTabbedPanel extends JPanel {

    private static final long serialVersionUID = -7856570342778191232L;

    public JTabbedPanel(LayoutManager layout) {
        super(layout);
    }

    public JTabbedPanel() {
        super();
    }

    abstract public void onDisplay(int i);

    abstract public void onHide();

}
