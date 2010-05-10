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
import java.util.Vector;

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

        public String toString() {
            return this.name();
        }
    }

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7422046260361380162L;
    private ActionListener actionListener;
    private boolean changes;
    private ConfigEntry conditionEntry;
    private Boolean compareValue;
    private transient ListController controller;
    private Object defaultValue;
    private String description;
    private boolean enabled = true;
    private int end;
    private ConfigGroup group;
    private PropertyChangeListener guiListener;
    private String helptags = null;
    private ImageIcon imageIcon;
    private String label;
    private Object[] list;
    private Vector<ConfigEntry> listener = new Vector<ConfigEntry>();
    private Property propertyInstance;
    private String propertyName;
    private PropertyType propertyType = PropertyType.NORMAL;
    private int start;
    private int step = 1;
    private int type;

    /**
     * Konstruktor für Komponenten die nix brauchen. z.B. JSeparator
     * 
     * @param type
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
     */
    public ConfigEntry(int type, ActionListener actionListener, String label, String description, ImageIcon icon) {
        this.type = type;
        this.actionListener = actionListener;
        this.label = label;
        this.description = description;
        this.imageIcon = icon;
        this.enabled = true;
    }

    public ConfigEntry(int type, ListController controller, String label) {
        this.type = type;
        this.controller = controller;
        this.label = label;
        this.propertyName = null;
        this.enabled = true;
    }

    /**
     * Konstruktor z.B. für Combobox oder radiofield ( mehrere werte(list), eine
     * auswahl (property)
     * 
     * @param type
     * @param propertyInstance
     *            EINE INstanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname über den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param list
     *            Liste mit allen werten aus denen ausgewählt werden kann
     * @param label
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
     */
    public ConfigEntry(int type, Property propertyInstance, String propertyName, String label) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
        this.enabled = true;
    }

    /**
     * Konstruktor z.B. für einen JSpinner (property, label, range (start/end)
     * 
     * @param type
     * @param propertyInstance
     *            EINE INstanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname über den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param label
     * @param start
     *            Range-start
     * @param end
     *            range-ende
     */
    public ConfigEntry(int type, Property propertyInstance, String propertyName, String label, int start, int end) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
        this.start = start;
        this.end = end;
    }

    /**
     * Konstruktor für ein einfaches Label
     * 
     * @param type
     * @param label
     */
    public ConfigEntry(int type, String label) {
        this.type = type;
        this.label = label;
        this.enabled = true;
    }

    public void addListener(ConfigEntry configEntry) {
        if (configEntry != null) {
            listener.add(configEntry);
        }
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

    public Vector<ConfigEntry> getListener() {
        return listener;
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

    /** return if this configentry got changed and has to be saved */
    public boolean hasChanges() {
        return changes;
    }

    public boolean isConditionalEnabled(PropertyChangeEvent evt) {
        if (evt.getSource() == conditionEntry) return compareValue.equals((Boolean) evt.getNewValue());
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ConfigEntry setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    /** Gets set if this config entry has changes */
    public void setChanges(boolean b) {
        changes = b;
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
    public ConfigEntry setDefaultValue(Object value) {
        defaultValue = value;
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConfigEntry setEnabled(boolean value) {
        enabled = value;
        return this;
    }

    public ConfigEntry setEnabledCondidtion(ConfigEntry conditionEntry, boolean compareValue) {
        if (conditionEntry.getType() != ConfigContainer.TYPE_CHECKBOX) new Exception("Only checkbox-configentries are allowed").printStackTrace();
        this.conditionEntry = conditionEntry;
        this.compareValue = compareValue;
        conditionEntry.addListener(this);
        return this;
    }

    public ConfigEntry setEnd(int end) {
        this.end = end;
        return this;
    }

    public ConfigEntry setGroup(ConfigGroup cg) {
        this.group = cg;
        return this;
    }

    public void setGuiListener(PropertyChangeListener gce) {
        if (guiListener == null) {
            guiListener = gce;
        }
    }

    public void setHelptags(String helptags) {
        this.helptags = helptags;
    }

    public void setImageIcon(ImageIcon imageIcon) {
        this.imageIcon = imageIcon;
    }

    public ConfigEntry setLabel(String label) {
        this.label = label;
        return this;
    }

    public ConfigEntry setList(Object[] list) {
        this.list = list;
        return this;
    }

    public ConfigEntry setPropertyInstance(Property propertyInstance) {
        this.propertyInstance = propertyInstance;
        return this;
    }

    public ConfigEntry setPropertyName(String propertyName) {
        this.propertyName = propertyName;
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

    public ConfigEntry setStart(int start) {
        this.start = start;
        return this;
    }

    /**
     * Setzt die Schrittbreite für alle Komponenten die mit Schritten arbeiten.
     * z.B. JSpiner
     * 
     * @param step
     * @return this. damit ist eine Struktur new
     *         ConfigEntry(...).setdefaultValue(...).setStep(...).setBla...
     *         möglich
     * 
     */
    public ConfigEntry setStep(int step) {
        this.step = step;
        return this;
    }

    public ConfigEntry setType(int type) {
        this.type = type;
        return this;
    }

    public void valueChanged(Object newValue) {
        for (ConfigEntry next : listener) {
            if (next.getGuiListener() != null) {
                next.getGuiListener().propertyChange(new PropertyChangeEvent(this, getPropertyName(), null, newValue));
            }
        }
    }

}