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

package jd.gui.swing.jdgui.views.settings.sidebar;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.menu.WindowMenu;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ConfigEventListener;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.translate.JDT;

public class ConfigSidebar extends JPanel implements ControlListener, MouseMotionListener, MouseListener, ConfigEventListener {

    private static final long serialVersionUID = 6456662020047832983L;

    private JList             list;

    private Point             mouse;

    public ConfigSidebar() {
        super(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        JDController.getInstance().addControlListener(this);
        list = new JList() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (mouse != null) {
                    final Graphics2D g2 = (Graphics2D) g;
                    final AlphaComposite ac5 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
                    g2.setComposite(ac5);
                    int index = locationToIndex(mouse);
                    if (getModel().getElementAt(index) instanceof ExtensionHeader) { return; }
                    Point p = indexToLocation(index);
                    g2.fillRect(0, p.y, list.getWidth(), 55);

                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

                }

            }

            // public Dimension getPreferredScrollableViewportSize() {
            //
            // return this.getPreferredSize();
            // }
            //
            // public int getScrollableBlockIncrement(final Rectangle
            // visibleRect, final int orientation, final int direction) {
            // return Math.max(visibleRect.height * 9 / 10, 1);
            // }
            //
            // public boolean getScrollableTracksViewportHeight() {
            //
            // return false;
            // }
            //
            // public boolean getScrollableTracksViewportWidth() {
            // return true;
            // }
            //
            // public int getScrollableUnitIncrement(final Rectangle
            // visibleRect, final int orientation, final int direction) {
            // return Math.max(visibleRect.height / 10, 1);
            // }
        };
        list.addMouseMotionListener(this);
        list.addMouseListener(this);
        list.setModel(new ConfigListModel());
        list.setCellRenderer(new TreeRenderer());

        list.setOpaque(false);
        list.setBackground(null);

