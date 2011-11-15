package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtIconColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.event.predefined.changeevent.ChangeEvent;
import org.appwork.utils.event.predefined.changeevent.ChangeListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class FilterTableModel extends ExtTableModel<PackagizerRule> implements ChangeListener {

    private static final long serialVersionUID = -7756459932564776739L;

    public FilterTableModel(String id) {
        super(id);
        PackagizerController.getInstance().getEventSender().addListener(this, false);
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_enabled()) {

            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    private static final long serialVersionUID = 3938290423337000265L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("ok", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
                object.setEnabled(value);
                PackagizerController.getInstance().update();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_name()) {

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            protected Icon getIcon(final PackagizerRule value) {
                String key = value.getIconKey();
                if (key == null) {
                    return null;
                } else {
                    return NewTheme.I().getIcon(key, 18);
                }
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return value.getName();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filename()) {

            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFilenameFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filesize()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFilesizeFilter().toString();
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_filetype()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getFiletypeFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_hoster()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getHosterURLFilter().toString();
            }
        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.FilterTableModel_initColumns_source()) {
            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                return !value.isEnabled() ? "" : value.getSourceURLFilter().toString();
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_condition()) {
            {
                rendererField.setHorizontalAlignment(JLabel.RIGHT);
            }

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isDefaultVisible() {
                return false;
            }

            @Override
            public void resetRenderer() {
                super.resetRenderer();
                rendererField.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 1));

            }

            // @Override
            // public boolean isHidable() {
            // return false;
            // }

            @Override
            public String getStringValue(PackagizerRule value) {
                return _GUI._.settings_linkgrabber_filter_columns_if(value.toString());
            }
        });

        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_dest()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("save", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.getDownloadDestination() != null;
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {
                if (value.getDownloadDestination() == null) return null;
                return _GUI._.FilterTableModel_packagizer_tt_dest(value.getDownloadDestination());

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
            }

        });

        this.addColumn(new ExtIconColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_priority()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("prio_3", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {

                return value.getPriority()._();

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected Icon getIcon(PackagizerRule value) {
                return value.getPriority().loadIcon(18);
            }

        });
        //
        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_packagename()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("archive", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.getPackageName() != null;
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {
                if (value.getPackageName() == null) return null;
                return _GUI._.FilterTableModel_packagizer_tt_packagename(value.getPackageName());

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
            }

        });

        addColumn(new ExtTextColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_chunks()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("chunks", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                if (value.getChunks() == -1) { return ""; }
                return value.getChunks() + "";
            }
        });

        //
        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_extract()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("extract", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isAutoExtractionEnabled();
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {
                if (!value.isAutoExtractionEnabled()) return null;
                return _GUI._.FilterTableModel_packagizer_tt_autoextract();

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
            }

        });

        //
        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_add()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("add", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isAutoAddEnabled();
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {
                if (!value.isAutoAddEnabled()) return null;
                return _GUI._.FilterTableModel_packagizer_tt_autoadd();

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
            }

        });

        //
        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI._.settings_linkgrabber_filter_columns_start()) {

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(NewTheme.I().getIcon("media-playback-start", 14));
                        setHorizontalAlignment(CENTER);
                        setText(null);
                        return this;
                    }

                };

                return ret;
            }

            @Override
            public int getMaxWidth() {
                return 30;
            }

            @Override
            public boolean isHidable() {
                return true;
            }

            @Override
            protected boolean getBooleanValue(PackagizerRule value) {
                return value.isAutoStartEnabled() && value.isAutoAddEnabled();
            }

            @Override
            protected String getTooltipText(PackagizerRule value) {
                if (!value.isAutoStartEnabled() || !value.isAutoAddEnabled()) return null;
                return _GUI._.FilterTableModel_packagizer_tt_autostart();

            }

            @Override
            public boolean isEditable(PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
            }

        });
    }

    public void onChangeEvent(ChangeEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                _fireTableStructureChanged(PackagizerController.getInstance().list(), true);
            }
        };

    }
}
