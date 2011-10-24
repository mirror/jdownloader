package org.jdownloader.gui.views.linkgrabber;

import java.awt.Color;

import javax.swing.Box;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.jdownloader.controlling.filter.LinkFilterSettings;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterHosterTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterTypeTable;

public class LinkGrabberSidebar extends MigPanel {

    /**
     * 
     */
    private static final long      serialVersionUID = 4006309139115917564L;
    private MigPanel               quicksettings;
    private QuickFilterHosterTable hosterFilterTable;
    private QuickFilterTypeTable   filetypeFilterTable;
    private Header                 hosterFilter;
    private Header                 filetypeFilter;
    private Header                 quickSettingsHeader;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[][][][][grow,fill][]");
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        // header
        hosterFilter = new Header(LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter());
        filetypeFilter = new Header(LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter());
        quickSettingsHeader = new Header(LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_settings());

        //
        hosterFilterTable = new QuickFilterHosterTable(hosterFilter, table);
        hosterFilterTable.setVisible(LinkFilterSettings.LG_QUICKFILTER_HOSTER_VISIBLE.getValue());
        filetypeFilterTable = new QuickFilterTypeTable(filetypeFilter, table);
        filetypeFilterTable.setVisible(LinkFilterSettings.LG_QUICKFILTER_TYPE_VISIBLE.getValue());

        quicksettings = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]0[]0[]");
        if (c >= 0) {
            quicksettings.setBackground(new Color(c));
            quicksettings.setOpaque(true);
        }
        quicksettings.add(new Checkbox(LinkFilterSettings.ADD_AT_TOP, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.AUTO_CONFIRM_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.AUTO_START_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt()));

        quicksettings.add(new Checkbox(LinkFilterSettings.LINK_FILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter_tt()));

        LinkFilterSettings.LG_QUICKSETTINGS_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                quicksettings.setVisible(newValue);
            }

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        add(hosterFilter, "gaptop 7");
        add(hosterFilterTable, "hidemode 2");
        add(filetypeFilter, "gaptop 7");
        add(filetypeFilterTable, "hidemode 2");
        add(Box.createGlue());
        add(quickSettingsHeader, "gaptop 7");
        add(quicksettings, "hidemode 2");

    }
}
