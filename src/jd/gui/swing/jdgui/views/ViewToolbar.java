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

package jd.gui.swing.jdgui.views;

import static jd.controlling.JDLogger.warning;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ViewToolbar extends JPanel {

    public ViewToolbar(String... actions) {
        ActionController.initActions();

        current = actions;

        this.updateToolbar();
    }

    private static String[] defaultlist = new String[] {};

    private static final long serialVersionUID = 7533137014274040205L;

    public static String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2, sizegroup toolbar";

    private static final String GUIINSTANCE = "GUI";

    private static final String PROPERTY_CHANGE_LISTENER = "PROPERTY_CHANGE_LISTENER";

    public static final int EAST = 1;
    public static final int WEST = 2;
    public static final int NORTH = 3;
    public static final int SOUTH = 4;

    public static final int FIRST_COL = -1;

    public static final int LAST_COL = -2;

    private String[] current = null;

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

    protected String getColConstraints(String[] list) {
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
    protected String getColConstraint(int col, String string) {
        switch (col) {
        case FIRST_COL:
            return halign == EAST ? "3[grow]5" : "3";
        case LAST_COL:
            return halign == EAST ? "" : "[grow,fill]";
        default:
            return "[]5";
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
                    ab.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", 16, 16));
                    if (!textPainted) {
                        ab.setText("");
                    }
                    String shortCut = action.getShortCutString();
                    ab.setToolTipText(action.getTooltipText() + (shortCut != null ? " [" + shortCut + "]" : ""));
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
                                String shortCut = action.getShortCutString();
                                ab.setToolTipText(action.getTooltipText() + (shortCut != null ? " [" + shortCut + "]" : ""));
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
    }

    /**
     * This method may be overridden to create custum toolbars
     * 
     * @param i
     * @param action
     * @return
     */
    public String getButtonConstraint(int i, ToolBarAction action) {
        if (halign == EAST) return BUTTON_CONSTRAINTS + ", alignx right";
        return BUTTON_CONSTRAINTS;
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
