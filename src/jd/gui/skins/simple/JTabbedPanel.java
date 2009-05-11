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

    abstract public void onDisplay();

    abstract public void onHide();
/**
 * Can be overwritten, if a JTabbedPanel resized without a viewport. for example: table panels
 * @return
 */
    public boolean needsViewport() {
       
        return true;
    }



}
