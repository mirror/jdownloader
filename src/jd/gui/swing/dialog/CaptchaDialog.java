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

package jd.gui.swing.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.CaptchaController;
import jd.gui.UserIO;
import jd.gui.skins.SwingGui;
import jd.nutils.Screen;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingworker.SwingWorker;

/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 * 
 * @author astaldo
 */
public class CaptchaDialog extends JCountdownDialog implements ActionListener, KeyListener {

    private static final long serialVersionUID = -2046990134131595481L;

    private JButton btnBAD;

    private JButton btnOK;

    private String captchaText = null;

    private boolean abort = false;

    private JTextField textField;

    private int flag;

    private String method;

    private File imagefile;

    private String defaultValue;

    private String explain;

    private SwingWorker<Object, Object> jacWorker;

    public CaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain) {
        super(SwingGui.getInstance().getMainFrame());
        this.flag = flag;
        this.method = methodname;
        this.imagefile = captchafile;
        this.defaultValue = suggestion;
        this.explain = explain;
        this.init();
    }

    public void init() {

        this.setModal(true);

        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (imagefile != null && imagefile.exists()) {
            imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
        } else {
            imageIcon = JDTheme.II("gui.images.config.ocr");
        }

        textField = new JTextField(10);
        textField.addKeyListener(this);

        textField.setText(this.defaultValue);
        btnOK = new JButton(JDL.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnBAD = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
        btnBAD.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (explain != null) {
            JTextField tf;
            add(tf = new JTextField(), "");
            tf.setBorder(null);
            tf.setBackground(null);
            tf.setOpaque(false);
            tf.setText(explain);
            tf.setEditable(false);
        }
        add(new JLabel(imageIcon), "alignx center");
        add(textField);
        add(this.countDownLabel, "split 3,growx");
        add(btnOK, "alignx right");
        add(btnBAD, "alignx right");
        this.setMinimumSize(new Dimension(300, -1));
        this.pack();
        this.setResizable(false);
        if (SwingGui.getInstance() == null || SwingGui.getInstance().getMainFrame().getExtendedState() == JFrame.ICONIFIED || !SwingGui.getInstance().getMainFrame().isVisible() || !SwingGui.getInstance().getMainFrame().isActive()) {
            this.setLocation(Screen.getDockBottomRight(this));
        } else {
            this.setLocation(Screen.getCenterOfComponent(SwingGui.getInstance().getMainFrame(), this));
        }
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
        final String title = getTitle();
        this.setTitle(title + "-JAntiCaptcha");
        jacWorker = new SwingWorker<Object, Object>() {

            private String code;

            // @Override
            protected Object doInBackground() throws Exception {
                CaptchaController cc = new CaptchaController(method, imagefile, null, null);
                this.code = cc.getCode(flag | UserIO.NO_USER_INTERACTION);
                return null;
            }

            public void done() {
                setTitle(title);
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
        setTitle(JDL.L("gui.captchaWindow.askForInput", "Please enter..."));
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    // @Override
    protected void onCountdown() {
        this.captchaText = textField.getText();
        this.dispose();
    }
}
