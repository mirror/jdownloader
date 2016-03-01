package org.jdownloader.gui.views.downloads;

import java.awt.Point;

import jd.controlling.packagecontroller.AbstractNode;
import net.miginfocom.swing.MigLayout;

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
import org.jdownloader.gui.views.downloads.properties.DownloadsPropertiesScrollPane;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

public class DownloadsPabelWidgetContainer extends WidgetContainer implements GenericConfigEventListener<Boolean> {
    /**
     *
     */
    private static final long             serialVersionUID       = 1L;
    private final DownloadsTable          table;
    private DownloadsPropertiesScrollPane propertiesPanelScrollPane;
    private boolean                       propertiesPanelVisible = false;

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
            if (showProperties) {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]" + LAFOptions.getInstance().getExtension().customizeLayoutGetDefaultGap() + "[]"));
                add(wrap(createPropertiesPanel()), "");
                add(wrap(getOverView()), "");
                createPropertiesPanel().update(selectedObject);
            } else {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]"));
                add(wrap(getOverView()), "");
            }
        } else {
            if (showProperties) {
                setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[]"));
                add(wrap(createPropertiesPanel()), "");
                // do this after adding the panels, else a repaint might get lost
                createPropertiesPanel().update(selectedObject);
            } else {
                setVisible(false);
            }
        }
        revalidate();
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

    private OverviewHeaderScrollPane overviewScrollPane = null;

    private OverviewHeaderScrollPane getOverView() {
        if (overviewScrollPane != null) {
            return overviewScrollPane;
        }
        final DownloadOverview overView = new DownloadOverview(table);
        final OverviewHeaderScrollPane ret = new OverviewHeaderScrollPane(overView);
        LAFOptions.getInstance().applyPanelBackground(ret);
        ret.setColumnHeaderView(new DownloadOverViewHeader(overView) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onCloseAction() {
                if (overviewScrollPane == ret) {
                    overviewScrollPane = null;
                }
                CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.setValue(false);
                CustomizeableActionBar iconComp = bottomBar;
                final Point loc = bottomBar.getLocationOnScreen();
                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "overviewclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.DownloadsPanel_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }
            }
        });
        overviewScrollPane = ret;
        return ret;
    }

    private final CustomizeableActionBar bottomBar;

    public DownloadsPabelWidgetContainer(final DownloadsTable table, CustomizeableActionBar bottomBar) {
        super(table, CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE);
        this.table = table;
        this.bottomBar = bottomBar;
        CFG_GUI.DOWNLOAD_TAB_OVERVIEW_VISIBLE.getEventSender().addListener(this);
        CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.getEventSender().addListener(this);
    }

    protected DownloadsPropertiesScrollPane createPropertiesPanel() {
        if (propertiesPanelScrollPane != null) {
            return propertiesPanelScrollPane;
        }
        final DownloadPropertiesBasePanel propertiesContentPanel = new DownloadPropertiesBasePanel(table);
        final DownloadsPropertiesScrollPane propertiesScrollPane = new DownloadsPropertiesScrollPane(propertiesContentPanel, table);
        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);
        propertiesScrollPane.setColumnHeaderView(new DownloadPropertiesHeader(propertiesContentPanel) {

            @Override
            protected void onCloseAction() {
                CFG_GUI.DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE.setValue(false);
                final CustomizeableActionBar iconComp = bottomBar;
                final Point loc = iconComp.getLocationOnScreen();
                if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                    HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() - iconComp.getHeight() / 2, loc.y + iconComp.getHeight() / 2), "propertiesclosed", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.DownloadsPanel_onCloseAction(), _GUI.T.Linkgrabber_properties_onCloseAction_help(), new AbstractIcon(IconKey.ICON_BOTTOMBAR, 32));
                }
            }
        });
        this.propertiesPanelScrollPane = propertiesScrollPane;
        return propertiesScrollPane;
    }
}
