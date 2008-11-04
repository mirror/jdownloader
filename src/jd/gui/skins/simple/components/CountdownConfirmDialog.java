//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.gui.skins.simple.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class CountdownConfirmDialog extends JDialog implements ActionListener, HyperlinkListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private static final long serialVersionUID = 1L;
    public final static int STYLE_OK = 1 << 1;
    public final static int STYLE_CANCEL = 1 << 2;
    public final static int STYLE_YES = 1 << 3;
    public final static int STYLE_NO = 1 << 4;
    public final static int STYLE_STOP_COUNTDOWN = 1 << 5;
    public final static int STYLE_MSGLABLE = 1 << 6;
    public final static int STYLE_DETAILLABLE = 1 << 7;

    public static boolean showCountdownConfirmDialog(Frame owner, String msg, int countdown) {
        CountdownConfirmDialog d = new CountdownConfirmDialog(owner, msg, countdown);

        return d.result;
    }

    private JButton btnBAD;
    private JButton btnCnTh;
    /**
     * Bestätigungsknopf
     */
    private JButton btnOK;
    private Thread countdownThread;

    private Component htmlArea;
    public boolean result = false;
    private JScrollPane scrollPane;
    public boolean window_Closed = false;
    private String titleText;

    public CountdownConfirmDialog(Frame owner, String msg, int countdown) {
        this(owner, msg, countdown, false, STYLE_OK | STYLE_CANCEL | STYLE_STOP_COUNTDOWN);
    }

    public CountdownConfirmDialog(final Frame owner, final String msg, final int countdown, final boolean defaultResult, final int style) {
        this(owner, null, msg, countdown, defaultResult, style);
    }

    public CountdownConfirmDialog(final Frame owner, final String title, final String msg, final int countdown, final boolean defaultResult, final int style) {
        this(owner, title, countdown, defaultResult, style, msg);
    }

    public CountdownConfirmDialog(final Frame owner, final String title, final int countdown, final boolean defaultResult, final int style, final String... msg) {
        super(owner);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setModal(true);
        setAlwaysOnTop(true);
        addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
                window_Closed = true;
                setVisible(false);
            }

            public void windowClosing(WindowEvent e) {
            }

            public void windowDeactivated(WindowEvent e) {
            }

            public void windowDeiconified(WindowEvent e) {
            }

            public void windowIconified(WindowEvent e) {
            }

            public void windowOpened(WindowEvent e) {
            }
        });
        setLayout(new GridBagLayout());

        countdownThread = new Thread() {

            @Override
            public void run() {

                while (!isVisible() && isDisplayable()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) { return; }
                    if (titleText != null) {
                        setTitle(JDUtilities.formatSeconds(c) + " mm:ss  >> " + titleText);
                    } else {
                        setTitle(JDUtilities.formatSeconds(c) + " mm:ss");
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) {

                    return; }

                }
                result = defaultResult;
                setVisible(false);

            }

        };
        this.titleText = title;

        if (title != null) {
            this.setTitle(title);
        }
        if ((style & STYLE_MSGLABLE) != 0) {
            htmlArea = new JLabel(msg[0]);
        } else {
            htmlArea = new JTextPane();
            ((JTextPane) htmlArea).setEditable(false);
            ((JTextPane) htmlArea).setContentType("text/html");
            ((JTextPane) htmlArea).setText(msg[0]);
            ((JTextPane) htmlArea).requestFocusInWindow();
            ((JTextPane) htmlArea).addHyperlinkListener(this);

        }

        scrollPane = new JScrollPane(htmlArea);
        JDUtilities.addToGridBag(this, scrollPane, 0, 0, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        if ((style & STYLE_DETAILLABLE) != 0) {
            final JButton btnDetails = new JButton(JDLocale.L("gui.btn_details", "details"));
            final JPanel pan = new JPanel();
            btnDetails.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    JTextArea detailLable = new JTextArea();
                    detailLable.setText(msg[1]);
                    detailLable.setEditable(false);

                    JScrollPane sp = new JScrollPane(detailLable, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
                    sp.setPreferredSize(new Dimension(getWidth() - 20, 200));
                    // detailLable.setLineWrap(true);
                    setSize(new Dimension(getWidth(), getHeight() + 200 - btnDetails.getHeight()));
                    pan.remove(btnDetails);
                    pan.invalidate();
                    pan.repaint();
                    pan.add(sp);
                    sp.repaint();
                    sp.validate();
                    pan.validate();
                    countdownThread = null;
                }
            });
            pan.add(btnDetails);
            JDUtilities.addToGridBag(this, pan, 0, 1, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        }
        int d = 0;
        if ((style & STYLE_STOP_COUNTDOWN) != 0) {
            btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
            btnCnTh.addActionListener(this);
            JDUtilities.addToGridBag(this, btnCnTh, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        }
        if ((style & STYLE_OK) != 0 || (style & STYLE_YES) != 0) {
            btnOK = new JButton((style & STYLE_YES) != 0 ? JDLocale.L("gui.btn_yes", "Ja") : JDLocale.L("gui.btn_ok", "OK"));
            btnOK.addActionListener(this);
            getRootPane().setDefaultButton(btnOK);

            JDUtilities.addToGridBag(this, btnOK, d++, 2, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }
        if ((style & STYLE_CANCEL) != 0 || (style & STYLE_NO) != 0) {
            btnBAD = new JButton((style & STYLE_NO) != 0 ? JDLocale.L("gui.btn_no", "Nein") : JDLocale.L("gui.btn_cancel", "CANCEL"));
            btnBAD.addActionListener(this);
            JDUtilities.addToGridBag(this, btnBAD, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        countdownThread.start();
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {
            countdownThread = null;
        } else if (e.getSource() == btnOK) {
            result = true;
            setVisible(false);
        } else if (e.getSource() == btnBAD) {
            setVisible(false);
        }

        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
        countdownThread = null;
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                JLinkButton.openURL(e.getURL());
            } catch (BrowserLaunchingInitializingException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (UnsupportedOperatingSystemException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

}
