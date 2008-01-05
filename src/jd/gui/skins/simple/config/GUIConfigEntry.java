package jd.gui.skins.simple.config;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fasst ein label / input Paar zusammen und macht das lesen und
 * schreiben einheitlich. Es lassen sich so Dialogelemente Automatisiert
 * einfügen.
 * 
 */

public class GUIConfigEntry extends JPanel {
    /**
     * 
     */
    private static final long serialVersionUID = -1391952049282528582L;

    /**
     * Checkbox id
     */

    protected Logger          logger           = JDUtilities.getLogger();

    /**
     * Die input Komponente
     */

    private JComponent[]      input;

    private Insets            insets           = new Insets(1, 5, 1, 5);

    private ConfigEntry       configEntry;

    /**
     * Erstellt einen neuen GUIConfigEntry
     * 
     * @param type TypID z.B. GUIConfigEntry.TYPE_BUTTON
     * @param propertyInstance Instanz einer propertyklasse (Extends property).
     * @param propertyName Name der Eigenschaft
     * @param label Label
     */
    GUIConfigEntry(ConfigEntry cfg) {
        this.configEntry = cfg;

        this.setLayout(new GridBagLayout());
        input = new JComponent[1];
JComponent left=null;
JComponent right=null;
JComponent total=null;
Insets insets= new Insets(2, 5, 2, 10);
        switch (configEntry.getType()) {
            
            
            
  case ConfigContainer.TYPE_PASSWORDFIELD:
                
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
               
                input[0] = new JPasswordField(50);
                input[0].setEnabled(configEntry.isEnabled());
             
                JDUtilities.addToGridBag(this, right=input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
                
                break;
            
            
            case ConfigContainer.TYPE_TEXTFIELD:
                
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
               
                input[0] = new JTextField(50);
                input[0].setEnabled(configEntry.isEnabled());
             
                JDUtilities.addToGridBag(this, right=input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
                
                break;
            case ConfigContainer.TYPE_TEXTAREA:
              
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, GridBagConstraints.REMAINDER, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
               input[0] = new JTextArea(20,20);
                input[0].setEnabled(configEntry.isEnabled());
           
                JDUtilities.addToGridBag(this,  total=new JScrollPane(input[0]), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
               total.setMinimumSize(new Dimension(200,200));
               total=null;
                break;
            case ConfigContainer.TYPE_CHECKBOX:

                // logger.info("ADD CheckBox");
               
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
            
                input[0] = new JCheckBox();
                input[0].setEnabled(configEntry.isEnabled());
            
                JDUtilities.addToGridBag(this, right=input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
                break;
            case ConfigContainer.TYPE_BROWSEFILE:
                // logger.info("ADD Browser");
              
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
                input[0] = new BrowseFile();
                ((BrowseFile) input[0]).setEnabled(configEntry.isEnabled());

                ((BrowseFile) input[0]).setEditable(true);
                JDUtilities.addToGridBag(this,right= input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
              
                break;
            case ConfigContainer.TYPE_BROWSEFOLDER:
                // logger.info("ADD BrowserFolder");
               
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
                input[0] = new BrowseFile();

                ((BrowseFile) input[0]).setEditable(true);
                ((BrowseFile) input[0]).setEnabled(configEntry.isEnabled());
                ((BrowseFile) input[0]).setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
           
                JDUtilities.addToGridBag(this,right= input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
                break;
            case ConfigContainer.TYPE_SPINNER:
                // logger.info("ADD Spinner");
             
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
                input[0] = new JSpinner(new SpinnerNumberModel(configEntry.getStart(), configEntry.getStart(), configEntry.getEnd(), configEntry.getStep()));
                input[0].setEnabled(configEntry.isEnabled());
                // ((JSpinner)input[0])
                JDUtilities.addToGridBag(this,right= input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
              
                break;
            case ConfigContainer.TYPE_BUTTON:
                // //logger.info("ADD Button");
                input[0] = new JButton(configEntry.getLabel());
                ((JButton) input[0]).addActionListener(configEntry.getActionListener());
                input[0].setEnabled(configEntry.isEnabled());
                JDUtilities.addToGridBag(this, input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
           
                break;
            case ConfigContainer.TYPE_COMBOBOX:
              
                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
                // logger.info(configEntry.getLabel());
                // logger.info("ADD Combobox");
                input[0] = new JComboBox(configEntry.getList());
                for (int i = 0; i < configEntry.getList().length; i++) {

                    if (configEntry.getList()[i].equals(configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName()))) {
                        ((JComboBox) input[0]).setSelectedIndex(i);

                        break;
                    }
                }
                input[0].setEnabled(configEntry.isEnabled());
                JDUtilities.addToGridBag(this, right=input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 0, null, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
             
                break;
            case ConfigContainer.TYPE_RADIOFIELD:
                // //logger.info("ADD Radio");
                input = new JComponent[configEntry.getList().length];
                JRadioButton radio;
               
                ButtonGroup group = new ButtonGroup();
                for (int i = 0; i < configEntry.getList().length; i++) {
                    radio = new JRadioButton(configEntry.getList()[i].toString());
                    radio.setActionCommand(configEntry.getList()[i].toString());
                    input[i] = radio;
                    input[i].setEnabled(configEntry.isEnabled());
                    // Group the radio buttons.

                    group.add(radio);

                    Object p = configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName());
                    if (p == null) p = "";

                    if (configEntry.getList()[i].toString().equals(p.toString())) {
                        radio.setSelected(true);

                    }
                    JDUtilities.addToGridBag(this, input[i], 0, i, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                }
                break;
            case ConfigContainer.TYPE_LABEL:

                JDUtilities.addToGridBag(this, left=new JLabel(configEntry.getLabel()), GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                
              
                break;
            case ConfigContainer.TYPE_SEPERATOR:
                // //logger.info("ADD Seperator");
                input[0] = new JSeparator(SwingConstants.HORIZONTAL);
               
                JDUtilities.addToGridBag(this, input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.WEST);
                
                break;

        }

    }
   

    public boolean isExpertEntry() {
        return configEntry.isExpertEntry();
    }

    /**
     * Setz daten ind ei INput Komponente
     * 
     * @param text
     */
    public void setData(Object text) {
        if (text == null && configEntry.getDefaultValue() != null) text = configEntry.getDefaultValue();
        // //logger.info(configEntry.getDefaultValue()+" - "+text + "
        // "+input.length+" -
        // "+input[0]);
        switch (configEntry.getType()) {
            case ConfigContainer.TYPE_TEXTFIELD:
                ((JTextField) input[0]).setText(text == null ? "" : text.toString());
                break;

            case ConfigContainer.TYPE_TEXTAREA:
                ((JTextArea) input[0]).setText(text == null ? "" : text.toString());
                break;
            case ConfigContainer.TYPE_CHECKBOX:
                if (text == null) text = false;
                ((JCheckBox) input[0]).setSelected((Boolean) text);
                break;
            case ConfigContainer.TYPE_BUTTON:

                break;
            case ConfigContainer.TYPE_COMBOBOX:
                JComboBox box = (JComboBox) input[0];
                box.setSelectedItem(text);

                break;
            case ConfigContainer.TYPE_LABEL:

                break;
            case ConfigContainer.TYPE_BROWSEFOLDER:
            case ConfigContainer.TYPE_BROWSEFILE:
                ((BrowseFile) input[0]).setText(text == null ? "" : text.toString());
                break;

            case ConfigContainer.TYPE_SPINNER:
                int value = text instanceof Integer ? (Integer) text : Integer.parseInt(text.toString());

                ((JSpinner) input[0]).setModel(new SpinnerNumberModel(value, configEntry.getStart(), configEntry.getEnd(), configEntry.getStep()));
                break;
            case ConfigContainer.TYPE_RADIOFIELD:
                for (int i = 0; i < configEntry.getList().length; i++) {
                    JRadioButton radio = (JRadioButton) input[i];
                    if (radio.getActionCommand().equals(text)) {
                        radio.setSelected(true);
                    }
                    else {
                        radio.setSelected(false);
                    }
                }

            case ConfigContainer.TYPE_SEPERATOR:

                break;
        }

    }

    /**
     * Gibt den zusstand der Inputkomponente zurück
     * 
     * @return
     */
    public Object getText() {
        // //logger.info(configEntry.getType()+"_2");
        switch (configEntry.getType()) {
            case ConfigContainer.TYPE_TEXTFIELD:
                return ((JTextField) input[0]).getText();
            case ConfigContainer.TYPE_TEXTAREA:
                return ((JTextArea) input[0]).getText();
            case ConfigContainer.TYPE_CHECKBOX:
                return ((JCheckBox) input[0]).isSelected();
            case ConfigContainer.TYPE_BUTTON:
                return null;
            case ConfigContainer.TYPE_COMBOBOX:
                return ((JComboBox) input[0]).getSelectedItem();
            case ConfigContainer.TYPE_LABEL:
                return null;
            case ConfigContainer.TYPE_RADIOFIELD:
                JRadioButton radio;
                for (int i = 0; i < input.length; i++) {

                    radio = (JRadioButton) input[i];

                    if (radio.getSelectedObjects() != null && radio.getSelectedObjects()[0] != null) return radio.getSelectedObjects()[0];
                }
                return null;
            case ConfigContainer.TYPE_SEPERATOR:
                return null;
            case ConfigContainer.TYPE_BROWSEFOLDER:
            case ConfigContainer.TYPE_BROWSEFILE:
                return ((BrowseFile) input[0]).getText();
            case ConfigContainer.TYPE_SPINNER:

                return ((JSpinner) input[0]).getValue();

        }

        return null;
    }

    public ConfigEntry getConfigEntry() {
        return configEntry;
    }

    public void setConfigEntry(ConfigEntry configEntry) {
        this.configEntry = configEntry;
    }

}
