package jd.gui.skins.simple;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;

public class ContentPanel extends JXPanel {
    /**
     * 
     */
    private static final long serialVersionUID = 1606909731977454208L;
    private JTabbedPanel rightPanel = null;
    public static ContentPanel PANEL;

    public ContentPanel() {
        PANEL = this;
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
    }

    public void display(JTabbedPanel panel) {
        if (rightPanel != null) {
            this.remove(rightPanel);

            rightPanel.onHide();
        }
        rightPanel = panel;

        this.add(rightPanel, "cell 0 0");
        rightPanel.onDisplay();
        this.revalidate();
        this.repaint();
    }

    public JTabbedPanel getRightPanel() {
        return rightPanel;
    }

}
