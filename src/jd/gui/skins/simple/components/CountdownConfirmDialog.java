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

package jd.gui.skins.simple.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.nutils.Formatter;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class CountdownConfirmDialog extends JDialog implements ActionListener, HyperlinkListener {

    private static final long serialVersionUID = 1L;

    public final static int STYLE_OK = 1 << 1;
    public final static int STYLE_CANCEL = 1 << 2;
    public final static int STYLE_YES = 1 << 3;
    public final static int STYLE_NO = 1 << 4;
    public final static int STYLE_STOP_COUNTDOWN = 1 << 5;
    public final static int STYLE_MSGLABLE = 1 << 6;
    public final static int STYLE_DETAILLABLE = 1 << 7;
    public final static int STYLE_NOTALWAYSONTOP = 1 << 8;
    public final static int STYLE_INPUTFIELD = 1 << 9;
    public final static int STYLE_NO_MSGLABLE = 1 << 10;

    public static boolean showCountdownConfirmDialog(JFrame owner, String msg, int countdown) {
        CountdownConfirmDialog d = new CountdownConfirmDialog(owner, msg, countdown);
        return d.result;
    }

    private JButton btnCnTh;
    private JButton btnOK;
    private JButton btnBAD;
    private JTextField inputField;
    private Thread countdownThread;

    private Component htmlArea;
    private boolean result = false;
    private JScrollPane scrollPane;

    private String titleText;
    private String input = null;

    public CountdownConfirmDialog(JFrame owner, String msg, int countdown) {
        this(owner, null, countdown, false, STYLE_OK | STYLE_CANCEL | STYLE_STOP_COUNTDOWN, msg);
    }

    public CountdownConfirmDialog(final JFrame owner, final String title, final int countdown, final boolean defaultResult, final int style, final String... msg) {
        super(owner);
        this.titleText = title;

        if (title != null) this.setTitle(title);

        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        setModal(true);
        if ((style & STYLE_NOTALWAYSONTOP) == 0) setAlwaysOnTop(true);

        setLayout(new MigLayout("", "[center]"));

        countdownThread = new Thread() {

            // @Override
            public void run() {

                while (!isVisible() && isDisplayable()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) { return; }
                    if (titleText != null) {
                        setTitle(Formatter.formatSeconds(c) + ">>" + titleText);
                    } else {
                        setTitle(Formatter.formatSeconds(c) + " mm:ss");
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) return;

                }
                result = defaultResult;
                setVisible(false);

            }

        };

        if ((style & STYLE_NO_MSGLABLE) == 0) {
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
        }
        if ((style & STYLE_INPUTFIELD) != 0) {
            if (msg != null) {
                if ((style & STYLE_NO_MSGLABLE) != 0) {
                    inputField = new JTextField(msg[0]);
                } else if (msg.length > 1) {
                    inputField = new JTextField(msg[1]);
                }
            }
            if (inputField == null) inputField = new JTextField();
            inputField.addKeyListener(new KeyListener() {

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                }

                public void keyTyped(KeyEvent e) {
                    if (countdownThread != null && countdownThread.isAlive()) {
                        countdownThread.interrupt();
                    }
                    countdownThread = null;
                }
            });
            input = inputField.getText();
        }

        if ((style & STYLE_NO_MSGLABLE) == 0) {
            scrollPane = new JScrollPane(htmlArea);
            this.add(scrollPane, "growx, span, wrap");
        }

        if ((style & STYLE_INPUTFIELD) != 0) this.add(inputField, "growx, span, wrap");

        if ((style & STYLE_DETAILLABLE) != 0) {
            final JButton btnDetails = new JButton(JDLocale.L("gui.btn_details", "details"));
            final JPanel pan = new JPanel(new MigLayout("ins 0"));
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
                    pan.add(sp, "cell 0 0");
                    pan.invalidate();
                    pan.repaint();
                    sp.repaint();
                    sp.validate();
                    pan.validate();
                    countdownThread = null;
                }
            });
            pan.add(btnDetails, "cell 0 0");
            this.add(pan, "growx, span, wrap");
        }

        if ((style & STYLE_STOP_COUNTDOWN) != 0) {
            btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
            btnCnTh.addActionListener(this);
            this.add(btnCnTh);
        }

        if ((style & STYLE_OK) != 0 || (style & STYLE_YES) != 0) {
            btnOK = new JButton((style & STYLE_YES) != 0 ? JDLocale.L("gui.btn_yes", "Ja") : JDLocale.L("gui.btn_ok", "OK"));
            btnOK.addActionListener(this);
            getRootPane().setDefaultButton(btnOK);
            this.add(btnOK);
        }

        if ((style & STYLE_CANCEL) != 0 || (style & STYLE_NO) != 0) {
            btnBAD = new JButton((style & STYLE_NO) != 0 ? JDLocale.L("gui.btn_no", "Nein") : JDLocale.L("gui.btn_cancel", "CANCEL"));
            btnBAD.addActionListener(this);
            this.add(btnBAD);
        }

        pack();

        setLocation(Screen.getCenterOfComponent(null, this));
        countdownThread.start();
        setVisible(true);
    }

    public boolean getResult() {
        return result;
    }

    public String getInput() {
        return input;
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {
            countdownThread = null;
        } else if (e.getSource() == btnOK) {
            result = true;
            if (inputField != null) input = inputField.getText();
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
            } catch (Exception e1) {
                JDLogger.exception(e1);
            }
        }
    }

}
