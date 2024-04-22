package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.views.settings.panels.advanced.EditColumn.ResetAction;

import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;
import org.jdownloader.settings.advanced.AdvancedConfigManager;

public class AdvancedConfigTableModel extends ExtTableModel<AdvancedConfigEntry> {
    private static final long   serialVersionUID = 1L;
    private volatile String     text             = null;
    private final AtomicBoolean resetOnlyFilter  = new AtomicBoolean(false);

    public AdvancedConfigTableModel(String id) {
        super(id);
    }

    private boolean containsKeyword(final AdvancedConfigEntry configEntry, final String[] finds) {
        final String[] keywords = configEntry != null ? configEntry.getKeywords() : null;
        if (keywords == null || keywords.length == 0 || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            } else {
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
        }
        return result;
    }

    private boolean contains(final String values[], final String[] finds) {
        if (values == null) {
            return false;
        }
        for (final String value : values) {
            if (contains(value, finds)) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(final String value, final String[] finds) {
        if (StringUtils.isEmpty(value) || finds == null) {
            return false;
        }
        boolean result = false;
        for (final String find : finds) {
            if ("".equals(find)) {
                // ignore empty
                continue;
            } else if (result) {
                if (StringUtils.containsIgnoreCase(value, find)) {
                    continue;
                } else {
                    return false;
                }
            } else {
                if (StringUtils.containsIgnoreCase(value, find)) {
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
        if (resetOnlyFilter.get()) {
            final ResetAction resetAction = new ResetAction(null);
            for (final Iterator<AdvancedConfigEntry> it = newtableData.iterator(); it.hasNext();) {
                final AdvancedConfigEntry next = it.next();
                resetAction.setEntry(next);
                if (!resetAction.isEnabled()) {
                    it.remove();
                }
            }
        }
        final String ltext = text;
        if (!StringUtils.isEmpty(ltext)) {
            final String finds[] = ltext.replaceAll("[^a-zA-Z0-9 ,]+", "").replace("colour", "color").replace("directory", "folder").toLowerCase(Locale.ENGLISH).split("(\\s|,)");
            if (finds.length > 0) {
                for (final Iterator<AdvancedConfigEntry> it = newtableData.iterator(); it.hasNext();) {
                    final AdvancedConfigEntry next = it.next();
                    if (next != null) {
                        if (contains(next.getInternalKey(), finds)) {
                            continue;
                        } else if (containsKeyword(next, finds)) {
                            continue;
                        } else if (contains(next.getDescription(), finds)) {
                            continue;
                        } else if (contains(next.getKeyText(), finds)) {
                            continue;
                        }
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
                if (value.hasDescription()) {
                    return value.getDescription();
                } else {
                    return null;
                }
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
        addColumn(new EditColumn() {
            @Override
            protected boolean isResetOnlyFilterEnabled() {
                return resetOnlyFilter.get();
            }

            @Override
            protected void setResetOnlyFilterEnabled(boolean enabled) {
                if (resetOnlyFilter.compareAndSet(!enabled, enabled)) {
                    refresh(AdvancedConfigTableModel.this.text);
                }
            }
        });
    }

    public void refresh(final String filterText) {
        AdvancedConfigTableModel.this.text = filterText;
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                final List<AdvancedConfigEntry> list = AdvancedConfigManager.getInstance().list();
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        _fireTableStructureChanged(list, true);
                    }
                };
                return null;
            }
        });
    }
}
