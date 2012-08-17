package jd.gui.swing.jdgui.views.settings.panels.addons;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;

import jd.gui.UserIO;
import jd.gui.swing.jdgui.BasicJDTable;
import jd.gui.swing.jdgui.menu.AddonsMenu;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.appwork.swing.exttable.ExtTableHeaderRenderer;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.SelectionHighlighter;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtLongColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ExtensionsTable extends BasicJDTable<LazyExtension> implements SettingsComponent {
    private static final long serialVersionUID = 1L;

    public ExtensionsTable() {
        super(new InternalTableModel());
        addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    private static class InternalTableModel extends ExtTableModel<LazyExtension> {

        private static final long        serialVersionUID = 5847076032639053531L;
        private java.util.List<LazyExtension> pluginsOptional;

        public InternalTableModel() {
            super("addonTable");
            pluginsOptional = new ArrayList<LazyExtension>(ExtensionController.getInstance().getExtensions());
            Collections.sort(pluginsOptional, new Comparator<LazyExtension>() {

                public int compare(LazyExtension o1, LazyExtension o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            setTableData(pluginsOptional);
        }

        @Override
        protected void initColumns() {

            this.addColumn(new ExtCheckColumn<LazyExtension>(_GUI._.extensiontablemodel_column_enabled()) {
                private static final long serialVersionUID = 1L;

                public ExtTableHeaderRenderer getHeaderRenderer(final JTableHeader jTableHeader) {

                    final ExtTableHeaderRenderer ret = new ExtTableHeaderRenderer(this, jTableHeader) {
                        private static final long serialVersionUID = 1L;

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
                protected boolean getBooleanValue(LazyExtension value) {
                    return value._isEnabled();
                }

                @Override
                protected void setBooleanValue(boolean value, LazyExtension object) {
                    if (value == object._isEnabled()) return;
                    if (value) {
                        try {
                            object._setEnabled(true);

                            if (object._getExtension().getGUI() != null) {
                                int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN, object.getName(), _JDT._.gui_settings_extensions_show_now(object.getName()));

                                if (UserIO.isOK(ret)) {
                                    // activate panel
                                    object._getExtension().getGUI().setActive(true);
                                    // bring panel to front
                                    object._getExtension().getGUI().toFront();

                                }
                            }
                        } catch (StartException e) {
                            Dialog.getInstance().showExceptionDialog(_JDT._.dialog_title_exception(), e.getMessage(), e);
                        } catch (StopException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {

                            object._setEnabled(false);
                        } catch (StartException e) {
                            e.printStackTrace();
                        } catch (StopException e) {
                            Dialog.getInstance().showExceptionDialog(_JDT._.dialog_title_exception(), e.getMessage(), e);
                        }
                    }
                    /*
                     * we save enabled/disabled status here, plugin must be
                     * running when enabled
                     */

                    AddonsMenu.getInstance().onUpdated();

                    // ConfigSidebar.getInstance(null).updateAddons();
                    // addons.updateShowcase();
                }
            });

            this.addColumn(new ExtTextColumn<LazyExtension>(_GUI._.gui_column_plugin(), this) {

                private static final long serialVersionUID = -3960914415647488335L;

                @Override
                protected Icon getIcon(LazyExtension value) {
                    return value._getIcon(16);
                }

                @Override
                public String getStringValue(LazyExtension value) {
                    return value.getName();
                }

            });
            this.addColumn(new ExtLongColumn<LazyExtension>(_GUI._.gui_column_version(), this) {

                private static final long serialVersionUID = -7390851512040553114L;

                @Override
                protected long getLong(LazyExtension value) {
                    return value.getVersion();
                }

            });

        }

    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,growy,pushy";
    }

    public boolean isMultiline() {
        return false;
    }

}
