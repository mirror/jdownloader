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
import jd.gui.swing.jdgui.GraphicalUserInterfaceSettings;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.nutils.JDImage;
import jd.nutils.Screen;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.JsonConfig;
import org.jdesktop.swingworker.SwingWorker;
import org.jdownloader.gui.translate._GUI;

public class Balloon {
    private static final int          MAX       = 5;
    private static ArrayList<JWindow> WINDOWS   = null;
    public static int                 COUNTDOWN = 10 * 1000;
    private static String             LASTSTRING;

    /**
     * Displays only if mainframe is hidden
     */
    public static void showIfHidden(final String title, final ImageIcon icon, final String message) {
        final SwingGui swingGui = SwingGui.getInstance();
        if (swingGui != null && !swingGui.getMainFrame().isActive()) {
            Balloon.show(title, icon, message);
        }
    }

    public static void show(final String title, final ImageIcon icon, final String message) {
        if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isBalloonNotificationEnabled()) return;

        if (LASTSTRING != null && LASTSTRING.equals(title + message)) return;

        LASTSTRING = title + message;

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {

                final JWindow w = new JWindow() {

                    private static final long                     serialVersionUID = 8925461815465551749L;
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

                final JDMouseAdapter closeAdapter = new JDMouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        w.setVisible(false);
                        w.dispose();
                    }
                };

                final JLabel lblTitle = new JLabel(title);
                lblTitle.setIcon(new ImageIcon(JDImage.getImage("logo/logo_16_16")));
                lblTitle.setToolTipText(_GUI._.jd_gui_swing_components_Balloon_toolTip());
                lblTitle.addMouseListener(new JDMouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        SwingGui.getInstance().getMainFrame().setVisible(true);
                        SwingGui.getInstance().getMainFrame().toFront();
                    }
                });
                lblTitle.addMouseListener(closeAdapter);

                final JTextPane textField = new JTextPane();
                textField.setContentType("text/html");
                textField.setBorder(null);
                textField.setOpaque(false);
                textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
                textField.setText(message);
                textField.setEditable(false);
                textField.addHyperlinkListener(JLink.getHyperlinkListener());
                textField.addMouseListener(closeAdapter);

                final JPanel panel = new JPanel(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
                if (icon != null) panel.add(new JLabel(icon), "split 2, alignx left, aligny top");
                panel.add(textField, "growx, pushx");

                final JPanel container = new JPanel();
                container.setBorder(BorderFactory.createLineBorder(container.getBackground().darker()));
                container.setLayout(new MigLayout("ins 5,wrap 1", "[grow,fill]", "[][][grow,fill]"));
                container.add(lblTitle);
                container.add(new JSeparator(), "growx, pushx");
                container.add(panel, "wmax 338");
                container.addMouseListener(closeAdapter);

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
        int height = 0;
        for (final JWindow w : WINDOWS) {
            try {
                width = Math.max(Math.min((int) w.getPreferredSize().getWidth(), 350), width);
                height = Math.max(Math.min((int) w.getPreferredSize().getHeight(), 100), height);
            } catch (Exception e) {
            }
        }
        for (final JWindow w : WINDOWS) {
            try {
                w.setSize(width, height);
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
        point.y -= y;
        w.setLocation(point);
    }

}