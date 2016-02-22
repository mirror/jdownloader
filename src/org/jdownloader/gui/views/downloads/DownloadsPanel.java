package org.jdownloader.gui.views.downloads;

import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.Box;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.ExtractionListener;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import net.miginfocom.swing.MigLayout;

public class DownloadsPanel extends SwitchPanel implements DownloadControllerListener, GenericConfigEventListener<Boolean>, ExtractionListener {

    /**
     *
     */
    private static final long         serialVersionUID = -2610465878903778445L;
    private final DownloadsTable      table;
    private final JScrollPane         tableScrollPane;
    private final DownloadsTableModel tableModel;
    private ScheduledFuture<?>        timer            = null;
    private CustomizeableActionBar    bottomBar;

    private WidgetContainer           panelContainer;
    public static DownloadsPanel      INSTANCE;

    public DownloadsPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]2[]"));
        INSTANCE = this;
        tableModel = DownloadsTableModel.getInstance();
        table = new DownloadsTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.addComponentListener(new ComponentListener() {

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentResized(ComponentEvent e) {

                table.scrollToSelection(-1);
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
        LAFOptions.getInstance().getExtension().customizeLinksTable(table, tableScrollPane);

        HorizontalScrollbarAction.setup(CFG_GUI.HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED, table);
        bottomBar = new CustomizeableActionBar(MenuManagerDownloadTabBottomBar.getInstance()) {
            protected SelectionInfo<?, ?> getCurrentSelection() {

                return table.getSelectionInfo(true, true);

            }

            @Override
            public void updateGui() {
                super.updateGui();
                table.updateContextShortcuts();
            }

        };

        DownloadController.DOWNLOADLIST_LOADED.executeWhen(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        layoutComponents(true);
                        revalidate();

                    }
                };
            }
        }, new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        layoutComponents(false);
                        revalidate();
                    }
                };
            }
        });
    }

    private MigPanel createLoaderPanel() {
        final MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[20%][][]");
        loaderPanel.setOpaque(true);
        loaderPanel.setBackground(LAFOptions.getInstance().getColorForPanelBackground());

        final CircledProgressBar loader = new CircledProgressBar() {
            public int getAnimationFPS() {
                return 25;
            }
        };

        loader.setValueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_BOTTY_ROBOT, 256), 1.0f));

        loader.setNonvalueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_BOTTY_ROBOT, 256), 0.1f));
        ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
        ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
        loader.setIndeterminate(true);

        final JProgressBar ph = new JProgressBar();

        ph.setString(_GUI.T.DownloadsTable_DownloadsTable_init_plugins());

        SecondLevelLaunch.HOST_PLUGINS_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ph.setString(_GUI.T.DownloadsTable_DownloadsTable_object_wait_for_loading_links());

                    }

                };
            }
        });

        ph.setStringPainted(true);
        ph.setIndeterminate(true);
        loaderPanel.add(Box.createHorizontalGlue());
        loaderPanel.add(loader);
        loaderPanel.add(ph, "alignx center,  width 256!");
        return loaderPanel;
    }

    public DownloadsTable getTable() {
        return table;
    }

    private void layoutComponents(boolean downloadListLoaded) {
        if (!downloadListLoaded) {
            MigPanel loader = createLoaderPanel();
            setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]"));
            add(new JScrollPane(loader), "alignx center,aligny 20%");
        } else {
            setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]0[]0[]"));
            this.add(tableScrollPane);
            this.panelContainer = new DownloadsPabelWidgetContainer(table, bottomBar);
            panelContainer.relayout();
            this.add(panelContainer, "hidemode 3,gaptop " + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap());
            add(bottomBar, "height 24!,gaptop " + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap());
        }
    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        synchronized (this) {
            if (timer != null) {
                timer.cancel(false);
            }
            timer = tableModel.getThreadPool().scheduleWithFixedDelay(new Runnable() {
                long lastContentChanges = -1;

                public void run() {
                    if (!getTable().isEditing()) {
                        long contentChanges = DownloadController.getInstance().getContentChanges();
                        if (lastContentChanges != contentChanges && tableModel.isFilteredView()) {
                            /*
                             * in case we have content changes(eg downloads started) and an active filteredView, we need to recreate the
                             * tablemodel to reflect possible status changes in filtered view
                             */
                            tableModel.recreateModel();
                        } else {
                            /* just refresh the table */
                            tableModel.refreshModel();
                        }
                        lastContentChanges = contentChanges;
                    }
                }

            }, 250, CFG_GUI.CFG.getDownloadsTableRefreshInterval(), TimeUnit.MILLISECONDS);
        }
        DownloadController.getInstance().addListener(this);
        table.requestFocusInWindow();

        if (panelContainer != null) {
            panelContainer.refreshAfterTabSwitch();
        }
    }

    @Override
    protected void onHide() {
        Point point = this.tableScrollPane.getViewport().getViewRect().getLocation();
        CFG_GUI.CFG.setDownloadListScrollPosition(new int[] { table.getRowIndexByPoint(point), point.x });
        synchronized (this) {
            if (timer != null) {
                timer.cancel(false);
                timer = null;
            }
        }
        DownloadController.getInstance().removeListener(this);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
    }

    @Override
    public void onDownloadControllerAddedPackage(FilePackage pkg) {
    }

    @Override
    public void onDownloadControllerStructureRefresh(FilePackage pkg) {
        tableModel.recreateModel();
    }

    @Override
    public void onDownloadControllerStructureRefresh() {
        tableModel.recreateModel();
    }

    @Override
    public void onDownloadControllerStructureRefresh(AbstractNode node, Object param) {
        tableModel.recreateModel();
    }

    @Override
    public void onDownloadControllerRemovedPackage(FilePackage pkg) {
        tableModel.recreateModel();
    }

    @Override
    public void onDownloadControllerRemovedLinklist(List<DownloadLink> list) {
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, DownloadLinkProperty property) {
        tableModel.refreshModel();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg, FilePackageProperty property) {
        tableModel.refreshModel();
    }

    @Override
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink) {
        tableModel.refreshModel();
    }

    @Override
    public void onDownloadControllerUpdatedData(FilePackage pkg) {
        tableModel.refreshModel();
    }

    @Override
    public void onExtractionEvent(ExtractionEvent event) {
    }
}
