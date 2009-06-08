package jd.gui.skins.simple;

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
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.config.SubConfiguration;
import jd.gui.skins.simple.components.JLinkButton;
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
    public static void showIfHidden(String title, ImageIcon icon, String htmlmessage) {
        if (!SimpleGUI.CURRENTGUI.isActive()) Balloon.show(title, icon, htmlmessage);
    }

    public static void show(String title, ImageIcon icon, String htmlmessage) {
        show(title, null, icon, htmlmessage);
    }

    public static void show(String title, ImageIcon ii, ImageIcon icon, String htmlmessage) {
        if (LASTSTRING != null && LASTSTRING.equals(title + htmlmessage)) return;
        LASTSTRING = title + htmlmessage;

        show(title, ii, createDefault(icon, htmlmessage));
    }

    public static void show(final String title, ImageIcon ii, final JPanel panel) {
        if (!SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty(SimpleGuiConstants.PARAM_SHOW_BALLOON, true)) return;

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

                w.setMinimumSize(new Dimension(100, 40));
                w.setLayout(new MigLayout("ins 0", "[grow,fill]", "[grow,fill]"));
                JPanel container = new JPanel();
                container.setBorder(BorderFactory.createLineBorder(container.getBackground().darker()));
                container.setLayout(new MigLayout("ins 5,wrap 1", "[grow,fill]", "[][][grow,fill]"));
                w.add(container);

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
                        SimpleGUI.CURRENTGUI.setVisible(true);
                        SimpleGUI.CURRENTGUI.toFront();
                    }
                });
                titlePanel.add(lbl);
                titlePanel.add(bt, "aligny top,alignx right");

                container.add(titlePanel);
                container.add(new JSeparator(), "growx,pushx");
                container.add(panel);

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

    private static JPanel createDefault(ImageIcon ii2, String string2) {
        JPanel p = new JPanel(new MigLayout("ins 0", "[fill,grow]"));
        if (ii2 != null) {
            p.add(new JLabel(ii2), "split 2,alignx left, aligny top");
        }
        JTextPane textField;
        textField = new JTextPane();
        p.add(textField, "pushx,growx");
        textField.setContentType("text/html");

        textField.setBorder(null);
        textField.setOpaque(false);
        // textField.setOpaque(false);
        textField.setText(string2);
        textField.setEditable(false);

        textField.addHyperlinkListener(new HyperlinkListener() {

            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        JLinkButton.openURL(e.getURL());
                    } catch (Exception e1) {
                    }
                }
            }

        });
        return p;
    }

}
