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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import jd.controlling.ListController;

public class ConfigEntry implements Serializable {

    public static enum PropertyType {
        NONE, NORMAL, NEEDS_RESTART;

        public static PropertyType getMax(final PropertyType... changes) {
            final ArrayList<PropertyType> sorter = new ArrayList<PropertyType>();
            for (final PropertyType type : changes) {
                sorter.add(type);
            }
            Collections.sort(sorter);
            final PropertyType ret = sorter.get(sorter.size() - 1);

            return ret;
        }

        public PropertyType getMax(final PropertyType propertyType) {
            return getMax(propertyType, this);
        }

    }

    private static final long serialVersionUID = 7422046260361380162L;

    /**
     * Generelle Variablen
     */
    private final int type;
    private ConfigGroup group;
    private String label;
    private Object defaultValue;
    private boolean enabled = true;
    private Property propertyInstance = null;
    private String propertyName = null;
    private PropertyType propertyType = PropertyType.NORMAL;
    private GuiConfigListener guiListener;

    /**
     * Variablen fuer den Vergleich mit einem anderen ConfigEntry.
     */
    private ConfigEntry conditionEntry;
    private Boolean compareValue;
    private final ArrayList<ConfigEntry> listener = new ArrayList<ConfigEntry>();

    /**
     * Variablen fuer einen Button-Eintrag.
     */
    private String description;
    private ActionListener actionListener;
    private ImageIcon imageIcon;

    /**
     * Variablen fuer einen ListController-Eintrag.
     */
    private transient ListController controller;

    /**
     * Variablen fuer einen ComboBox- oder RadioField-Eintrag.
     */
    private Object[] list;

    /**
     * Variablen fuer einen Spinner-Eintrag.
     */
    private int start;
    private int end;
    private int step = 1;

    /**
     * Variablen fuer einen Komponenten-Eintrag.
     */
    private JComponent component;
    private String constraints;

    /**
     * Konstruktor fuer Komponenten die nix brauchen. z.B. JSeparator
     * 
     * @param type
     * @see ConfigContainer#TYPE_SEPARATOR
     */
    public ConfigEntry(final int type) {
        this.type = type;
    }

    /**
     * Konstruktor fuer Komponenten, welche eine Swing-Komponente darstellen
     * sollen
     * 
     * @param type
     * @param component
     * @param constraints
     * @see ConfigContainer#TYPE_COMPONENT
     */
    public ConfigEntry(final int type, final JComponent component, final String constraints) {
        this.type = type;
        this.component = component;
        this.constraints = constraints;
    }

    /**
     * Konstruktor fuer z.B. Buttons (Label + Actionlistener)
     * 
     * @param type
     *            Typ ID (ConfigContainer.TYPE_*)
     * @param actionListener
     *            Actionlistener. Actionlistener werden z.B. von Buttons
     *            unterstuetzt
     * @param label
     *            Label fuer die Komponente
     * @see ConfigContainer#TYPE_BUTTON
     */
    public ConfigEntry(final int type, final ActionListener actionListener, final String description, final String label, final ImageIcon icon) {
        this.type = type;
        this.actionListener = actionListener;
        this.description = description;
        this.label = label;
        this.imageIcon = icon;
    }

    /**
     * @see ConfigContainer#TYPE_LISTCONTROLLED
     */
    public ConfigEntry(final int type, final ListController controller, final String label) {
        this.type = type;
        this.controller = controller;
        this.label = label;
    }

