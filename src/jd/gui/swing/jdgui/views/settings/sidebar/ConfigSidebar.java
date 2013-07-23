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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings;
import net.miginfocom.swing.MigLayout;

import org.appwork.controlling.SingleReachableState;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.ConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.gui.LAFOptions;

public class ConfigSidebar extends JPanel implements MouseMotionListener, MouseListener, ConfigEventListener {

    private static final long    serialVersionUID = 6456662020047832983L;

    private JList                list;

    private Point                mouse;

    private SettingsSidebarModel treemodel        = null;

    public ConfigSidebar() {
        super(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
        list = new JList() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Point lmouse = mouse;
                if (lmouse != null) {
                    final Graphics2D g2 = (Graphics2D) g;

                    final AlphaComposite ac5 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f);
                    g2.setComposite(ac5);
                    int index = locationToIndex(lmouse);

                    if (index >= 0 && getModel().getElementAt(index) instanceof ExtensionHeader) { return; }
                    if (index >= 0 && getModel().getElementAt(index) instanceof AdvancedSettings) {
                        Point p = indexToLocation(index);
                        if (p != null) {
                            g2.fillRect(0, p.y, list.getWidth(), TreeRenderer.SMALL_DIMENSION.height);
                            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        }

                    } else {
                        Point p = indexToLocation(index);
                        if (p != null) {
                            g2.fillRect(0, p.y, getWidth(), TreeRenderer.DIMENSION.height);

                            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                        }
                    }
                }
                ConfigSidebar.this.revalidate();
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
        list.setModel(treemodel = new SettingsSidebarModel(list));
        // treemodel.addListDataListener(new ListDataListener() {
        //
        // @Override
        // public void intervalRemoved(ListDataEvent e) {
        // }
        //
        // @Override
        // public void intervalAdded(ListDataEvent e) {
        // }
        //
        // @Override
        // public void contentsChanged(ListDataEvent e) {
        // System.out.println("Updated");
        // revalidate();
        // }
        // });
        list.setCellRenderer(new TreeRenderer());

        list.setOpaque(false);
        list.setBackground(null);

        list.addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                try {
                    if (list.getModel().getElementAt(index) instanceof CheckBoxedEntry) {
                        Point point = list.indexToLocation(index);
                        int x = 0;
                        int y = 0;
                        if (point != null) {
                            x = e.getPoint().x - point.x;
                            y = e.getPoint().y - point.y;
                        }
                        if (x > 3 && x < 18 && y > 3 && y < 18) {

                            list.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                            list.setToolTipText(_JDT._.settings_sidebar_tooltip_enable_extension());
                        } else {
                            list.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            if (list.getModel().getElementAt(index) instanceof CheckBoxedEntry) {
                                list.setToolTipText(((CheckBoxedEntry) list.getModel().getElementAt(index)).getDescription());

                            } else {
                                list.setToolTipText(null);
                            }
                        }
                    }
                } catch (final ArrayIndexOutOfBoundsException e2) {
                }
            }
        });
        setBackground(null);
        setOpaque(false);
        JScrollPane sp;

        this.add(sp = new JScrollPane(list) {

            public Dimension getPreferredSize() {
                Dimension ret = super.getPreferredSize();
                ret.width = Math.max(TreeRenderer.DIMENSION.width, TreeRenderer.SMALL_DIMENSION.width) + getVerticalScrollBar().getPreferredSize().width;
                return ret;
            }

            public Dimension getMinimumSize() {
                Dimension pref = getPreferredSize();
                if (pref.height == 0) return pref;
                pref = new Dimension(pref);
                pref.height = 0;
                super.setMinimumSize(pref);
                return pref;
            }

        });

        LAFOptions.getInstance().applyBackground(LAFOptions.getInstance().getColorForPanelBackground(), list);
        sp.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, (LAFOptions.getInstance().getColorForPanelHeaderLine())));
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            private String lastE;

            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) { return; }

                if (lastE != null) {

                    if (lastE.equals(e.toString())) {

                    return; }
                }

                lastE = e.toString();
                SwitchPanel op = getSelectedPanel();
                if (op instanceof ExtensionConfigPanel) {
                    ((ExtensionConfigPanel<?>) op).getExtension().getSettings()._getStorageHandler().getEventSender().addListener(ConfigSidebar.this, true);
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

    public void setSelectedTreeEntry(Class<?> class1) {
        for (int i = 0; i < list.getModel().getSize(); i++) {
            Object el = list.getModel().getElementAt(i);
            if (el instanceof LazyExtension) {
                if (class1.getName().equals(((LazyExtension) el).getClassname())) {
                    list.setSelectedIndex(i);
                    return;
                }
            } else {
                Class<? extends Object> cl = el.getClass();
                if (class1 == cl) {
                    list.setSelectedIndex(i);
                    return;
                }
            }
        }
        list.setSelectedIndex(0);
    }

    /**
     * Updates the Addon subtree
     */
    public void updateAddons() {
        treemodel.fill(false);
    }

    public SingleReachableState getTreeCompleteState() {
        return treemodel.getTreeCompleteState();
    }

    public boolean treeInitiated() {
        return treemodel.getSize() > 0;
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
        if (list.getModel().getElementAt(index) instanceof CheckBoxedEntry) {
            Point point = list.indexToLocation(index);
            int x = e.getPoint().x - point.x;
            int y = e.getPoint().y - point.y;
            if (x > 3 && x < 18) {
                if (y > 3 && y < 18) {

                    CheckBoxedEntry object = ((CheckBoxedEntry) list.getModel().getElementAt(index));
                    boolean value = !((CheckBoxedEntry) list.getModel().getElementAt(index))._isEnabled();
                    if (value == object._isEnabled()) return;
                    // ugly...
                    // we should refactor the CheckBoxedEntry pretty soon..
                    if (object instanceof LazyExtension) {
                        ExtensionController.getInstance().setEnabled((LazyExtension) object, value);
                    } else {
                        try {
                            object._setEnabled(value);
                        } catch (StartException e1) {
                            e1.printStackTrace();
                        } catch (StopException e1) {
                            e1.printStackTrace();
                        }
                    }
                    /*
                     * we save enabled/disabled status here, plugin must be running when enabled
                     */

                    // AddonsMenu.getInstance().onUpdated();

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
        if (list.getSelectedValue() instanceof AbstractExtension) {
            AbstractExtension<?, ?> ext = ((AbstractExtension) list.getSelectedValue());
            if (ext.hasConfigPanel()) {
                return ext.getConfigPanel();

            } else {
                return new EmptyExtensionConfigPanel(ext);
            }

        } else if (list.getSelectedValue() instanceof LazyExtension) {
            AbstractExtension<?, ?> ext = ((LazyExtension) list.getSelectedValue())._getExtension();
            if (ext == null) {
                try {
                    ((LazyExtension) list.getSelectedValue()).init();
                    ext = ((LazyExtension) list.getSelectedValue())._getExtension();
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

    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                repaint();
            }
        };
    }

    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }
}
