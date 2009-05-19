package jd.gui.skins.simple;

import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JViewport;

import net.miginfocom.swing.MigLayout;

public class ContentPanel extends JPanel {

    private static final long serialVersionUID = 1606909731977454208L;
    private JTabbedPanel rightPanel = null;
    private JViewport viewport;

    // public static ContentPanel PANEL;

    public ContentPanel() {
        // PANEL = this;
        viewport = new JViewport();
        this.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));

    }

    public void display(JTabbedPanel panel) {
        if (rightPanel == panel) return;
        JDCollapser.getInstance().setCollapsed(true);
        if (rightPanel != null) {

            this.removeAll();
            rightPanel.setEnabled(false);
            rightPanel.setVisible(false);

            rightPanel.onHide();
        }
        rightPanel = panel;

        if (rightPanel.needsViewport()) {
            viewport.setView(rightPanel);
            this.add(viewport, "cell 0 0");

        } else {
            this.add(rightPanel, "cell 0 0");
        }
        rightPanel.setEnabled(true);
        rightPanel.setVisible(true);
        rightPanel.onDisplay();
        this.revalidate();
        this.repaint();
        this.setPreferredSize(new Dimension(100, 10));

    }

    public JTabbedPanel getRightPanel() {
        return rightPanel;
    }

}
