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
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class CountdownConfirmDialog extends JDialog implements ActionListener, HyperlinkListener {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * Bestätigungsknopf
     */
    private JButton btnOK;

    private JButton btnBAD;

    private Thread countdownThread;

    private Component htmlArea;

    private JScrollPane scrollPane;

    public boolean result = false;

    private JButton btnCnTh;
    public final static int STYLE_OK = 1 << 1;
    public final static int STYLE_CANCEL = 1 << 2;
    public final static int STYLE_STOP_COUNTDOWN = 1 << 3;
    public final static int STYLE_MSGLABLE = 1 << 4;

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();
    public CountdownConfirmDialog(Frame owner, String msg, int countdown)
    {
        this(owner, msg, countdown, false, STYLE_OK | STYLE_CANCEL | STYLE_STOP_COUNTDOWN);
    }
    public CountdownConfirmDialog(final Frame owner, final String msg, final int countdown, final boolean defaultResult, final int style) {
        super(owner);
        setModal(true);

        setLayout(new GridBagLayout());

        this.countdownThread = new Thread() {

            public void run() {
                int c = countdown;

                while (--c >= 0) {
                    if (countdownThread == null) return;
                    setTitle(JDUtilities.formatSeconds(c) + " mm:ss");

                    try {
                        Thread.sleep(1000);

                    } catch (InterruptedException e) {
                    }
                    if (!isVisible()) return;

                }
                result = defaultResult;
                dispose();

            }

        };
        this.countdownThread.start();
        if ((style & STYLE_MSGLABLE) != 0) {
            htmlArea = new JLabel(msg);
        } else {
            htmlArea = new JTextPane();
            ((JTextPane) htmlArea).setEditable(false);
            ((JTextPane) htmlArea).setContentType("text/html");
            ((JTextPane) htmlArea).setText(msg);
            ((JTextPane) htmlArea).requestFocusInWindow();
            ((JTextPane) htmlArea).addHyperlinkListener(this);
            
        }
        scrollPane = new JScrollPane(htmlArea);
        JDUtilities.addToGridBag(this, scrollPane, 0, 0, 3, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);
        int d = 0;
        if ((style & STYLE_STOP_COUNTDOWN) != 0) {
            btnCnTh = new JButton(JDLocale.L("gui.btn_cancelCountdown", "Stop Countdown"));
            btnCnTh.addActionListener(this);
            JDUtilities.addToGridBag(this, btnCnTh, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        }
        if ((style & STYLE_OK) != 0) {
            btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
            btnOK.addActionListener(this);
            getRootPane().setDefaultButton(btnOK);

            JDUtilities.addToGridBag(this, btnOK, d++, 2, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }
        if ((style & STYLE_CANCEL) != 0) {
            btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
            btnBAD.addActionListener(this);
            JDUtilities.addToGridBag(this, btnBAD, d++, 2, 1, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        }
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        setVisible(true);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnCnTh) {

            countdownThread = null;

        } else if (e.getSource() == btnOK) {
            this.result = true;
            setVisible(false);
            dispose();
        } else if (e.getSource() == btnBAD) {
            setVisible(false);
            dispose();
        }

        if (countdownThread != null && countdownThread.isAlive()) this.countdownThread.interrupt();
        countdownThread = null;
    }

    public static boolean showCountdownConfirmDialog(Frame owner, String msg, int countdown) {
        CountdownConfirmDialog d = new CountdownConfirmDialog(owner, msg, countdown);

        return d.result;
    }

    public static void main(String[] args) {
       showCountdownConfirmDialog(new JFrame(), "<h2>test</h2>", 10);

    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            
            JLinkButton.openURL( e.getURL());
            
          }
        
    }
}
