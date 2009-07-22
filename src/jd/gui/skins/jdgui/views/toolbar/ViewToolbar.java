package jd.gui.skins.jdgui.views.toolbar;

import static jd.controlling.JDLogger.warning;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;

import jd.controlling.JDLogger;
import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.actions.ActionControlEvent;
import jd.gui.skins.jdgui.actions.ActionController;
import jd.gui.skins.jdgui.actions.ToolBarAction;
import jd.gui.skins.jdgui.actions.event.ActionControllerListener;
import jd.gui.skins.jdgui.borders.JDBorderFactory;
import jd.gui.skins.simple.GuiRunnable;
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

    private static String BUTTON_CONSTRAINTS = "gaptop 2, gapleft 2";

    private static final String GUIINSTANCE = "GUI";

    private static final String PROPERTY_CHANGE_LISTENER = "PROPERTY_CHANGE_LISTENER";

    public static final int EAST = 1;
    public static final int WEST = 2;
    public static final int NORTH = 3;
    public static final int SOUTH = 4;

    private String[] current = null;

    private boolean updateing = false;

    private int halign = ViewToolbar.WEST;

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
        
        if(halign == EAST){
            sb.append("[grow]");
            for (int i = 0; i < list.length; ++i) {
                sb.append("[]");
            }
         
        }else{
            for (int i = 0; i < list.length; ++i) {
                sb.append("[]");
            }
            sb.append("[grow,fill]");
        }
       
        return sb.toString();
    }

    private void initToolbar(String[] list) {
        synchronized (list) {
            SwingGui.checkEDT();
            setLayout(new MigLayout("ins 0, gap 0", getColConstraints(list)));
            AbstractButton ab;
            JToggleButton tbt;
            if (list != null) {

                if (halign == EAST) {
                    BUTTON_CONSTRAINTS += ", alignx right";
                }
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
                        // ab.setText("");
                        break;
                    case SEPARATOR:
                        add(new JSeparator(JSeparator.VERTICAL), "height 32,gapleft 10,gapright 10");
                        break;

                    case TOGGLE:

                        add(ab = tbt = new JToggleButton(action), BUTTON_CONSTRAINTS);
                        // tbt.setText("");
                        break;

                    }
                    if (ab != null) {
                        // ab.setText("");
                        // ab.setContentAreaFilled(false);

                        ab.addMouseListener(new MouseListener() {

                            public void mouseClicked(MouseEvent e) {
                                // TODO Auto-generated method stub

                            }

                            public void mouseEntered(MouseEvent e) {
                                // ((AbstractButton)e.getSource()).setContentAreaFilled(true);
                            }

                            public void mouseExited(MouseEvent e) {
                                // ((AbstractButton)e.getSource()).setContentAreaFilled(false);

                            }

                            public void mousePressed(MouseEvent e) {
                                // TODO Auto-generated method stub

                            }

                            public void mouseReleased(MouseEvent e) {
                                // TODO Auto-generated method stub

                            }

                        });
                        ab.setFocusPainted(false);
                        ab.setIcon(JDTheme.II(action.getValue(ToolBarAction.IMAGE_KEY) + "", 16, 16));
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
                                    // ab.setText("");
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

    }

}
