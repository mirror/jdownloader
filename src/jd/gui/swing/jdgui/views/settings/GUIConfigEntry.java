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

package jd.gui.swing.jdgui.views.settings;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.GuiConfigListener;
import jd.config.container.JDLabelContainer;
import jd.controlling.JDLogger;
import jd.gui.swing.components.ComboBrowseFile;
import jd.gui.swing.components.JDTextArea;
import jd.gui.swing.components.JDTextField;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

/**
 * Diese Klasse fasst ein label / input Paar zusammen und macht das lesen und
 * schreiben einheitlich. Es lassen sich so Dialogelemente Automatisiert
 * einfügen.
 */
public class GUIConfigEntry implements GuiConfigListener, ActionListener, ChangeListener, DocumentListener {

    private static final long serialVersionUID = -1391952049282528582L;

    private final ConfigEntry configEntry;

    /**
     * Die input Komponente
     */
    private JComponent input;

    private JComponent decoration;

    private Logger logger = JDLogger.getLogger();

    /**
     * Erstellt einen neuen GUIConfigEntry
     * 
     * @param configEntry
     */
    public GUIConfigEntry(ConfigEntry configEntry) {
        this.configEntry = configEntry;
        configEntry.setGuiListener(this);

        if (configEntry.getLabel() != null && configEntry.getLabel().trim().length() > 0) {
            decoration = new JLabel(configEntry.getLabel());
        }

        switch (configEntry.getType()) {
        case ConfigContainer.TYPE_PASSWORDFIELD:
            input = new JPasswordField();
            ((JPasswordField) input).setHorizontalAlignment(JPasswordField.RIGHT);

            Document doc = ((JPasswordField) input).getDocument();
            doc.addDocumentListener(this);
            break;
        case ConfigContainer.TYPE_TEXTFIELD:
            input = new JDTextField();
            ((JDTextField) input).setHorizontalAlignment(JDTextField.RIGHT);
            doc = ((JDTextField) input).getDocument();
            doc.addDocumentListener(this);
            break;
        case ConfigContainer.TYPE_TEXTAREA:
        case ConfigContainer.TYPE_LISTCONTROLLED:
            input = new JDTextArea();
            doc = ((JDTextArea) input).getDocument();
            doc.addDocumentListener(this);
            break;
        case ConfigContainer.TYPE_CHECKBOX:
            input = new JCheckBox();
            ((JCheckBox) input).addChangeListener(this);
            break;
        case ConfigContainer.TYPE_BROWSEFILE:
            input = new ComboBrowseFile(configEntry.getPropertyName());
            ((ComboBrowseFile) input).setEditable(true);
            break;
        case ConfigContainer.TYPE_BROWSEFOLDER:
            input = new ComboBrowseFile(configEntry.getPropertyName());
            ((ComboBrowseFile) input).setEditable(true);
            ((ComboBrowseFile) input).setFileSelectionMode(ComboBrowseFile.DIRECTORIES_ONLY);
            break;
        case ConfigContainer.TYPE_SPINNER:
            input = new JSpinner(new SpinnerNumberModel(configEntry.getStart(), configEntry.getStart(), configEntry.getEnd(), configEntry.getStep()));
            ((JSpinner) input).addChangeListener(this);
            break;
        case ConfigContainer.TYPE_BUTTON:
            input = new JButton(configEntry.getDescription());
            if (configEntry.getImageIcon() != null) {
                ((JButton) input).setIcon(configEntry.getImageIcon());
            }
            ((JButton) input).addActionListener(this);
            ((JButton) input).addActionListener(configEntry.getActionListener());
            break;
        case ConfigContainer.TYPE_COMBOBOX:
        case ConfigContainer.TYPE_COMBOBOX_INDEX:
            input = new JComboBox(configEntry.getList());
            if (configEntry.getList().length > 0) {
                if (configEntry.getList()[0] instanceof JDLabelContainer) {
                    ((JComboBox) input).setRenderer(new JDLabelListRenderer());
                    ((JComboBox) input).setMaximumRowCount(10);
                }
            }

            ((JComboBox) input).addActionListener(this);
            if (configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName()) instanceof String) {
                for (int i = 0; i < configEntry.getList().length; i++) {
                    if (configEntry.getList()[i].toString().equals(configEntry.getPropertyInstance().getStringProperty(configEntry.getPropertyName()))) {
                        ((JComboBox) input).setSelectedIndex(i);
                        break;
                    }
                }
            } else {
                for (int i = 0; i < configEntry.getList().length; i++) {
                    if (configEntry.getList()[i].equals(configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName()))) {
                        ((JComboBox) input).setSelectedIndex(i);
                        break;
                    }
                }
            }
            break;
        case ConfigContainer.TYPE_RADIOFIELD:
            input = new JPanel(new MigLayout("ins 0", "", ""));
            JRadioButton radio;

