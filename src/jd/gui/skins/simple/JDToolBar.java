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

import jd.controlling.JDLogger;
import jd.gui.skins.jdgui.actions.ActionController;
import jd.gui.skins.jdgui.actions.ToolBarAction;
import jd.gui.skins.simple.components.SpeedMeterPanel;
import net.miginfocom.swing.MigLayout;

public class JDToolBar extends JToolBar {

    private static JDToolBar INSTANCE = null;

    private static final long serialVersionUID = 7533138014274040205L;

    private static final String defaultlist[] = new String[] { "toolbar.control.start", "toolbar.control.pause", "toolbar.control.stop", "toolbar.separator", "toolbar.quickconfig.clipboardoberserver", "toolbar.quickconfig.reconnecttoggle", "toolbar.separator", "toolbar.interaction.reconnect", "toolbar.interaction.update" };

    private static final String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private static final String GUIINSTANCE = "GUI";

    private String[] current = null;

    private SpeedMeterPanel speedmeter;

    public static synchronized JDToolBar getInstance() {
        if (INSTANCE == null) INSTANCE = new JDToolBar();
        return INSTANCE;
    }

    private JDToolBar() {
        super(JToolBar.HORIZONTAL);
        // noTitlePainter = noTitlePane;
        setRollover(true);
        setFloatable(false);
        setLayout(new MigLayout("ins 0,gap 0", "[][][][][][][][][][][][][][grow,fill]"));
        ActionController.initActions();

        current = defaultlist;
        // allows to load userdefined toolbars
        // current =
        // SubConfiguration.getConfig(JDGuiConstants.CONFIG_PARAMETER).getGenericProperty(JDGuiConstants.CFG_KEY_TOOLBAR_ACTIONLIST,
        // defaultlist);
        initToolbar(current);
        addSpeedMeter();
        INSTANCE = this;
    }

    public void setList(String[] newlist) {
        synchronized (current) {
            if (newlist == null || newlist.length == 0) {
                current = defaultlist;
            } else {
                current = newlist;
            }
        }
        new GuiRunnable<Object>() {
            public Object runSave() {
                removeAll();
                initToolbar(current);
                addSpeedMeter();
                revalidate();
                return null;
            }
        }.start();
    }

    public String[] getList() {
        return current;
    }

    private void initToolbar(String[] list) {
        synchronized (list) {
            AbstractButton ab;
            JToggleButton tbt;
            for (String key : list) {
                ToolBarAction action = ActionController.getToolBarAction(key);

                if (action == null) {
                    warning("The Action " + key + " is not available");
                    continue;

                }
                action.init();
                ab = null;
                switch (action.getType()) {
                case NORMAL:

                    add(ab = new JButton(action), BUTTON_CONSTRAINTS);

                    break;
                case TOGGLE:

                    add(ab = tbt = new JToggleButton(action), BUTTON_CONSTRAINTS);
                    tbt.setText("");
                    break;
                case SEPARATOR:
                    add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
                    break;
                }
                if (ab != null) {
                    ab.setText("");
                    ab.setToolTipText(action.getTooltipText());
                    ab.setEnabled(action.isEnabled());
                    ab.setSelected(action.isSelected());
                    action.putValue(GUIINSTANCE, ab);
                    // external changes on the action get deligated to the
                    // buttons
                    action.addPropertyChangeListener(new PropertyChangeListener() {
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

                }
                // inits the actions
            }
        }
    }

    private void addSpeedMeter() {
        speedmeter = new SpeedMeterPanel();
        add(speedmeter, "cell 0 13,dock east,hidemode 3,height 30!,width 30:200:300");
    }

}