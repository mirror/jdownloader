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

package jd.config;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.ImageIcon;

import jd.controlling.ListController;

public class ConfigEntry implements Serializable {

    public static enum PropertyType {
        NONE, NORMAL, NEEDS_RESTART;

        public static PropertyType getMax(PropertyType... changes) {
            ArrayList<PropertyType> sorter = new ArrayList<PropertyType>();
            for (PropertyType type : changes) {
                sorter.add(type);
            }
            Collections.sort(sorter);
            PropertyType ret = sorter.get(sorter.size() - 1);

            return ret;
        }

        public PropertyType getMax(PropertyType propertyType) {
            return getMax(propertyType, this);
        }

    }

    private static final long serialVersionUID = 7422046260361380162L;

    /**
     * Generelle Variablen
     */
    private int type;
    private ConfigGroup group;
    private String label;
    private Object defaultValue;
    private String helptags = null;
    private boolean enabled = true;
    private Property propertyInstance = null;
    private String propertyName = null;
    private PropertyType propertyType = PropertyType.NORMAL;
    private PropertyChangeListener guiListener;

    /**
     * Variablen für den Vergleich mit einem anderen ConfigEntry.
     */
    private ConfigEntry conditionEntry;
    private Boolean compareValue;
    private ConfigEntry listener = null;

    /**
     * Variablen für einen Button-Eintrag.
     */
    private String description;
    private ActionListener actionListener;
    private ImageIcon imageIcon;

    /**
     * Variablen für einen ListController-Eintrag.
     */
    private transient ListController controller;

    /**
     * Variablen für einen ComboBox- oder RadioField-Eintrag.
     */
    private Object[] list;

    /**
     * Variablen für einen Spinner-Eintrag.
     */
    private int start;
    private int end;
    private int step = 1;

    /**
     * Konstruktor für Komponenten die nix brauchen. z.B. JSeparator
     * 
     * @param type
     * @see ConfigContainer#TYPE_SEPARATOR
     */
    public ConfigEntry(int type) {
        this.type = type;
    }

    /**
     * Konstruktor für z.B. Buttons (Label + Actionlistener)
     * 
     * @param type
     *            Typ ID (ConfigContainer.TYPE_*)
     * @param listener
     *            Actionlistener. Actionlistener werden z.B. von Buttons
     *            unterstützt
     * @param label
     *            Label für die Komponente
     * @see ConfigContainer#TYPE_BUTTON
     */
    public ConfigEntry(int type, ActionListener actionListener, String label, String description, ImageIcon icon) {
        this.type = type;
        this.actionListener = actionListener;
        this.label = label;
        this.description = description;
        this.imageIcon = icon;
    }

    /**
     * @see ConfigContainer#TYPE_LISTCONTROLLED
     */
    public ConfigEntry(int type, ListController controller, String label) {
        this.type = type;
        this.controller = controller;
        this.label = label;
    }

