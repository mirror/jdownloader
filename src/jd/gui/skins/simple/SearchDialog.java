package jd.gui.skins.simple;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

import jd.plugins.Plugin;
import jd.utils.JDUtilities;

/**
 * Diese Klasse zeigt den Suchdialog für die Search Plugins
 * 
 * @author coalado
 */
public class SearchDialog extends JDialog implements ActionListener {


	/**
     * 
     */
    private static final long serialVersionUID = -215687640717318362L;

    /**
	 * In dieses Textfeld wird der Code eingegeben
	 */
	private JTextField textField;

	/**
	 * Bestätigungsknopf
	 */
	private JButton btnOK;

    private JComboBox cbo;

    private String searchText;
	

    @SuppressWarnings("unused")
    private static Logger logger = Plugin.getLogger();

	/**
	 * Erstellt einen neuen Dialog.
	 * 
	 * @param owner Das übergeordnete Fenster
     * @param plugin Das Plugin, das dieses Captcha auslesen möchte (name des Hosts wird von JAC benötigt)
	 * @param imageAddress Die Adresse des Bildes, das angezeigt werden soll
	 */
	public SearchDialog(Frame owner) {
		super(owner);
		setModal(true);
		setLayout(new GridBagLayout());
		
		setTitle("jDownloader Suche");
		textField = new JTextField(30);
		btnOK     = new JButton("Suche starten");
		cbo= new JComboBox(JDUtilities.getPluginsForSearchCategories());
        textField.selectAll();
		btnOK.addActionListener(this);
		getRootPane().setDefaultButton(btnOK);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JDUtilities.addToGridBag(this, new JLabel("Nach was wollen Sie suchen?"),     GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
		JDUtilities.addToGridBag(this, cbo,    GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
		JDUtilities.addToGridBag(this, textField,   GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null,	GridBagConstraints.NONE, GridBagConstraints.EAST);
		JDUtilities.addToGridBag(this, btnOK,    GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, null,	GridBagConstraints.NONE, GridBagConstraints.EAST);

		pack();
		setLocation(JDUtilities.getCenterOfComponent(null, this));
		textField.requestFocusInWindow();
		setVisible(true);
		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == btnOK) {
			this.searchText=textField.getText();
			dispose();
		}
	}

	/**
	 * Liefert den eingetippten Text zurück
	 * 
	 * @return Der Text, den der Benutzer eingetippt hat
	 */
	public String getText() {
	    
	 
		return cbo.getSelectedItem().toString().toLowerCase()+":::"+searchText;
	}
}
