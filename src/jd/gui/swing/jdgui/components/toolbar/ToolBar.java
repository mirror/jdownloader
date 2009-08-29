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

import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.ShortCuts;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ActionControlEvent;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.actions.event.ActionControllerListener;
import net.miginfocom.swing.MigLayout;

public class ToolBar extends JToolBar implements ActionControllerListener {

    private static final long serialVersionUID = 7533137014274040205L;

    public static final ArrayList<String> CONTROL_LIST = new ArrayList<String>();
    static {
        CONTROL_LIST.add("toolbar.control.start");
        CONTROL_LIST.add("toolbar.control.pause");
        CONTROL_LIST.add("toolbar.control.stop");

    }

    public static final ArrayList<String> MOVE_LIST = new ArrayList<String>();
    static {

        MOVE_LIST.add("action.downloadview.movetotop");
        MOVE_LIST.add("action.downloadview.moveup");
        MOVE_LIST.add("action.downloadview.movedown");
        MOVE_LIST.add("action.downloadview.movetobottom");

    }

    public static final ArrayList<String> CONFIG_LIST = new ArrayList<String>();
    static {

        CONFIG_LIST.add("toolbar.quickconfig.clipboardoberserver");
        CONFIG_LIST.add("toolbar.quickconfig.reconnecttoggle");
        CONFIG_LIST.add("toolbar.control.stopmark");

    }
    public static final ArrayList<String> DEFAULT_LIST = new ArrayList<String>();
    static {
        DEFAULT_LIST.add("toolbar.control.start");
        DEFAULT_LIST.add("toolbar.control.pause");
        DEFAULT_LIST.add("toolbar.control.stop");
        DEFAULT_LIST.add("toolbar.separator");
        DEFAULT_LIST.add("action.downloadview.movetotop");
        DEFAULT_LIST.add("action.downloadview.moveup");
        DEFAULT_LIST.add("action.downloadview.movedown");
        DEFAULT_LIST.add("action.downloadview.movetobottom");
        DEFAULT_LIST.add("toolbar.separator");
        DEFAULT_LIST.add("toolbar.quickconfig.clipboardoberserver");
        DEFAULT_LIST.add("toolbar.quickconfig.reconnecttoggle");
        DEFAULT_LIST.add("toolbar.control.stopmark");
        DEFAULT_LIST.add("toolbar.separator");
        DEFAULT_LIST.add("toolbar.interaction.reconnect");
        DEFAULT_LIST.add("toolbar.interaction.update");

    }

    private String[] current = null;

    private boolean updateing = false;

    private JRootPane rootpane;

    public ToolBar() {
        super(JToolBar.HORIZONTAL);

        setRollover(true);
        setFloatable(false);

        ActionController.initActions();

        // this.updateToolbar();
        current = DEFAULT_LIST.toArray(new String[] {});

        // please add listener here. to avoid the toolbar beiong pained multible
        // times
        ActionController.getBroadcaster().addListener(this);

    }

    public void setList(String[] newlist) {
        if (newlist == current) return;
        synchronized (current) {
            if (newlist == null || newlist.length == 0) {
                current = DEFAULT_LIST.toArray(new String[] {});
            } else {
                current = newlist;
            }
        }
        this.updateToolbar();
    }

    public String[] getList() {
        return current;
    }

    private String getColConstraints(String[] list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; ++i) {
            sb.append("[]");
        }
        sb.append("[grow,fill]");
        return sb.toString();
    }

    /**
     * USed to register the shortcuts to the rootpane during init
     * 
     * @param jdGui
     */
    public void registerAccelerators(SwingGui jdGui) {
        rootpane = jdGui.getMainFrame().getRootPane();

    }

    private void initToolbar(String[] list) {
        if (list == null) return;
        synchronized (list) {
            SwingGui.checkEDT();
            setLayout(new MigLayout("ins 0, gap 0", getColConstraints(list)));
            AbstractButton ab;
            boolean lastseperator = false;
            for (int i = 0; i < list.length; i++) {
                String key = list[i];
                ToolBarAction action = ActionController.getToolBarAction(key);

                if (action == null) {
                    JDLogger.warning("The Action " + key + " is not available");
                    continue;
                }

                action.init();
                if (!action.isVisible()) {
                    JDLogger.warning("Action " + action + " is set to invisble");
                    continue;
                }

                ab = null;
                switch (action.getType()) {
                case NORMAL:

                    // add(ab = new JMenuItem(action), BUTTON_CONSTRAINTS);
                    ab = add(action);
                    lastseperator = false;
                    // ab.setText("");
                    break;
                case SEPARATOR:
                    if (!lastseperator) {
                        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
                        lastseperator = true;
                    }
                    break;

                case TOGGLE:
                    // ab = add(action);
                    add(ab = new JToggleButton(action));

                    if ((action.getValue(Action.SMALL_ICON) != null || action.getValue(Action.LARGE_ICON_KEY) != null)) {
                        ab.setText("");
                    }
                    lastseperator = false;
                    // add(ab = tbt = new JCheckBoxMenuItem(action),
                    // BUTTON_CONSTRAINTS);
                    // tbt.setText("");
                    break;

                }

                if (ab != null) {

                    KeyStroke ks = (KeyStroke) (action.getValue(Action.ACCELERATOR_KEY));
                    rootpane.getInputMap(JButton.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, action);
                    rootpane.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW).put(ks, action);
                    rootpane.getActionMap().put(action, action);
                    // this.mainFrame.getRootPane().getActionMap().

                    // getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW).put(ks,
                    // action);

                    if (action.getValue(Action.ACCELERATOR_KEY) != null) {
                        ab.setToolTipText(action.getTooltipText() + " [" + ShortCuts.getAcceleratorString((KeyStroke) action.getValue(Action.ACCELERATOR_KEY)) + "]");
                    } else if (action.getValue(Action.MNEMONIC_KEY) != null) {
                        ab.setToolTipText(action.getTooltipText() + " [Alt+" + new String(new byte[] { ((Integer) action.getValue(Action.MNEMONIC_KEY)).byteValue() }) + "]");
                    } else {
                        ab.setToolTipText(action.getTooltipText());
                    }

                }
            }
        }
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
    public void updateToolbar() {

        new GuiRunnable<Object>() {
            @Override
            public Object runSave() {
                setVisible(false);
                removeAll();
                initToolbar(current);

                setVisible(true);
                revalidate();
                return null;
            }
        }.waitForEDT();
    }

}
