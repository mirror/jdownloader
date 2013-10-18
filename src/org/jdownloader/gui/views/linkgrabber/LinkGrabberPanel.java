package org.jdownloader.gui.views.linkgrabber;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorHighlightListener;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.WindowManager.FrameState;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.LeftRightDividerItem;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.gui.views.linkgrabber.overview.LinkgrabberOverViewHeader;
import org.jdownloader.gui.views.linkgrabber.overview.LinkgrabberOverview;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.NewLinksInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkGrabberPanel extends SwitchPanel implements LinkCollectorListener, GenericConfigEventListener<Boolean> {
    /**
     * 
     */
    private static final long     serialVersionUID = 1L;
    private LinkGrabberTableModel tableModel;
    private LinkGrabberTable      table;

    public LinkGrabberTable getTable() {
        return table;
    }

    private JScrollPane                                       tableScrollPane;
    private LinkGrabberSidebar                                sidebar;

    private HeaderScrollPane                                  sidebarScrollPane;

    private NullsafeAtomicReference<OverviewHeaderScrollPane> overViewScrollBar = new NullsafeAtomicReference<OverviewHeaderScrollPane>(null);
    private CustomizeableActionBar                            rightBar;
    private CustomizeableActionBar                            leftBar;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[]2[fill]", "[grow, fill]2[]"));

        tableModel = LinkGrabberTableModel.getInstance();
        table = new LinkGrabberTable(this, tableModel);
        tableScrollPane = new JScrollPane(table);
        HorizontalScrollbarAction.setup(CFG_GUI.HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED, table);

        // filteredAdd.setVisible(false);
        LinkCollector.getInstance().getEventsender().addListener(new LinkCollectorHighlightListener() {

            @Override
            public void onHighLight(CrawledLink parameter) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            System.out.println("Highlight");
                            if (CFG_GUI.CFG.isSwitchToLinkgrabberTabOnNewLinksAddedEnabled()) JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);

                            switch (CFG_GUI.CFG.getNewLinksAction()) {
                            case FOCUS:
                                JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                                break;
                            case NOTHING:
                                JDGui.getInstance().flashTaskbar();
                                return;
                            case TO_FRONT:
                                JDGui.getInstance().setFrameState(FrameState.TO_FRONT);
                                break;
                            }
                        } catch (Throwable e) {
                        }
                    }
                };

            }

            @Override
            public boolean isThisListenerEnabled() {
                return org.jdownloader.settings.staticreferences.CFG_GUI.CFG.isLinkgrabberAutoTabSwitchEnabled() || CFG_GUI.CFG.getNewLinksAction() != NewLinksInLinkgrabberAction.NOTHING;
            }

            @Override
            public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
            }

            @Override
            public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
            }

            @Override
            public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
            }

        });

        rightBar = new CustomizeableActionBar(MenuManagerLinkgrabberTabBottombar.getInstance()) {
            protected SelectionInfo<?, ?> getCurrentSelection() {
                return tableModel.createSelectionInfo();

            }

            protected MenuContainerRoot prepare(MenuContainerRoot menuData) {
                MenuContainerRoot ret = new MenuContainerRoot();
                boolean foundSeperator = false;
                for (MenuItemData mi : menuData.getItems()) {
                    if (mi instanceof LeftRightDividerItem) {
                        foundSeperator = true;
                        continue;
                    }
                    if (foundSeperator) {
                        ret.add(mi);
                    }
                }
                return ret;
            }

            @Override
            public void updateGui() {
                super.updateGui();
                table.updateContextShortcuts();
            }

        };

        leftBar = new CustomizeableActionBar(MenuManagerLinkgrabberTabBottombar.getInstance()) {
            protected SelectionInfo<?, ?> getCurrentSelection() {
                return tableModel.createSelectionInfo();

            }

            protected MenuContainerRoot prepare(MenuContainerRoot menuData) {
                MenuContainerRoot ret = new MenuContainerRoot();
                for (MenuItemData mi : menuData.getItems()) {
                    if (mi instanceof LeftRightDividerItem) {
                        break;
                    }

                    ret.add(mi);

                }
                return ret;
            }

            @Override
            public void updateGui() {
                super.updateGui();
                table.updateContextShortcuts();
            }

        };
        // leftBar.add(Box.createGlue());
        layoutComponents();

        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this);
        MenuManagerLinkgrabberTableContext.getInstance().setPanel(this);

    }

    private Component getOverView() {
        OverviewHeaderScrollPane ret = overViewScrollBar.get();
        if (ret != null) {
            return ret;
        } else {
            final LinkgrabberOverview loverView = new LinkgrabberOverview(table) {
                @Override
                public void removeListeners() {
                    super.removeListeners();
                    overViewScrollBar.set(null);
                }
            };
            ret = new OverviewHeaderScrollPane(loverView);
            final OverviewHeaderScrollPane finalRet = ret;
            LAFOptions.getInstance().applyPanelBackground(ret);
            ret.setColumnHeaderView(new LinkgrabberOverViewHeader() {

                @Override
                protected void onCloseAction() {
                    CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.setValue(false);
                    loverView.removeListeners();
                }
            });
            overViewScrollBar.compareAndSet(null, ret);
        }
        return ret;
    }

    private void layoutComponents() {
        removeAll();
        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]2[]2[]0"));
        if (org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getValue()) {

            if (sidebarScrollPane == null) {
                createSidebar();
            }

            if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                this.add(tableScrollPane, "");
                add(sidebarScrollPane, "spany 2,growx");
                add(getOverView());

            } else {
                this.add(tableScrollPane, "spany 2");
                add(sidebarScrollPane, "spany 2,growx");
            }

        } else {

            if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {

                this.add(tableScrollPane, "spanx");
                add(getOverView(), "spanx");
            } else {
                this.add(tableScrollPane, "spany 2,spanx,growx");
            }

        }

        add(leftBar, "height 24!");
        add(rightBar, "height 24!,growx");
    }

    private void createSidebar() {
        sidebar = new LinkGrabberSidebar(table);

        sidebarScrollPane = new HeaderScrollPane(sidebar) {
            {
                getVerticalScrollBar().setUnitIncrement(24);
            }

            public Dimension getPreferredSize() {
                Dimension ret = super.getPreferredSize();
                Insets borderInsets = getBorder().getBorderInsets(sidebarScrollPane);
                ret.width = sidebar.getPreferredSize().width + getVerticalScrollBar().getPreferredSize().width + borderInsets.left + borderInsets.right;

                return ret;
            }

            public Dimension getMinimumSize() {
                Dimension pref = getPreferredSize();
                pref.height = 0;
                return pref;
            }

            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            // protected int getHeaderHeight() {
            // return (int)
            // table.getTableHeader().getPreferredSize().getHeight();
            // }
        };

        // ScrollPaneUI udi = sp.getUI();

        LAFOptions.getInstance().applyPanelBackground(sidebarScrollPane);
        sidebarScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setColumnHeaderView(new LinkGrabberSideBarHeader(table));
        // ExtButton bt = new ExtButton(new AppAction() {
        // {
        // setSmallIcon(NewTheme.I().getIcon("close", -1));
        // setToolTipText(_GUI._.LinkGrabberSideBarHeader_LinkGrabberSideBarHeader_object_());
        // }
        //
        // public void actionPerformed(ActionEvent e) {
        // org.jdownloader.settings.statics.GUI.LINKGRABBER_SIDEBAR_ENABLED.setValue(false);
        // }
        // });
        //
        // sidebarScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER,
        // bt);

    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        LinkCollector.getInstance().getEventsender().addListener(this);
        table.requestFocusInWindow();
    }

    @Override
    protected void onHide() {
        LinkCollector.getInstance().getEventsender().removeListener(this);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (!newValue && keyHandler == org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE) {
            JDGui.help(_GUI._.LinkGrabberPanel_onConfigValueModified_title_(), _GUI._.LinkGrabberPanel_onConfigValueModified_msg_(), NewTheme.I().getIcon("warning_green", 32));
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                removeAll();
                layoutComponents();

                revalidate();
            }
        };
    }

    public void onLinkCollectorAbort(LinkCollectorEvent event) {
    }

    public void onLinkCollectorFilteredLinksAvailable(LinkCollectorEvent event) {
    }

    public void onLinkCollectorFilteredLinksEmpty(LinkCollectorEvent event) {
    }

    public void onLinkCollectorDataRefresh(LinkCollectorEvent event) {
        tableModel.refreshModel(!QueuePriority.HIGH.equals(event.getPrio()));
    }

    public void onLinkCollectorStructureRefresh(LinkCollectorEvent event) {
        tableModel.recreateModel(!QueuePriority.HIGH.equals(event.getPrio()));
    }

    public void onLinkCollectorLinksRemoved(LinkCollectorEvent event) {
        tableModel.recreateModel(!QueuePriority.HIGH.equals(event.getPrio()));
    }

    public void onLinkCollectorLinkAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    @Override
    public void onLinkCollectorDupeAdded(LinkCollectorEvent event, CrawledLink parameter) {
    }

    public void onLinkCollectorContentRemoved(LinkCollectorEvent event) {
        tableModel.recreateModel(!QueuePriority.HIGH.equals(event.getPrio()));
    }

    public void onLinkCollectorContentAdded(LinkCollectorEvent event) {
    }

    public void onLinkCollectorContentModified(LinkCollectorEvent event) {
    }

    @Override
    public void onLinkCrawlerAdded(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStarted(LinkCollectorCrawler parameter) {
    }

    @Override
    public void onLinkCrawlerStopped(LinkCollectorCrawler parameter) {
    }

}
