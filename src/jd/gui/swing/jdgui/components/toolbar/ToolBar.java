//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.components.toolbar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.CustomToolbarAction;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import net.miginfocom.swing.MigLayout;

import org.appwork.utils.Application;
import org.appwork.utils.swing.EDTRunner;

public class ToolBar extends JToolBar {

    private static final long             serialVersionUID = 7533137014274040205L;

    private static final Object           UPDATELOCK       = new Object();

    public static final ArrayList<String> DEFAULT_LIST     = new ArrayList<String>();
    static {
        ToolBar.DEFAULT_LIST.add("toolbar.control.start");
        ToolBar.DEFAULT_LIST.add("toolbar.control.pause");
        ToolBar.DEFAULT_LIST.add("toolbar.control.stop");

        ToolBar.DEFAULT_LIST.add("toolbar.separator");
        ToolBar.DEFAULT_LIST.add("action.settings");
        ToolBar.DEFAULT_LIST.add("toolbar.separator");
        ToolBar.DEFAULT_LIST.add("toolbar.quickconfig.clipboardoberserver");
        ToolBar.DEFAULT_LIST.add("toolbar.quickconfig.reconnecttoggle");
        ToolBar.DEFAULT_LIST.add("toolbar.control.stopmark");
        ToolBar.DEFAULT_LIST.add("premiumMenu.toggle");
        ToolBar.DEFAULT_LIST.add("toolbar.separator");
        ToolBar.DEFAULT_LIST.add("toolbar.interaction.reconnect");
        ToolBar.DEFAULT_LIST.add("toolbar.interaction.update");
    }

    private String[]                      current          = null;

    private JRootPane                     rootpane;

    public ToolBar() {
        super(SwingConstants.HORIZONTAL);

        this.setRollover(true);
        this.setFloatable(false);

        this.current = ToolBar.DEFAULT_LIST.toArray(new String[] {});
    }

    protected String getColConstraints(final String[] list) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; ++i) {
            sb.append("[]2");
        }
        sb.append("[grow,fill]");
        return sb.toString();
    }

    public String[] getList() {
        return this.current;
    }

    private void initToolbar(final String[] list) {
        if (list == null) { return; }
        synchronized (list) {
            SwingGui.checkEDT();
            this.setLayout(new MigLayout("ins 0", this.getColConstraints(list), "[grow,fill,32!]"));
            AbstractButton ab;
            boolean lastseparator = false;
            for (final String key : list) {
                final ToolBarAction action = ActionController.getToolBarAction(key);

                if (action == null) {
                    JDLogger.warning("The Action " + key + " is not available");
                    continue;
                }

                action.init();

                ab = null;

                if (action instanceof CustomToolbarAction) {
                    ((CustomToolbarAction) action).addTo(this);
                    lastseparator = false;
                    continue;
                }
                switch (action.getType()) {
                case NORMAL:
                    this.add(ab = new JButton(action), "w 32!");
                    if (Application.getJavaVersion() >= 16000000) {
                        ab.setHideActionText(true);
                    }
                    lastseparator = false;
                    break;
                case SEPARATOR:
                    if (!lastseparator) {
                        this.add(new JSeparator(SwingConstants.VERTICAL), "gapleft 10,gapright 10");
                        lastseparator = true;
                    }
                    break;
                case TOGGLE:
                    this.add(ab = new JToggleButton(action), "w 32!");
                    if (Application.getJavaVersion() >= 16000000) {
                        ab.setHideActionText(true);
                    }
                    if (Application.getJavaVersion() < 16000000) {
                        final AbstractButton button = ab;
                        ab.setSelected(action.isSelected());
                        action.addPropertyChangeListener(new PropertyChangeListener() {

                            public void propertyChange(final PropertyChangeEvent evt) {
                                if (evt.getPropertyName() == Action.SELECTED_KEY) {
                                    new GuiRunnable<Object>() {

                                        @Override
                                        public Object runSave() {
                                            button.setSelected((Boolean) evt.getNewValue());
                                            return null;
                                        }

                                    }.start();
                                } else if (evt.getPropertyName() == Action.LARGE_ICON_KEY) {
                                    new GuiRunnable<Object>() {

                                        @Override
                                        public Object runSave() {
                                            button.setIcon((Icon) evt.getNewValue());
                                            return null;
                                        }

                                    }.start();
                                }
                            }
                        });
                    }
                    lastseparator = false;
                    break;
                }

                if (ab != null) {
                    if (Application.getJavaVersion() < 16000000) {
                        if (action.getValue(Action.LARGE_ICON_KEY) != null) {
                            ab.setIcon((Icon) action.getValue(Action.LARGE_ICON_KEY));
                        }
                    }
                    final Object value = action.getValue(Action.ACCELERATOR_KEY);
                    if (value == null) {
                        continue;
                    }
                    final KeyStroke ks = (KeyStroke) value;
                    this.rootpane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, action);
                    this.rootpane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, action);
                    this.rootpane.getActionMap().put(action, action);

                    final String shortCut = action.getShortCutString();
                    ab.setToolTipText(action.getTooltipText() + (shortCut != null ? " [" + shortCut + "]" : ""));
                }
            }
        }
    }

    /**
     * USed to register the shortcuts to the rootpane during init
     * 
     * @param jdGui
     */
    public void registerAccelerators(final SwingGui jdGui) {
        this.rootpane = jdGui.getMainFrame().getRootPane();
    }

    public void setList(final String[] newlist) {
        if (newlist == this.current) { return; }
        synchronized (ToolBar.UPDATELOCK) {
            if (newlist == null || newlist.length == 0) {
                this.current = ToolBar.DEFAULT_LIST.toArray(new String[] {});
            } else {
                this.current = newlist;
            }
            this.updateToolbar();
        }
    }

    /**
     * Overwrite this if you want to add special objects (like speedmeterpanel)
     * after the updateprocess.
     */
    protected void updateSpecial() {
    }

    /**
     * Updates the toolbar
     */
    private final void updateToolbar() {
        synchronized (ToolBar.UPDATELOCK) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    ToolBar.this.setVisible(false);
                    ToolBar.this.removeAll();
                    ToolBar.this.initToolbar(ToolBar.this.current);
                    ToolBar.this.updateSpecial();
                    ToolBar.this.setVisible(true);
                    ToolBar.this.revalidate();
                }

            };
        }
    }

}
