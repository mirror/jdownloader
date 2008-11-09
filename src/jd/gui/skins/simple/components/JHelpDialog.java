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

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;
import edu.stanford.ejalbert.exception.BrowserLaunchingInitializingException;
import edu.stanford.ejalbert.exception.UnsupportedOperatingSystemException;

public class JHelpDialog extends JDialog implements ActionListener {

    public abstract class Action {
        public abstract boolean doAction();
    }

    private static final long serialVersionUID = 1L;

    public static int STATUS_ANSWER_1 = 1;

    public static int STATUS_ANSWER_2 = 2;

    public static int STATUS_ANSWER_3 = 3;

    public static int STATUS_UNANSWERED = 0;

    public static int showHelpMessage(JFrame parent, String title, String message, final URL url, String helpMsg, int countdown) {
        if (title == null) title = JDLocale.L("gui.dialogs.helpDialog.defaultTitle", "jDownloader Soforthilfe");
        if (helpMsg == null) helpMsg = JDLocale.L("gui.dialogs.helpDialog.btn.help", "Hilfe anzeigen");

        JHelpDialog d = new JHelpDialog(parent, title, message, countdown);
        Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
        int minWidth = Math.min(d.getPreferredSize().width, (int) (size.width * .50));
        int minHeight = Math.min(d.getPreferredSize().height, (int) (size.height * .75));
        d.setMaximumSize(new Dimension(Math.max(minWidth, 640), Math.max(minHeight, 540)));
        d.pack();
        d.getBtn3().setVisible(false);
        d.getBtn1().setText(helpMsg);
        d.getBtn2().setText(JDLocale.L("gui.btn_ok", "OK"));
        d.action1 = d.new Action() {
            @Override
            public boolean doAction() {
                try {
                    JLinkButton.openURL(url);
          
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return true;
            }
        };
        d.showDialog();
        return d.getStatus();
    }

    private Action action1;
    private Action action2;
    private Action action3;

    private JButton btn1;
    private JButton btn2;
    private JButton btn3;
    private JButton btn4;

    private JTextPane htmlArea;
    protected Insets insets = new Insets(5, 5, 5, 5);

    protected Logger logger = JDUtilities.getLogger();

    private JFrame parentFrame;
    private int status = STATUS_UNANSWERED;

    private Thread countdownThread;
    private boolean stopTimer = false;

    public JHelpDialog(JFrame frame, final String title, String html, final int countdown) {
        super(frame);
        parentFrame = frame;
        setLayout(new GridBagLayout());
        this.setModal(false);
        setBtn1(new JButton("UNSET"));
        setBtn2(new JButton("UNSET"));
        setBtn3(new JButton("UNSET"));
        btn4 = new JButton(JDLocale.L("gui.dialogs.helpDialog.btn.stopTimer", "Stop Timer"));

        getBtn1().addActionListener(this);
        getBtn2().addActionListener(this);
        getBtn3().addActionListener(this);
        btn4.addActionListener(this);

        setTitle(title);
        htmlArea = new JTextPane();

        htmlArea.setEditable(false);
        htmlArea.setContentType("text/html");

        htmlArea.setText(html);
        htmlArea.setOpaque(false);
        htmlArea.requestFocusInWindow();
        setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        Icon imageIcon = new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.config.tip")));

        JDUtilities.addToGridBag(this, new JLabel(imageIcon), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, htmlArea, 1, 0, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

        JDUtilities.addToGridBag(this, getBtn1(), 2, 1, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(this, getBtn2(), 3, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        JDUtilities.addToGridBag(this, getBtn3(), 4, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
        if (countdown > 0) {
            JDUtilities.addToGridBag(this, btn4, 1, 1, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);

            countdownThread = new Thread() {

                @Override
                public void run() {
                    int c = countdown;

                    while (--c >= 0 && stopTimer == false) {
                        if (countdownThread == null) return;
                        setTitle(title + " [" + JDUtilities.formatSeconds(c) + "]");

                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                        }
                        if (!isVisible()) return;

                    }
                    if (stopTimer == false) {
                        dispose();
                    } else {
                        setTitle(title);
                    }
                }

            };

            countdownThread.start();
        }
        pack();
        getRootPane().setDefaultButton(getBtn1());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.setAlwaysOnTop(true);
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == getBtn1()) {
            setStatus(STATUS_ANSWER_1);
            if (action1 != null) {
                action1.doAction();
            } else {
                dispose();
            }
        } else if (e.getSource() == getBtn2()) {
            setStatus(STATUS_ANSWER_2);
            if (action2 != null) {
                action2.doAction();
            } else {
                dispose();
            }
        } else if (e.getSource() == getBtn3()) {
            setStatus(STATUS_ANSWER_3);
            if (action3 != null) {
                action3.doAction();
            } else {
                dispose();
            }
        } else if (e.getSource() == btn4) {
            stopTimer = true;
            btn4.setVisible(false);
        } else {
            dispose();
        }
    }

    /**
     * @return the btn1
     */
    public JButton getBtn1() {
        return btn1;
    }

    /**
     * @return the btn2
     */
    public JButton getBtn2() {
        return btn2;
    }

    /**
     * @return the btn3
     */
    public JButton getBtn3() {
        return btn3;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }

    /**
     * @param btn1
     *            the btn1 to set
     */
    public void setBtn1(JButton btn1) {
        this.btn1 = btn1;
    }

    /**
     * @param btn2
     *            the btn2 to set
     */
    public void setBtn2(JButton btn2) {
        this.btn2 = btn2;
    }

    /**
     * @param btn3
     *            the btn3 to set
     */
    public void setBtn3(JButton btn3) {
        this.btn3 = btn3;
    }

    /**
     * @param status
     *            the status to set
     */
    public void setStatus(int status) {
        this.status = status;
    }

    public void showDialog() {
        setVisible(true);
        this.setLocation(JDUtilities.getCenterOfComponent(parentFrame, this));
        setVisible(false);
        setModal(true);
        setVisible(true);
    }

    public void setAction1(Action action1) {
        this.action1 = action1;
    }

    public void setAction2(Action action2) {
        this.action2 = action2;
    }

    public void setAction3(Action action3) {
        this.action3 = action3;
    }

}
