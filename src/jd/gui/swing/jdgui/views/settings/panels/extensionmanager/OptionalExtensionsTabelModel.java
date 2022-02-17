package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

import org.appwork.swing.MigPanel;
import org.appwork.swing.exttable.ExtColumn;
import org.appwork.swing.exttable.ExtDefaultRowSorter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.swing.exttable.columns.ExtCheckColumn;
import org.appwork.swing.exttable.columns.ExtComponentColumn;
import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.renderer.RendererMigPanel;
import org.jdownloader.extensions.OptionalExtension;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.InstallLog;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyRestartRequest;
import org.jdownloader.updatev2.UpdateController;
import org.jdownloader.updatev2.UpdaterListener;

public class OptionalExtensionsTabelModel extends ExtTableModel<OptionalExtension> {
    private static final long serialVersionUID = -5584463272737285033L;

    public OptionalExtensionsTabelModel() {
        super("optionalsExtensionTable");
    }

    protected boolean isSortStateSaverEnabled() {
        return false;
    }

    @Override
    public boolean move(List<OptionalExtension> transferData, int dropRow) {
        return false;
    }

    @Override
    protected ExtColumn<OptionalExtension> getDefaultSortColumn() {
        return null;
    }

    @Override
    protected void initColumns() {
        this.addColumn(new ExtCheckColumn<OptionalExtension>(_GUI.T.lit_enabled(), this) {
            /**
             *
             */
            private static final long serialVersionUID = 1550064357138224168L;

            @Override
            public int getMaxWidth() {
                return 32;
            }

            @Override
            protected boolean isDefaultResizable() {
                return false;
            }

            @Override
            protected boolean getBooleanValue(OptionalExtension value) {
                return value.isInstalled() && value.getLazyExtension()._isEnabled();
            }

            @Override
            public boolean isEnabled(OptionalExtension obj) {
                return obj.isInstalled();
            }

            @Override
            public boolean isEditable(OptionalExtension obj) {
                return obj.isInstalled();
            }

            @Override
            protected void setBooleanValue(final boolean value, final OptionalExtension object) {
                if (object.isInstalled()) {
                    try {
                        object.getLazyExtension()._setEnabled(value);
                    } catch (StartException e) {
                        Dialog.getInstance().showExceptionDialog(_JDT.T.dialog_title_exception(), e.getMessage(), e);
                    } catch (StopException e) {
                        Dialog.getInstance().showExceptionDialog(_JDT.T.dialog_title_exception(), e.getMessage(), e);
                    }
                }
            }
        });
        this.addColumn(new ExtTextColumn<OptionalExtension>(_GUI.T.lit_name(), this) {
            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            protected Icon getIcon(OptionalExtension value) {
                return value.getIcon();
            }

            @Override
            public boolean isAutoWidthEnabled() {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public String getStringValue(OptionalExtension object) {
                return object.getName();
            }
        });
        final ExtComponentColumn<OptionalExtension> installation;
        this.addColumn(installation = new ExtComponentColumn<OptionalExtension>(_GUI.T.lit_installation()) {
            private final AbstractIcon       addIcon     = new AbstractIcon(IconKey.ICON_ADD, 16);
            private final AbstractIcon       removeIcon  = new AbstractIcon(IconKey.ICON_REMOVE, 16);
            private final AbstractIcon       restartIcon = new AbstractIcon(IconKey.ICON_RESTART, 16);
            private final JButton            editorBtn;
            private final JButton            rendererBtn;
            private OptionalExtension        editing;
            protected final MigPanel         editor;
            protected final RendererMigPanel renderer;
            {
                editorBtn = new JButton("");
                editorBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (editing != null) {
                            if (editing.isRestartRequired()) {
                                RestartController.getInstance().asyncRestart(new SmartRlyRestartRequest(false));
                            } else {
                                toggleOptionalExtension(editing);
                            }
                        }
                    }
                });
                rendererBtn = new JButton("");
                this.editor = new MigPanel("ins 1", "[grow,fill]", "[18!]") {
                    @Override
                    public void requestFocus() {
                    }
                };
                editor.add(editorBtn);
                this.renderer = new RendererMigPanel("ins 1", "[grow,fill]", "[18!]");
                renderer.add(rendererBtn);
                setClickcount(1);
                setRowSorter(new ExtDefaultRowSorter<OptionalExtension>() {
                    public int compare(boolean x, boolean y) {
                        return (x == y) ? 0 : (x ? 1 : -1);
                    }

                    public int compare(OptionalExtension o1, OptionalExtension o2) {
                        if (ExtColumn.SORT_ASC.equals(this.getSortOrderIdentifier())) {
                            return compare(o1.isInstalled(), o2.isInstalled());
                        } else {
                            return compare(o2.isInstalled(), o1.isInstalled());
                        }
                    };
                });
            }

            @Override
            public boolean isResizable() {
                return true;
            }

            @Override
            public int getMaxWidth() {
                return 160;
            }

            @Override
            public boolean isSortable(OptionalExtension obj) {
                return true;
            }

            @Override
            protected boolean isDefaultResizable() {
                return true;
            }

            @Override
            public boolean isHidable() {
                return false;
            }

            @Override
            public int getDefaultWidth() {
                return 160;
            }

            @Override
            protected JComponent getInternalEditorComponent(OptionalExtension value, boolean isSelected, int row, int column) {
                return editor;
            }

            @Override
            protected JComponent getInternalRendererComponent(OptionalExtension value, boolean isSelected, boolean hasFocus, int row, int column) {
                return renderer;
            }

            private void updateButton(JButton button, OptionalExtension value) {
                if (value.isInstalled()) {
                    if (value.isRestartRequired()) {
                        button.setIcon(restartIcon);
                        button.setText(_GUI.T.InstalledExtension_waiting_for_restart());
                    } else {
                        button.setIcon(removeIcon);
                        button.setText(_GUI.T.InstalledExtension_getPanel_());
                    }
                } else {
                    if (value.isRestartRequired()) {
                        button.setIcon(restartIcon);
                        button.setText(_GUI.T.UninstalledExtension_waiting_for_restart());
                    } else {
                        button.setIcon(addIcon);
                        button.setText(_GUI.T.UninstalledExtension_getPanel_());
                    }
                }
                button.setEnabled(UpdateController.getInstance().isHandlerSet());
            }

            @Override
            public void configureRendererComponent(OptionalExtension value, boolean isSelected, boolean hasFocus, int row, int column) {
                updateButton(rendererBtn, value);
            }

            @Override
            public void configureEditorComponent(OptionalExtension value, boolean isSelected, int row, int column) {
                editing = value;
                updateButton(editorBtn, value);
            }

            @Override
            public void resetEditor() {
            }

            @Override
            public void resetRenderer() {
            }
        });
        this.addColumn(new ExtTextColumn<OptionalExtension>(_GUI.T.lit_desciption(), this) {
            private static final long serialVersionUID = -7209180150340921804L;

            @Override
            public String getStringValue(OptionalExtension object) {
                return object.getDescription();
            }
        });
        installation.setSortOrderIdentifier(ExtColumn.SORT_DESC);
        setSortColumn(installation);
    }

    private void toggleOptionalExtension(final OptionalExtension ext) {
        final ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {
            private volatile String labelString = null;
            private volatile int    progress    = 1;

            public int getProgress() {
                return progress;
            }

            public String getString() {
                return labelString;
            }

            public void run() throws Exception {
                try {
                    final UpdaterListener listener;
                    UpdateController.getInstance().getEventSender().addListener(listener = new UpdaterListener() {
                        @Override
                        public void onUpdatesAvailable(boolean selfupdate, InstallLog installlog) {
                        }

                        @Override
                        public void onUpdaterStatusUpdate(final String label, Icon icon, final double p) {
                            progress = (int) p;
                            labelString = label;
                        }
                    });
                    if (ext.isInstalled()) {
                        UpdateController.getInstance().runExtensionUnInstallation(ext.getExtensionID());
                    } else {
                        UpdateController.getInstance().runExtensionInstallation(ext.getExtensionID());
                    }
                    try {
                        while (true) {
                            Thread.sleep(500);
                            if (!UpdateController.getInstance().isRunning()) {
                                break;
                            } else {
                                UpdateController.getInstance().waitForUpdate();
                            }
                        }
                        if (UpdateController.getInstance().hasPendingUpdates()) {
                            ext.setRestartRequired(true);
                            progress = 100;
                            if (ext.isInstalled()) {
                                labelString = _GUI.T.InstalledExtension_waiting_for_restart();
                            } else {
                                labelString = _GUI.T.UninstalledExtension_waiting_for_restart();
                            }
                        }
                    } finally {
                        UpdateController.getInstance().getEventSender().removeListener(listener);
                    }
                } catch (Exception e) {
                    LogController.CL().log(e);
                }
            }

            @Override
            public String getLabelString() {
                return null;
            }
        }, 0, ext.getName(), ext.isInstalled() ? _GUI.T.InstalledExtension_getPanel_install_in_progress() : _GUI.T.UninstalledExtension_getPanel_install_in_progress(), null);
        try {
            Dialog.getInstance().showDialog(dialog);
        } catch (DialogNoAnswerException e1) {
            e1.printStackTrace();
        }
    }
}