package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.Locale;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

import jd.controlling.ClipboardMonitoring;

public class AdvancedConfigTableModel extends ExtTableModel<AdvancedConfigEntry> {
    private static final long serialVersionUID = 1L;
    private volatile String   text             = null;

    public AdvancedConfigTableModel(String id) {
        super(id);

    }

    private boolean containsKeyword(final AdvancedConfigEntry configEntry, final String[] finds) {
        final String[] keywords = configEntry != null ? configEntry.getKeywords() : null;
        if (keywords == null || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            }
            for (final String keyword : keywords) {
                if (result) {
                    if (StringUtils.containsIgnoreCase(keyword, find)) {
                        continue;
                    } else {
                        return false;
                    }
                } else {
                    if (StringUtils.containsIgnoreCase(keyword, find)) {
                        result = true;
                    } else {
                        return false;
                    }
                }
            }
        }
        return result;
    }

    private boolean containsInternalKey(final AdvancedConfigEntry configEntry, final String[] finds) {
        final String internalKey = configEntry != null ? configEntry.getInternalKey() : null;
        if (StringUtils.isEmpty(internalKey) || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            }
            if (result) {
                if (StringUtils.containsIgnoreCase(internalKey, find)) {
                    continue;
                } else {
                    return false;
                }
            } else {
                if (StringUtils.containsIgnoreCase(internalKey, find)) {
                    result = true;
                } else {
                    return false;
                }
            }
        }
        return result;
    }

    private boolean containsDescription(final AdvancedConfigEntry configEntry, final String[] finds) {
        final String description = configEntry != null ? configEntry.getDescription() : null;
        if (StringUtils.isEmpty(description) || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            }
            if (result) {
                if (StringUtils.containsIgnoreCase(description, find)) {
                    continue;
                } else {
                    return false;
                }
            } else {
                if (StringUtils.containsIgnoreCase(description, find)) {
                    result = true;
                } else {
                    return false;
                }
            }
        }
        return result;
    }

    private boolean containsKeyText(final AdvancedConfigEntry configEntry, final String[] finds) {
        final String keyText = configEntry != null ? configEntry.getKeyText() : null;
        if (StringUtils.isEmpty(keyText) || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            }
            if (result) {
                if (StringUtils.containsIgnoreCase(keyText, find)) {
                    continue;
                } else {
                    return false;
                }
            } else {
                if (StringUtils.containsIgnoreCase(keyText, find)) {
                    result = true;
                } else {
                    return false;
                }
            }
        }
        return result;
    }

    @Override
    public void _fireTableStructureChanged(java.util.List<AdvancedConfigEntry> newtableData, boolean refreshSort) {
        final String ltext = text;
        if (!StringUtils.isEmpty(ltext)) {
            final String finds[] = ltext.replaceAll("[^a-zA-Z0-9 ]+", "").replace("colour", "color").replace("directory", "folder").toLowerCase(Locale.ENGLISH).split("\\s");
            if (finds.length > 0) {
                for (final Iterator<AdvancedConfigEntry> it = newtableData.iterator(); it.hasNext();) {
                    final AdvancedConfigEntry next = it.next();
                    if (containsInternalKey(next, finds)) {
                        continue;
                    }
                    if (containsKeyword(next, finds)) {
                        continue;
                    }
                    if (containsDescription(next, finds)) {
                        continue;
                    }
                    if (containsKeyText(next, finds)) {
                        continue;
                    }
                    it.remove();
                }
            }
        }
        super._fireTableStructureChanged(newtableData, refreshSort);
    }

    @Override
    protected void initColumns() {
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI.T.AdvancedTableModel_initColumns_key_()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String getTooltipText(AdvancedConfigEntry obj) {
                return obj.getDescription();
            }

            @Override
            public String getStringValue(AdvancedConfigEntry value) {
                return value.getKeyText();
            }

            @Override
            public boolean isEditable(AdvancedConfigEntry obj) {
                return false;
            }

            @Override
            public boolean onDoubleClick(MouseEvent e, AdvancedConfigEntry obj) {
                ClipboardMonitoring.getINSTANCE().setCurrentContent(obj.getKey());
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
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI.T.AdvancedTableModel_initColumns_desc_()) {
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
            public boolean onDoubleClick(MouseEvent e, AdvancedConfigEntry obj) {
                ClipboardMonitoring.getINSTANCE().setCurrentContent(obj.getDescription());
                return true;
            }

            @Override
            public boolean isEditable(AdvancedConfigEntry obj) {
                return false;
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
        addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI.T.AdvancedTableModel_initColumns_type_()) {
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

}
