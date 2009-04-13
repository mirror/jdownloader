package jd.gui.skins.simple;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;
import org.jdesktop.swingx.JXTaskPane;

public class JDCollapser extends JXTaskPane implements MouseListener {

    private static final long serialVersionUID = 6864885344815243560L;
    private static JDCollapser INSTANCE = null;

    public static JDCollapser getInstance() {
        if (INSTANCE == null) INSTANCE = new JDCollapser();
        return INSTANCE;
    }

    private JTabbedPanel panel;

    private JDCollapser() {
        super();
        this.setVisible(false);
        this.setCollapsed(true);
        this.addMouseListener(this);
        getContentPane().setLayout(new MigLayout("ins 0,wrap 1", "[grow, fill]", "[grow,fill]"));
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {

        this.setCollapsed(true);

    }

    public void setCollapsed(boolean b) {
        if (b == this.isCollapsed()) return;
        super.setCollapsed(b);
        if (b) {
            if (panel != null) {
                panel.onHide();
                panel = null;
            }

            new SwingWorker() {

                @Override
                protected Object doInBackground() throws Exception {
                    Thread.sleep(500);
                    return null;
                }

                protected void done() {
                    setVisible(false);

                }
            }.execute();
        }
    }

    public void setContentPanel(JTabbedPanel panel) {
        if (panel == this.panel) return;

        if (this.panel != null) {
            this.panel.onHide();

        }
        getContentPane().removeAll();
        this.panel = panel;
        panel.onDisplay();

        getContentPane().add(this.panel, "cell 0 0");
        this.invalidate();
        this.repaint();

    }
}
