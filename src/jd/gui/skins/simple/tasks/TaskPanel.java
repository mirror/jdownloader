package jd.gui.skins.simple.tasks;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
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
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXTaskPane;

public abstract class TaskPanel extends JXTaskPane implements MouseListener, PropertyChangeListener, ActionListener {

    private static final long serialVersionUID = 2136414459422852581L;

    public static final int ACTION_TOGGLE = -1;
    public static final int ACTION_CLICK = -2;
    protected EventListenerList listenerList;
    private String panelID = "taskpanel";
    protected static final String GAP_BUTTON_LEFT = "gapleft 10";
    private ArrayList<SingletonPanel> panels;
    protected static final String D1_BUTTON_ICON = "spanx,alignx left,gaptop 2";
    protected static final String D1_LABEL_ICON = "spanx,alignx left,gaptop 7,gapleft 7";
    protected static final String D2_LABEL = "spanx,alignx left,gaptop 2,gapleft 27";
    protected static final String D1_LABEL = "spanx,alignx left,gaptop 7,gapleft 7";
    protected static final String D2_PROGRESSBAR = "height 10!,gaptop 7,gapleft 27, width null:110:180";
    protected static final String D1_COMPONENT = "spanx,alignx left,gaptop 2,gapleft 7";
    public boolean pressed;

    public TaskPanel(String string, ImageIcon ii, String pid) {
        this.setTitle(string);
        this.setIcon(ii);
        this.addMouseListener(this);
        this.listenerList = new EventListenerList();
        this.setPanelID(pid);
        this.addPropertyChangeListener(this);
        this.setLayout(new MigLayout("ins 5 5 5 15, wrap 1", "[fill,grow]", "[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]0[]"));
        setDeligateCollapsed(SubConfiguration.getConfig("gui").getBooleanProperty(getPanelID() + "_collapsed", false));
        this.panels = new ArrayList<SingletonPanel>();
    }

    public void setCollapsed(boolean collapsed) {

        //        
    }

    public void setDeligateCollapsed(boolean collapsed) {
        super.setCollapsed(collapsed);
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

    public ArrayList<SingletonPanel> getPanels() {
        return panels;

    }

    public void broadcastEvent(final ActionEvent e) {
        // new Thread() {
        // public void run() {
        //              

        for (ActionListener listener : (ActionListener[]) listenerList.getListeners(ActionListener.class)) {
            listener.actionPerformed(e);
        }

        // }
        // }.start();
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
        super.setCollapsed(false);

        broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));
    }

    public JTabbedPanel getPanel(int i) {
        return panels.get(i).getPanel();
    }

    public void setPanelID(String panelID) {
        this.panelID = panelID;
    }

    public String getPanelID() {
        return panelID;
    }

    public JButton createButton(String string, Icon i) {
        JButton bt;
        if (i != null) {
            bt = new JButton(string, i);
        } else {
            bt = new JButton(string);
        }

        bt.setContentAreaFilled(false);
        bt.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bt.setFocusPainted(false);
        bt.setBorderPainted(false);
        bt.setHorizontalAlignment(JButton.LEFT);
        bt.addActionListener(this);
        bt.addMouseListener(new MouseAdapter() {

            private Font originalFont;

            @SuppressWarnings("unchecked")
            @Override
            public void mouseEntered(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();

                originalFont = src.getFont();
                if (src.isEnabled()) {
                    Map attributes = originalFont.getAttributes();
                    attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
                    src.setFont(originalFont.deriveFont(attributes));
                }

            }

            @Override
            public void mouseExited(MouseEvent evt) {
                JButton src = (JButton) evt.getSource();
                src.setFont(originalFont);
            }
        });
        return bt;
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("collapsed")) {
            SubConfiguration cfg = SubConfiguration.getConfig("gui");
            if (pressed) {
                broadcastEvent(new ActionEvent(this, ACTION_TOGGLE, "Toggle"));
                cfg.setProperty(getPanelID() + "_collapsed", this.isCollapsed());
                cfg.save();
            }
        }

    }

}