            ButtonGroup group = new ButtonGroup();

            for (Object obj : configEntry.getList()) {
                radio = new JRadioButton(obj.toString());

                radio.setActionCommand(obj.toString());
                input.add(radio);

                radio.addActionListener(this);
                group.add(radio);

                Object p = configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName());
                if (p == null) p = "";

                if (obj.toString().equals(p.toString())) {
                    radio.setSelected(true);
                }
            }
            break;
        case ConfigContainer.TYPE_LABEL:
            break;
        case ConfigContainer.TYPE_SEPARATOR:
            decoration = new JSeparator(JSeparator.HORIZONTAL);
            break;
        case ConfigContainer.TYPE_COMPONENT:
            decoration = configEntry.getComponent();
            break;
        }

        if (input != null) enableComponent(input, configEntry.isEnabled());

        if (configEntry.getHelptags() != null) {
            String tooltip = JDL.LF("gui.tooltips.quickhelp", "Quickhelp available: %s (ctrl+shift+CLICK)", configEntry.getHelptags());
            if (decoration != null) {
                decoration.setName(configEntry.getHelptags());
                decoration.setToolTipText(tooltip);
            }
            if (input != null) {
                input.setName(configEntry.getHelptags());
                input.setToolTipText(tooltip);
            }
        }
    }

    public JComponent getInput() {
        return input;
    }

    public void actionPerformed(ActionEvent e) {
        configEntry.valueChanged(getText());
    }

    public void changedUpdate(DocumentEvent e) {
        configEntry.valueChanged(getText());
    }

    public ConfigEntry getConfigEntry() {
        return configEntry;
    }

    /**
     * Gibt den zusstand der Inputkomponente zurück
     * 
     * @return
     */
    public Object getText() {
        switch (configEntry.getType()) {
        case ConfigContainer.TYPE_PASSWORDFIELD:
            return new String(((JPasswordField) input).getPassword());
        case ConfigContainer.TYPE_TEXTFIELD:
        case ConfigContainer.TYPE_TEXTAREA:
        case ConfigContainer.TYPE_LISTCONTROLLED:
            return ((JTextComponent) input).getText();
        case ConfigContainer.TYPE_CHECKBOX:
            return ((JCheckBox) input).isSelected();
        case ConfigContainer.TYPE_COMBOBOX:
            return ((JComboBox) input).getSelectedItem();
        case ConfigContainer.TYPE_COMBOBOX_INDEX:
            return ((JComboBox) input).getSelectedIndex();
        case ConfigContainer.TYPE_RADIOFIELD:
            JRadioButton radio;
            Component[] inputs = input.getComponents();
            for (Component element : inputs) {
                radio = (JRadioButton) element;
                if (radio.getSelectedObjects() != null && radio.getSelectedObjects()[0] != null) return radio.getSelectedObjects()[0];
            }
            return null;
        case ConfigContainer.TYPE_BROWSEFOLDER:
        case ConfigContainer.TYPE_BROWSEFILE:
            return ((ComboBrowseFile) input).getText();
        case ConfigContainer.TYPE_SPINNER:
            return ((JSpinner) input).getValue();
        case ConfigContainer.TYPE_BUTTON:
        case ConfigContainer.TYPE_LABEL:
        case ConfigContainer.TYPE_SEPARATOR:
        case ConfigContainer.TYPE_COMPONENT:
            return null;
        }

        return null;
    }

    public void insertUpdate(DocumentEvent e) {
        configEntry.valueChanged(getText());
    }

    private final void enableComponent(JComponent cmp, boolean enabled) {
        cmp.setEnabled(enabled);
        if (cmp.getComponents() != null) {
            for (Component c : cmp.getComponents()) {
                c.setEnabled(enabled);
            }
        }
    }

    public void dataChanged(ConfigEntry source, Object newData) {
        if (input == null) return;
        boolean state = configEntry.isConditionalEnabled(source, newData);
        enableComponent(input, state);
    }

    public JComponent getDecoration() {
        return decoration;
    }

    public void removeUpdate(DocumentEvent e) {
        configEntry.valueChanged(getText());
    }

    /**
     * Sets data to the input component.
     * 
     * @param text
     */
    public void setData(Object text) {
        if (text == null && configEntry.getDefaultValue() != null) {
            text = configEntry.getDefaultValue();
        }
        switch (configEntry.getType()) {
        case ConfigContainer.TYPE_PASSWORDFIELD:
        case ConfigContainer.TYPE_TEXTFIELD:
        case ConfigContainer.TYPE_LISTCONTROLLED:
        case ConfigContainer.TYPE_TEXTAREA:
            ((JTextComponent) input).setText(text == null ? "" : text.toString());
            break;
        case ConfigContainer.TYPE_CHECKBOX:
            if (text == null) text = false;
            try {
                ((JCheckBox) input).setSelected((Boolean) text);
            } catch (Exception e) {
                logger.severe("Falcher Wert: " + text);
                ((JCheckBox) input).setSelected(false);
            }
            break;
        case ConfigContainer.TYPE_COMBOBOX:
            ((JComboBox) input).setSelectedItem(text);
            break;
        case ConfigContainer.TYPE_COMBOBOX_INDEX:
            if (text instanceof Integer) {
                ((JComboBox) input).setSelectedIndex((Integer) text);
            } else {
                ((JComboBox) input).setSelectedItem(text);
            }
            break;
        case ConfigContainer.TYPE_BROWSEFOLDER:
        case ConfigContainer.TYPE_BROWSEFILE:
            ((ComboBrowseFile) input).setText(text == null ? "" : text.toString());
            break;
        case ConfigContainer.TYPE_SPINNER:
            int value = text instanceof Integer ? (Integer) text : Integer.parseInt(text.toString());
            try {
                value = Math.min((Integer) ((SpinnerNumberModel) ((JSpinner) input).getModel()).getMaximum(), value);
                value = Math.max((Integer) ((SpinnerNumberModel) ((JSpinner) input).getModel()).getMinimum(), value);
                ((JSpinner) input).setModel(new SpinnerNumberModel(value, configEntry.getStart(), configEntry.getEnd(), configEntry.getStep()));
            } catch (Exception e) {
                JDLogger.exception(e);
            }
            break;
        case ConfigContainer.TYPE_RADIOFIELD:
            Component[] inputs = input.getComponents();
            for (int i = 0; i < configEntry.getList().length; i++) {
                JRadioButton radio = (JRadioButton) inputs[i];
                if (radio.getActionCommand().equals(text)) {
                    radio.setSelected(true);
                } else {
                    radio.setSelected(false);
                }
            }
            break;
        case ConfigContainer.TYPE_BUTTON:
        case ConfigContainer.TYPE_LABEL:
        case ConfigContainer.TYPE_SEPARATOR:
        case ConfigContainer.TYPE_COMPONENT:
            break;
        }
        configEntry.valueChanged(getText());
    }

    public void stateChanged(ChangeEvent e) {
        configEntry.valueChanged(getText());
    }

    /**
     * updates config --> gui
     */
    public void load() {
        if (configEntry.getPropertyInstance() != null && configEntry.getPropertyName() != null) {
            setData(configEntry.getPropertyInstance().getProperty(configEntry.getPropertyName(), configEntry.getDefaultValue()));
        } else if (configEntry.getListController() != null) {
            setData(configEntry.getListController().getList());
        }
    }

    /**
     * Saves the gui to config
     */
    public void save() {
        if (configEntry.getPropertyInstance() != null && configEntry.getPropertyName() != null) {
            configEntry.getPropertyInstance().setProperty(configEntry.getPropertyName(), getText());
        } else if (configEntry.getListController() != null) {
            configEntry.getListController().setList(getText() + "");
        }
    }

}
