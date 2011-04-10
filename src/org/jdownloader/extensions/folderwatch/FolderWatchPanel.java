package org.jdownloader.extensions.folderwatch;


 import org.jdownloader.extensions.folderwatch.translate.*;
import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;

import org.jdownloader.extensions.folderwatch.data.History;
import org.jdownloader.extensions.folderwatch.data.HistoryEntry;

import jd.controlling.JSonWrapper;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.InfoPanel;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.nutils.JDFlags;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class FolderWatchPanel extends SwitchPanel {

    private static final long           serialVersionUID = -4451556977039313203L;

    private static final String         JDL_PREFIX       = "plugins.optional.folderwatch.panel.";

    private static FolderWatchTable     table;
    private static FolderWatchInfoPanel infoPanel;
    private static JSonWrapper          config;

    private static FolderWatchPanel     INSTANCE;

    private FolderWatchExtension           owner;

    private FolderWatchPanel() {
    }

    public static synchronized FolderWatchPanel getInstance() {
        if (INSTANCE == null) INSTANCE = new FolderWatchPanel();
        return INSTANCE;
    }

    public FolderWatchPanel(JSonWrapper jSonWrapper, FolderWatchExtension owner) {
        this.owner = owner;
        FolderWatchPanel.table = new FolderWatchTable();
        FolderWatchPanel.config = jSonWrapper;

        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("", "[]min[][grow,fill]min[grow, fill]"));
        this.add(new JScrollPane(table), "width max,wrap");
        this.add(new ViewToolbar("action.folderwatch.history.clear", "action.folderwatch.history.reimport"), "align center");
    }

    private void initActions() {
        new ThreadedAction("action.folderwatch.history.clear", "gui.images.clear") {
            private static final long serialVersionUID = 3349495273700955040L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, T._.action_folderwatch_clear_message()), UserIO.RETURN_OK)) {
                            History.clear();
                            config.setProperty(FolderWatchConstants.PROPERTY_HISTORY, null);
                            config.save();
                            refresh();
                        }

                        return null;
                    }
                }.start();
            }
        };

        new ThreadedAction("action.folderwatch.history.reimport", "gui.images.add") {
            private static final long serialVersionUID = 9034432457172125570L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {

                        if (table.getSelectedRowCount() > 0) {
                            int[] rows = table.getSelectedRows();

                            for (int row : rows) {
                                HistoryEntry container = (HistoryEntry) table.getValueAt(row, 2);
                                owner.importContainer(container.getAbsolutePath());
                            }
                        }
                        return null;
                    }
                }.start();
            }
        };
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        refresh();
    }

    public FolderWatchInfoPanel getInfoPanel() {
        if (infoPanel == null) {
            infoPanel = new FolderWatchInfoPanel("gui.images.addons.unrar");
        }

        return infoPanel;
    }

    public void refresh() {
        table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();

        getInfoPanel().update();
    }

    public class FolderWatchInfoPanel extends InfoPanel {

        private static final long serialVersionUID = -4944779193095436056L;

        public FolderWatchInfoPanel(String iconKey) {
            super(iconKey);

            addInfoEntry(T._.plugins_optional_folderwatch_panel_filestatus(), "", 0, 0);
        }

        public void update() {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    HistoryEntry container = (HistoryEntry) table.getValueAt(table.getSelectedRow(), 3);

                    String info = "";

                    if (container != null) {
                        container = History.updateEntry(container);

                        if (container.isExisting()) {
                            info = T._.plugins_optional_folderwatch_panel_filestatus_exists();
                        } else {
                            info = T._.plugins_optional_folderwatch_panel_filestatus_notexists();
                        }
                    }

                    updateInfo(T._.plugins_optional_folderwatch_panel_filestatus(), info);

                    return null;
                }
            }.start();
        }
    }

}