package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.util.Iterator;
import java.util.Locale;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedTableModel extends ExtTableModel<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;
    private String            text             = null;

    public AdvancedTableModel(String id) {
        super(id);

    }

    @Override
    public void _fireTableStructureChanged(java.util.List<AdvancedConfigEntry> newtableData, boolean refreshSort) {
        String ltext = text;
        if (ltext != null) {
            ltext = ltext.toLowerCase();
            AdvancedConfigEntry next;
            for (Iterator<AdvancedConfigEntry> it = newtableData.iterator(); it.hasNext();) {
                next = it.next();
                if (!next.getKey().toLowerCase().contains(ltext)) {
                    if (next.getDescription() == null || !next.getDescription().toLowerCase(Locale.ENGLISH).contains(ltext)) {
                        if (!createKeyText(next).toLowerCase(Locale.ENGLISH).contains(ltext)) {
                            it.remove();
                        }
                    }
                }
            }
        }
        super._fireTableStructureChanged(newtableData, refreshSort);
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI._.AdvancedTableModel_initColumns_key_()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return createKeyText(value);
            }

            @Override
            public boolean isEditable(AdvancedConfigEntry obj) {
                return true;
            }

            @Override
            public int getDefaultWidth() {
                return 200;
            }

            @Override
            public boolean isHidable() {
                return false;
            }
        });
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI._.AdvancedTableModel_initColumns_desc_()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getDescription();
            }

            @Override
            public boolean isEditable(AdvancedConfigEntry obj) {
                return true;
            }

            @Override
            public int getDefaultWidth() {
                return 200;
            }

            @Override
            public boolean isHidable() {
                return true;
            }
        });

        addColumn(new AdvancedValueColumn());
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI._.AdvancedTableModel_initColumns_type_()) {
            private static final long serialVersionUID = 1L;

            @Override
            public int getDefaultWidth() {

                return 100;
            }

            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getTypeString();
            }

        });
        addColumn(new EditColumn());
    }

    public void refresh(final String filterText) {
        this.text = filterText;
        _fireTableStructureChanged(AdvancedConfigManager.getInstance().list(), true);
    }

    protected String createKeyText(AdvancedConfigEntry value) {
        String getterName = value.getKeyHandler().getGetter().getMethod().getName();
        if (getterName.startsWith("is")) {
            getterName = getterName.substring(2);
        } else if (getterName.startsWith("get")) {
            getterName = getterName.substring(3);
        }
        getterName = getterName.replaceAll("([a-z])([A-Z])", "$1 $2");
        if (getterName.endsWith(" Enabled")) {
            getterName = getterName.substring(0, getterName.length() - 8);
        }

        return value.getConfigInterface()._getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + ": " + getterName;
    }

}
