package jd.gui.skins.simple;

import java.awt.LayoutManager;

import org.jdesktop.swingx.JXPanel;

public abstract class JTabbedPanel extends JXPanel {

    private static final long serialVersionUID = -7856570342778191232L;

    public JTabbedPanel(LayoutManager layout) {
        super(layout);
    }

    public JTabbedPanel() {
        super();
    }

    abstract public void onDisplay();

    abstract public void onHide();

}
