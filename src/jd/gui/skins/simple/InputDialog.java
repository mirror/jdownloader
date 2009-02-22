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

package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDUtilities;

public class InputDialog extends JDialog implements ActionListener, KeyListener {

    private static final long serialVersionUID = 5880899982952719438L;

    private JButton btnBAD;

    private JButton btnOK;

    private String inputtext = null;

    boolean abort = false;

    public int countdown = 0;
    private Thread countdownThread;

    private JTextField textField;

    public InputDialog(final JFrame owner, final String title, final String def) {
        super(owner);
        countdown = Math.max(2, JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.PARAM_INPUTTIMEOUT, 20));
        JDSounds.PT("sound.captcha.onCaptchaInput");
        this.setModal(true);
        this.setLayout(new GridBagLayout());

        countdownThread = new Thread() {

            @Override
            public void run() {
                int c = countdown;
                while (!isVisible()) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                while (--c >= 0) {
                    if (!isVisible()) return;
                    if (countdownThread == null) return;
                    if (title != null) {
                        setTitle(title + " [" + JDUtilities.formatSeconds(c) + "]");
                    } else {
                        setTitle("[" + JDUtilities.formatSeconds(c) + "]");
                    }
                    if (c <= 3) JDSounds.P("sound.captcha.onCaptchaInputEmergency");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if (countdown < 0) return;
                    if (!isVisible()) return;

                }
                if (countdown < 0) return;
                inputtext = textField.getText();
                dispose();
            }

        };
        if (JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getIntegerProperty(SimpleGUI.PARAM_INPUTTIMEOUT, 20) != 0) {
            countdownThread.start();
        } else {
            if (title != null) setTitle(title);
        }

        textField = new JTextField(20);
        textField.addKeyListener(this);
        if (def != null) textField.setText(def);
        textField.selectAll();

        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnBAD.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JDUtilities.addToGridBag(this, textField, 1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnOK, 2, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnBAD, 3, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        this.pack();
        this.setResizable(false);
        this.setLocation(JDUtilities.getCenterOfComponent(null, this));

        this.toFront();
        this.setAlwaysOnTop(true);
        this.requestFocus();
        textField.requestFocusInWindow();

        this.setVisible(true);
        this.toFront();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            inputtext = textField.getText();
        } else if (e.getSource() == btnBAD) {
            abort = true;
            inputtext = null;
        }
        dispose();
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
    }

    /**
     * Liefert den eingetippten Text zurück
     * 
     * @return Der Text, den der Benutzer eingetippt hat
     */
    public String getInputText() {
        if (abort == true) return null;
        return inputtext;
    }

    public void keyPressed(KeyEvent e) {
        countdown = -1;
        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }
        countdownThread = null;
        setTitle(JDLocale.L("gui.captchaWindow.askForInput", "Bitte eingeben!"));
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }
}
