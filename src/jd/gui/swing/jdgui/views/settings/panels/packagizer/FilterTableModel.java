package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtComponentRowHighlighter;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.controlling.packagizer.PackagizerControllerListener;
import org.jdownloader.controlling.packagizer.PackagizerRule;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.updatev2.gui.LAFOptions;

public class FilterTableModel extends ExtTableModel<PackagizerRule> implements PackagizerControllerListener {
    private static final long serialVersionUID = -7756459932564776739L;

    public FilterTableModel(String id) {
        super(id);
        PackagizerController.getInstance().getEventSender().addListener(this, false);
        addExtComponentRowHighlighter(new ExtComponentRowHighlighter<PackagizerRule>(LAFOptions.getInstance().getColorForTableAccountErrorRowForeground(), LAFOptions.getInstance().getColorForTableAccountErrorRowBackground(), null) {
            public int getPriority() {
                return Integer.MAX_VALUE;
            }

            @Override
            public boolean accept(ExtColumn<PackagizerRule> column, PackagizerRule value, boolean selected, boolean focus, int row) {
                return value._isBroken();
            }
        });
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<PackagizerRule>(_GUI.T.settings_linkgrabber_filter_columns_enabled()) {
            private static final long serialVersionUID = -4667150369226691276L;

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {
                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                    private static final long serialVersionUID = 3938290423337000265L;
                    private final Icon        ok               = NewTheme.I().getIcon(IconKey.ICON_OK, 14);

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        setIcon(ok);
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
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            @Override
            protected void setBooleanValue(boolean value, PackagizerRule object) {
                object.setEnabled(value);
                PackagizerController.getInstance().update();
            }
        });
        addColumn(new OrderColumn());
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI.T.settings_linkgrabber_filter_columns_name()) {
            /**
             *
             */
            private static final long serialVersionUID = 2457046463046132551L;

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            protected Icon getIcon(final PackagizerRule value) {
                final String key = value.getIconKey();
                if (key == null) {
                    return null;
                } else {
                    return NewTheme.I().getIcon(key, 18);
                }
            }

            @Override
            public String getStringValue(PackagizerRule value) {
                if (!value.isValid()) {
                    return _GUI.T.FilterTableModel_getStringValue_name_invalid(value.getName());
                }
                if (value.isStaticRule()) {
                    return _GUI.T.FilterTableModel_initColumns_static_(value.getName());
                }
                return value.getName();
            }
        });
        addColumn(new ExtTextColumn<PackagizerRule>(_GUI.T.settings_linkgrabber_filter_columns_cond()) {
            /**
             *
             */
            private static final long serialVersionUID = -5750253374104171542L;

            @Override
            public boolean isEnabled(PackagizerRule obj) {
                if (!obj.isValid()) {
                    return true;
                }
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final PackagizerRule obj) {
                return false;
            }

            protected Icon getIcon(final PackagizerRule value) {
                if (!value.isValid()) {
                    return new AbstractIcon(IconKey.ICON_ERROR, 18);
                }
                return null;
            }

            // @Override
            // protected String getTooltipText(PackagizerRule obj) {
            // return super.getTooltipText(obj);
            // }
            @Override
            public String getStringValue(PackagizerRule value) {
                if (!value.isValid()) {
                    return _GUI.T.FilterTableModel_initColumns_invalid_condition_();
                }
                return value.toString();
            }
        });
    }

    @Override
    public boolean move(java.util.List<PackagizerRule> transferData, int dropRow) {
        try {
            java.util.List<PackagizerRule> list = PackagizerController.getInstance().list();
            final java.util.List<PackagizerRule> newdata = new ArrayList<PackagizerRule>();
            List<PackagizerRule> before = new ArrayList<PackagizerRule>(list.subList(0, dropRow));
            List<PackagizerRule> after = new ArrayList<PackagizerRule>(list.subList(dropRow, list.size()));
            before.removeAll(transferData);
            after.removeAll(transferData);
            newdata.addAll(before);
            newdata.addAll(transferData);
            newdata.addAll(after);
            PackagizerController.getInstance().setList(newdata);
            this._fireTableStructureChanged(newdata, true);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    @Override
    public void onPackagizerUpdate() {
        _fireTableStructureChanged(PackagizerController.getInstance().list(), true);
    }

    @Override
    public void onPackagizerRunBeforeLinkcheck(CrawledLink link, final STATE state) {
    }

    @Override
    public void onPackagizerRunAfterLinkcheck(CrawledLink link, final STATE state) {
    }
}