        list.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (list.getModel().getElementAt(index) instanceof AbstractExtensionWrapper) {
                    Point point = list.indexToLocation(index);
                    int x = e.getPoint().x - point.x;
                    int y = e.getPoint().y - point.y;
                    if (x > 3 && x < 18 && y > 3 && y < 18) {

                        list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        list.setToolTipText(JDT._.settings_sidebar_tooltip_enable_extension());
                    } else {
                        list.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        if (list.getModel().getElementAt(index) instanceof AbstractExtensionWrapper) {
                            list.setToolTipText(((AbstractExtensionWrapper) list.getModel().getElementAt(index)).getDescription());

                        } else {
                            list.setToolTipText(null);
                        }
                    }
                }
            }
        });
        setBackground(null);
        setOpaque(false);
        JScrollPane sp;
        this.add(sp = new JScrollPane(list));

        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
        if (c >= 0) {
            list.setBackground(new Color(c));
            list.setOpaque(true);
        }
        sp.setBorder(null);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                SwitchPanel op = getSelectedPanel();
                if (op instanceof ExtensionConfigPanel) {
                    ((ExtensionConfigPanel<?>) op).getExtension().getSettings().removeListener(ConfigSidebar.this);
                    ((ExtensionConfigPanel<?>) op).getExtension().getSettings().addListener(ConfigSidebar.this);
                }
            }
        });

    }

    public void addListener(ListSelectionListener x) {
        list.getSelectionModel().addListSelectionListener(x);
    }

    // @Override
    // public void setOpaque(boolean isOpaque) {
    // // super.setOpaque(isOpaque);
    // }

    // public boolean isOpaque() {
    // return true;
    // }

    public void controlEvent(ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_SYSTEM_EXIT) {
            saveCurrentState();
        }
    }

    public void setSelectedTreeEntry(Class<?> class1) {
        for (int i = 0; i < list.getModel().getSize(); i++) {
            if (class1 == list.getModel().getElementAt(i).getClass()) {
                list.setSelectedIndex(i);
                break;
            }
        }
    }

    /**
     * Saves the selected ConfigPanel
     */
    private void saveCurrentState() {
        /* getPanel is null in case the user selected a rootnode */
        // SwitchPanel panel = ((TreeEntry)
        // tree.getLastSelectedPathComponent()).getPanel();
        // if (panel == null) return;
        // GUIUtils.getConfig().setProperty(PROPERTY_LAST_PANEL,
        // panel.getClass().getName());
        // GUIUtils.getConfig().save();
    }

    /**
     * Updates the Addon subtree
     */
    public void updateAddons() {

    }

    public void mouseDragged(MouseEvent e) {
        mouse = e.getPoint();
        list.repaint();

    }

    public void mouseMoved(MouseEvent e) {
        mouse = e.getPoint();
        list.repaint();
    }

    public void mouseClicked(MouseEvent e) {
        // mouse = null;
        // list.repaint();
    }

    public void mousePressed(MouseEvent e) {

        int index = list.locationToIndex(e.getPoint());
        if (list.getModel().getElementAt(index) instanceof AbstractExtensionWrapper) {
            Point point = list.indexToLocation(index);
            int x = e.getPoint().x - point.x;
            int y = e.getPoint().y - point.y;
            if (x > 3 && x < 18) {
                if (y > 3 && y < 18) {

                    AbstractExtensionWrapper object = ((AbstractExtensionWrapper) list.getModel().getElementAt(index));
                    boolean value = !((AbstractExtensionWrapper) list.getModel().getElementAt(index))._isEnabled();
                    if (value == object._isEnabled()) return;
                    if (value) {
                        try {
                            object._setEnabled(true);

                            if (object._getExtension().getGUI() != null) {
                                int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, object.getName(), JDT._.gui_settings_extensions_show_now(object.getName()));

                                if (UserIO.isOK(ret)) {
                                    // activate panel
                                    object._getExtension().getGUI().setActive(true);
                                    // bring panel to front
                                    object._getExtension().getGUI().toFront();

                                }
                            }
                        } catch (StartException e1) {
                            Dialog.getInstance().showExceptionDialog(JDT._.dialog_title_exception(), e1.getMessage(), e1);
                        } catch (StopException e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        try {

                            object._setEnabled(false);
                        } catch (StartException e1) {
                            e1.printStackTrace();
                        } catch (StopException e1) {
                            Dialog.getInstance().showExceptionDialog(JDT._.dialog_title_exception(), e1.getMessage(), e1);
                        }
                    }
                    /*
                     * we save enabled/disabled status here, plugin must be
                     * running when enabled
                     */

                    AddonsMenu.getInstance().update();
                    WindowMenu.getInstance().update();
                    // ConfigSidebar.getInstance(null).updateAddons();
                    // addons.updateShowcase();

                    list.repaint();
                }
            }
        }
    }

    public void mouseReleased(MouseEvent e) {
        // mouse = null;
        // list.repaint();
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
        mouse = null;
        list.repaint();
    }

    public synchronized SwitchPanel getSelectedPanel() {
        if (list.getSelectedValue() instanceof AbstractExtensionWrapper) {
            AbstractExtension ext = ((AbstractExtensionWrapper) list.getSelectedValue())._getExtension();
            if (ext == null) {
                try {
                    ((AbstractExtensionWrapper) list.getSelectedValue()).init();
                    ext = ((AbstractExtensionWrapper) list.getSelectedValue())._getExtension();
                } catch (Exception e) {
                    Log.exception(e);
                    Dialog.getInstance().showExceptionDialog("Error", e.getMessage(), e);
                    return null;
                }

            }
            if (ext.hasConfigPanel()) {
                return ext.getConfigPanel();

            } else {
                return new EmptyExtensionConfigPanel(ext);
            }

        } else if (list.getSelectedValue() instanceof SwitchPanel) {
            return (SwitchPanel) list.getSelectedValue();
        } else {
            return null;
        }

    }

    public void onConfigValueModified(ConfigInterface config, String key, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                repaint();
            }
        };
    }
}
