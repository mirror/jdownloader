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

package jd.gui.skins.jdgui.components;

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

import jd.gui.skins.SwingGui;
import jd.gui.skins.jdgui.GUIUtils;
import jd.gui.skins.jdgui.JDGuiConstants;
import jd.gui.skins.jdgui.components.linkbutton.JLink;
import jd.gui.skins.jdgui.interfaces.JDMouseAdapter;
import jd.gui.skins.jdgui.swing.GuiRunnable;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;

public class Balloon {
    private static final int GAP = 20;
    private static final int MAX = 5;
    private static ArrayList<JWindow> WINDOWS = null;
    public static int COUNTDOWN = 10 * 1000;
    private static String LASTSTRING;

    public static void main(String args[]) throws InterruptedException {
        Balloon.show("AccountController", JDTheme.II("gui.images.accounts", 32, 32), "Premiumaccounts are globally disabled!<br/>Click <a href='http://jdownloader.org/knowledge/wiki/gui/premiummenu'>here</a> for help.");

        Thread.sleep(2 * 1000);

        Balloon.show("title", null, JDTheme.II("gui.images.help", 32, 32), "This is <b>just dummy</b><br/> text.<a href='http://www.google.de'>LINK</a> you added 5 links");
    }

    /**
     * Displays only if mainframe is hidden
     */
    public static void showIfHidden(String title, ImageIcon icon, String htmlmessage) {
        if (SwingGui.getInstance() != null && !SwingGui.getInstance().isActive()) Balloon.show(title, icon, htmlmessage);
    }

    public static void show(String title, ImageIcon icon, String htmlmessage) {
        Balloon.show(title, null, icon, htmlmessage);
    }

    public static void show(String title, ImageIcon ii, ImageIcon icon, String htmlmessage) {
        if (LASTSTRING != null && LASTSTRING.equals(title + htmlmessage)) return;
        LASTSTRING = title + htmlmessage;

        show(title, ii, createDefault(icon, htmlmessage));
    }

    public static void show(final String title, ImageIcon ii, final JPanel panel) {
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
                    private SwingWorker<Object, Object> timer;

                    public void dispose() {
                        Balloon.remove(this);
                        super.dispose();

                    }

                    public void setVisible(boolean b) {

                        if (b) {
                            Balloon.add(this);
                            this.timer = new SwingWorker<Object, Object>() {

                                @Override
                                protected Object doInBackground() throws Exception {
                                    Thread.sleep(COUNTDOWN);

                                    return null;
                                }

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

                JLabel lbl = new JLabel(title);
                lbl.setIcon(icon);

                JLabel bt = new JLabel("[X]");
                bt.addMouseListener(new JDMouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        w.setVisible(false);
                        w.dispose();
                    }
                });

                JPanel titlePanel = new JPanel(new MigLayout("ins 0", "[grow,fill][]"));
                titlePanel.addMouseListener(new JDMouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        SwingGui.getInstance().setVisible(true);
                        SwingGui.getInstance().toFront();
                    }
                });
                titlePanel.add(lbl);
                titlePanel.add(bt, "aligny top,alignx right");

                JPanel container = new JPanel();
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

    protected static void add(JWindow window) {
        if (WINDOWS == null) WINDOWS = new ArrayList<JWindow>();
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
        int y = 0;
        int width = 0;
        for (JWindow w : WINDOWS) {
            try {
                width = Math.max(Math.min((int) w.getPreferredSize().getWidth(), 350), width);
            } catch (Exception e) {

            }
        }
        for (JWindow w : WINDOWS) {
            try {
                w.setSize(width, w.getHeight());
            } catch (Exception e) {

            }

        }

        for (JWindow w : WINDOWS) {
            try {
                locate(w, y);
                y += w.getHeight() + 3;

            } catch (Exception e) {

            }
        }
    }

    protected static void remove(JWindow window) {
        synchronized (WINDOWS) {
            WINDOWS.remove(window);
        }
        layout();

    }

    private static void locate(JWindow w, int y) {
        Point point = Screen.getDockBottomRight(w);
        point.x -= GAP;
        point.y -= (GAP + y);
        w.setLocation(point);

    }

    private static JPanel createDefault(final ImageIcon ii, final String string2) {
        return new GuiRunnable<JPanel>() {

            @Override
            public JPanel runSave() {

                JPanel p = new JPanel(new MigLayout("ins 0", "[fill,grow]"));
                if (ii != null) {
                    p.add(new JLabel(ii), "split 2,alignx left, aligny top");
                }
                JTextPane textField = new JTextPane();
                p.add(textField, "pushx,growx");
                textField.setContentType("text/html");
                textField.setBorder(null);
                textField.setOpaque(false);
                // textField.setOpaque(false);
                textField.setText(string2);
                textField.setEditable(false);
                textField.addHyperlinkListener(JLink.getHyperlinkListener());
                return p;
            }
        }.getReturnValue();

    }

}
