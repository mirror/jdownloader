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

package jd.gui.userio.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.CaptchaController;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import jd.utils.JDSounds;
import jd.utils.JDTheme;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;

/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 * 
 * @author astaldo
 */
public class CaptchaDialog extends JCountdownDialog implements ActionListener, KeyListener {

    private JButton btnBAD;

    private JButton btnOK;

    private String captchaText = null;

    boolean abort = false;

    private JTextField textField;

    private int flag;

    private String method;

    private File imagefile;

    private String defaultValue;

    private String explain;

    private SwingWorker jacWorker;

    public CaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain) {
        super(SimpleGUI.CURRENTGUI);
        this.flag = flag;
        this.method = methodname;
        this.imagefile = captchafile;
        this.defaultValue = suggestion;
        this.explain = explain;
        this.init();
    }

    public void init() {

        // countdown = ;
        JDSounds.PT("sound.captcha.onCaptchaInput");
        this.setModal(true);
        // this.addWindowListener(new WindowListener() {
        //
        // public void windowActivated(WindowEvent e) {
        // }
        //
        // public void windowClosed(WindowEvent e) {
        // }
        //
        // public void windowClosing(WindowEvent e) {
        // // workaround für den scheiss compiz fehler
        //
        // CaptchaDialog cd = new CaptchaDialog(owner, plugin, file, def);
        // cd.countdown = countdown;
        // countdown = -1;
        // cd.setVisible(true);
        // // captchaText = cd.getCaptchaText();
        // dispose();
        //
        // }
        //
        // public void windowDeactivated(WindowEvent e) {
        // }
        //
        // public void windowDeiconified(WindowEvent e) {
        // }
        //
        // public void windowIconified(WindowEvent e) {
        // }
        //
        // public void windowOpened(WindowEvent e) {
        // }
        // });
        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (imagefile != null && imagefile.exists()) {
            imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
        } else {
            imageIcon = JDTheme.II("gui.images.config.ocr");
        }
        //
        // if (plugin != null &&
        // !JDUtilities.getConfiguration().getBooleanProperty
        // (Configuration.PARAM_CAPTCHA_JAC_DISABLE, false) &&
        // JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(),
        // plugin.getHost())) {
        // setTitle(JDLocale.L("gui.captchaWindow.title",
        // "jAntiCaptcha active!"));
        // final String host = plugin.getHost();
        // jacThread = new Thread("JAC") {
        // @Override
        // public void run() {
        //
        // String code = null;
        // try {
        // code = ExtendedUserInput.getCaptcha(plugin, host, file, true);
        // } catch (InterruptedException e1) {
        // // TODO Auto-generated catch block
        // JDLogger.exception(e1);
        // }
        // try {
        // Thread.sleep(1000);
        // } catch (InterruptedException e) {
        // }
        // if (textField.getText().length() == 0 ||
        // code.toLowerCase().startsWith(textField.getText().toLowerCase())) {
        //
        // if (isVisible() && textField.getText().equals(code) &&
        // textField.getText().length() > 0) {
        // captchaText = code;
        // dispose();
        // }
        // textField.setText(code);
        // } else {
        // textField.setText(JDLocale.L("gui.captchaWindow.askForInput",
        // "Please enter..."));
        // setTitle(JDLocale.L("gui.captchaWindow.title_error",
        // "jAntiCaptcha Error. Please enter proper code!"));
        //
        // }
        //
        // }
        // };
        // jacThread.start();
        //
        // } else {

        // this.startCountDown();

        textField = new JTextField(10);
        textField.addKeyListener(this);

        textField.setText(this.defaultValue);
        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "Cancel"));
        btnBAD.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (this.explain != null) {
            JTextField tf;
            add(tf = new JTextField());
            tf.setBorder(null);
            tf.setBackground(null);
            tf.setOpaque(false);
            tf.setText(explain);
            tf.setEditable(false);
        }
        add(new JLabel(imageIcon), "alignx center");
        add(textField);

        add(btnOK, "split 2,tag ok");
        add(btnBAD, "tag cancel");

        this.pack();
        this.setResizable(false);
        this.setLocation(Screen.getCenterOfComponent(null, this));
        this.setLocation(Screen.getDockBottomRight(this));
        this.toFront();
        this.setAlwaysOnTop(true);
        this.requestFocus();
        textField.requestFocusInWindow();
        textField.selectAll();
        this.countdown(Math.max(2, SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20)));
        if ((flag & UserIO.NO_JAC) == 0) {
            startJAC();
        }
        this.setVisible(true);
        this.toFront();

    }

    private void startJAC() {
        this.setTitle(getTitle() + "-JAntiCaptcha");
        jacWorker = new SwingWorker() {

            private String code;

            @Override
            protected Object doInBackground() throws Exception {
                CaptchaController cc = new CaptchaController(method, imagefile,null,null);
                this.code = cc.getCode(flag | UserIO.NO_USER_INTERACTION);
                return null;
            }

            public void done() {

                if (!this.isCancelled() && code != null) textField.setText(code);
            }
        };
        jacWorker.execute();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            captchaText = textField.getText();
        } else if (e.getSource() == btnBAD) {
            abort = true;
            captchaText = null;
        }
        dispose();

        keyPressed(null);
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

        this.interrupt();
        if (jacWorker != null) {
            jacWorker.cancel(true);
            jacWorker = null;
        }
        setTitle(JDLocale.L("gui.captchaWindow.askForInput", "Please enter..."));
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    @Override
    protected void onCountdown() {
        this.captchaText = textField.getText();
        this.dispose();
    }
}
