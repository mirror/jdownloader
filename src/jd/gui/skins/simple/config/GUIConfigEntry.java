//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.config;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.BrowseFile;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fasst ein label / input Paar zusammen und macht das lesen und
 * schreiben einheitlich. Es lassen sich so Dialogelemente Automatisiert
 * einfügen.
 * 
 */

public class GUIConfigEntry extends JPanel implements ActionListener,ChangeListener,PropertyChangeListener,DocumentListener {
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

    // private Insets insets = new Insets(1, 5, 1, 5);

    private ConfigEntry       configEntry;

    private Insets insets;

    private JComponent left;

    private JComponent right;

    private JComponent total;

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
        cfg.setGuiListener(this);
        this.addPropertyChangeListener(cfg);
        this.setLayout(new GridBagLayout());
        //this.setBorder(BorderFactory.createEtchedBorder());
        input = new JComponent[1];
         left = null;
         right = null;
         total = null;
        insets = new Insets(2, 5, 2, 10);
        switch (configEntry.getType()) {

            case ConfigContainer.TYPE_LINK:

                try {
                    input[0] = new JLinkButton(configEntry.getLabel(), new URL(configEntry.getPropertyName()));
                    
                }
                catch (MalformedURLException e) {
                    input[0] = new JLabel(configEntry.getPropertyName());
                    e.printStackTrace();
                }
                this.addInstantHelpLink();
                input[0].setEnabled(configEntry.isEnabled());
               
                JDUtilities.addToGridBag(this, left = input[0], 0, 0, 2, 1, 1, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

                break;

            case ConfigContainer.TYPE_PASSWORDFIELD:

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new JPasswordField();
                PlainDocument doc = (PlainDocument) ((JPasswordField) input[0]).getDocument();
                doc.addDocumentListener(this);
               // input[0].setMaximumSize(new Dimension(160,20));
                input[0].setEnabled(configEntry.isEnabled());
               ((JPasswordField) input[0]).setHorizontalAlignment(JTextField.RIGHT);
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 1, 1,insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

                break;

            case ConfigContainer.TYPE_TEXTFIELD:

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new JTextField();
               doc = (PlainDocument) ((JTextField) input[0]).getDocument();
                doc.addDocumentListener(this);
                input[0].setEnabled(configEntry.isEnabled());
                ((JTextField) input[0]).setHorizontalAlignment(JTextField.RIGHT);
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);

                break;
            case ConfigContainer.TYPE_TEXTAREA:

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new JTextArea(20, 20);
                input[0].setEnabled(configEntry.isEnabled());
                doc = (PlainDocument) ((JTextArea) input[0]).getDocument();
                doc.addDocumentListener(this);
                JDUtilities.addToGridBag(this, total = new JScrollPane(input[0]), 0, 1, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
                total.setMinimumSize(new Dimension(200, 200));
                total = null;
                break;
            case ConfigContainer.TYPE_CHECKBOX:

                // logger.info("ADD CheckBox");

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0,0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new JCheckBox();
                input[0].setEnabled(configEntry.isEnabled());
                ((JCheckBox)input[0]).addChangeListener(this);
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 0, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
                break;
            case ConfigContainer.TYPE_BROWSEFILE:
                // logger.info("ADD Browser");

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new BrowseFile();
                ((BrowseFile) input[0]).setEnabled(configEntry.isEnabled());
                
                ((BrowseFile) input[0]).setEditable(true);
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 0, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

                break;
            case ConfigContainer.TYPE_BROWSEFOLDER:
                // logger.info("ADD BrowserFolder");

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0,insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new BrowseFile();

                ((BrowseFile) input[0]).setEditable(true);
                ((BrowseFile) input[0]).setEnabled(configEntry.isEnabled());
                ((BrowseFile) input[0]).setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 0, 0,insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);
                break;
            case ConfigContainer.TYPE_SPINNER:
                // logger.info("ADD Spinner");
                
                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                input[0] = new JSpinner(new SpinnerNumberModel(configEntry.getStart(), configEntry.getStart(), configEntry.getEnd(), configEntry.getStep()));
                input[0].setEnabled(configEntry.isEnabled());
                ((JSpinner)input[0]).addChangeListener(this);
                // ((JSpinner)input[0])
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 0, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

                break;
            case ConfigContainer.TYPE_BUTTON:
                // //logger.info("ADD Button");
                input[0] = new JButton(configEntry.getLabel());
                ((JButton) input[0]).addActionListener(configEntry.getActionListener());
                input[0].setEnabled(configEntry.isEnabled());
                JDUtilities.addToGridBag(this, input[0], GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, GridBagConstraints.RELATIVE, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                break;
            case ConfigContainer.TYPE_COMBOBOX:

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 0, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                // logger.info(configEntry.getLabel());
                // logger.info("ADD Combobox");
                input[0] = new JComboBox(configEntry.getList());
                ((JComboBox)input[0]).addActionListener(this);
                for (int i = 0; i < configEntry.getList().length; i++) {

                    if (configEntry.getList()[i].equals(configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName()))) {
                        ((JComboBox) input[0]).setSelectedIndex(i);

                        break;
                    }
                }
                input[0].setEnabled(configEntry.isEnabled());
                JDUtilities.addToGridBag(this, right = input[0], 2, 0, 1, 1, 0, 0, insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

                break;
            case ConfigContainer.TYPE_RADIOFIELD:
                // //logger.info("ADD Radio");
                input = new JComponent[configEntry.getList().length];
                JRadioButton radio;
                this.addInstantHelpLink();
                ButtonGroup group = new ButtonGroup();
               
                for (int i = 0; i < configEntry.getList().length; i++) {
                    radio = new JRadioButton(configEntry.getList()[i].toString());
                    ((JRadioButton)input[0]).addActionListener(this);
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

                JDUtilities.addToGridBag(this, left = new JLabel(configEntry.getLabel()), 0, 0, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
                this.addInstantHelpLink();
                break;
            case ConfigContainer.TYPE_SEPARATOR:
                // //logger.info("ADD Seperator");
                input[0] = new JSeparator(SwingConstants.HORIZONTAL);

                JDUtilities.addToGridBag(this, input[0], 0, 0, 1, 1, 1, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.WEST);

                break;

        }
        this.firePropertyChange(this.getConfigEntry().getPropertyName(),null , this.getText());
    }

    private void addInstantHelpLink() {
       // JDUtilities.addToGridBag(this,  new JLabel("HELP"), 1, 0, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
      if(configEntry.getInstantHelp()!=null){
          try {
              String url=configEntry.getInstantHelp();
              JLinkButton link = new JLinkButton("",new ImageIcon(JDUtilities.getImage(JDTheme.V("gui.images.help")).getScaledInstance(20, 20, Image.SCALE_FAST)), new URL(url));
              JDUtilities.addToGridBag(this,  link, 1, 0, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
              
          }
          catch (MalformedURLException e) {
              JDUtilities.addToGridBag(this,  new JLabel(configEntry.getInstantHelp()), 1, 0, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
               
          }
      }else{
          JDUtilities.addToGridBag(this,  new JLabel(""), 1, 0, 1, 1, 1, 0, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
           
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
            case ConfigContainer.TYPE_LINK:
                try {
                    ((JLinkButton) input[0]).setLinkURL(new URL(text == null ? "" : text.toString()));
                }
                catch (MalformedURLException e1) {

                    e1.printStackTrace();
                }
                break;

            case ConfigContainer.TYPE_PASSWORDFIELD:
                ((JPasswordField) input[0]).setText(text == null ? "" : text.toString());
                break;

            case ConfigContainer.TYPE_TEXTFIELD:
                ((JTextField) input[0]).setText(text == null ? "" : text.toString());
              
                
               
                break;

            case ConfigContainer.TYPE_TEXTAREA:
                ((JTextArea) input[0]).setText(text == null ? "" : text.toString());
                break;
            case ConfigContainer.TYPE_CHECKBOX:
                if (text == null) text = false;
                try {
                    ((JCheckBox) input[0]).setSelected((Boolean) text);
                }
                catch (Exception e) {
                    logger.severe("Falcher Wert: " + text);
                    ((JCheckBox) input[0]).setSelected(false);
                }
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

            case ConfigContainer.TYPE_SEPARATOR:

                break;
        }
        this.getConfigEntry().valueChanged(this.getText());}

    /**
     * Gibt den zusstand der Inputkomponente zurück
     * 
     * @return
     */
    public Object getText() {
        // //logger.info(configEntry.getType()+"_2");
        switch (configEntry.getType()) {
            case ConfigContainer.TYPE_LINK:

                return ((JLinkButton) input[0]).getLinkURL().toString();

            case ConfigContainer.TYPE_PASSWORDFIELD:
                return new String(((JPasswordField) input[0]).getPassword());
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
            case ConfigContainer.TYPE_SEPARATOR:
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

 

    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
        if(input[0]==null)return;
        logger.info("New Value "+evt.getNewValue());
        if(this.getConfigEntry().isConditionalEnabled(evt)){
            input[0].setEnabled(true);
            for(JComponent i:input){
                i.setEnabled(true);
            }
            if(left!=null)left.setEnabled(true);
            if(right!=null)right.setEnabled(true);
            if(total!=null)total.setEnabled(true);
            
        }else{
            for(JComponent i:input){
                i.setEnabled(false);
            }
            if(left!=null)left.setEnabled(false);
            if(right!=null)right.setEnabled(false);
            if(total!=null)total.setEnabled(false);
        }
    }

    public void changedUpdate(DocumentEvent e) {
        this.getConfigEntry().valueChanged(this.getText());
        
    }

    public void insertUpdate(DocumentEvent e) {
        this.getConfigEntry().valueChanged(this.getText());
    }

    public void removeUpdate(DocumentEvent e) {
        this.getConfigEntry().valueChanged(this.getText());
    }
    
    public void stateChanged(ChangeEvent e) {
        this.getConfigEntry().valueChanged(this.getText());
     }

    public void actionPerformed(ActionEvent e) {
        this.getConfigEntry().valueChanged(this.getText());
        
    }

}
