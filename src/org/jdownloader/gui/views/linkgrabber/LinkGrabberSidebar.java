package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.Box;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.quickfilter.FilterTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterHosterTable;

public class LinkGrabberSidebar extends MigPanel {

    private LinkGrabberTable       table;
    private MigPanel               quicksettings;
    private QuickFilterHosterTable hosterFilterTable;
    private FilterTable            filetypeFilterTable;
    private LinkFilterSettings     config;
    private Header                 hosterFilter;
    private Header                 filetypeFilter;
    private Header                 quickSettingsHeader;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[][][][][grow,fill][]");
        config = JsonConfig.create(LinkFilterSettings.class);
        this.table = table;
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }

        hosterFilterTable = new QuickFilterHosterTable(table);
        hosterFilterTable.setVisible(config.isLinkgrabberHosterQuickfilterEnabled());
        filetypeFilterTable = new FilterTable<CrawledPackage, CrawledLink>();
        filetypeFilterTable.setVisible(config.isLinkgrabberFiletypeQuickfilterEnabled());

        quicksettings = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]0[]0[]");

        quicksettings.add(new Checkbox(LinkFilterSettings.ADD_AT_TOP, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.AUTO_CONFIRM_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.AUTO_START_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt()));

        quicksettings.add(new Checkbox(LinkFilterSettings.LINK_FILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter_tt()));
        hosterFilter = new Header(LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter()) {

            @Override
            protected void setContentsVisible(boolean selected) {
                hosterFilterTable.setVisible(selected);
            }

        };

        filetypeFilter = new Header(LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter()) {

            @Override
            protected void setContentsVisible(boolean selected) {
                filetypeFilterTable.setVisible(selected);
            }

        };

        quickSettingsHeader = new Header(LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_settings()) {

            @Override
            protected void setContentsVisible(boolean selected) {
                quicksettings.setVisible(selected);
            }

        };

        add(hosterFilter, "gaptop 7");
        add(hosterFilterTable, "hidemode 2");
        add(filetypeFilter, "gaptop 7");
        add(filetypeFilterTable, "hidemode 2");
        add(Box.createGlue());
        add(quickSettingsHeader, "gaptop 7");
        add(quicksettings, "hidemode 2");

    }

    // protected void updateVisibility() {
    // new EDTRunner() {
    //
    // @Override
    // protected void runInEDT() {
    // quicksettings.setVisible(config.isLinkgrabberQuickSettingsVisible());
    // filetypeFilterTable.setVisible(config.isLinkgrabberFiletypeQuickfilterEnabled());
    // hosterFilterTable.setVisible(config.isLinkgrabberHosterQuickfilterEnabled());
    //
    // }
    // };
    // }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    public void paint(Graphics g) {
        super.paint(g);
    }

    protected void paintBorder(Graphics g) {
        super.paintBorder(g);
    }
}
