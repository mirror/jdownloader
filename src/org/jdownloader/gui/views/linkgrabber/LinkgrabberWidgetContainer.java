package org.jdownloader.gui.views.linkgrabber;

import java.awt.Container;
import java.awt.Point;

import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.Application;
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
import org.jdownloader.gui.views.linkgrabber.properties.LinkgrabberPropertiesScrollPane;
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

    private boolean                         propertiesPanelVisible;

    private CustomizeableActionBar          rightBar;
    private LinkgrabberPropertiesScrollPane propertiesPanelScrollPane;
    private CustomizeableActionBar          leftBar;

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
    public LinkGrabberTable getTable() {
        return (LinkGrabberTable) super.getTable();
    }

    public LinkgrabberWidgetContainer(final LinkGrabberTable table, CustomizeableActionBar leftBar, CustomizeableActionBar rightBar) {
        super(table, CFG_GUI.LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE);

        this.rightBar = rightBar;
        this.leftBar = leftBar;

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
                add(wrap(createPropertiesPanel()), "");

                add(wrap(getOverView()), "");
                // update after adding the panels. else a repaint might get lost
                createPropertiesPanel().update(getTable().getModel().getObjectbyRow(getTable().getSelectionModel().getLeadSelectionIndex()));

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

                add(wrap(createPropertiesPanel()), "");
                createPropertiesPanel().update(getTable().getModel().getObjectbyRow(getTable().getSelectionModel().getLeadSelectionIndex()));

                // update after adding the panels. else a repaint might get lost
            } else {
                setVisible(false);

            }
        }
        final Container p = getParent();
        if (p != null) {

            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    revalidate();
                    if (Application.getJavaVersion() >= Application.JAVA17) {
                        p.revalidate();
                    } else {
                        p.invalidate();
                        p.validate();
                    }
                    doLayout();
                    p.doLayout();
                }
            });
        }
    }

    protected LinkgrabberPropertiesScrollPane createPropertiesPanel() {
        if (propertiesPanelScrollPane != null) {
            return propertiesPanelScrollPane;
        }
        final LinkgrabberProperties loverView = new LinkgrabberProperties(getTable());
        LinkgrabberPropertiesScrollPane propertiesScrollPane = new LinkgrabberPropertiesScrollPane(loverView, getTable()) {
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

    private OverviewHeaderScrollPane overviewScrollPane;

    private OverviewHeaderScrollPane getOverView() {
        if (overviewScrollPane != null) {
            return overviewScrollPane;
        }
        final LinkgrabberOverview overview = new LinkgrabberOverview(getTable());
        final OverviewHeaderScrollPane ret = new OverviewHeaderScrollPane(overview);
        LAFOptions.getInstance().applyPanelBackground(ret);
        ret.setColumnHeaderView(new LinkgrabberOverViewHeader(overview) {

            @Override
            protected void onCloseAction() {
                if (overviewScrollPane == ret) {
                    overviewScrollPane = null;
                }
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
