//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.tasks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.EventListenerList;

import jd.gui.skins.jdgui.interfaces.SwitchPanel;
import jd.gui.skins.simple.Factory;
import jd.gui.skins.simple.SingletonPanel;
import net.miginfocom.swing.MigLayout;

import com.jtattoo.plaf.AbstractLookAndFeel;

public abstract class TaskPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 2136414459422852581L;

    public static final int ACTION_CLICK = -2;
    protected EventListenerList listenerList;
    private String panelID = "taskpanel";

    private ArrayList<SingletonPanel> panels;

    public boolean pressed;

    private ImageIcon icon;

    private String taskName;

    private boolean activeTab;

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public TaskPanel(String title, ImageIcon ii, String pid) {
        // this.setTitle(string);
        // this.setIcon(ii);
        // this.addMouseListener(this);
        this.taskName = title;

        this.setIcon(ii);

        this.listenerList = new EventListenerList();
        this.setPanelID(pid);

        if (UIManager.getLookAndFeel() instanceof AbstractLookAndFeel) {
            // AbstractLookAndFeel laf =
            // (AbstractLookAndFeel)UIManager.getLookAndFeel();

            // this.setBackground(laf.getTheme().getToolbarBackgroundColor());
        }
        // this.addPropertyChangeListener(this);
        this.setLayout(new MigLayout("ins 5 3 5 3, wrap 1", "[fill,grow]"));
        // setDeligateCollapsed(SubConfiguration.getConfig("gui").getBooleanProperty(getPanelID()
        // + "_collapsed", false));
        this.panels = new ArrayList<SingletonPanel>();

    }

    private void setIcon(ImageIcon ii) {

        icon = ii;

    }

    public ImageIcon getIcon() {
        return icon;
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
        for (ActionListener listener : listenerList.getListeners(ActionListener.class)) {
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
        return listenerList.getListeners(ActionListener.class);
    }

    // public void mouseReleased(MouseEvent e) {
    // if (super.isCollapsed()) {
    // super.setCollapsed(false);
    // } else {
    // super.setCollapsed(true);
    // }
    //
    // broadcastEvent(new ActionEvent(this, ACTION_CLICK, "Toggle"));
    // }

    public SwitchPanel getPanel(int i) {
        return panels.get(i).getPanel();
    }

    public void setPanelID(String panelID) {
        this.panelID = panelID;
    }

    public String getPanelID() {
        return panelID;
    }

    public JButton createButton(String string, Icon i) {
        return Factory.createButton(string, i, this);
    }

    /**
     * is called if the tab is hidden
     */
    public void onHide() {
        
    }

    public boolean isActiveTab() {
        return activeTab;
    }

    public void setActiveTab(boolean activeTab) {
        this.activeTab = activeTab;
    }

    /**
     * gets called if the tab gets displayed
     */
    public void onDisplay() {
     
    }
    // public void propertyChange(PropertyChangeEvent evt) {
    // if (evt.getPropertyName().equals("collapsed")) {
    // SubConfiguration cfg = SubConfiguration.getConfig("gui");
    // if (pressed) {
    // broadcastEvent(new ActionEvent(this, ACTION_TOGGLE, "Toggle"));
    // cfg.setProperty(getPanelID() + "_collapsed", this.isCollapsed());
    // cfg.save();
    // pressed = false;
    // }
    // }
    //
    // }

}
