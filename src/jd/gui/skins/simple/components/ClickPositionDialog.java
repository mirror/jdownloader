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

import java.awt.Cursor;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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
import javax.swing.WindowConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ClickPositionDialog extends JDialog implements ActionListener, HyperlinkListener, MouseListener {

    private static final long serialVersionUID = 4827346842931L;

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private JButton btnBAD;
    private JButton btnCnTh;
    /**
     * Bestätigungsknopf
     */

    private Thread countdownThread;

    private JTextPane htmlArea;
    public Point result = new Point(-1, -1);

    private String titleText;
    private JLabel button;

    public ClickPositionDialog(final Frame owner, final File image, final String title, final String msg, final int countdown, final Point defaultResult) {
        super(owner);
        setModal(true);

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
        JDUtilities.addToGridBag(this, button, 0, 0, 3, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);

        if (msg != null) {
            htmlArea = new JTextPane();
            htmlArea.setEditable(false);
            htmlArea.setContentType("text/html");
            htmlArea.setText(msg);
            htmlArea.requestFocusInWindow();
            htmlArea.addHyperlinkListener(this);

            JDUtilities.addToGridBag(this, new JScrollPane(htmlArea), 0, 1, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        }

        int d = 0;

        btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
        btnCnTh.addActionListener(this);
        JDUtilities.addToGridBag(this, btnCnTh, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnBAD.addActionListener(this);
        JDUtilities.addToGridBag(this, btnBAD, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setAlwaysOnTop(true);
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        countdownThread.start();
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {
            countdownThread = null;
        } else if (e.getSource() == btnBAD) {
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

            JLinkButton.openURL(e.getURL());

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
