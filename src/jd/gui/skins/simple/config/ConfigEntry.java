package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import jd.Property;
import jd.plugins.Plugin;

/**
 * Diese Klasse fasst ein label / input Paar zusammen und macht das lesen und
 * schreiben einheitlich. Es lassen sich so Dialogelemente Automatisiert
 * einfügen.
 * 
 * @author coalado
 * TODO: Diese Klasse für Combobox und andere Element ausbauen
 */

public class ConfigEntry extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = -1391952049282528582L;

    /**
     * Checkbox id
     */
    public static final int TYPE_CHECKBOX  = 3;

    public static final int TYPE_BUTTON    = 2;

    public static final int TYPE_COMBOBOX  = 1;

    public static final int TYPE_TEXTFIELD = 0;

    protected Logger        logger         = Plugin.getLogger();

    /**
     * Typ des input feldes
     */
    private int             type;

    /**
     * Defaultwert
     */

    private Object          defaultValue   = null;

    /**
     * Die input Komponente
     */

    private JComponent      input;

    /**
     * Propertyname
     */
    private String          propertyName   = null;

    /**
     * Instanz aus der die Property gelesen und gespeichert werden kann
     */
    private Property        propertyInstance;

    /**
     * Erstellt einen neuen COnfigentry
     * 
     * @param type TypID z.B. ConfigEntry.TYPE_BUTTON
     * @param propertyInstance Instanz einer propertyklasse (Extends property).
     * @param propertyName Name der Eigenschaft
     * @param label Label
     */
    ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
        this.type = type;
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;
        this.setLayout(new BorderLayout());

        this.add(new JLabel(label), BorderLayout.CENTER);
        if (type == TYPE_TEXTFIELD) {
            logger.info("ADD Textfield");
            input = new JTextField(50);
            this.add(input, BorderLayout.EAST);

        }

        else if (type == TYPE_COMBOBOX) {
            logger.info("ADD Combobox");
        }
        else if (type == TYPE_CHECKBOX) {
            logger.info("ADD CheckBox");

            input = new JCheckBox();
            this.add(input, BorderLayout.EAST);
        }

    }

    /**
     * Erstellt einen neuen ConfigEntry
     * 
     * @param type Input Typ (ID)
     * @param listener (den listener für den INput)
     * @param label (Label=)
     */
    public ConfigEntry(int type, ActionListener listener, String label) {
        this.type = type;
        this.setLayout(new BorderLayout());

        if (type == TYPE_BUTTON) {
            logger.info("ADD Button");
            input = new JButton(label);
            ((JButton) input).addActionListener(listener);
            this.add(input, BorderLayout.CENTER);

        }
    }

    /**
     * Setz daten ind ei INput Komponente
     * 
     * @param text
     */
    public void setData(Object text) {
        if (text == null && defaultValue != null) text = defaultValue;
        logger.info(text + " ");
        if (type == TYPE_TEXTFIELD) {

            ((JTextField) input).setText(text == null ? "" : text.toString());

        }
        else if (type == TYPE_CHECKBOX) {

            ((JCheckBox) input).setSelected((Boolean) text);
        }
    }

    /**
     * Gibt den zusstand der Inputkomponente zurück
     * 
     * @return
     */
    public Object getText() {
        if (type == TYPE_TEXTFIELD) {
            return ((JTextField) input).getText();

        }
        else if (type == TYPE_CHECKBOX) {
            return ((JCheckBox) input).isSelected();
        }
        return null;
    }

    /**
     * Gibt den namen de Eigenschaft zurück
     * 
     * @return
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Gibt die verwendete propertyinstanz zurück
     * 
     * @return
     */
    public Property getPropertyInstance() {
        return propertyInstance;
    }

    /**
     * Setzt die zu verwendene Propertyiinstanz. In dieses Objekt wird beim
     * speichernd er WErt abgelegt
     * 
     * @param propertyInstance
     */
    public void setPropertyInstance(Property propertyInstance) {
        this.propertyInstance = propertyInstance;
    }

    /**
     * defaulttext wenn der wert aus der propertyinstanz null ist
     * 
     * @param value
     */
    public void setDefaultText(Object value) {
        defaultValue = value;

    }
}
