package jd.gui.swing.jdgui.views.settings.components;

import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ComboBox<T> extends JComboBox implements SettingsComponent {

    private static final long serialVersionUID = -1580999899097054630L;
    private ListCellRenderer  orgRenderer;
    private String[]          translations;

    public ComboBox(T... options) {
        super(options);
        // this.setSelectedIndex(selection);
    }

    @SuppressWarnings("unchecked")
    public T getValue() {
        return (T) getSelectedItem();
    }

    public void setValue(T selected) {
        this.setSelectedItem(selected);
    }

    public ComboBox(T[] values, String[] names) {
        super(values);
        orgRenderer = getRenderer();
        this.translations = names;
        this.setRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (index == -1) index = getSelectedIndex();
                return orgRenderer.getListCellRendererComponent(list, translations[index], index, isSelected, cellHasFocus);
            }
        });
    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }
}
