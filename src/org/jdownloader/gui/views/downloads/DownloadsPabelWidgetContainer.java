package org.jdownloader.gui.views.downloads;

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.downloads.overviewpanel.DownloadOverViewHeader;
import org.jdownloader.gui.views.downloads.overviewpanel.DownloadOverview;
import org.jdownloader.gui.views.downloads.properties.DownloadPropertiesBasePanel;
import org.jdownloader.gui.views.downloads.properties.DownloadPropertiesHeader;
import org.jdownloader.gui.views.downloads.properties.PropertiesScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

public class DownloadsPabelWidgetContainer extends WidgetContainer implements GenericConfigEventListener<Boolean> {
    /**
     *
     */
    private static final long        serialVersionUID       = 1L;
    private DownloadsTable           table;
    private PropertiesScrollPane     propertiesPanelScrollPane;
    private boolean                  propertiesPanelVisible = false;
    private OverviewHeaderScrollPane overviewScrollPane;
    private DownloadOverview         overView;

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(final KeyHandler<Boolean> keyHandler, final Boolean newValue) {

        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (newValue && keyHandler == CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE) {
                    setPropertiesPanelVisible(true);

                }
                relayout();

            }
        };
    }

    public void setPropertiesPanelVisible(final boolean propertiesPanelVisible) {
        this.propertiesPanelVisible = propertiesPanelVisible;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (propertiesPanelScrollPane != null) {
                    if (!propertiesPanelVisible) {

                        propertiesPanelScrollPane.save();

                    }
                }
            }
        }.waitForEDT();

    }

    @Override
    public void relayout() {
        if (propertiesPanelScrollPane != null) {
            propertiesPanelScrollPane.save();
        }
        removeAll();
        setVisible(true);
        AbstractNode selectedObject = table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex());

        boolean showProperties = CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() && propertiesPanelVisible && selectedObject != null;
        if (CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled()) {
            // Dimension p = tableScrollPane.getPreferredSize();
            // add(Box.createHorizontalGlue());
            if (showProperties) {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[]"));

                createPropertiesPanel().update(selectedObject);

                add(wrap(createPropertiesPanel()), "");

                add(wrap(getOverView()), "");

                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddTable(tableScrollPane,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddProperties(propertiesPanel,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddOverview(getOverView(),this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddBottomBar(bottomBar,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);

            } else {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]"));

                add(wrap(getOverView()), "");
            }

        } else {
            if (showProperties) {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]"));
                createPropertiesPanel().update(selectedObject);

                add(wrap(createPropertiesPanel()), "");

            } else {
                setVisible(false);

            }
        }
        revalidate();

        // Container p = getParent();
        // if (p != null) {
        //
        // SwingUtilities.invokeLater(new Runnable() {
        //
        // @Override
        // public void run() {
        // revalidate();
        // getParent().revalidate();
        // doLayout();
        // getParent().doLayout();
        // }
        // });
        // }

    }

    @Override
    public Dimension getPreferredSize() {
        Dimension ret = super.getPreferredSize();

        return ret;
    }

    public void refreshAfterTabSwitch() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (propertiesPanelScrollPane != null) {
                    propertiesPanelScrollPane.refreshAfterTabSwitch();
                }
            }
        };

    }

    private OverviewHeaderScrollPane getOverView() {
        if (overviewScrollPane != null) {
            return overviewScrollPane;
        }
        OverviewHeaderScrollPane ret;
        overView = new DownloadOverview(table);
        ret = new OverviewHeaderScrollPane(overView);

        LAFOptions.getInstance().applyPanelBackground(ret);
        final DownloadOverViewHeader header;
        ret.setColumnHeaderView(header = new DownloadOverViewHeader(overView) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onCloseAction() {
                CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = bottomBar;
                Point loc = bottomBar.getLocationOnScreen();

                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "overviewclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.DownloadsPanel_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }

            }

        });

        overviewScrollPane = ret;

        return ret;
    }

    private CustomizeableActionBar bottomBar;

    // private DownloadPropertiesBasePanel propertiesContentPanel;

    public DownloadsPabelWidgetContainer(final DownloadsTable table, CustomizeableActionBar bottomBar) {
        this.table = table;
        this.bottomBar = bottomBar;

        final DelayedRunnable propertiesDelayer = new DelayedRunnable(100l, 1000l) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (table.getSelectedRowCount() > 0) {
                            setPropertiesPanelVisible(true);
                        } else {
                            setPropertiesPanelVisible(false);

                        }
                        relayout();
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
                if (e == null || e.getValueIsAdjusting() || table.getModel().isTableSelectionClearing() || !CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled()) {
                    return;
                }
                propertiesDelayer.run();
            }
        });

        CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.getEventSender().addListener(this);

    }

    private PropertiesScrollPane createPropertiesPanel() {
        if (propertiesPanelScrollPane != null) {
            return propertiesPanelScrollPane;
        }

        DownloadPropertiesBasePanel propertiesContentPanel = new DownloadPropertiesBasePanel(table);
        PropertiesScrollPane propertiesScrollPane = new PropertiesScrollPane(propertiesContentPanel, table);

        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);
        propertiesScrollPane.setColumnHeaderView(new DownloadPropertiesHeader(propertiesContentPanel) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = bottomBar;
                Point loc = iconComp.getLocationOnScreen();
                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "propertiesclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.Linkgrabber_properties_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }

            }
        });
        // propertiesScrollPane.getViewport().setViewSize(propertiesContentPanel.getPreferredSize());
        this.propertiesPanelScrollPane = propertiesScrollPane;
        // propertiesPanelScrollPane.update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
        return propertiesPanelScrollPane;
    }
}
