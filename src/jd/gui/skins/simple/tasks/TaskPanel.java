package jd.gui.skins.simple.tasks;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.event.EventListenerList;

import jd.config.SubConfiguration;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SingletonPanel;
import jd.utils.JDUtilities;

import org.jdesktop.swingx.JXTaskPane;

public abstract class TaskPanel extends JXTaskPane implements MouseListener, PropertyChangeListener, ActionListener {

    private static final long serialVersionUID = 2136414459422852581L;

    public static final int ACTION_TOGGLE = -1;
    public static final int ACTION_CLICK = -2;
    protected EventListenerList listenerList;
    private String panelID = "taskpanel";
    protected static final String GAP_BUTTON_LEFT = "gapleft 10";
    private ArrayList<SingletonPanel> panels;

    private boolean pressed;

    public TaskPanel(String string, ImageIcon ii, String pid) {
        this.setTitle(string);
        this.setIcon(ii);
        this.addMouseListener(this);
        this.listenerList = new EventListenerList();
        this.setPanelID(pid);
        this.addPropertyChangeListener(this);
        this.setCollapsed(JDUtilities.getSubConfig("gui").getBooleanProperty(getPanelID() + "_collapsed", false));

        this.panels = new ArrayList<SingletonPanel>();
    }

    protected JButton addButton(JButton bt) {
        bt.addActionListener(this);
        bt.setHorizontalAlignment(JButton.LEFT);
        add(bt, "spanx,alignx leading," + GAP_BUTTON_LEFT + ",gaptop 2");
        return bt;
    }

    /**
     * Adds an <code>ActionListener</code> to the button.
     * 
     * @param l
     *            the <code>ActionListener</code> to be added
     */
    public void addActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
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

    public void addPanel(SingletonPanel singletonPanel) {
        panels.add(singletonPanel);
        singletonPanel.setTaskPanel(this);

    }

    public void addPanelAt(int id, SingletonPanel singletonPanel) {
        while (panels.size() <= id) {
            panels.add(null);
        }
        panels.set(id, singletonPanel);

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
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        pressed = false;
    }

    public void mousePressed(MouseEvent e) {
        this.pressed = true;
    }

    public void mouseReleased(MouseEvent e) {

        broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));

    }

    public JTabbedPanel getPanel(int i) {
        // TODO Auto-generated method stub
        return panels.get(i).getPanel();
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
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);

        bt.addMouseListener(new java.awt.event.MouseAdapter() {

            private Font originalFont;

            public void mouseEntered(java.awt.event.MouseEvent evt) {
                JButton src = (JButton) evt.getSource();
            
                originalFont = src.getFont();
                if(src.isEnabled()){
                Map attributes = originalFont.getAttributes();
                attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                src.setFont(originalFont.deriveFont(attributes));
                }

            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                JButton src = (JButton) evt.getSource();
                src.setFont(originalFont);
            }
        });
        return bt;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("collapsed")) {
            SubConfiguration cfg = JDUtilities.getSubConfig("gui");
            if (pressed) {
                broadcastEvent(new ActionEvent(this, ACTION_TOGGLE, "Toggle"));
                cfg.setProperty(getPanelID() + "_collapsed", this.isCollapsed());
                cfg.save();
            }
        }

    }

}
