package jd.gui.swing.jdgui.views.settings.panels.urlordertable;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtTextAreaColumn;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.controlling.DefaultDownloadLinkViewImpl;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.UrlDisplayEntry;
import org.jdownloader.settings.UrlDisplayType;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;

public class UrlOrderTableModel extends ExtTableModel<UrlDisplayEntry> implements GenericConfigEventListener<Object> {

    public UrlOrderTableModel() {
        super("UrlOrderTableModel");

        CFG_GENERAL.URL_ORDER.getEventSender().addListener(this, true);

        update();
    }

    private void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // make sure that this class is loaded. it contains the logic to restore old settings.
                DefaultDownloadLinkViewImpl.DISPLAY_URL_TYPE.getClass();
                ArrayList<UrlDisplayEntry> lst = new ArrayList<UrlDisplayEntry>();
                UrlDisplayEntry[] newOrder = CFG_GENERAL.CFG.getUrlOrder();
                HashSet<UrlDisplayType> dupe = new HashSet<UrlDisplayType>();
                if (newOrder != null) {
                    for (UrlDisplayEntry e : newOrder) {
                        try {
                            if (dupe.add(UrlDisplayType.valueOf(e.getType()))) {
                                lst.add(e);
                            }
                        } catch (Throwable e1) {

                        }
                    }
                }
                for (UrlDisplayType t : UrlDisplayType.values()) {
                    if (dupe.add(t)) {
                        lst.add(new UrlDisplayEntry(t.name(), true));
                    }
                }
                _fireTableStructureChanged(lst, true);
            }
        };

    }

    @Override
    public boolean move(List<UrlDisplayEntry> transferData, int dropRow) {
        try {
            final java.util.List<UrlDisplayEntry> newdata = new ArrayList<UrlDisplayEntry>(this.getTableData().size());
            final List<UrlDisplayEntry> before = new ArrayList<UrlDisplayEntry>(this.getTableData().subList(0, dropRow));
            final List<UrlDisplayEntry> after = new ArrayList<UrlDisplayEntry>(this.getTableData().subList(dropRow, this.getTableData().size()));
            before.removeAll(transferData);
            after.removeAll(transferData);
            newdata.addAll(before);
            newdata.addAll(transferData);
            newdata.addAll(after);
            CFG_GENERAL.CFG.setUrlOrder(newdata.toArray(new UrlDisplayEntry[] {}));
            // HosterRuleController.getInstance().setList(newdata);

            return true;
        } catch (final Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    @Override
    protected void initColumns() {

        this.addColumn(new ExtCheckColumn<UrlDisplayEntry>(_GUI._.premiumaccounttablemodel_column_enabled()) {

            private final JComponent empty = new RendererMigPanel("ins 0", "[]", "[]");

            public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {

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
            public JComponent getRendererComponent(UrlDisplayEntry value, boolean isSelected, boolean hasFocus, int row, int column) {

                JComponent ret = super.getRendererComponent(value, isSelected, hasFocus, row, column);

                return ret;
            }

            @Override
            public boolean isSortable(final UrlDisplayEntry obj) {
                return false;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(UrlDisplayEntry value) {
                return value.isEnabled();
            }

            @Override
            public boolean isEditable(UrlDisplayEntry obj) {
                return true;
            }

            @Override
            protected void setBooleanValue(boolean value, final UrlDisplayEntry object) {
                object.setEnabled(!object.isEnabled());
                CFG_GENERAL.CFG.setUrlOrder(getTableData().toArray(new UrlDisplayEntry[] {}));

            }
        });

        addColumn(new ExtTextAreaColumn<UrlDisplayEntry>(_GUI._.UrlOrderTableModel_type()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isSortable(final UrlDisplayEntry obj) {
                return false;
            }

            @Override
            public boolean isEnabled(UrlDisplayEntry obj) {
                return obj.isEnabled();
            }

            @Override
            public int getMaxWidth() {
                return 300;
            }

            @Override
            public int getMinWidth() {
                return 64;
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 140;
            }

            @Override
            public String getStringValue(UrlDisplayEntry value) {
                return UrlDisplayType.valueOf(value.getType()).getTranslatedName();
            }
        });

        addColumn(new ExtTextAreaColumn<UrlDisplayEntry>(_GUI._.UrlOrderTableModel_type_desc()) {
            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public boolean isEnabled(UrlDisplayEntry obj) {
                return obj.isEnabled();
            }

            @Override
            public boolean isSortable(final UrlDisplayEntry obj) {
                return false;
            }

            @Override
            public String getStringValue(UrlDisplayEntry value) {
                return UrlDisplayType.valueOf(value.getType()).getTranslatedDescription();
            }
        });
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
        update();
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        update();
    }

}
