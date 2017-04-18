package org.jdownloader.gui.views.linkgrabber;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollectorCrawler;
import jd.controlling.linkcollector.LinkCollectorEvent;
import jd.controlling.linkcollector.LinkCollectorHighlightListener;
import jd.controlling.linkcollector.LinkCollectorListener;
import jd.controlling.linkcrawler.CrawledLink;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.MainTabbedPane;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.gui.views.linkgrabber.bottombar.LeftRightDividerItem;
import org.jdownloader.gui.views.linkgrabber.bottombar.MenuManagerLinkgrabberTabBottombar;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DockingPosition;
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

    private JScrollPane                tableScrollPane;
    private LinkGrabberSidebar         sidebar;

    private HeaderScrollPane           sidebarScrollPane;

    private CustomizeableActionBar     rightBar;
    private CustomizeableActionBar     leftBar;

    private JComponent                 sidebarContainer;
    private LinkgrabberWidgetContainer widgetContainer;

    public LinkGrabberPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[]2[fill]", "[grow, fill]2[]"));

        tableModel = LinkGrabberTableModel.getInstance();
        table = new LinkGrabberTable(this, tableModel);

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

        HorizontalScrollbarAction.setup(CFG_GUI.HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED, table);

        // filteredAdd.setVisible(false);
        LinkCollector.getInstance().getEventsender().addListener(new LinkCollectorHighlightListener() {

            @Override
            public void onHighLight(CrawledLink parameter) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        try {
                            switch (CFG_GUI.CFG.getNewLinksActionV2()) {
                            case SWITCH:
                                JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
                                break;
                            case FOCUS:
                                JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
                                JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                                break;
                            case NOTHING:
                                JDGui.getInstance().flashTaskbar();
                                break;
                            case TO_FRONT:
                                JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
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
                return CFG_GUI.CFG.getNewLinksActionV2() != NewLinksInLinkgrabberAction.NOTHING;
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

            @Override
            public void onLinkCrawlerFinished() {
            }

        });

        rightBar = new CustomizeableActionBar(MenuManagerLinkgrabberTabBottombar.getInstance()) {
            protected SelectionInfo<?, ?> getCurrentSelection() {
                return table.getSelectionInfo(true, true);

            }

            protected MenuContainerRoot prepare(MenuContainerRoot menuData) {
                MenuContainerRoot ret = new MenuContainerRoot();
                menuData._setOwner(MenuManagerLinkgrabberTabBottombar.getInstance());
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
                ret._setOwner(MenuManagerLinkgrabberTabBottombar.getInstance());
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

    private void layoutComponents() {
        if (widgetContainer != null) {
            widgetContainer.save();
        }
        removeAll();

        switch (CFG_GUI.CFG.getLinkgrabberBottombarPosition()) {
        case NORTH:

            setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[fill]0", "[]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[grow, fill]0[]"));

            break;
        case SOUTH:
            setLayout(new MigLayout("ins 0, wrap 2", "[grow,fill]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[fill]0", "[grow, fill]0[]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[]"));

            break;
        }
        if (CFG_GUI.LINKGRABBER_BOTTOMBAR_POSITION.getValue() == DockingPosition.NORTH) {
            add(leftBar, "height 24!");

            add(rightBar, "height 24!");
        }
        String constrains = "spanx";
        if (widgetContainer == null) {
            widgetContainer = new LinkgrabberWidgetContainer(table, leftBar, rightBar);
            widgetContainer.relayout();
        }
        if (CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.isEnabled()) {
            constrains = "";
            this.add(tableScrollPane, "");
            if (sidebarContainer == null) {
                createSidebar();
            }

            add(sidebarContainer, "spany " + 2);

            add(widgetContainer);
        } else {
            this.add(tableScrollPane, "spanx");
            add(widgetContainer, "spanx");
            widgetContainer.relayout();
        }

        // if (propertiesPanelVisible) {
        //
        // add(this.propertiesPanel, constrains);
        // propertiesPanel.update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
        // }
        // if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
        // add(getOverView(), constrains);
        // }

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
                if (!(MainTabbedPane.getInstance().getSelectedView() instanceof LinkGrabberView)) {
                    int widthWithout = sidebar.getPreferredSize().width + scrollbarWidth + borderInsets.left + borderInsets.right;

                    ret.width = widthWithout;

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

        // ScrollPaneUI udi = sp.getUI();

        LAFOptions.getInstance().applyPanelBackground(sidebarScrollPane);
        sidebarScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScrollPane.setColumnHeaderView(new LinkGrabberSideBarHeader(table));

        sidebarContainer = LAFOptions.getInstance().getExtension().customizeLayoutWrapTitledPanels(sidebarScrollPane);
    }

    @Override
    protected void onShow() {
        tableModel.recreateModel(false);
        LinkCollector.getInstance().getEventsender().addListener(this);
        table.requestFocusInWindow();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (widgetContainer != null) {
                    widgetContainer.refreshAfterTabSwitch();
                }
            }
        };

    }

    @Override
    protected void onHide() {
        LinkCollector.getInstance().getEventsender().removeListener(this);

        Point point = this.tableScrollPane.getViewport().getViewRect().getLocation();
        CFG_GUI.CFG.setLinkgrabberListScrollPosition(new int[] { table.getRowIndexByPoint(point), point.x });
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
                                HelpDialog.show(false, false, null, "propertiespanelvisible", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.LinkGrabberPanel_setPropertiesPanelVisible(), _GUI.T.LinkGrabberPanel_setPropertiesPanelVisible_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                            }

                        } else {
                            table.getSelectionModel().setSelectionInterval(0, 0);
                        }
                    }
                }
            }.waitForEDT();

        }

        if (!newValue && keyHandler == org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE) {
            JDGui.help(_GUI.T.LinkGrabberPanel_onConfigValueModified_title_(), _GUI.T.LinkGrabberPanel_onConfigValueModified_msg_(), new AbstractIcon(IconKey.ICON_WARNING_GREEN, 32));
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

    @Override
    public void onLinkCrawlerNewJob(LinkCollectingJob job) {
    }

    @Override
    public void onLinkCrawlerFinished() {
    }

}
