package jd.gui.swing.jdgui.events;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import jd.controlling.JDLogger;
import jd.gui.swing.components.MouseFollower;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class QuickHelp {

    private int lastPoint = 0;

    private final JPanel mouseOver;

    private final JLabel lbl;

    public QuickHelp() {
        lbl = new JLabel(JDTheme.II("gui.images.help", 24, 24));

        mouseOver = new JPanel(new MigLayout("ins 3"));
        mouseOver.add(lbl, "alignx left");
        mouseOver.setBorder(BorderFactory.createLineBorder(mouseOver.getBackground().darker()));
    }

    public void dispatchMouseEvent(MouseEvent e) {
        if ((e.getID() == MouseEvent.MOUSE_RELEASED || e.getID() == MouseEvent.MOUSE_CLICKED) && lastPoint > 0) {
            lastPoint--;
        } else if (e.getID() == MouseEvent.MOUSE_PRESSED && e.isControlDown() && e.isShiftDown()) {
            /*
             * Searches down the components and tries to find the deepest
             * component. a stringbuilder grabs the component path and tries to
             * find the english expression. Finally it calls the
             * jdownloaderquickhelp page
             */
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

}
