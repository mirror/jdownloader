//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.utils.JDUtilities;

public class ConfigEntry implements Serializable, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static Logger logger = JDUtilities.getLogger();

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7422046260361380162L;
    private ActionListener actionListener;

    private String compareOperator;
    private ConfigEntry conditionEntry;
    private Object conditionValue;
    private ConfigContainer container;
    private Object defaultValue;
    private boolean enabled = true;
    private int end;
    private PropertyChangeListener guiListener;
    private String instantHelp;

    private String label;

    private Object[] list;

    private Vector<ConfigEntry> listener = new Vector<ConfigEntry>();

    private Property propertyInstance;

    private String propertyName;

    private int start;

    // private Object newValue;

    private int step = 1;

    private int type;;

    /**
     * KOnstruktor für Komponenten die nix brauchen. z.B. JSpearator
     * 
     * @param type
     */
    public ConfigEntry(int type) {
        this.type = type;
    }

    /**
     * Konstruktor für z.B. Buttons (Label+ Actionlistener)
     * 
     * @param type
     *            Typ ID (ConfigContainer.TYPE_*)
     * @param listener
     *            Actionlistener. Actionlistener werden z.B. von Buttons
     *            unterstützt
     * @param label
     *            Label für die Komponente
     */
    public ConfigEntry(int type, ActionListener listener, String label) {

        this.type = type;
        this.label = label;
        actionListener = listener;
        enabled = true;
    }

    public ConfigEntry(int type, ConfigContainer premiumConfig) {
        this.type = type;
        container = premiumConfig;
        enabled = true;
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
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;

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
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;
        this.label = label;
        enabled = true;
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
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;

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
        enabled = true;
    }

    /**
     * Konstruktor für z.B. einen Link (label& url
     * 
     * 
     * public ConfigEntry(int type, String label, String link) {
     */

    public ConfigEntry(int type, String label, String link) {
        this.type = type;
        propertyName = link;
        this.label = label;
        enabled = true;
    }

    public ConfigEntry(int type, Property propertyInstance,String propertyName, int num) {
        this.type = type;
        this.propertyName = propertyName;
        this.propertyInstance = propertyInstance;
        this.end=num;
        
    }

    private void addConditionListener(ConfigEntry configEntry) {
        if (configEntry != null) {
            listener.add(configEntry);

        }

    }

    public void addListener(ConfigEntry configEntry) {
        if (configEntry != null) {
            listener.add(configEntry);

        }

    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public String getCompareOperator() {
        return compareOperator;
    }

    public ConfigEntry getConditionEntry() {
        return conditionEntry;
    }

    public Object getConditionValue() {
        return conditionValue;
    }

    public ConfigContainer getContainer() {
        return container;
    }

    public Object getDefaultValue() {
        return defaultValue;

    }

    public int getEnd() {
        return end;
    }

    public PropertyChangeListener getGuiListener() {
        return guiListener;
    }

    public String getInstantHelp() {
        return instantHelp;
    }

    public String getLabel() {
        return label;
    }

    public Object[] getList() {
        return list;
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

    @SuppressWarnings("unchecked")
    public boolean isConditionalEnabled(PropertyChangeEvent evt) {
        if (evt.getSource() == conditionEntry) {
            if (compareOperator.equals("<")) {
                if (conditionValue instanceof Comparable) {
                    return ((Comparable) evt.getNewValue()).compareTo(conditionValue) < 0;
                } else if (conditionValue instanceof Integer) {
                    return (Integer) conditionValue < (Integer) evt.getNewValue();
                } else {
                    return true;
                }

            } else if (compareOperator.equals(">")) {
                if (conditionValue instanceof Comparable) {
                    return ((Comparable) evt.getNewValue()).compareTo(conditionValue) > 0;
                } else if (conditionValue instanceof Integer) {
                    return (Integer) conditionValue > (Integer) evt.getNewValue();
                } else {
                    return true;
                }
            } else if (compareOperator.equals("!=")) {
                if (conditionValue instanceof Comparable) {
                    return ((Comparable) evt.getNewValue()).compareTo(conditionValue) != 0;
                } else if (conditionValue instanceof Integer) {
                    return !((Integer) conditionValue).equals(evt.getNewValue());
                } else {
                    return true;
                }
            } else {
                if (conditionValue instanceof Comparable) {
                    return ((Comparable) evt.getNewValue()).compareTo(conditionValue) == 0;
                } else if (conditionValue instanceof Integer) {
                    return ((Integer) conditionValue).equals(evt.getNewValue());
                } else {
                    return true;
                }

            }

        }
        return true;
    }

    public boolean isEnabled() {

        return enabled;
    }

    public void propertyChange(PropertyChangeEvent evt) {

    }

    public ConfigEntry setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
        return this;
    }

    public void setCompareOperator(String compareOperator) {
        this.compareOperator = compareOperator;
    }

    public void setConditionEntry(ConfigEntry conditionEntry) {
        this.conditionEntry = conditionEntry;
        conditionEntry.addConditionListener(this);
    }

    public void setConditionValue(Object conditionValue) {
        this.conditionValue = conditionValue;
    }

    public void setContainer(ConfigContainer container) {
        this.container = container;
    }

    /**
     * Legtd en defaultwert fest, falls in der propertyinstanz keiner gefunden
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

    public ConfigEntry setEnabled(boolean value) {
        enabled = value;
        return this;
    }

    public ConfigEntry setEnabledCondidtion(ConfigEntry old, String comp, Object value) {
        setConditionEntry(old);
        setCompareOperator(comp);
        setConditionValue(value);
        return this;

    }

    public ConfigEntry setEnd(int end) {
        this.end = end;
        return this;
    }

    public void setGuiListener(PropertyChangeListener gce) {
        if (guiListener == null) {
            guiListener = gce;
        }

    }

    public ConfigEntry setInstantHelp(String l) {
        instantHelp = l;
        return this;

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
        ConfigEntry next;
        for (Iterator<ConfigEntry> it = listener.iterator(); it.hasNext();) {
            next = it.next();
            if (next.getGuiListener() != null) {
                next.getGuiListener().propertyChange(new PropertyChangeEvent(this, getPropertyName(), null, newValue));
            }
        }
    }

}