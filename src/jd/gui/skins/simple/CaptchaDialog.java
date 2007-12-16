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
import javax.swing.JTextField;

import jd.captcha.JAntiCaptcha;
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
    public CaptchaDialog(Frame owner, final Plugin plugin, final File file) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());
        ImageIcon imageIcon = null;
        String code = "";
        final Configuration  configuration=JDUtilities.getConfiguration();
        imageIcon = new ImageIcon(file.getAbsolutePath());
        String host = plugin.getHost();
        logger.info(configuration.getBooleanProperty(Configuration.PARAM_MANUAL_CAPTCHA_USE_JAC)+"_");
        if (configuration.getBooleanProperty(Configuration.PARAM_MANUAL_CAPTCHA_USE_JAC,true)&&JAntiCaptcha.hasMethod(JDUtilities.getJACMethodsDirectory(), host)) {
            setTitle(JDLocale.L("gui.captchaWindow.title","jAntiCaptcha aktiv!"));
            new Thread("JAC") {
                public void run() {

                    String code = JDUtilities.getCaptcha(null, plugin, file);
                    if (textField.getText().length() == 0 || code.toLowerCase().startsWith(textField.getText().toLowerCase())) {
                        textField.setText(code);
                        setTitle(JDLocale.L("gui.captchaWindow.title_wait","jAntiCaptcha fertig. Warte ")+JDUtilities.formatSeconds(configuration.getIntegerProperty(Configuration.PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC,10000)/1000));
                        logger.finer("jAntiCaptcha fertig. Warte "+JDUtilities.formatSeconds(configuration.getIntegerProperty(Configuration.PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC,10000)/1000));
                        try {
                           Thread.sleep(Math.abs(configuration.getIntegerProperty(Configuration.PARAM_MANUAL_CAPTCHA_WAIT_FOR_JAC,3000)));
                        }
                        catch (InterruptedException e) {      e.printStackTrace();
                        }
                        if(isVisible()&&textField.getText().equalsIgnoreCase(code) && textField.getText().length()>0){
                            captchaText = textField.getText();
                            dispose();
                        }
                    }else{
                        textField.setText(JDLocale.L("gui.captchaWindow.askForInput","Bitte eingeben!"));
                        setTitle(JDLocale.L("gui.captchaWindow.title_error","jAntiCaptcha Fehler. Bitte Code eingeben!"));
                        
                    }
                   
                }
            }.start();
        }
        JLabel label = new JLabel(imageIcon);
        textField = new JTextField(10);
        btnOK = new JButton(JDLocale.L("gui.btn_ok","OK"));
        textField.setText(code);
        textField.selectAll();
        btnOK.addActionListener(this);
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JDUtilities.addToGridBag(this, label, 0, 0, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, textField, 0, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK, 1, 1, 1, 1, 1, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        textField.requestFocusInWindow();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            captchaText = textField.getText();
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
