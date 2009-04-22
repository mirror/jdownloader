package jd.gui.skins.simple;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXCollapsiblePane;

public class JDSeparator extends JXCollapsiblePane implements PropertyChangeListener, MouseListener {

    private static final long serialVersionUID = 3007033193590223026L;
    private JLabel minimize;
    private ImageIcon left;
    private ImageIcon right;
    private boolean minimized;

    // private boolean mouseover;

    public JDSeparator() {
        setLayout(new MigLayout("ins 0,wrap 1"));
        addMouseListener(this);

        // SimpleGUI.CURRENTGUI.getLeftcolPane().addMouseListener(this);
        left = JDTheme.II("gui.images.minimize.left", 5, 10);
        right = JDTheme.II("gui.images.minimize.right", 5, 10);
        add(minimize = new JLabel(left), "width 4!,gapright 1");
        minimize.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.hide.tooltip", "Hide the Sidebar"));
        minimize.addMouseListener(this);
        minimize.setOpaque(false);

        // setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMinimized(false);
    }

    private void setMinimized(boolean b) {
        this.minimized = b;
        if (b) {
            minimize.setIcon(right);
            minimize.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.show.tooltip", "Show the Sidebar"));

        } else {
            minimize.setIcon(left);
            minimize.setToolTipText(JDLocale.L("gui.sidebar.splitbuttons.hide.tooltip", "Hide the Sidebar"));

        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof JXCollapsiblePane && evt.getPropertyName().equals("collapsed")) {
            if (((JXCollapsiblePane) evt.getSource()).isCollapsed()) {
                setMinimized(true);
            } else {
                setMinimized(false);
            }
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getSource() == this || e.getSource() == minimize) {
            if (minimized) {
                SimpleGUI.CURRENTGUI.hideSideBar(false);
            } else {
                SimpleGUI.CURRENTGUI.hideSideBar(true);
            }
            // mouseover = false;
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

}