    /**
     * Konstruktor z.B. für Combobox oder radiofield ( mehrere werte(list), eine
     * auswahl (property)
     * 
     * @param type
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname über den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param list
     *            Liste mit allen werten aus denen ausgewählt werden kann
     * @param label
     * @see ConfigContainer#TYPE_COMBOBOX
     * @see ConfigContainer#TYPE_COMBOBOX_INDEX
     * @see ConfigContainer#TYPE_RADIOFIELD
     */
    public ConfigEntry(int type, Property propertyInstance, String propertyName, Object[] list, String label) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.list = list;
        this.label = label;
    }

    /**
     * Konstruktor für z.B. ein Textfeld (label& ein eingabefeld
     * 
     * @param type
     *            TYP ID
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname über den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param label
     *            angezeigtes label
     * @see ConfigContainer#TYPE_BROWSEFILE
     * @see ConfigContainer#TYPE_BROWSEFOLDER
     * @see ConfigContainer#TYPE_PASSWORDFIELD
     * @see ConfigContainer#TYPE_TEXTAREA
     * @see ConfigContainer#TYPE_TEXTFIELD
     */
    public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
    }

    /**
     * Konstruktor z.B. für einen JSpinner (property, label, range (start/end),
     * step)
     * 
     * @param type
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname über den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param label
     * @param start
     *            Range-Start
     * @param end
     *            Range-Ende
     * @param step
     *            Schrittweite
     * @see ConfigContainer#TYPE_SPINNER
     */
    public ConfigEntry(int type, Property propertyInstance, String propertyName, String label, int start, int end, int step) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
        this.start = start;
        this.end = end;
        this.step = step;
    }

    /**
     * @deprecated Use
     *             {@link ConfigEntry#ConfigEntry(int, Property, String, String, int, int, int)}
     *             instead.
     */
    @Deprecated
    public ConfigEntry(int type, Property propertyInstance, String propertyName, String label, int start, int end) {
        this(type, propertyInstance, propertyName, label, start, end, 1);
    }

    /**
     * @deprecated Use
     *             {@link ConfigEntry#ConfigEntry(int, Property, String, String, int, int, int)}
     *             instead.
     */
    @Deprecated
    public ConfigEntry setStep(int step) {
        this.step = step;
        return this;
    }

    /**
     * Konstruktor für ein einfaches Label
     * 
     * @param type
     * @param label
     * @see ConfigContainer#TYPE_LABEL
     */
    public ConfigEntry(int type, String label) {
        this.type = type;
        this.label = label;
    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public int getEnd() {
        return end;
    }

    public ConfigGroup getGroup() {
        return group;
    }

    public PropertyChangeListener getGuiListener() {
        return guiListener;
    }

    public String getHelptags() {
        if (helptags == null) return label;
        return helptags;
    }

    public ImageIcon getImageIcon() {
        return imageIcon;
    }

    public String getLabel() {
        return label;
    }

    public Object[] getList() {
        return list;
    }

    public ListController getListController() {
        if (controller == null) {
            controller = new ListController() {
                public String getList() {
                    return "";
                }

                public void setList(String list) {
                }
            };
        }
        return controller;
    }

    public Property getPropertyInstance() {
        return propertyInstance;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public int getStart() {
        return start;
    }

    public int getStep() {
        return step;
    }

    /**
     * Gibt den Typ zurück
     * 
     * @return Typ des Eintrages
     */
    public int getType() {
        return type;
    }

    public boolean isConditionalEnabled(PropertyChangeEvent evt) {
        if (evt.getSource() == conditionEntry) return compareValue.equals((Boolean) evt.getNewValue());
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Legt den defaultwert fest, falls in der propertyinstanz keiner gefunden
     * wurde.
     * 
     * @param value
     * @return this. damit ist eine Struktur new
     *         ConfigEntry(...).setdefaultValue(...).setStep(...).setBla...
     *         möglich
     */
    public ConfigEntry setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ConfigEntry setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ConfigEntry setEnabledCondidtion(ConfigEntry conditionEntry, boolean compareValue) {
        this.conditionEntry = conditionEntry;
        this.compareValue = compareValue;
        conditionEntry.setListener(this);
        return this;
    }

    public void setListener(ConfigEntry listener) {
        this.listener = listener;
    }

    public void setGroup(ConfigGroup group) {
        this.group = group;
    }

    public void setGuiListener(PropertyChangeListener gce) {
        if (guiListener == null) guiListener = gce;
    }

    public ConfigEntry setHelptags(String helptags) {
        this.helptags = helptags;
        return this;
    }

    /**
     * Sets the propoertyType. one of PropertyType enum.
     * 
     * @param propertyType
     * @return
     */
    public ConfigEntry setPropertyType(PropertyType propertyType) {
        this.propertyType = propertyType;
        return this;
    }

    public void valueChanged(Object newValue) {
        if (listener != null && listener.getGuiListener() != null) {
            listener.getGuiListener().propertyChange(new PropertyChangeEvent(this, getPropertyName(), null, newValue));
        }
    }

}