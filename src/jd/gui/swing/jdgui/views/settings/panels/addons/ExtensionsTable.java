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
import jd.gui.swing.jdgui.menu.WindowMenu;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.table.ExtTableHeaderRenderer;
import org.appwork.utils.swing.table.ExtTableModel;
import org.appwork.utils.swing.table.SelectionHighlighter;
import org.appwork.utils.swing.table.columns.ExtCheckColumn;
import org.appwork.utils.swing.table.columns.ExtLongColumn;
import org.appwork.utils.swing.table.columns.ExtTextColumn;
import org.jdownloader.extensions.AbstractExtensionWrapper;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class ExtensionsTable extends BasicJDTable<AbstractExtensionWrapper> implements SettingsComponent {
    private static final long serialVersionUID = 1L;

    public ExtensionsTable() {
        super(new InternalTableModel());
        addRowHighlighter(new SelectionHighlighter(null, new Color(200, 200, 200, 80)));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }

    private static class InternalTableModel extends ExtTableModel<AbstractExtensionWrapper> {

        private static final long                   serialVersionUID = 5847076032639053531L;
        private ArrayList<AbstractExtensionWrapper> pluginsOptional;

        public InternalTableModel() {
            super("addonTable");
            pluginsOptional = new ArrayList<AbstractExtensionWrapper>(ExtensionController.getInstance().getExtensions());
            Collections.sort(pluginsOptional, new Comparator<AbstractExtensionWrapper>() {

                public int compare(AbstractExtensionWrapper o1, AbstractExtensionWrapper o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            tableData = new ArrayList<AbstractExtensionWrapper>(pluginsOptional);
        }

        @Override
        protected void initColumns() {

            this.addColumn(new ExtCheckColumn<AbstractExtensionWrapper>(_GUI._.extensiontablemodel_column_enabled()) {
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
                protected int getMaxWidth() {
                    return 30;
                }

                @Override
                public boolean isHidable() {
                    return false;
                }

                @Override
                protected boolean getBooleanValue(AbstractExtensionWrapper value) {
                    return value._isEnabled();
                }

                @Override
                protected void setBooleanValue(boolean value, AbstractExtensionWrapper object) {
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

                    AddonsMenu.getInstance().update();
                    WindowMenu.getInstance().update();
                    // ConfigSidebar.getInstance(null).updateAddons();
                    // addons.updateShowcase();
                }
            });

            this.addColumn(new ExtTextColumn<AbstractExtensionWrapper>(_GUI._.gui_column_plugin(), this) {

                private static final long serialVersionUID = -3960914415647488335L;

                @Override
                protected Icon getIcon(AbstractExtensionWrapper value) {
                    return value._getIcon(16);
                }

                @Override
                public String getStringValue(AbstractExtensionWrapper value) {
                    return value.getName();
                }

            });
            this.addColumn(new ExtLongColumn<AbstractExtensionWrapper>(_GUI._.gui_column_version(), this) {

                private static final long serialVersionUID = -7390851512040553114L;

                @Override
                protected long getLong(AbstractExtensionWrapper value) {
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
