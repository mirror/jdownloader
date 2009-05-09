package jd.gui.skins.simple;

import java.awt.EventQueue;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.controlling.JDController;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;

public class JDCollapser extends JPanel implements MouseListener {

    private static final long serialVersionUID = 6864885344815243560L;
    private static JDCollapser INSTANCE = null;

    public static JDCollapser getInstance() {
        if (INSTANCE == null) INSTANCE = new JDCollapser();
        return INSTANCE;
    }

    private JTabbedPanel panel;

    private JDCollapser() {
        super();
        this.setVisible(true);
        // this.setCollapsed(true);
        this.addMouseListener(this);
   
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
      this.setVisible(!b);
    }

    public void setContentPanel(JTabbedPanel panel2) {
        if (panel2 == this.panel) return;

        if (this.panel != null) {
            this.panel.onHide();

        }
        removeAll();
       setLayout(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));

        this.panel = panel2;
        panel.onDisplay();

        // JScrollPane sp;

        add(panel);

        // getContentPane().add(panel);
        setCollapsed(false);
        revalidate();

    }

    public void setTitle(String l) {
        // TODO Auto-generated method stub
        
    }

    public void setIcon(ImageIcon ii) {
        // TODO Auto-generated method stub
        
    }
}
