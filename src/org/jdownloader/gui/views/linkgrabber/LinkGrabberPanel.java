package org.jdownloader.gui.views.linkgrabber;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorHighlightListener;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.interfaces.View;
import jd.http.Browser;
import net.miginfocom.swing.MigLayout;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.helpdialogs.HelpDialog;
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
import org.jdownloader.gui.views.linkgrabber.properties.LinkgrabberProperties;
import org.jdownloader.gui.views.linkgrabber.properties.LinkgrabberPropertiesHeader;
import org.jdownloader.gui.views.linkgrabber.properties.PropertiesScrollPane;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DockingPosition;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.NewLinksInLinkgrabberAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.SimpleHttpInterface;
import org.jdownloader.updatev2.SimpleHttpResponse;
import org.jdownloader.updatev2.SponsoringPanelInterface;
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

    private PropertiesScrollPane                              propertiesPanel;
    private MigPanel                                          sidebarContainer;

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

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[]2[fill]", "[grow, fill]2[]"));

        tableModel = LinkGrabberTableModel.getInstance();
        table = new LinkGrabberTable(this, tableModel);

        final DelayedRunnable propertiesDelayer = new DelayedRunnable(100l, 1000l) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        final AbstractNode node;
                        if (table.getModel().hasSelectedObjects()) {
                            node = table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex());
                        } else {
                            node = null;
                        }
                        if (node != null) {
                            setPropertiesPanelVisible(true);
                            propertiesPanel.update(node);
                        } else {
                            setPropertiesPanelVisible(false);
                        }
                    }
                };
            }

            @Override
            public String getID() {
                return "updateDelayer";
            }

        };
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e == null || e.getValueIsAdjusting() || table.getModel().isTableSelectionClearing() || !CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled()) {
                    return;
                }
                propertiesDelayer.run();
            }
        });
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
                            if (CFG_GUI.CFG.isSwitchToLinkgrabberTabOnNewLinksAddedEnabled()) {
                                JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
                            }

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
                return org.jdownloader.settings.staticreferences.CFG_GUI.CFG.isSwitchToLinkgrabberTabOnNewLinksAddedEnabled() || CFG_GUI.CFG.getNewLinksAction() != NewLinksInLinkgrabberAction.NOTHING;
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

            @Override
            public void onLinkCrawlerNewJob(LinkCollectingJob job) {
            }

        });

        rightBar = new CustomizeableActionBar(MenuManagerLinkgrabberTabBottombar.getInstance()) {
            protected SelectionInfo<?, ?> getCurrentSelection() {
                return table.getSelectionInfo(true, true);

            }

            protected MenuContainerRoot prepare(MenuContainerRoot menuData) {
                MenuContainerRoot ret = new MenuContainerRoot();
                boolean foundSeparator = false;
                for (MenuItemData mi : menuData.getItems()) {
                    if (mi instanceof LeftRightDividerItem) {
                        foundSeparator = true;
                        continue;
                    }
                    if (foundSeparator) {
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
                return table.getSelectionInfo(true, true);

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
        propertiesPanel = createPropertiesPanel();
        // leftBar.add(Box.createGlue());
        layoutComponents();

        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_BOTTOMBAR_POSITION.getEventSender().addListener(new GenericConfigEventListener<Enum>() {

            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                fireRelayout();
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }
        });
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
            ret.setColumnHeaderView(new LinkgrabberOverViewHeader(loverView) {

                @Override
                protected void onCloseAction() {
                    CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.setValue(false);
                    loverView.removeListeners();

                    CustomizeableActionBar iconComp = rightBar;
                    Point loc = rightBar.getLocationOnScreen();

                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "overviewclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsPanel_onCloseAction(), _GUI._.DownloadsPanel_onCloseAction_help(), NewTheme.I().getIcon("bottombar", 32));
                    }

                }
            });
            overViewScrollBar.compareAndSet(null, ret);
        }
        return ret;
    }

    private void layoutComponents() {
        propertiesPanel.save();
        removeAll();
        boolean propertiesPanelVisible = CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled();

        switch (CFG_GUI.CFG.getLinkgrabberBottombarPosition()) {
        case NORTH:

            if (CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.isEnabled()) {
                if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                    if (propertiesPanelVisible) {

                        // all panels visible
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]2[]0"));
                    }

                } else {
                    if (propertiesPanelVisible) {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0"));
                    }
                }

            } else {

                if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                    if (propertiesPanelVisible) {

                        // all panels visible
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]2[]0"));
                    }

                } else {
                    if (propertiesPanelVisible) {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[]2[grow, fill]0"));
                    }
                }

            }

            break;
        case SOUTH:

            if (CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.isEnabled()) {
                if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                    if (propertiesPanelVisible) {

                        // all panels visible
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]0[]2[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]2[]2[]0"));
                    }

                } else {
                    if (propertiesPanelVisible) {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]0[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]2[]0"));
                    }
                }

            } else {

                if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                    if (propertiesPanelVisible) {

                        // all panels visible
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]0[]2[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]2[]2[]0"));
                    }

                } else {
                    if (propertiesPanelVisible) {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]0[]2[]0"));
                    } else {
                        setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]0", "[grow, fill]2[]0"));
                    }
                }

            }

            break;
        }
        if (CFG_GUI.LINKGRABBER_BOTTOMBAR_POSITION.getValue() == DockingPosition.NORTH) {
            add(leftBar, "height 24!");

            add(rightBar, "height 24!");
        }
        String constrains = "spanx";

        if (CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getValue()) {
            constrains = "";
            this.add(tableScrollPane, "");
            if (sidebarContainer == null) {
                createSidebar();
            }
            int height = 1;
            if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
                height++;
            }
            if (propertiesPanelVisible) {
                height++;
            }

            add(sidebarContainer, "spany " + height);
        } else {
            this.add(tableScrollPane, "spanx");
        }
        if (propertiesPanelVisible) {

            add(this.propertiesPanel, "hidemode 2," + constrains);
            propertiesPanel.update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
        }
        if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
            add(getOverView(), constrains);
        }

        //
        // if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
        // this.add(tableScrollPane, "");
        // add(sidebarScrollPane, "spany 2,growx");
        // add(getOverView());
        //
        // } else {
        // this.add(tableScrollPane, "spany 2");
        // add(sidebarScrollPane, "spany 2,growx");
        // }
        //
        // } else {
        //
        // if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
        //
        // this.add(tableScrollPane, "spanx");
        // add(getOverView(), "spanx");
        // } else {
        // this.add(tableScrollPane, "spany 2,spanx,growx");
        // }
        //
        // }

        if (CFG_GUI.LINKGRABBER_BOTTOMBAR_POSITION.getValue() != DockingPosition.NORTH) {
            add(leftBar, "height 24!");

            add(rightBar, "height 24!");
        }
    }

    private PropertiesScrollPane createPropertiesPanel() {
        final LinkgrabberProperties loverView = new LinkgrabberProperties(table);
        PropertiesScrollPane propertiesScrollPane = new PropertiesScrollPane(loverView, table);

        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);
        propertiesScrollPane.setColumnHeaderView(new LinkgrabberPropertiesHeader(loverView) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = rightBar;
                Point loc = iconComp.getLocationOnScreen();
                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "propertiesclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.DownloadsPanel_onCloseAction(), _GUI._.Linkgrabber_properties_onCloseAction_help(), NewTheme.I().getIcon("bottombar", 32));
                }

            }
        });

        propertiesScrollPane.setVisible(false);

        propertiesScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), propertiesScrollPane.getBorder()));
        return propertiesScrollPane;
    }

    private void createSidebar() {
        sidebar = new LinkGrabberSidebar(table);
        sidebarContainer = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[grow,fill]");
        SwingUtils.setOpaque(sidebarContainer, false);
        sidebarScrollPane = new HeaderScrollPane(sidebar) {
            {
                getVerticalScrollBar().setUnitIncrement(24);
            }

            public Dimension getPreferredSize() {
                Dimension ret = super.getPreferredSize();
                Insets borderInsets = getBorder().getBorderInsets(sidebarScrollPane);
                int scrollbarWidth = getVerticalScrollBar().getPreferredSize().width;
                if (sidebar.getSponsoringPanel() == null || !(MainTabbedPane.getInstance().getSelectedView() instanceof LinkGrabberView)) {
                    int widthWithout = sidebar.getPreferredSize().width + scrollbarWidth + borderInsets.left + borderInsets.right;

                    ret.width = widthWithout;

                } else {

                    autoHideSponsor();
                    boolean isVisible = sidebar.getSponsoringPanel().isVisible();
                    sidebar.getSponsoringPanel().setVisible(true);
                    int widthWith = sidebar.getPreferredSize().width + borderInsets.left + borderInsets.right;

                    sidebar.getSponsoringPanel().setVisible(false);
                    int widthWithout = sidebar.getPreferredSize().width + scrollbarWidth + borderInsets.left + borderInsets.right;
                    sidebar.getSponsoringPanel().setVisible(isVisible);
                    ret.width = Math.max(widthWith, widthWithout);

                }

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

        sidebarScrollPane.getViewport().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                if (sidebar.getSponsoringPanel() != null) {
                    autoHideSponsor();
                }

                // if (sidebarScrollPane.getVerticalScrollBar().isVisible()) {
                // sidebar.hideNonScrollComponents();
                // } else {
                // sidebar.showNonScrollComponents();
                // }
            }
        });
        // ScrollPaneUI udi = sp.getUI();

        LAFOptions.getInstance().applyPanelBackground(sidebarScrollPane);
        sidebarScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setColumnHeaderView(new LinkGrabberSideBarHeader(table));

        if (false && System.getProperty("nativeswing") != null) {
            try {
                SponsoringPanelInterface panel = (SponsoringPanelInterface) Class.forName("org.jdownloader.sponsor.bt.DJBTSponsoringPanel").newInstance();
                panel.setHttpClient(new SimpleHttpInterface() {

                    @Override
                    public SimpleHttpResponse get(final String url) throws IOException {
                        final String str = new Browser().getPage(url);
                        return new SimpleHttpResponse() {

                            @Override
                            public String getHtmlText() {

                                return str;

                            }
                        };
                    }
                });

                final JComponent sponsoringPanel = panel.getPanel();
                GUIEventSender.getInstance().addListener(new GUIListener() {

                    @Override
                    public void onKeyModifier(int parameter) {
                    }

                    @Override
                    public void onGuiMainTabSwitch(View oldView, View newView) {
                        if (newView instanceof LinkGrabberView) {
                            sponsoringPanel.setVisible(true);
                        } else {
                            sponsoringPanel.setVisible(false);
                        }
                    }
                });
                panel.init();
                JScrollPane sp = new JScrollPane(sponsoringPanel);
                LAFOptions.getInstance().applyPanelBackground(sp);

                sidebarContainer.setLayout("ins 0,wrap 1", "[grow,fill]", "[grow,fill]2[]");
                sidebarContainer.add(sidebarScrollPane);
                Insets borderInsets = sp.getBorder().getBorderInsets(sidebarScrollPane);
                sidebarContainer.add(sp, "width " + (300 + borderInsets.left + borderInsets.right) + "!,height " + (250 + borderInsets.top + borderInsets.bottom) + "!");

                return;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        sidebarContainer.add(sidebarScrollPane);
    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        LinkCollector.getInstance().getEventsender().addListener(this);
        table.requestFocusInWindow();
        if (propertiesPanel != null) {
            propertiesPanel.refreshAfterTabSwitch();
        }
    }

    @Override
    protected void onHide() {
        LinkCollector.getInstance().getEventsender().removeListener(this);
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        if (keyHandler != null && keyHandler == CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE && newValue == Boolean.TRUE) {
            new EDTRunner() {

                @Override
                protected void runInEDT() {
                    if (table.getSelectionModel().isSelectionEmpty()) {
                        if (table.getRowCount() == 0) {

                            if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                                HelpDialog.show(false, false, null, "propertiespanelvisible", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.LinkGrabberPanel_setPropertiesPanelVisible(), _GUI._.LinkGrabberPanel_setPropertiesPanelVisible_help(), NewTheme.I().getIcon("bottombar", 32));
                            }

                        } else {
                            table.getSelectionModel().setSelectionInterval(0, 0);
                        }
                    }
                }
            }.waitForEDT();

        }

        if (!newValue && keyHandler == org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE) {
            JDGui.help(_GUI._.LinkGrabberPanel_onConfigValueModified_title_(), _GUI._.LinkGrabberPanel_onConfigValueModified_msg_(), NewTheme.I().getIcon("warning_green", 32));
        }
        fireRelayout();
    }

    public void fireRelayout() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

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

    public void autoHideSponsor() {
        if (!(MainTabbedPane.getInstance().getSelectedView() instanceof LinkGrabberView)) {
            if (sidebar.getSponsoringPanel().isVisible()) {
                sidebar.getSponsoringPanel().setVisible(false);
            }
            return;
        }
        double viewPortHeight = sidebarScrollPane.getViewportBorderBounds().getHeight();

        int heightWithSponsor = sidebar.getPreferredSize().height;
        boolean v = sidebar.getSponsoringPanel().isVisible();
        if (!v) {
            heightWithSponsor += sidebar.getSponsoringPanel().getPreferredSize().getHeight() + 4;
        }
        if (heightWithSponsor > viewPortHeight) {
            if (v) {
                sidebar.getSponsoringPanel().setVisible(false);
            }
        } else {
            if (!v) {
                sidebar.getSponsoringPanel().setVisible(true);
            }
        }
    }

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

}
