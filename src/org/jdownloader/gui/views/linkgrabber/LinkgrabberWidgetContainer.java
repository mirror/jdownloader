package org.jdownloader.gui.views.linkgrabber;

import java.awt.Container;
import java.awt.Point;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

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
import org.jdownloader.gui.views.downloads.WidgetContainer;
import org.jdownloader.gui.views.downloads.bottombar.CustomizeableActionBar;
import org.jdownloader.gui.views.linkgrabber.overview.LinkgrabberOverViewHeader;
import org.jdownloader.gui.views.linkgrabber.overview.LinkgrabberOverview;
import org.jdownloader.gui.views.linkgrabber.properties.LinkgrabberProperties;
import org.jdownloader.gui.views.linkgrabber.properties.LinkgrabberPropertiesHeader;
import org.jdownloader.gui.views.linkgrabber.properties.PropertiesScrollPane;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkgrabberWidgetContainer extends WidgetContainer implements GenericConfigEventListener<Boolean> {

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

    private LinkGrabberTable         table;
    private boolean                  propertiesPanelVisible;
    private LinkgrabberOverview      overview;
    private OverviewHeaderScrollPane overviewScrollPane;
    private CustomizeableActionBar   rightBar;
    private PropertiesScrollPane     propertiesPanelScrollPane;
    private CustomizeableActionBar   leftBar;

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

    public LinkgrabberWidgetContainer(final LinkGrabberTable table, CustomizeableActionBar leftBar, CustomizeableActionBar rightBar) {
        this.table = table;
        this.rightBar = rightBar;
        this.leftBar = leftBar;
        final DelayedRunnable propertiesDelayer = new DelayedRunnable(100l, 1000l) {

            @Override
            public void delayedrun() {
                System.out.println("-->" + System.currentTimeMillis());
                // new Exception().printStackTrace();
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
                if (e == null || e.getValueIsAdjusting() || table.getModel().isTableSelectionClearing() || !CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled()) {
                    return;
                }
                System.out.println(System.currentTimeMillis());
                propertiesDelayer.run();
            }
        });
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this);
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.getEventSender().addListener(this);
    }

    @Override
    public void relayout() {
        if (propertiesPanelScrollPane != null) {
            propertiesPanelScrollPane.save();
        }
        removeAll();
        setVisible(true);
        boolean showProperties = CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.isEnabled() && propertiesPanelVisible;
        if (CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.isEnabled()) {
            // Dimension p = tableScrollPane.getPreferredSize();
            // add(Box.createHorizontalGlue());
            if (showProperties) {
                setLayout(new MigLayout("ins " + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + " 0 0 0, wrap 1", "[grow,fill]", "[]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[]"));
                createPropertiesPanel().update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));
                add(wrap(createPropertiesPanel()), "");

                add(wrap(getOverView()), "");

                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddTable(tableScrollPane,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddProperties(propertiesPanel,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddOverview(getOverView(),this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);
                // LAFOptions.getInstance().getExtension().customizeDownloadsPanelLayoutAddBottomBar(bottomBar,this,CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.isEnabled(),showProperties);

            } else {
                setLayout(new MigLayout("ins " + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + " 0 0 0, wrap 1", "[grow,fill]", "[]"));

                add(wrap(getOverView()), "");
            }

        } else {
            if (showProperties) {
                setLayout(new MigLayout("ins 2 0 0 0, wrap 1", "[grow,fill]", "[]"));
                createPropertiesPanel().update(table.getModel().getObjectbyRow(table.getSelectionModel().getLeadSelectionIndex()));

                add(wrap(createPropertiesPanel()), "");

            } else {
                setVisible(false);

            }
        }
        Container p = getParent();
        if (p != null) {

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    revalidate();
                    getParent().revalidate();
                    doLayout();
                    getParent().doLayout();
                }
            });
        }
    }

    private PropertiesScrollPane createPropertiesPanel() {
        if (propertiesPanelScrollPane != null) {
            return propertiesPanelScrollPane;
        }
        final LinkgrabberProperties loverView = new LinkgrabberProperties(table);
        PropertiesScrollPane propertiesScrollPane = new PropertiesScrollPane(loverView, table) {
            @Override
            public void setVisible(boolean aFlag) {
                if (!aFlag) {
                    loverView.save();
                }
                super.setVisible(aFlag);
            }
        };

        propertiesScrollPane.setColumnHeaderView(new LinkgrabberPropertiesHeader(loverView) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = rightBar;
                Point loc = iconComp.getLocationOnScreen();
                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "propertiesclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.Linkgrabber_properties_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }

            }
        });
        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);
        this.propertiesPanelScrollPane = propertiesScrollPane;

        return propertiesScrollPane;
    }

    private OverviewHeaderScrollPane getOverView() {
        if (overviewScrollPane != null) {
            return overviewScrollPane;
        }

        overview = new LinkgrabberOverview(table);
        OverviewHeaderScrollPane ret = new OverviewHeaderScrollPane(overview);
        final OverviewHeaderScrollPane finalRet = ret;
        LAFOptions.getInstance().applyPanelBackground(ret);
        ret.setColumnHeaderView(new LinkgrabberOverViewHeader(overview) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.LINKGRABBER_TAB_OVERVIEW_VISIBLE.setValue(false);

                CustomizeableActionBar iconComp = rightBar;
                Point loc = rightBar.getLocationOnScreen();

                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "overviewclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.DownloadsPanel_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }

            }
        });
        overviewScrollPane = ret;

        return overviewScrollPane;
    }

    public void save() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (propertiesPanelScrollPane != null) {
                    propertiesPanelScrollPane.save();
                }
            }
        };
    }

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
}
