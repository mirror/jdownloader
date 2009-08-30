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

package jd.gui.swing.jdgui.views.toolbar;

import static jd.controlling.JDLogger.warning;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ActionControlEvent;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.actions.event.ActionControllerListener;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ViewToolbar extends JPanel implements ActionControllerListener {
    public ViewToolbar() {

        ActionController.initActions();

        // this.updateToolbar();
        current = defaultlist;

        // please add listener here. to avoid the toolbar beiong pained multible
        // times
        ActionController.getBroadcaster().addListener(this);
    }

    private static String[] defaultlist = new String[] {

    };

    private static final long serialVersionUID = 7533137014274040205L;

    public static String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private static final String GUIINSTANCE = "GUI";

    private static final String PROPERTY_CHANGE_LISTENER = "PROPERTY_CHANGE_LISTENER";

    public static final int EAST = 1;
    public static final int WEST = 2;
    public static final int NORTH = 3;
    public static final int SOUTH = 4;

    public static final int FIRST_COL = -1;

    public static final int LAST_COL = -2;

    private String[] current = null;

    private boolean updateing = false;

    protected int halign = ViewToolbar.WEST;

    private boolean contentPainted = true;

    private boolean textPainted = true;

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

    private String getColConstraints(String[] list) {
        StringBuilder sb = new StringBuilder();
        sb.append(getColConstraint(FIRST_COL, null));
        for (int i = 0; i < list.length; ++i) {
            sb.append(getColConstraint(i, current[i]));
        }
        sb.append(getColConstraint(LAST_COL, null));
        return sb.toString();
    }

    /**
     * This method can be overridden to layout custom Toolbars.
     * 
     * @param i
     * @param string
     * @return
     */
    public String getColConstraint(int col, String string) {
        switch (col) {
        case FIRST_COL:
            return halign == EAST ? "[grow]" : "";
        case LAST_COL:
            return halign == EAST ? "" : "[grow,fill]";
        default:
            return "[]";
        }

    }

    private void initToolbar(String[] list) {
        if (list == null) return;
        synchronized (list) {
            SwingGui.checkEDT();
            setLayout(new MigLayout("ins 0, gap 0", getColConstraints(list)));
            AbstractButton ab;

            int i = -1;
            for (String key : list) {
                i++;
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
                    add(ab = new JButton(action), getButtonConstraint(i, action));
                    break;
                case SEPARATOR:
                    add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
                    break;

                case TOGGLE:
                    add(ab = new JToggleButton(action), getButtonConstraint(i, action));
                    break;

                }
                if (ab != null) {
                    ab.setContentAreaFilled(contentPainted);
                    ab.setFocusPainted(false);
                    if (action.getValue(Action.MNEMONIC_KEY) != null) {
                        ab.setToolTipText(action.getTooltipText() + " [Alt+" + new String(new byte[] { ((Integer) action.getValue(Action.MNEMONIC_KEY)).byteValue() }) + "]");
                    } else {
                        ab.setToolTipText(action.getTooltipText());
                    }

                    ab.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", 16, 16));
                    if (!textPainted) {
                        ab.setText("");
                    }
                    ab.setToolTipText(action.getTooltipText());
                    ab.setEnabled(action.isEnabled());
                    ab.setSelected(action.isSelected());
                    setDefaults(i, ab);
                    action.putValue(GUIINSTANCE, ab);
                    PropertyChangeListener pcl;
                    // external changes on the action get deligated to the
                    // buttons
                    action.addPropertyChangeListener(pcl = new PropertyChangeListener() {
                        public void propertyChange(PropertyChangeEvent evt) {
                            ToolBarAction action = (ToolBarAction) evt.getSource();
                            try {
                                AbstractButton ab = ((AbstractButton) action.getValue(GUIINSTANCE));
                                // ab.setText("");
                                if (action.getValue(Action.MNEMONIC_KEY) != null) {
                                    ab.setToolTipText(action.getTooltipText() + " [Alt+" + new String(new byte[] { ((Integer) action.getValue(Action.MNEMONIC_KEY)).byteValue() }) + "]");
                                } else {
                                    ab.setToolTipText(action.getTooltipText());
                                }
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

    /**
     * May be overridden
     * 
     * @param i
     * @param ab
     */
    public void setDefaults(int i, AbstractButton ab) {
        // TODO Auto-generated method stub

    }

    /**
     * This method may be overridden to create custum toolbars
     * 
     * @param i
     * @param action
     * @return
     */
    public String getButtonConstraint(int i, ToolBarAction action) {
        // TODO Auto-generated method stub
        if (halign == EAST) { return BUTTON_CONSTRAINTS + ", alignx right"; }
        return BUTTON_CONSTRAINTS;
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
    protected void updateToolbar() {
        if (current == null) return;
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

    /**
     * Sets Align CONSTANTS ViewToolbar.EAST | WEST
     * 
     */

    public void setHorizontalAlign(int align) {
        this.halign = align;
        updateToolbar();

    }

    /**
     * default:true Sets if the buttons should paint their content
     * 
     * @param b
     */
    public void setContentPainted(boolean b) {
        contentPainted = b;
        updateToolbar();

    }

    /**
     * default:true returns if the action text is painted in the buttons
     * 
     * @return
     */
    public boolean isTextPainted() {
        return textPainted;
    }

    /**
     * Sets if the action text gets painted default:true
     * 
     * @param textPainted
     */
    public void setTextPainted(boolean textPainted) {
        this.textPainted = textPainted;
        updateToolbar();
    }

    /**
     * returns if the button's content gets painted
     * 
     * @return
     */
    public boolean isContentPainted() {
        return contentPainted;
    }

}
