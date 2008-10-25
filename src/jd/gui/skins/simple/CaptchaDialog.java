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

package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jd.captcha.JAntiCaptcha;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 * 
 * @author astaldo
 */
public class CaptchaDialog extends JDialog implements ActionListener, KeyListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    private static final long serialVersionUID = 5880899982952719438L;

    private JButton btnBAD;

    /**
     * Bestätigungsknopf
     */
    private JButton btnOK;

    /**
     * Das ist der eingegebene captcha Text
     */
    private String captchaText = null;

    boolean abort = false;

    public int countdown = 0;
    private Thread countdownThread;

    private Thread jacThread;

    /**
     * In dieses Textfeld wird der Code eingegeben
     */
    private JTextField textField;

    /**
     * Erstellt einen neuen Dialog.
     * 
     * @param owner
     *            Das übergeordnete Fenster
     * @param plugin
     *            Das Plugin, das dieses Captcha auslesen möchte (name des Hosts
     *            wird von JAC benötigt)
     * @param file
     *            Pfad des Bildes, das angezeigt werden soll
     * @param def
     *            Defaultwert, der zu Beginn angezeigt wird
     */
    public CaptchaDialog(final JFrame owner, final Plugin plugin, final File file, final String def) {
        super(owner);

        countdown = Math.max(2, JDUtilities.getSubConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20));
        JDSounds.PT("sound.captcha.onCaptchaInput");
        this.setModal(true);
        this.addWindowListener(new WindowListener() {

            public void windowActivated(WindowEvent e) {
            }

            public void windowClosed(WindowEvent e) {
            }

            public void windowClosing(WindowEvent e) {
                // workaround für den scheiss compiz fehler

                CaptchaDialog cd = new CaptchaDialog(owner, plugin, file, def);
                cd.countdown = countdown;
                countdown = -1;
                cd.setVisible(true);
                // captchaText = cd.getCaptchaText();
                dispose();

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
        this.setLayout(new GridBagLayout());
        String code = "";
        ImageIcon imageIcon = new ImageIcon(file.getAbsolutePath());

        if (plugin != null && plugin.getCaptchaDetectionID() != Plugin.CAPTCHA_USER_INPUT && !JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false) && JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), plugin.getHost())) {
            setTitle(JDLocale.L("gui.captchaWindow.title", "jAntiCaptcha aktiv!"));
            final String host = plugin.getHost();
            jacThread = new Thread("JAC") {
                @Override
                public void run() {

                    String code=null;
                    try {
                        code = JDUtilities.getCaptcha(plugin, host, file, true);
                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    if (textField.getText().length() == 0 || code.toLowerCase().startsWith(textField.getText().toLowerCase())) {

                        if (isVisible() && textField.getText().equals(code) && textField.getText().length() > 0) {
                            captchaText = code;
                            dispose();
                        }
                        textField.setText(code);
                    } else {
                        textField.setText(JDLocale.L("gui.captchaWindow.askForInput", "Bitte eingeben!"));
                        setTitle(JDLocale.L("gui.captchaWindow.title_error", "jAntiCaptcha Fehler. Bitte Code eingeben!"));

                    }

                }
            };
            jacThread.start();

        } else {

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
                        setTitle(JDLocale.L("gui.captchaWindow.askForInput", "Bitte eingeben!") + " [" + JDUtilities.formatSeconds(c) + "]");
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
                    captchaText = textField.getText();
                    dispose();
                }

            };
            countdownThread.start();

        }

        if (def != null) code = def;
        textField = new JTextField(10);
        textField.addKeyListener(this);
        textField.setText(code);
        textField.selectAll();

        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        btnBAD.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JDUtilities.addToGridBag(this, new JLabel(imageIcon), 0, 0, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, textField, 0, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnBAD, 2, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        if (plugin != null && plugin.getLastCaptcha() != null && plugin.getLastCaptcha().getLetterComperators() != null) {
            JPanel p = new JPanel();
            p.add(new JLabel("Current Captcha: "));
            JDUtilities.addToGridBag(this, p, 0, 2, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);

        }
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
            captchaText = textField.getText();
        } else if (e.getSource() == btnBAD) {
            abort = true;
            captchaText = null;
        }
        dispose();

        if (countdownThread != null && countdownThread.isAlive()) {
            countdownThread.interrupt();
        }

        if (jacThread != null && jacThread.isAlive()) {
            jacThread.interrupt();
        }
    }

    /**
     * Liefert den eingetippten Text zurück
     * 
     * @return Der Text, den der Benutzer eingetippt hat
     */
    public String getCaptchaText() {
        if (abort == true) return null;
        return captchaText;
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
