package org.jdownloader.plugins.config;

import java.awt.event.MouseEvent;
import java.util.ArrayList;

import jd.controlling.ClipboardMonitoring;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedConfigTableModel;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedTable;
import jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedValueColumn;
import jd.gui.swing.jdgui.views.settings.panels.advanced.EditColumn;
import jd.plugins.Plugin;
import jd.plugins.PluginConfigPanelNG;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DevConfig;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.advanced.AdvancedConfigEntry;

public class BasicAdvancedConfigPluginPanel extends PluginConfigPanelNG {
    private final class BasicAdvancedConfigTable extends AdvancedTable {
        private BasicAdvancedConfigTable(AdvancedConfigTableModel model) {
            super(model);
        }

        protected void initRowHeight() {
            setRowHeight(calculateAutoRowHeight() + 10);
        }
    }

    private final class BasicAdvancedConfigTableModel extends AdvancedConfigTableModel {
        private BasicAdvancedConfigTableModel(String id) {
            super(id);
        }

        @Override
        public void refresh(String filterText) {
            _fireTableStructureChanged(register(cfg), true);
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
                public boolean isHidable() {
                    return false;
                }

                @Override
                public boolean isSortable(AdvancedConfigEntry obj) {
                    return false;
                }

                @Override
                public boolean isAutoWidthEnabled() {
                    return true;
                }

                @Override
                protected boolean isDefaultResizable() {
                    return true;
                }

                @Override
                public boolean isResizable() {
                    return true;
                }

                @Override
                public int getDefaultWidth() {
                    return 0;
                }

                @Override
                protected int adjustWidth(int w) {
                    return this.calculateMinimumHeaderWidth();
                }

                @Override
                public String getHeaderTooltip() {
                    return null;
                }

                @Override
                public boolean isPaintWidthLockIcon() {
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
            addColumn(new AdvancedValueColumn() {
                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                public boolean isSortable(AdvancedConfigEntry obj) {
                    return false;
                }

                @Override
                public boolean isAutoWidthEnabled() {
                    return true;
                }

                @Override
                protected boolean isDefaultResizable() {
                    return true;
                }

                @Override
                public boolean isResizable() {
                    return true;
                }

                @Override
                public int getDefaultWidth() {
                    return 0;
                }

                @Override
                protected int adjustWidth(int w) {
                    return this.calculateMinimumHeaderWidth();
                }

                @Override
                public String getHeaderTooltip() {
                    return null;
                }

                @Override
                public boolean isPaintWidthLockIcon() {
                    return false;
                }
            });
            addColumn(new ExtTextColumn<AdvancedConfigEntry>(_GUI.T.AdvancedTableModel_initColumns_type_()) {
                private static final long serialVersionUID = 1L;

                public boolean isDefaultVisible() {
                    return false;
                }

                @Override
                public String getStringValue(AdvancedConfigEntry value) {
                    return value.getTypeString();
                }

                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                public boolean isSortable(AdvancedConfigEntry obj) {
                    return false;
                }

                @Override
                public boolean isAutoWidthEnabled() {
                    return true;
                }

                @Override
                protected boolean isDefaultResizable() {
                    return true;
                }

                @Override
                public boolean isResizable() {
                    return true;
                }

                @Override
                public int getDefaultWidth() {
                    return 0;
                }

                @Override
                protected int adjustWidth(int w) {
                    return this.calculateMinimumHeaderWidth();
                }

                @Override
                public String getHeaderTooltip() {
                    return null;
                }

                @Override
                public boolean isPaintWidthLockIcon() {
                    return false;
                }
            });
            addColumn(new EditColumn());
        }
    }

    private AdvancedConfigTableModel model;
    private ConfigInterface          cfg;
    private String                   description;
    private String                   filter;

    public BasicAdvancedConfigPluginPanel(String description, ConfigInterface cfg, String filter) {
        this.cfg = cfg;
        this.description = description;
        this.filter = filter;
    }

    @Override
    protected void initPluginSettings(Plugin protoType) {
        if (description != null) {
            addStartDescription(description);
        }
        // JLabel lbl;
        // lbl.setForeground(LAFOptions.getInstance().getColorForErrorForeground());
        add(new BasicAdvancedConfigTable(model = new BasicAdvancedConfigTableModel("ConfigPanel_" + cfg.getClass().getSimpleName())), "gapleft" + getLeftGap() + ",aligny top,pushx,growx,spanx");
        model.refresh(filter);
    }

    public String getLeftGap() {
        return "32";
    }

    public ArrayList<AdvancedConfigEntry> register(ConfigInterface cfg) {
        final ArrayList<AdvancedConfigEntry> configInterfaces = new ArrayList<AdvancedConfigEntry>();
        for (final KeyHandler m : cfg._getStorageHandler().getKeyHandler()) {
            if (m.getAnnotation(AboutConfig.class) != null && (m.getAnnotation(DevConfig.class) == null)) {
                if (m.getSetMethod() == null) {
                    throw new RuntimeException("Setter for " + m.getKey() + " missing");
                } else if (m.getGetMethod() == null) {
                    throw new RuntimeException("Getter for " + m.getKey() + " missing");
                } else {
                    synchronized (configInterfaces) {
                        configInterfaces.add(new AdvancedConfigEntry(cfg, m));
                    }
                }
            }
        }
        return configInterfaces;
    }

    @Override
    public void reset() {
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }
}
