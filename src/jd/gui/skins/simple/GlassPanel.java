package jd.gui.skins.simple;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

public class GlassPanel extends JPanel implements MouseListener, MouseMotionListener {

    public GlassPanel(MigLayout migLayout) {
        super(migLayout);
//        this.addMouseListener(this);
//        this.addMouseMotionListener(this);
    }

    private void redispatchMouseEvent(MouseEvent e, boolean repaint) {
        doDispatch(e);

    }

    protected Component getRealComponent(Point pt) {
        // get the mouse click point relative to the content pane
        Point containerPoint = SwingUtilities.convertPoint(this, pt, SimpleGUI.CURRENTGUI.getRootPane());

        // find the component that under this point
        Component component = SwingUtilities.getDeepestComponentAt(SimpleGUI.CURRENTGUI.getRootPane(), containerPoint.x, containerPoint.y);
        return component;
    }

    protected void doDispatch(MouseEvent e) {
        // since it's not a popup we need to redispatch it.

        Component component = getRealComponent(e.getPoint());

        // return if nothing was found
        if (component == null) { return; }

        // convert point relative to the target component
        Point componentPoint = SwingUtilities.convertPoint(this, e.getPoint(), component);

        // redispatch the event
        component.dispatchEvent(new MouseEvent(component, e.getID(), e.getWhen(), e.getModifiers(), componentPoint.x, componentPoint.y, e.getClickCount(), e.isPopupTrigger()));
    }

    public void mouseClicked(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

    public void mouseEntered(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

    public void mouseExited(MouseEvent e) {
        redispatchMouseEvent(e, true);
    }

    public void mousePressed(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

    public void mouseDragged(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

    public void mouseMoved(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

    public void mouseReleased(MouseEvent e) {
        redispatchMouseEvent(e, true);

    }

}
