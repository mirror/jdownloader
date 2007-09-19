package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import jd.JDUtilities;
import jd.Property;
import jd.gui.skins.simple.components.BrowseFile;
import jd.plugins.Plugin;
import jd.plugins.PluginConfig;

/**
 * Diese Klasse fasst ein label / input Paar zusammen und macht das lesen und
 * schreiben einheitlich. Es lassen sich so Dialogelemente Automatisiert
 * einfügen.
 * 
 * @author coalado TODO: Diese Klasse für Combobox und andere Element ausbauen
 */

public class ConfigEntry extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = -1391952049282528582L;

    /**
     * Checkbox id
     */

    protected Logger          logger           = Plugin.getLogger();

    /**
     * Typ des input feldes
     */
    private int               type;

    /**
     * Defaultwert
     */

    private Object            defaultValue     = null;

    /**
     * Die input Komponente
     */

    private JComponent[]      input;

    /**
     * Propertyname
     */
    private String            propertyName     = null;

    /**
     * Instanz aus der die Property gelesen und gespeichert werden kann
     */
    private Property          propertyInstance;

    private Insets            insets           = new Insets(1, 5, 1, 5);

    private Object[]          list;

    private String            label;

    private ActionListener    listener;

    private int steps=1;

    private int start;

    private int end;

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
        this.label = label;
        this.setLayout(new BorderLayout());
        input = new JComponent[1];

        this.add(new JLabel(label), BorderLayout.CENTER);
        if (type == PluginConfig.TYPE_TEXTFIELD) {

            input[0] = new JTextField(50);
            this.add(input[0], BorderLayout.EAST);

        }
        else if (type == PluginConfig.TYPE_CHECKBOX) {
            logger.info("ADD CheckBox");

            input[0] = new JCheckBox();
            this.add(input[0], BorderLayout.EAST);
        }
        else if (type == PluginConfig.TYPE_BROWSEFILE) {
            logger.info("ADD Browser");

            input[0] = new BrowseFile();
            
            ((BrowseFile)input[0]).setEditable(true);
            this.add(input[0], BorderLayout.EAST);
        }
        
        else if (type == PluginConfig.TYPE_SPINNER) {
            logger.info("ADD Spinner");

            input[0] = new BrowseFile();
            
            ((BrowseFile)input[0]).setEditable(true);
            this.add(input[0], BorderLayout.EAST);
        }
    }
    /**
     * Erstellt einen neuen COnfigentry
     * 
     * @param type TypID z.B. ConfigEntry.TYPE_BUTTON
     * @param propertyInstance Instanz einer propertyklasse (Extends property).
     * @param propertyName Name der Eigenschaft
     * @param label Label
     */
    ConfigEntry(int type, Property propertyInstance, String propertyName, String label,int start, int end) {
        this.type = type;
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;
        this.label = label;
        this.setLayout(new BorderLayout());
        input = new JComponent[1];
this.start=start;
this.end=end;
        this.add(new JLabel(label), BorderLayout.CENTER);
        if (type == PluginConfig.TYPE_SPINNER) {
            logger.info("ADD Spinner");

            input[0] = new JSpinner(new SpinnerNumberModel(start, start, end, getSteps()));
            
//            ((JSpinner)input[0])
            this.add(input[0], BorderLayout.EAST);
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
        this.listener = listener;
        this.label = label;
        this.setLayout(new BorderLayout());
        input = new JComponent[1];
        if (type == PluginConfig.TYPE_BUTTON) {
            // logger.info("ADD Button");
            input[0] = new JButton(label);
            ((JButton) input[0]).addActionListener(listener);
            this.add(input[0], BorderLayout.CENTER);

        }
    }

    public ConfigEntry(int type, Property propertyInstance, String propertyName, Object[] list, String label) {
        this.type = type;
        this.list = list;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
        this.setLayout(new BorderLayout());
        input = new JComponent[1];
        if (type == PluginConfig.TYPE_COMBOBOX) {
            // logger.info("ADD Combobox");
            input[0] = new JComboBox(list);
            for (int i = 0; i < list.length; i++) {
                if (list[i].equals(propertyInstance.getProperty(propertyName))) {
                    ((JComboBox) input[0]).setSelectedIndex(i);
                    break;
                }
            }

            this.add(input[0], BorderLayout.CENTER);

        }
        else if (type == PluginConfig.TYPE_RADIOFIELD) {
            // logger.info("ADD Radio");
            input = new JComponent[list.length];
            JRadioButton radio;
            this.setLayout(new GridBagLayout());
            ButtonGroup group = new ButtonGroup();
            for (int i = 0; i < list.length; i++) {
                radio = new JRadioButton(list[i].toString());
                radio.setActionCommand(list[i].toString());
                input[i] = radio;
                // Group the radio buttons.

                group.add(radio);

                Object p = propertyInstance.getProperty(propertyName);
                if (p == null) p = "";

                if (list[i].toString().equals(p.toString())) {
                    radio.setSelected(true);

                }
                JDUtilities.addToGridBag(this, input[i], 0, i, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
            }

        }

    }

    public ConfigEntry(int type, String label) {
        this.label = label;
        this.type = type;
        input = new JComponent[1];
        if (type == PluginConfig.TYPE_LABEL) {
            this.type = type;
            this.setLayout(new BorderLayout());
            this.add(new JLabel(label), BorderLayout.WEST);
        }
    }

    public ConfigEntry(int type) {
        this.type = type;
        this.setLayout(new BorderLayout());
        input = new JComponent[1];
        if (type == PluginConfig.TYPE_SEPERATOR) {
            logger.info("ADD Seperator");
            input[0] = new JSeparator(SwingConstants.HORIZONTAL);
            this.add(input[0], BorderLayout.CENTER);

        }
    }

    /**
     * Setz daten ind ei INput Komponente
     * 
     * @param text
     */
    public void setData(Object text) {
        if (text == null && defaultValue != null) text = defaultValue;
        // logger.info(defaultValue+" - "+text + " "+input.length+" -
        // "+input[0]);
        switch (type) {
            case PluginConfig.TYPE_TEXTFIELD:
                ((JTextField) input[0]).setText(text == null ? "" : text.toString());
                break;
            case PluginConfig.TYPE_CHECKBOX:
                if (text == null) text = false;
                ((JCheckBox) input[0]).setSelected((Boolean) text);
                break;
            case PluginConfig.TYPE_BUTTON:

                break;
            case PluginConfig.TYPE_COMBOBOX:
                JComboBox box = (JComboBox) input[0];
                box.setSelectedItem(text);

                break;
            case PluginConfig.TYPE_LABEL:

                break;
                
            case PluginConfig.TYPE_BROWSEFILE:
                ((BrowseFile) input[0]).setText(text == null ? "" : text.toString());
                break;
                
            case PluginConfig.TYPE_SPINNER:
               int value=text instanceof Integer?(Integer)text:Integer.parseInt(text.toString());
             
                ((JSpinner) input[0]).setModel(new SpinnerNumberModel(value, start, end, getSteps()));
                break;
            case PluginConfig.TYPE_RADIOFIELD:
                for (int i = 0; i < list.length; i++) {
                    JRadioButton radio = (JRadioButton) input[i];
                    if (radio.getActionCommand().equals(text)) {
                        radio.setSelected(true);
                    }
                    else {
                        radio.setSelected(false);
                    }
                }

            case PluginConfig.TYPE_SEPERATOR:

                break;
        }

        if (type == PluginConfig.TYPE_TEXTFIELD) {

            ((JTextField) input[0]).setText(text == null ? "" : text.toString());

        }
        else if (type == PluginConfig.TYPE_CHECKBOX) {

            ((JCheckBox) input[0]).setSelected((Boolean) text);
        }
    }

    /**
     * Gibt den zusstand der Inputkomponente zurück
     * 
     * @return
     */
    public Object getText() {
        switch (type) {
            case PluginConfig.TYPE_TEXTFIELD:
                return ((JTextField) input[0]).getText();
            case PluginConfig.TYPE_CHECKBOX:
                return ((JCheckBox) input[0]).isSelected();
            case PluginConfig.TYPE_BUTTON:
                return null;
            case PluginConfig.TYPE_COMBOBOX:
                return ((JComboBox) input[0]).getSelectedItem();
            case PluginConfig.TYPE_LABEL:
                return null;
            case PluginConfig.TYPE_RADIOFIELD:
                JRadioButton radio;
                for (int i = 0; i < input.length; i++) {

                    radio = (JRadioButton) input[i];

                    if (radio.getSelectedObjects() != null && radio.getSelectedObjects()[0] != null) return radio.getSelectedObjects()[0];
                }
                return null;
            case PluginConfig.TYPE_SEPERATOR:
                return null;
                
            case PluginConfig.TYPE_BROWSEFILE:
                return ((BrowseFile) input[0]).getText();

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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public ActionListener getListener() {
        return listener;
    }

    public void setListener(ActionListener listener) {
        this.listener = listener;
    }
    /**
     * ERlaubt das setzen einer Schrttgröße. Manche Komponenten brauchen das
     * @param i
     */
    public void setSteps(int i) {
       this.steps=i;
        
    }
    public int getSteps() {
        return steps;
    }
    public int getStart() {
        return start;
    }
    public void setStart(int start) {
        this.start = start;
    }
    public int getEnd() {
        return end;
    }
    public void setEnd(int end) {
        this.end = end;
    }
}
