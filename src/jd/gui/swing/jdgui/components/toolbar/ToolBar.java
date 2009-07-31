package jd.gui.swing.jdgui.components.toolbar;

import static jd.controlling.JDLogger.warning;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import jd.controlling.JDLogger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ActionControlEvent;
import jd.gui.swing.jdgui.actions.ActionController;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.actions.event.ActionControllerListener;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

public class ToolBar extends JToolBar implements ActionControllerListener {
    private static String[] defaultlist = new String[] {
            "toolbar.control.start",
            "toolbar.control.pause",
            "toolbar.control.stop",

            "toolbar.separator",
            "action.downloadview.movetotop",
            "action.downloadview.moveup",
            "action.downloadview.movedown",
            "action.downloadview.movetobottom",
            "toolbar.separator",
            "toolbar.quickconfig.clipboardoberserver",
            "toolbar.quickconfig.reconnecttoggle",

            "toolbar.separator",
            "toolbar.interaction.reconnect",
            "toolbar.interaction.update",

    };

    private static final long serialVersionUID = 7533137014274040205L;

    private static final String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private static final String GUIINSTANCE = "GUI";

    private static final String PROPERTY_CHANGE_LISTENER = "PROPERTY_CHANGE_LISTENER";

    private String[] current = null;

    private boolean updateing = false;

    private int preferredIconSize;

    public ToolBar() {
        this(24);

    }

    public ToolBar(int iconSize) {
        super(JToolBar.HORIZONTAL);
        preferredIconSize = iconSize;

        setRollover(true);
        setFloatable(false);

        ActionController.initActions();

        // this.updateToolbar();
        current = defaultlist;
        this.updateToolbar();
        // please add listener here. to avoid the toolbar beiong pained multible
        // times
        ActionController.getBroadcaster().addListener(this);

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

    private String getColConstraints(String[] list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.length; ++i) {
            sb.append("[]");
        }
        sb.append("[grow,fill]");
        return sb.toString();
    }

    private void initToolbar(String[] list) {
        synchronized (list) {
            SwingGui.checkEDT();
            setLayout(new MigLayout("ins 0, gap 0", getColConstraints(list)));
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
                        ab.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", preferredIconSize, preferredIconSize));

                        if (action.getValue(Action.MNEMONIC_KEY) != null) {
                            ab.setToolTipText(action.getTooltipText() + " [Alt+" + new String(new byte[]{((Integer) action.getValue(Action.MNEMONIC_KEY)).byteValue()}) + "]");
                      } else {
                            ab.setToolTipText(action.getTooltipText());
                        }

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
                                    if (action.getValue(Action.MNEMONIC_KEY) != null) {
                                        ab.setToolTipText(action.getTooltipText() + " [Alt+" + new String(new byte[]{((Integer) action.getValue(Action.MNEMONIC_KEY)).byteValue()}) + "]");
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
