package jd.gui.skins.simple;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.JDUtilities;
import jd.captcha.JAntiCaptcha;
import jd.plugins.Plugin;

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
	private JTextField textField;

	/**
	 * Bestätigungsknopf
	 */
	private JButton btnOK;

	/**
	 * Das ist der eingegebene captcha Text
	 */
	private String captchaText = null;
    private static Logger logger = Plugin.getLogger();

	/**
	 * Erstellt einen neuen Dialog.
	 * 
	 * @param owner Das übergeordnete Fenster
     * @param plugin Das Plugin, das dieses Captcha auslesen möchte (name des Hosts wird von JAC benötigt)
	 * @param imageAddress Die Adresse des Bildes, das angezeigt werden soll
	 */
	public CaptchaDialog(Frame owner, Plugin plugin, String imageAddress) {
		super(owner);
		setModal(true);
		setLayout(new GridBagLayout());
		ImageIcon imageIcon = null;
		BufferedImage image;
		String code = "";
		try {
            logger.finer("imageAddress:"+imageAddress);
            image = ImageIO.read(new URL(imageAddress));
			imageIcon = new ImageIcon(image);
			code = JAntiCaptcha.getCaptchaCode(image,null, "rapidshare.com");
		} 
        catch (MalformedURLException e) { e.printStackTrace(); } 
        catch (IOException e)           { e.printStackTrace(); }
		JLabel label = new JLabel(imageIcon);
		textField = new JTextField(10);
		btnOK     = new JButton("OK");
		textField.setText(code);
        textField.selectAll();
		btnOK.addActionListener(this);
		getRootPane().setDefaultButton(btnOK);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JDUtilities.addToGridBag(this, label,     0, 0, 2, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.CENTER);
		JDUtilities.addToGridBag(this, textField, 0, 1, 1, 1, 1, 1, null,	GridBagConstraints.NONE, GridBagConstraints.EAST);
		JDUtilities.addToGridBag(this, btnOK,     1, 1, 1, 1, 1, 1, null,	GridBagConstraints.NONE, GridBagConstraints.WEST);

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
		return captchaText;
	}
}
