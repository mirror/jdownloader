package org.jdownloader.gui.views.downloads;

import java.awt.Component;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLinkProperty;
import jd.plugins.FilePackage;
import jd.plugins.FilePackageProperty;
import jd.plugins.LinkStatusProperty;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.download.DownloadControllerListener;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.downloads.overviewpanel.DownloadOverViewHeader;
import org.jdownloader.gui.views.downloads.overviewpanel.DownloadOverview;
import org.jdownloader.gui.views.downloads.properties.DownloadPropertiesBasePanel;
import org.jdownloader.gui.views.downloads.properties.DownloadPropertiesHeader;
import org.jdownloader.gui.views.downloads.properties.PropertiesScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadsPanel extends SwitchPanel implements DownloadControllerListener, GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long                         serialVersionUID  = -2610465878903778445L;
    private DownloadsTable                            table;
    private JScrollPane                               tableScrollPane;
    private DownloadsTableModel                       tableModel;
    private ScheduledFuture<?>                        timer             = null;
    private CustomizeableActionBar                    bottomBar;
    private NullsafeAtomicReference<HeaderScrollPane> overViewScrollBar = new NullsafeAtomicReference<HeaderScrollPane>(null);
    private PropertiesScrollPane                      propertiesPanel;

    private PropertiesScrollPane createPropertiesPanel() {
        final DownloadPropertiesBasePanel loverView = new DownloadPropertiesBasePanel(table);
        PropertiesScrollPane propertiesScrollPane = new PropertiesScrollPane(loverView, table);

        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);
        propertiesScrollPane.setColumnHeaderView(new DownloadPropertiesHeader(loverView) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = bottomBar;
                Point loc = iconComp.getLocationOnScreen();
                if(CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled())HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "propertiesclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsPanel_onCloseAction(), _GUI._.Linkgrabber_properties_onCloseAction_help(), NewTheme.I().getIcon("bottombar", 32));

            }
        });
        propertiesScrollPane.setVisible(false);
        propertiesScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), propertiesScrollPane.getBorder()));
        return propertiesScrollPane;
    }

    public void setPropertiesPanelVisible(final boolean propertiesPanelVisible) {
        // if (propertiesPanelVisible == this.propertiesPanelVisible) return;

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                propertiesPanel.setVisible(propertiesPanelVisible);
                revalidate();

            }
        };

    }

    public DownloadsPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]2[]"));
        tableModel = DownloadsTableModel.getInstance();
        table = new DownloadsTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(null);
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
        propertiesPanel = createPropertiesPanel();

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() || e.getValueIsAdjusting()) return;
                if (table.getSelectedRowCount() > 0) {
                    setPropertiesPanelVisible(true);

                    propertiesPanel.update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));

                } else {
                    setPropertiesPanelVisible(false);

                }

            }
        });
        DownloadController.DOWNLOADLIST_LOADED.executeWhen(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        layoutComponents(true);
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
                    }
                };
            }
        });

        GenericConfigEventListener<Boolean> relayoutListener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, final Boolean newValue) {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        if (CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                            getOverView();
                        }

                        layoutComponents(DownloadController.DOWNLOADLIST_LOADED.isReached());
                        if (newValue && keyHandler == CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE) {
                            setPropertiesPanelVisible(true);

                            propertiesPanel.update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
                        }
                    }
                };
            }
        };
        CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(relayoutListener);
        CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.getEventSender().addListener(relayoutListener);
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_ENABLED.getEventSender().addListener(this);
        //
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_TOGGLE_BUTTON_ENABLED.getEventSender().addListener(this);
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_VISIBLE.getEventSender().addListener(this);
        // setBackground(LAFOptions.getInstance().getColorForPanelBackground());
        // super.setOpaque(true);
    }

    // public void setOpaque(boolean isOpaque) {
    // }

    private MigPanel createLoaderPanel() {
        final MigPanel loaderPanel = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[20%][][]");
        // loaderPanel.setPreferredSize(new Dimension(200, 200));

        loaderPanel.setOpaque(true);
        loaderPanel.setBackground(LAFOptions.getInstance().getColorForPanelBackground());

        final CircledProgressBar loader = new CircledProgressBar() {
            public int getAnimationFPS() {
                return 25;
            }
        };

        loader.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 1.0f));

        loader.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("robot", 256), 0.1f));
        ((ImagePainter) loader.getValueClipPainter()).setBackground(null);
        ((ImagePainter) loader.getValueClipPainter()).setForeground(null);
        loader.setIndeterminate(true);

        final JProgressBar ph = new JProgressBar();

        ph.setString(_GUI._.DownloadsTable_DownloadsTable_init_plugins());

        SecondLevelLaunch.HOST_PLUGINS_COMPLETE.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        ph.setString(_GUI._.DownloadsTable_DownloadsTable_object_wait_for_loading_links());

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
            if (CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled()) {

                // Dimension p = tableScrollPane.getPreferredSize();
                // add(Box.createHorizontalGlue());
                if (CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled()) {
                    setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]0[]2[]2[]"));

                    this.add(tableScrollPane, "");
                    add(propertiesPanel, "hidemode 2");
                    add(getOverView(), "");
                    add(bottomBar, "height 24!");
                } else {
                    setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]2[]2[]"));

                    this.add(tableScrollPane, "");
                    add(getOverView(), "");
                    add(bottomBar, "height 24!");
                }

            } else {
                if (CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled()) {

                    setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow, fill]0[]2[]"));

                    this.add(tableScrollPane, "");
                    add(propertiesPanel, "hidemode 2");
                    add(bottomBar, "height 24!");
                } else {
                    setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow, fill]2[]"));
                    this.add(tableScrollPane, "");
                    add(bottomBar, "height 24!");
                }
            }

        }

    }

    private Component getOverView() {
        HeaderScrollPane ret = overViewScrollBar.get();
        if (ret != null) {
            return ret;
        } else {
            final DownloadOverview overView = new DownloadOverview(table) {
                @Override
                public void removeListeners() {
                    super.removeListeners();
                    overViewScrollBar.set(null);
                }
            };
            ret = new OverviewHeaderScrollPane(overView);
            final HeaderScrollPane finalRet = ret;
            LAFOptions.getInstance().applyPanelBackground(ret);
            ret.setColumnHeaderView(new DownloadOverViewHeader() {

                @Override
                protected void onCloseAction() {
                    CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.setValue(false);
                    overView.removeListeners();

                    CustomizeableActionBar iconComp = bottomBar;
                    Point loc = bottomBar.getLocationOnScreen();

                    if(CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled())HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "overviewclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsPanel_onCloseAction(), _GUI._.DownloadsPanel_onCloseAction_help(), NewTheme.I().getIcon("bottombar", 32));

                }

            });
            overViewScrollBar.compareAndSet(null, ret);
        }
        return ret;
    }

    // private void createSidebar() {
    // sidebar = new DownloadViewSidebar(table);
    //
    // sidebarScrollPane = new HeaderScrollPane(sidebar) {
    //
    // /**
    // *
    // */
    // private static final long serialVersionUID = 1L;
    // // protected int getHeaderHeight() {
    // // return (int)
    // // table.getTableHeader().getPreferredSize().getHeight();
    // // }
    // };
    //
    // // ScrollPaneUI udi = sp.getUI();
    // int c =
    // LAFOptions.getInstance().getPanelBackgroundColor();
    // // LayoutManager lm = sp.getLayout();
    //
    // if (c >= 0) {
    // sidebarScrollPane.setBackground(new Color(c));
    // sidebarScrollPane.setOpaque(true);
    //
    // }
    // sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    // sidebarScrollPane.setColumnHeaderView(new DownloadViewSideBarHeader());
    // // ExtButton bt = new ExtButton(new AppAction() {
    // // {
    // // setSmallIcon(NewTheme.I().getIcon("close", -1));
    // //
    // setToolTipText(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader_object_());
    // // }
    // //
    // // public void actionPerformed(ActionEvent e) {
    // //
    // org.jdownloader.settings.statics.GUI.LINKGRABBER_SIDEBAR_ENABLED.setValue(false);
    // // }
    // // });
    // //
    // // sidebarScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,
    // // bt);
    // //
    // org.jdownloader.settings.statics.LINKFILTER.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getEventSender().addListener(new
    // // GenericConfigEventListener<Boolean>() {
    // //
    // // public void onConfigValidatorError(KeyHandler<Boolean> keyHandler,
    // // Boolean invalidValue, ValidationException validateException) {
    // // }
    // //
    // // public void onConfigValueModified(KeyHandler<Boolean> keyHandler,
    // // Boolean newValue) {
    // //
    // // if (Boolean.TRUE.equals(newValue)) {
    // // SwingUtilities.invokeLater(new Runnable() {
    // //
    // // public void run() {
    // //
    // sidebarScrollPane.getVerticalScrollBar().setValue(sidebarScrollPane.getVerticalScrollBar().getMaximum());
    // // }
    // // });
    // //
    // // }
    // // }
    // // });
    //
    // }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        synchronized (this) {
            if (timer != null) timer.cancel(false);
            timer = tableModel.getThreadPool().scheduleWithFixedDelay(new Runnable() {
                long lastContentChanges = -1;

                public void run() {
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

            }, 250, 1000, TimeUnit.MILLISECONDS);
        }
        DownloadController.getInstance().addListener(this);
        table.requestFocusInWindow();
        if (propertiesPanel != null) propertiesPanel.refreshAfterTabSwitch();
    }

    @Override
    protected void onHide() {
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
    public void onDownloadControllerUpdatedData(DownloadLink downloadlink, LinkStatusProperty property) {
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
}
