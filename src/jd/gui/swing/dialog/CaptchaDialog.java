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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.swing.SwingGui;
import jd.gui.userio.DummyFrame;
import jd.nutils.Screen;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 */
public class CaptchaDialog extends JCountdownDialog implements ActionListener, KeyListener, WindowListener {

    private static final long serialVersionUID = -2046990134131595481L;

    private JButton btnBAD;

    private JButton btnOK;

    private String captchaText = null;

    private boolean abort = false;

    private JTextField textField;

    private File imagefile;

    private String defaultValue;

    private String explain;

    private String host;

    private ImageIcon icon;

    public CaptchaDialog(String host, ImageIcon icon, File imagefile, String defaultValue, String explain) {
        super(DummyFrame.getDialogParent());

        this.host = host;
        this.icon = icon;
        this.imagefile = imagefile;
        this.defaultValue = defaultValue;
        this.explain = explain;

        this.init();
    }

    public void init() {
        this.setModal(true);
        this.setTitle((host != null ? (host + ": ") : "") + JDL.L("gui.captchaWindow.askForInput", "Please enter..."));
        if (icon != null) this.setIconImage(icon.getImage());
        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        ImageIcon imageIcon = null;

        if (imagefile != null && imagefile.exists()) {
            imageIcon = new ImageIcon(this.imagefile.getAbsolutePath());
        } else {
            imageIcon = JDTheme.II("gui.images.config.ocr");
        }

        int size = SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.PARAM_CAPTCHA_SIZE, 100);
        if (size != 100) {
            imageIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(imageIcon.getIconWidth() * (size / 100), imageIcon.getIconHeight() * (size / 100), Image.SCALE_SMOOTH));
        }

        textField = new JTextField(10);
        textField.addKeyListener(this);
        textField.setText(this.defaultValue);

        btnOK = new JButton(JDL.L("gui.btn_ok", "OK"));
        btnOK.addActionListener(this);

        btnBAD = new JButton(JDL.L("gui.btn_cancel", "Cancel"));
        btnBAD.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(JCountdownDialog.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
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
        keyPressed(null);
        dispose();
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

    public void windowActivated(WindowEvent e) {
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowClosing(WindowEvent e) {
        abort = true;
        captchaText = null;
        keyPressed(null);
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

}
