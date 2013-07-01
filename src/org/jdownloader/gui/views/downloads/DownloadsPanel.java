package org.jdownloader.gui.views.downloads;

import java.awt.Component;
import java.awt.Dimension;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JScrollPane;

import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadControllerEvent;
import jd.controlling.downloadcontroller.DownloadControllerListener;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.laf.LookAndFeelController;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.gui.views.downloads.overviewpanel.DownloadOverview;
import org.jdownloader.gui.views.downloads.overviewpanel.OverViewHeader;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.gui.views.downloads.table.HorizontalScrollbarAction;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DownloadsPanel extends SwitchPanel implements DownloadControllerListener, GenericConfigEventListener<Boolean> {

    /**
     * 
     */
    private static final long   serialVersionUID = -2610465878903778445L;
    private DownloadsTable      table;
    private JScrollPane         tableScrollPane;
    private DownloadsTableModel tableModel;
    private ScheduledFuture<?>  timer            = null;
    private BottomBar           bottomBar;
    private DownloadOverview    overView;
    private HeaderScrollPane    overViewScrollBar;
    private MigPanel            loaderPanel;

    public DownloadsPanel() {
        super(new MigLayout("ins 0, wrap 2", "[grow,fill]2[fill]", "[grow, fill]2[]2[]"));
        tableModel = DownloadsTableModel.getInstance();
        table = new DownloadsTable(tableModel);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(null);
        HorizontalScrollbarAction.setup(CFG_GUI.HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED, table);
        bottomBar = new BottomBar(table);

        layoutComponents();

        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        removeAll();
                        layoutComponents();
                    }
                };
            }
        });

        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_ENABLED.getEventSender().addListener(this);
        //
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_TOGGLE_BUTTON_ENABLED.getEventSender().addListener(this);
        // org.jdownloader.settings.statics.GUI.DOWNLOAD_VIEW_SIDEBAR_VISIBLE.getEventSender().addListener(this);

    }

    public DownloadsTable getTable() {
        return table;
    }

    private void layoutComponents() {

        if (CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.isEnabled()) {
            setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow,fill]2[]2[]"));
            this.add(tableScrollPane, "");
            Dimension p = tableScrollPane.getPreferredSize();
            // add(Box.createHorizontalGlue());
            add(getOverView(), "");
            add(bottomBar, "height 24!");
        } else {
            setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[grow, fill]2[]"));
            this.add(tableScrollPane, "");
            add(bottomBar, "height 24!");
        }

    }

    private Component getOverView() {
        if (overView == null) {
            overView = new DownloadOverview(table);
            overViewScrollBar = new OverviewHeaderScrollPane(overView);

            LookAndFeelController.getInstance().getLAFOptions().applyPanelBackgroundColor(overViewScrollBar);

            // overViewScrollBar.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            // overViewScrollBar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            overViewScrollBar.setColumnHeaderView(new OverViewHeader() {

                @Override
                protected void onCloseAction() {
                    setOverViewVisible(false);

                }

            });
        }
        return overViewScrollBar;
    }

    protected void setOverViewVisible(final boolean b) {
        CFG_GUI.DOWNLOAD_PANEL_OVERVIEW_VISIBLE.setValue(b);

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
    // LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();
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

    public void onDownloadControllerEvent(DownloadControllerEvent event) {
        switch (event.getType()) {
        case REFRESH_STRUCTURE:
        case REMOVE_CONTENT:
            tableModel.recreateModel();
            break;
        case REFRESH_CONTENT:
            tableModel.refreshModel();
            break;
        }
    }

    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
    }
}
