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

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.gui.skins.simple.GuiRunnable;
import jd.nutils.Formatter;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

public class ClickPositionDialog extends JDialog implements ActionListener, HyperlinkListener, MouseListener {

    private static final long serialVersionUID = 4827346842931L;

    @SuppressWarnings("unused")
    private static Logger logger = jd.controlling.JDLogger.getLogger();

    private JButton btnBAD;
    private JButton btnCnTh;
    /**
     * Bestätigungsknopf
     */

    private Thread countdownThread;

    private JTextPane htmlArea;
    public Point result = new Point(-1, -1);
    public boolean abort = false;

    private String titleText;
    private JLabel button;

    public static ClickPositionDialog show(final Frame owner, final File image, final String title, final String msg, final int countdown, final Point defaultResult) {
        synchronized (JDUtilities.userio_lock) {
            GuiRunnable<ClickPositionDialog> run = new GuiRunnable<ClickPositionDialog>() {
                // @Override
                public ClickPositionDialog runSave() {
                    return new ClickPositionDialog(owner, image, title, msg, countdown, defaultResult);
                }
            };
            return run.getReturnValue();
        }
    }

    private ClickPositionDialog(final Frame owner, final File image, final String title, final String msg, final int countdown, final Point defaultResult) {
        super(owner);
        setModal(true);

        setLayout(new MigLayout("wrap 1", "[center]"));

        countdownThread = new Thread() {

            // @Override
            public void run() {

                while (!isVisible() && isDisplayable()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
                    }
                }
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) { return; }
                    if (titleText != null) {

                        setTitle(Formatter.formatSeconds(c) + " mm:ss  >> " + titleText);
                    } else {
                        setTitle(Formatter.formatSeconds(c) + " mm:ss");
                    }

                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) {

                    return; }

                }
                result = defaultResult;
                dispose();

            }

        };
        this.titleText = title;

        if (title != null) {
            this.setTitle(title);
        }

        button = new JLabel(new ImageIcon(image.getAbsolutePath()));
        button.addMouseListener(this);
        button.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        button.setToolTipText(msg);
        this.add(button, "growx");

        if (msg != null) {
            htmlArea = new JTextPane();
            htmlArea.setEditable(false);
            htmlArea.setContentType("text/html");
            htmlArea.setText(msg);
            htmlArea.requestFocusInWindow();
            htmlArea.addHyperlinkListener(this);

            this.add(new JScrollPane(htmlArea), "growx");
        }

        btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
        btnCnTh.addActionListener(this);
        this.add(btnCnTh, "split 2");

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnBAD.addActionListener(this);
        this.add(btnBAD);

        setDefaultCloseOperation(ClickPositionDialog.DISPOSE_ON_CLOSE);
        this.setAlwaysOnTop(true);
        this.pack();
        this.setLocation(Screen.getCenterOfComponent(null, this));
        this.countdownThread.start();
        this.setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {
            countdownThread = null;
        } else if (e.getSource() == btnBAD) {
            abort = true;
            setVisible(false);
            dispose();
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

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
        this.result = e.getPoint();
        setVisible(false);
        dispose();
    }

}
