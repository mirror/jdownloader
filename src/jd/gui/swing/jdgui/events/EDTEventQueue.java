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

package jd.gui.swing.jdgui.events;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import jd.controlling.JDLogger;
import jd.gui.swing.components.MouseFollower;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class EDTEventQueue extends EventQueue {

    private int lastPoint = 0;

    private JPanel mouseOver;

    private JLabel lbl;

    public EDTEventQueue() {
        super();

        mouseOver = new JPanel(new MigLayout("ins 3"));
        mouseOver.add(lbl = new JLabel(JDTheme.II("gui.images.help", 24, 24)), "alignx left");
        mouseOver.setBorder(BorderFactory.createLineBorder(mouseOver.getBackground().darker()));

    }

    abstract class MenuAbstractAction extends AbstractAction {

        private static final long serialVersionUID = 1L;
        JTextComponent c;

        public MenuAbstractAction(JTextComponent c, String text, ImageIcon icon, String acc) {
            super(text);
            this.c = c;
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
            }

            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));

        }

        @Override
        public boolean isEnabled() {
            return c.isEditable() && c.isEnabled() && c.getSelectedText() != null;
        }
    }

    @Override
    protected void dispatchEvent(AWTEvent ev) {
        qh: if (ev instanceof MouseEvent) {
            MouseEvent e = (MouseEvent) ev;
            if ((e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) && lastPoint > 0) {
                lastPoint--;
                break qh;
            } else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.isControlDown() && e.isShiftDown()) {
                // Searches down the components and tries to find the deepest
                // component.
                // a stringbuilder grabs the component path and tries to find
                // the english expression.
                // Finally it calls the jdownloaderquickhelp page
                this.lastPoint = 2;
                Point point = e.getPoint();
                Component source = JDGui.getInstance().getMainFrame().getContentPane();
                point.x -= (source.getLocationOnScreen().x - JDGui.getInstance().getMainFrame().getLocationOnScreen().x);
                point.y -= (source.getLocationOnScreen().y - JDGui.getInstance().getMainFrame().getLocationOnScreen().y);
                final StringBuilder sb = new StringBuilder();
                while (source != null) {
                    Component source2 = source.getComponentAt(point);
                    if (source instanceof JTabbedPane) {
                        source2 = ((JTabbedPane) source).getSelectedComponent();
                    }
                    if (source2 == source || source2 == null) {
                        if (sb.length() > 0) {
                            new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        String url = "http://jdownloader.org/quickhelp/" + sb;
                                        JLink.openURL(url);
                                        return;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }

                                }
                            }.start();
                            return;
                        } else {
                            break;
                        }
                    }
                    if (source2 != null) {
                        point.x -= source2.getLocation().x;
                        point.y -= source2.getLocation().y;

                        if (source2.getName() != null) {
                            if (sb.length() > 0) sb.append(".");
                            String[] keys = JDL.getKeysFor(source2.getName());
                            if (keys == null || keys.length == 0) {
                                sb.append(source2.getName().replace(" ", "-"));
                            } else {
                                String def = JDL.getDefaultLocaleString(keys[0].toLowerCase().hashCode());
                                if (def == null) {
                                    sb.append(source2.getName().replace(" ", "-"));
                                } else {
                                    sb.append(Encoding.urlEncode(def.replace(" ", "-")));
                                }
                            }
                        }
                    }
                    source = source2;

                }

            } else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.isControlDown() && e.isAltDown()) {

                this.lastPoint = 2;
                Point point = e.getPoint();
                Component source = JDGui.getInstance().getMainFrame().getContentPane();
                point.x -= (source.getLocationOnScreen().x - JDGui.getInstance().getMainFrame().getLocationOnScreen().x);
                point.y -= (source.getLocationOnScreen().y - JDGui.getInstance().getMainFrame().getLocationOnScreen().y);
                StringBuilder sb2 = new StringBuilder();
                int i = 0;
                while (source != null) {
                    Component source2 = source.getComponentAt(point);
                    if (source instanceof JTabbedPane) {
                        source2 = ((JTabbedPane) source).getSelectedComponent();
                    }
                    if (source2 == source || source2 == null) {
                        System.out.println(sb2);
                        return;
                    }
                    if (source2 != null) {
                        point.x -= source2.getLocation().x;
                        point.y -= source2.getLocation().y;

                        if (source2 != source) {
                            sb2.append("\r\n" + Formatter.fillString("", " ", "", i * 3) + " - " + source2.getClass().getName() + "/" + (source2 instanceof JLabel) + "(" + source2.getName() + ")");
                            String text = null;
                            if (source2 instanceof JLabel) {
                                text = ((JLabel) source2).getText();
                            } else {
                                Method method = null;
                                try {
                                    method = source2.getClass().getMethod("getText", new Class[] {});
                                } catch (Exception e1) {
                                }
                                try {
                                    method = source2.getClass().getMethod("getTitle", new Class[] {});
                                } catch (Exception e1) {
                                }
                                if (method != null) {
                                    try {
                                        text = method.invoke(source2) + "";
                                    } catch (IllegalArgumentException e1) {
                                        e1.printStackTrace();
                                    } catch (IllegalAccessException e1) {
                                        e1.printStackTrace();
                                    } catch (InvocationTargetException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                            }
                            if (text != null) {
                                String[] keys = JDL.getKeysFor(text);
                                if (keys != null) {
                                    for (String t : keys) {
                                        sb2.append("\r\n" + Formatter.fillString("", " ", "", i * 3) + " Possible Translation: " + JDL.L(t, text) + " (" + t + ")");
                                    }

                                    JDLogger.getLogger().info(sb2 + "");

                                }
                                if (source2 instanceof JLabel) {
                                    ((JLabel) source2).setText(keys[0]);
                                }
                            }
                        }

                    }
                    source = source2;

                }
                return;
            } else if (e.getID() == MouseEvent.MOUSE_MOVED && e.isControlDown() && e.isShiftDown()) {

                Point point = e.getPoint();
                Component source = JDGui.getInstance().getMainFrame().getContentPane();

                point.x -= (source.getLocationOnScreen().x - JDGui.getInstance().getMainFrame().getLocationOnScreen().x);
                point.y -= (source.getLocationOnScreen().y - JDGui.getInstance().getMainFrame().getLocationOnScreen().y);
                while (source != null) {
                    Component source2 = source.getComponentAt(point);
                    if (source instanceof JTabbedPane) {
                        source2 = ((JTabbedPane) source).getSelectedComponent();
                    }
                    if (source == null || source2 == null || source2 == source) break;
                    source = source2;
                    point.x -= source.getLocation().x;
                    point.y -= source.getLocation().y;
                    if (source.getName() != null) {
                        lbl.setText(JDL.LF("gui.quickhelp.text", "Click for help: %s", source.getName()));
                        mouseOver.revalidate();
                        MouseFollower.show(mouseOver);
                        break;
                    }
                }
            } else {
                MouseFollower.hide();
            }

        }

        super.dispatchEvent(ev);
        if (!(ev instanceof MouseEvent)) return;

        MouseEvent e = (MouseEvent) ev;
        if (!e.isPopupTrigger()) return;
        if (e.getComponent() == null) return;
        Component c = null;
        Point point = e.getPoint();
        if (e.getSource() instanceof JDialog) {
            c = SwingUtilities.getDeepestComponentAt((JDialog) e.getSource(), (int) point.getX(), (int) point.getY());
        } else {
            Component source = JDGui.getInstance().getMainFrame().getContentPane();
            point.x -= (source.getLocationOnScreen().x - JDGui.getInstance().getMainFrame().getLocationOnScreen().x);
            point.y -= (source.getLocationOnScreen().y - JDGui.getInstance().getMainFrame().getLocationOnScreen().y);
            c = SwingUtilities.getDeepestComponentAt(source, (int) point.getX(), (int) point.getY());
        }
        if (!(c instanceof JTextComponent)) return;
        if (MenuSelectionManager.defaultManager().getSelectedPath().length > 0) return;
        final JTextComponent t = (JTextComponent) c;

        JPopupMenu menu = new JPopupMenu();

        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.cut", "Cut"), JDTheme.II("gui.icons.cut", 16, 16), JDL.L("gui.textcomponent.context.cut.acc", "ctrl X")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.cut();
            }

            @Override
            public boolean isEnabled() {
                return !(c instanceof JPasswordField);
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.copy", "Copy"), JDTheme.II("gui.icons.copy", 16, 16), JDL.L("gui.textcomponent.context.copy.acc", "ctrl C")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.copy();
            }

            @Override
            public boolean isEnabled() {
                return !(c instanceof JPasswordField) && c.isEnabled() && c.getSelectedText() != null;
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.paste", "Paste"), JDTheme.II("gui.icons.paste", 16, 16), JDL.L("gui.textcomponent.context.paste.acc", "ctrl V")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.paste();
            }

            @Override
            public boolean isEnabled() {
                if (c.isEditable() && c.isEnabled()) {
                    Transferable contents = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
                    return contents.isDataFlavorSupported(DataFlavor.stringFlavor);
                } else {
                    return false;
                }
            }
        });
        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.delete", "Delete"), JDTheme.II("gui.icons.delete", 16, 16), JDL.L("gui.textcomponent.context.delete.acc", "DELETE")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.replaceSelection(null);
            }
        });

        menu.add(new MenuAbstractAction(t, JDL.L("gui.textcomponent.context.selectall", "Select all"), JDTheme.II("gui.icons.select_all", 16, 16), JDL.L("gui.textcomponent.context.selectall.acc", "ctrl A")) {

            private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) {
                c.selectAll();
            }

            @Override
            public boolean isEnabled() {
                return c.isEnabled() && c.getText().length() > 0;
            }
        });

        Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), t);
        menu.show(t, pt.x, pt.y);
    }
}
