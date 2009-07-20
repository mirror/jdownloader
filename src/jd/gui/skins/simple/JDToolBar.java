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

package jd.gui.skins.simple;

import static jd.controlling.JDLogger.warning;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.actions.ActionControlEvent;
import jd.gui.skins.jdgui.actions.ActionController;
import jd.gui.skins.jdgui.actions.ToolBarAction;
import jd.gui.skins.jdgui.actions.event.ActionControllerListener;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import net.miginfocom.swing.MigLayout;

public class JDToolBar extends JToolBar implements ActionControllerListener, ControlListener {

    private static String[] defaultlist = new String[] { "toolbar.control.start", "toolbar.control.pause", "toolbar.control.stop", "toolbar.separator", "toolbar.quickconfig.clipboardoberserver", "toolbar.quickconfig.reconnecttoggle", "toolbar.separator", "toolbar.interaction.reconnect", "toolbar.interaction.update" };

    private static JDToolBar INSTANCE = null;

    private static final long serialVersionUID = 7533138014274040205L;

    private static final String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private static final String GUIINSTANCE = "GUI";

    private static final String PROPERTY_CHANGE_LISTENER = "PROPERTY_CHANGE_LISTENER";

    private String[] current = null;

    private SpeedMeterPanel speedmeter;

    private boolean updateing;

    public static synchronized JDToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new JDToolBar();
        return INSTANCE;
    }

    private JDToolBar() {
        super(JToolBar.HORIZONTAL);
        // noTitlePainter = noTitlePane;
        setRollover(true);
        setFloatable(false);
        setLayout(new MigLayout("ins 0"));
        speedmeter = new SpeedMeterPanel();
        ActionController.initActions();

        // this.updateToolbar();
        current = defaultlist;
        this.updateToolbar();
        // please add listener here. to avoid the toolbar beiong pained multible
        // times
        ActionController.getBroadcaster().addListener(this);
        INSTANCE = this;
        JDController.getInstance().addControlListener(this);
    }

    public void setList(String[] newlist) {
        if (newlist == current) return;
        synchronized (current) {
            if (newlist == null || newlist.length == 0) {
                current = defaultlist;
            } else {
                current = newlist;
            }
        }
        this.updateToolbar();
    }

    public String[] getList() {
        return current;
    }

    private void initToolbar(String[] list) {
        synchronized (list) {
            SwingGui.checkEDT();
            AbstractButton ab;
            JToggleButton tbt;
            if (list != null) {
                for (String key : list) {
                    ToolBarAction action = ActionController.getToolBarAction(key);

                    if (action == null) {
                        warning("The Action " + key + " is not available");
                        continue;

                    }

                    action.init();
                    if (!action.isVisible()) {
                        warning("Action " + action + " is set to invisble");
                        continue;

                    }

                    ab = null;
                    switch (action.getType()) {
                    case NORMAL:

                        add(ab = new JButton(action), BUTTON_CONSTRAINTS);
                        ab.setText("");
                        break;
                    case SEPARATOR:
                        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
                        break;

                    case TOGGLE:

                        add(ab = tbt = new JToggleButton(action), BUTTON_CONSTRAINTS);
                        tbt.setText("");
                        break;

                    }
                    if (ab != null) {
                        ab.setText("");
                        ab.setToolTipText(action.getTooltipText());
                        ab.setEnabled(action.isEnabled());
                        ab.setSelected(action.isSelected());

                        action.putValue(GUIINSTANCE, ab);
                        PropertyChangeListener pcl;
                        // external changes on the action get deligated to the
                        // buttons
                        action.addPropertyChangeListener(pcl = new PropertyChangeListener() {
                            public void propertyChange(PropertyChangeEvent evt) {
                                ToolBarAction action = (ToolBarAction) evt.getSource();
                                try {
                                    AbstractButton ab = ((AbstractButton) action.getValue(GUIINSTANCE));
                                    ab.setText("");
                                    ab.setToolTipText(action.getTooltipText());
                                    ab.setEnabled(action.isEnabled());
                                    ab.setSelected(action.isSelected());
                                } catch (Throwable w) {
                                    JDLogger.exception(w);
                                    action.removePropertyChangeListener(this);

                                }

                            }

                        });
                        if (action.getValue(PROPERTY_CHANGE_LISTENER) != null) {

                            action.removePropertyChangeListener((PropertyChangeListener) action.getValue(PROPERTY_CHANGE_LISTENER));
                        }
                        action.putValue(PROPERTY_CHANGE_LISTENER, pcl);

                    }
                }
            }
        }
    }

    private void addSpeedMeter() {
        add(speedmeter, "dock east,hidemode 3,height 30!,width 30:200:300, grow");
    }

    public synchronized void onActionControlEvent(ActionControlEvent event) {
        if (updateing) return;
        updateing = true;

        // currently visible buttons have a registered propertychangelistener
        // that updates them on change.
        // we only need a complete redraw for the visible event.
        if (event.getParameter() == ToolBarAction.VISIBLE) {
            updateToolbar();
        }
        updateing = false;
    }

    /**
     * UPdates the toolbar
     */
    private void updateToolbar() {
        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                setVisible(false);
                removeAll();
                initToolbar(current);
                addSpeedMeter();
                setVisible(true);
                revalidate();
                return null;
            }
        }.waitForEDT();
    }

    @Override
    public void controlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_DOWNLOAD_START:
            speedmeter.start();
            break;
        case ControlEvent.CONTROL_DOWNLOAD_STOP:
            speedmeter.stop();
            break;
        }
    }

}