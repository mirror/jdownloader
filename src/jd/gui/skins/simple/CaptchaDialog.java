package jd.gui.skins.simple;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.captcha.JAntiCaptcha;
import jd.captcha.LetterComperator;
import jd.captcha.pixelgrid.Letter;
import jd.config.Configuration;
import jd.plugins.Plugin;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse wird ein Captcha Bild angezeigt
 * 
 * @author astaldo
 */
public class CaptchaDialog extends JDialog implements ActionListener {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 5880899982952719438L;

    /**
     * In dieses Textfeld wird der Code eingegeben
     */
    private JTextField        textField;

    /**
     * Bestätigungsknopf
     */
    private JButton           btnOK;

    /**
     * Das ist der eingegebene captcha Text
     */
    private String            captchaText      = null;

    private JButton           btnBAD;

    @SuppressWarnings("unused")
    private static Logger     logger           = JDUtilities.getLogger();

    /**
     * Erstellt einen neuen Dialog.
     * 
     * @param owner Das übergeordnete Fenster
     * @param plugin Das Plugin, das dieses Captcha auslesen möchte (name des
     *            Hosts wird von JAC benötigt)
     * @param file Pfad des Bildes, das angezeigt werden soll
     */
    public CaptchaDialog(Frame owner, final Plugin plugin, final File file, final String def) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());
        ImageIcon imageIcon = null;
        String code = "";
        final Configuration configuration = JDUtilities.getConfiguration();
        imageIcon = new ImageIcon(file.getAbsolutePath());
        final String host = plugin.getHost();
        if (plugin.getCaptchaDetectionID() != Plugin.CAPTCHA_USER_INPUT && !configuration.getBooleanProperty(Configuration.PARAM_CAPTCHA_JAC_DISABLE, false) && JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host)) {
            setTitle(JDLocale.L("gui.captchaWindow.title", "jAntiCaptcha aktiv!"));
            new Thread("JAC") {
                public void run() {

                    String code = JDUtilities.getCaptcha(plugin, host, file, true);
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e) {
                    }
                    if (textField.getText().length() == 0 || code.toLowerCase().startsWith(textField.getText().toLowerCase())) {
                        textField.setText(code);
                        int wait = configuration.getIntegerProperty(Configuration.PARAM_CAPTCHA_INPUT_SHOWTIME, 10);
                        logger.finer("jAntiCaptcha fertig. Warte " + JDUtilities.formatSeconds(wait));
                        while (wait > 0) {

                            setTitle(JDUtilities.formatSeconds(wait) + " " + JDLocale.L("gui.captchaWindow.title_wait", "jAntiCaptcha fertig."));
                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                            }
                            wait--;
                        }
                        if (isVisible() && textField.getText().equalsIgnoreCase(code) && textField.getText().length() > 0) {
                            captchaText = textField.getText();
                            dispose();
                        }
                    }
                    else {
                        textField.setText(JDLocale.L("gui.captchaWindow.askForInput", "Bitte eingeben!"));
                        setTitle(JDLocale.L("gui.captchaWindow.title_error", "jAntiCaptcha Fehler. Bitte Code eingeben!"));

                    }

                }
            }.start();
        }
        else {

            final int countdown = JDUtilities.getSubConfig("JAC").getIntegerProperty(Configuration.AUTOTRAIN_SHOW_TIMEOUT, 20);
            new Thread() {

                public void run() {
                    int cd = 0;
                    while ((countdown - cd) >= 0) {
                        setTitle("Countdown " + JDUtilities.formatSeconds(countdown - cd));
                        cd++;
                        try {
                            Thread.sleep(1000);
                        }
                        catch (InterruptedException e) {
                        }

                    }
                    captchaText = textField.getText();
                    dispose();

                }

            }.start();

        }
        logger.info(plugin.getLastCaptcha() + "");

        JLabel label = new JLabel(imageIcon);

        textField = new JTextField(10);
        if (def != null) code = def;
        btnOK = new JButton(JDLocale.L("gui.btn_ok", "OK"));
        btnBAD = new JButton(JDLocale.L("gui.btn_cancel", "CANCEL"));
        textField.setText(code);
        textField.selectAll();
        btnOK.addActionListener(this);
        btnBAD.addActionListener(this);

        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        // setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JDUtilities.addToGridBag(this, label, 0, 0, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, textField, 0, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnBAD, 2, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        if (plugin.getLastCaptcha() != null && plugin.getLastCaptcha().getLetterComperators() != null) {
            JPanel p = new JPanel();
            p.add(new JLabel("Current Captcha: "));
            JDUtilities.addToGridBag(this, p, 0, 2, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);

            if (JDUtilities.getSubConfig("JAC").getBooleanProperty("SHOW_EXTENDED_CAPTCHA", true)) {

                JPanel p2 = new JPanel();
                p2.add(new JLabel("Detection: "));

                JPanel p3 = new JPanel();
                p3.add(new JLabel("Uncertainty: "));
                LetterComperator[] lcs = plugin.getLastCaptcha().getLetterComperators();
                for (int i = 0; i < lcs.length; i++) {
                    Letter a = lcs[i].getA();
                    Letter b = lcs[i].getB();
                    if (a != null) p.add(new JLabel(new ImageIcon(a.getImage(2))));
                    if (b != null) p2.add(new JLabel(new ImageIcon(b.getImage(2))));
                    p3.add(new JLabel("" + Math.round(lcs[i].getValityPercent())));
                }

                JDUtilities.addToGridBag(this, p2, 0, 3, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
                JDUtilities.addToGridBag(this, p3, 0, 4, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
            }
        }
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        textField.requestFocusInWindow();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            captchaText = textField.getText();
            dispose();
        }
        if (e.getSource() == btnBAD) {
            captchaText = null;
            dispose();
        }
    }

    /**
     * Liefert den eingetippten Text zurück
     * 
     * @return Der Text, den der Benutzer eingetippt hat
     */
    public String getCaptchaText() {

        while (captchaText == null) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {

                e.printStackTrace();
            }
        }
        return captchaText;
    }
}
