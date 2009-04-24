package jd.gui.skins.simple;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JScrollPane;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.controlling.JDController;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;
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
        this.setAnimated(SimpleGuiConstants.isAnimated());
        JDController.getInstance().addControlListener(new ConfigPropertyListener( SimpleGuiConstants.ANIMATION_ENABLED) {     

            @Override
            public void onPropertyChanged(Property source, String propertyName) {
                setAnimated(SimpleGuiConstants.isAnimated());
                
            }

        });
        this.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName() == JXCollapsiblePane.ANIMATION_STATE_KEY) {
                    if (evt.getNewValue().equals("collapsed")) {
                        JDCollapser.this.setVisible(false);
                    }
                }

            }

        });
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

        } else {
            setVisible(true);
        }
    }

    public void setContentPanel(JTabbedPanel panel) {
        if (panel == this.panel) return;

        if (this.panel != null) {
            this.panel.onHide();

        }
        getContentPane().removeAll();
        getContentPane().setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[shrink 100]"));

        this.panel = panel;
        panel.onDisplay();

        // JScrollPane sp;

        getContentPane().add(new JScrollPane(this.panel), "height n:n:" + (int) (SimpleGUI.CURRENTGUI.getHeight() * 0.65));
        // sp.setBorder(null);
        this.revalidate();

        this.repaint();

    }
}
