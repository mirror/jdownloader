package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Unrar;
import jd.gui.UIInterface;
import jd.gui.skins.simple.SimpleGUI;
import jd.unrar.jdUnrar;
import jd.utils.JDUtilities;
/**
 * Konfigurationspanel für Unrar
 * 
 * @author DwD
 * 
 */

public class ConfigPanelInteractionUnrar extends ConfigPanel implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -1543456288909278519L;
    /**
     * Instanz zum speichern der parameter
     */
    private Unrar unrar;
    ConfigPanelInteractionUnrar(Configuration configuration, UIInterface uiinterface, Unrar unrar) {
        super(uiinterface);
        this.unrar = unrar;
        initPanel();

        load();
    }

    public void save() {

        this.saveConfigEntries();
    }

    public void load() {

        this.loadConfigEntries();
    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, unrar, Unrar.PROPERTY_UNRARCOMMAND, "Befehl für die Englische Version von Unrar: ").setDefaultValue(new jdUnrar(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY)).getUnrarCommand()));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, unrar, Unrar.PROPERTY_AUTODELETE, "Bei erfolgreichem Entpacken automatisch löschen: ").setDefaultValue(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, unrar, Unrar.PROPERTY_OVERWRITE_FILES, "Dateien automatisch überschreiben: ").setDefaultValue(false));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, unrar, Unrar.PROPERTY_MAX_FILESIZE, "Maximale Dateigröße für die Passwortsuche in MB: ", 0, 500).setDefaultValue(2));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, "Passwortliste bearbeiten"));
        addGUIConfigEntry(ce);
        add(panel, BorderLayout.CENTER);

    }

    @Override
    public String getName() {

        return "Unrar";
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println(((SimpleGUI) this.uiinterface).getFrame());
        new jdUnrarPasswordListDialog(((SimpleGUI) this.uiinterface).getFrame()).setVisible(true);
    }
}
/**
 * Ein Dialog, der Passwort-Output anzeigen kann.
 * 
 * @author DwD
 */
class jdUnrarPasswordListDialog extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * JTextField wo der Passwort Output eingetragen wird
     */
    private JTextArea pwField;

    /**
     * JScrollPane fuer das pwField
     */
    private JScrollPane pwScrollPane;

    /**
     * Knopf zum schliessen des Fensters
     */
    private JButton btnCancel;
    /**
     * Knopf zum scheichern der Passwörter
     */
    private JButton btnSave;

    /**
     * Primary Constructor
     * 
     * @param owner
     *            The owning Frame
     */
    public jdUnrarPasswordListDialog(JFrame owner) {
        super(owner);
        setModal(true);
        setLayout(new GridBagLayout());

        btnCancel = new JButton("Abbrechen");
        btnCancel.addActionListener(this);
        btnSave = new JButton("Speichern");
        btnSave.addActionListener(this);
        getRootPane().setDefaultButton(btnSave);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        pwField = new JTextArea(10, 60);
        pwScrollPane = new JScrollPane(pwField);
        pwField.setEditable(true);
        jdUnrar unrar = new jdUnrar();
        String[] pws = unrar.returnPasswords();
        for (int i = 0; i < pws.length; i++) {
            pwField.append(pws[i] + System.getProperty("line.separator"));
        }

        JDUtilities.addToGridBag(this, pwScrollPane, 0, 0, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnSave, 0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnCancel, 1, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnSave) {
            jdUnrar unrar = new jdUnrar();
            unrar.editPasswordlist(pwField.getText().split(System.getProperty("line.separator")));
            dispose();

        } else {
            dispose();
        }
    }

}