    /**
     * Konstruktor z.B. fuer Combobox oder radiofield ( mehrere werte(list),
     * eine auswahl (property)
     * 
     * @param type
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname ueber den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param list
     *            Liste mit allen werten aus denen ausgewaehlt werden kann
     * @param label
     * @see ConfigContainer#TYPE_COMBOBOX
     * @see ConfigContainer#TYPE_COMBOBOX_INDEX
     * @see ConfigContainer#TYPE_RADIOFIELD
     */
    public ConfigEntry(final int type, final Property propertyInstance, final String propertyName, final Object[] list, final String label) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.list = list;
        this.label = label;
    }

    /**
     * Konstruktor fuer z.B. ein Textfeld (label& ein eingabefeld
     * 
     * @param type
     *            TYP ID
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname ueber den auf einen wert in der propertyInstanz
     *            zugegriffen wird
     * @param label
     *            angezeigtes label
     * @see ConfigContainer#TYPE_BROWSEFILE
     * @see ConfigContainer#TYPE_BROWSEFOLDER
     * @see ConfigContainer#TYPE_CHECKBOX
     * @see ConfigContainer#TYPE_PASSWORDFIELD
     * @see ConfigContainer#TYPE_TEXTAREA
     * @see ConfigContainer#TYPE_TEXTFIELD
     */
    public ConfigEntry(final int type, final Property propertyInstance, final String propertyName, final String label) {
        this.type = type;
        this.propertyInstance = propertyInstance;
        this.propertyName = propertyName;
        this.label = label;
    }

    /**
     * Konstruktor z.B. fuer einen JSpinner (property, label, range (start/end),
     * step)
     * 
     * @param type
     * @param propertyInstance
     *            EINE Instanz die von der propertyklasse abgeleitet wurde. mit
     *            hilfe von propertyName werden Informationen aus ihr gelesen
     *            und wieder in ihr abgelegt
     * @param propertyName
     *            propertyname ueber den auf einen wert in der propertyInstanz
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
    public ConfigEntry(final int type, final Property propertyInstance, final String propertyName, final String label, final int start, final int end, final int step) {
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
    public ConfigEntry(final int type, final Property propertyInstance, final String propertyName, final String label, final int start, final int end) {
        this(type, propertyInstance, propertyName, label, start, end, 1);
    }

    /**
     * @deprecated Use
     *             {@link ConfigEntry#ConfigEntry(int, Property, String, String, int, int, int)}
     *             instead.
     */
    @Deprecated
    public ConfigEntry setStep(final int step) {
        this.step = step;
        return this;
    }

    /**
     * Konstruktor fuer ein einfaches Label
     * 
     * @param type
     * @param label
     * @see ConfigContainer#TYPE_LABEL
     */
    public ConfigEntry(final int type, final String label) {
        this.type = type;
        this.label = label;
    }

    public ActionListener getActionListener() {
        return actionListener;
    }

    public JComponent getComponent() {
        return component;
    }

    public String getConstraints() {
        return constraints;
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

    public GuiConfigListener getGuiListener() {
        return guiListener;
    }

    /**
     * deprecated should think about removing it, there is nearly <b>no</b>
     * helppage in our wiki
     */
    public String getHelptags() {
        return label;
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

                public void setList(final String list) {
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
     * Gibt den Typ zurueck
     * 
     * @return Typ des Eintrages
     */
    public int getType() {
        return type;
    }

    public boolean isConditionalEnabled(final ConfigEntry source, final Object newData) {
        return (source == conditionEntry) ? compareValue.equals(newData) : true;
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
     *         moeglich
     */
    public ConfigEntry setDefaultValue(final Object defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public ConfigEntry setEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ConfigEntry setEnabledCondidtion(final ConfigEntry conditionEntry, final boolean compareValue) {
        this.conditionEntry = conditionEntry;
        this.compareValue = compareValue;
        conditionEntry.addListener(this);
        return this;
    }

    public void addListener(final ConfigEntry listener) {
        if (listener != null) {
            this.listener.add(listener);
        }
    }

    public void setGroup(final ConfigGroup group) {
        this.group = group;
    }

    public void setGuiListener(final GuiConfigListener guiListener) {
        if (this.guiListener == null) {
            this.guiListener = guiListener;
        }
    }

    /**
     * Sets the propoertyType. one of PropertyType enum.
     * 
     * @param propertyType
     * @return
     */
    public ConfigEntry setPropertyType(final PropertyType propertyType) {
        this.propertyType = propertyType;
        return this;
    }

    public void valueChanged(final Object newValue) {
        for (final ConfigEntry next : listener) {
            final GuiConfigListener nextGuiListener = next.getGuiListener();
            if (nextGuiListener != null) {
                nextGuiListener.dataChanged(this, newValue);
            }
        }
    }

}