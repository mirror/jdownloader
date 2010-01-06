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

package jd.gui.swing.components;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.JWindow;

import jd.gui.swing.GuiRunnable;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;

public class Balloon {
    private static final int GAP = 20;
    private static final int MAX = 5;
    private static ArrayList<JWindow> WINDOWS = null;
    public static int COUNTDOWN = 10 * 1000;
    private static String LASTSTRING;

    /**
     * Displays only if mainframe is hidden
     */
    public static void showIfHidden(final String title, final ImageIcon icon, final String htmlmessage) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui != null && !swingGui.getMainFrame().isActive()) {
            Balloon.show(title, icon, htmlmessage);
        }
    }

    public static void show(final String title, final ImageIcon icon, final String htmlmessage) {
        Balloon.show(title, null, icon, htmlmessage);
    }

    public static void show(final String title, final ImageIcon ii, final ImageIcon icon, final String htmlmessage) {
        if (LASTSTRING == null || !LASTSTRING.equals(title + htmlmessage)) {
            LASTSTRING = title + htmlmessage;
            show(title, ii, createDefault(icon, htmlmessage));
        }
    }

    public static void show(final String title, final ImageIcon ii, final JPanel panel) {
        if (!GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_SHOW_BALLOON, true)) return;

        final ImageIcon icon;
        if (ii == null) {
            icon = new ImageIcon(JDImage.getImage("logo/logo_16_16"));
        } else {
            icon = ii;
        }

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                final JWindow w = new JWindow() {

                    private static final long serialVersionUID = 8925461815465551749L;
                    private transient SwingWorker<Object, Object> timer;

                    @Override
                    public void dispose() {
                        Balloon.remove(this);
                        super.dispose();

                    }

                    @Override
                    public void setVisible(final boolean b) {

                        if (b) {
                            Balloon.add(this);
                            this.timer = new SwingWorker<Object, Object>() {

                                @Override
                                protected Object doInBackground() throws Exception {
                                    Thread.sleep(COUNTDOWN);
                                    return null;
                                }

                                @Override
                                public void done() {
                                    try {
                                        if (isVisible()) {
                                            setVisible(false);
                                            dispose();
                                        }
                                    } catch (Exception e) {
                                    }
                                }

                            };
                            timer.execute();
                        } else {
                            timer.cancel(true);
                            timer = null;
                        }
                        super.setVisible(b);
                    }

                };

                final JLabel lbl = new JLabel(title);
                lbl.setIcon(icon);

                final JLabel bt = new JLabel("[X]");
                bt.addMouseListener(new JDMouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        w.setVisible(false);
                        w.dispose();
                    }
                });

                final JPanel titlePanel = new JPanel(new MigLayout("ins 0", "[grow,fill][]"));
                titlePanel.addMouseListener(new JDMouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        SwingGui.getInstance().getMainFrame().setVisible(true);
                        SwingGui.getInstance().getMainFrame().toFront();
                    }
                });
                titlePanel.add(lbl);
                titlePanel.add(bt, "aligny top,alignx right");

                final JPanel container = new JPanel();
                container.setBorder(BorderFactory.createLineBorder(container.getBackground().darker()));
                container.setLayout(new MigLayout("ins 5,wrap 1", "[grow,fill]", "[][][grow,fill]"));
                container.add(titlePanel);
                container.add(new JSeparator(), "growx,pushx");
                container.add(panel);

                w.setMinimumSize(new Dimension(100, 40));
                w.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
                w.add(container);
                w.pack();
                w.setVisible(true);
                w.setAlwaysOnTop(true);
                return null;
            }

        }.start();
    }

    protected static void add(final JWindow window) {
        if (WINDOWS == null) {
            WINDOWS = new ArrayList<JWindow>();
        }
        synchronized (WINDOWS) {
            WINDOWS.add(window);
        }
        if (WINDOWS.size() > MAX) {
            JWindow win = WINDOWS.remove(0);
            win.dispose();
        } else {
            layout();
        }
    }

    private synchronized static void layout() {
        int width = 0;
        for (final JWindow w : WINDOWS) {
            try {
                width = Math.max(Math.min((int) w.getPreferredSize().getWidth(), 350), width);
            } catch (Exception e) {

            }
        }
        for (final JWindow w : WINDOWS) {
            try {
                w.setSize(width, w.getHeight());
            } catch (Exception e) {
            }
        }
        int y = 0;
        for (final JWindow w : WINDOWS) {
            try {
                locate(w, y);
                y += w.getHeight() + 3;
            } catch (Exception e) {
            }
        }
    }

    protected static void remove(final JWindow window) {
        synchronized (WINDOWS) {
            WINDOWS.remove(window);
        }
        layout();
    }

    private static void locate(final JWindow w, final int y) {
        final Point point = Screen.getDockBottomRight(w);
        point.x -= GAP;
        point.y -= (GAP + y);
        w.setLocation(point);
    }

    private static JPanel createDefault(final ImageIcon ii, final String string2) {
        return new GuiRunnable<JPanel>() {

            @Override
            public JPanel runSave() {
                final JPanel p = new JPanel(new MigLayout("ins 0", "[fill,grow]"));
                if (ii != null) {
                    p.add(new JLabel(ii), "split 2,alignx left, aligny top");
                }
                final JTextPane textField = new JTextPane();
                p.add(textField, "pushx,growx");
                textField.setContentType("text/html");
                textField.setBorder(null);
                textField.setOpaque(false);
                // textField.setOpaque(false);
                textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
                textField.setText(string2);
                textField.setEditable(false);
                textField.addHyperlinkListener(JLink.getHyperlinkListener());
                return p;
            }

        }.getReturnValue();
    }

}
