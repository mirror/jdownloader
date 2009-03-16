package jd.gui.skins.simple.tasks;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.EventListenerList;

import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTaskPane;

public class TaskPanel extends JXTaskPane implements MouseListener {

    public static final int ACTION_TOGGLE = -1;
    public static final int ACTION_CLICK = -2;
    protected EventListenerList listenerList;
    private String panelID = "taskpanel";
    private boolean status;

    public TaskPanel(String string, ImageIcon ii, String pid) {
        this.setTitle(string);
        this.setIcon(ii);
        this.addMouseListener(this);
        this.listenerList = new EventListenerList();
        this.setPanelID(pid);
        this.setCollapsed(JDUtilities.getSubConfig("gui").getBooleanProperty(getPanelID() + "_collapsed", false));
        status = this.isCollapsed();
    }

    /**
     * Adds an <code>ActionListener</code> to the button.
     * 
     * @param l
     *            the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    /**
     * Removes an <code>ActionListener</code> from the button.
     * 
     * @param l
     *            the listener to be removed
     */
    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);

    }

    public void broadcastEvent(ActionEvent e) {

        for (ActionListener listener : (ActionListener[]) this.listenerList.getListeners(ActionListener.class)) {
            listener.actionPerformed(e);
        }
    }

    /**
     * Returns an array of all the <code>ActionListener</code>s added to this
     * AbstractButton with addActionListener().
     * 
     * @return all of the <code>ActionListener</code>s added or an empty array
     *         if no listeners have been added
     */
    public ActionListener[] getActionListeners() {
        return (ActionListener[]) (listenerList.getListeners(ActionListener.class));
    }

    public void mouseClicked(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
        // TODO Auto-generated method stub

    }

    public void mouseReleased(MouseEvent e) {
        if (this.isCollapsed() != status) {

            broadcastEvent(new ActionEvent(this, ACTION_TOGGLE, "Toggle"));
            status = this.isCollapsed();
            JDUtilities.getSubConfig("gui").setProperty(getPanelID() + "_collapsed", this.isCollapsed());
            JDUtilities.getSubConfig("gui").save();
        }
        broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));

    }

    public void setPanelID(String panelID) {
        this.panelID = panelID;
    }

    public String getPanelID() {
        return panelID;
    }

    public JButton createButton(String string, Icon i) {
        JButton bt = new JButton(string, i);
        bt.setBorderPainted(false);
        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(12));

        return bt;
    }

}
