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
import org.jdownloader.gui.views.components.Checkbox;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.quickfilter.CustomFilterHeader;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterExceptionsTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterHosterTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterTypeTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickSettingsHeader;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class LinkGrabberSidebar extends MigPanel {

    /**
     * 
     */
    private static final long          serialVersionUID = 4006309139115917564L;
    private MigPanel                   quicksettings;
    private QuickFilterHosterTable     hosterFilterTable;
    private QuickFilterTypeTable       filetypeFilterTable;
    private Header                     hosterFilter;
    private Header                     filetypeFilter;
    private QuickSettingsHeader        quickSettingsHeader;
    private CustomFilterHeader         exceptions;
    private QuickFilterExceptionsTable exceptionsFilterTable;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[]");
        int c = LookAndFeelController.getInstance().getLAFOptions().getPanelBackgroundColor();

        if (c >= 0) {
            setBackground(new Color(c));
            setOpaque(true);
        }
        // header
        hosterFilter = new Header(LinkFilterSettings.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter());
        exceptions = new CustomFilterHeader();
        filetypeFilter = new Header(LinkFilterSettings.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter());
        quickSettingsHeader = new QuickSettingsHeader();

        //
        exceptionsFilterTable = new QuickFilterExceptionsTable(exceptions, table);
        exceptionsFilterTable.setVisible(LinkFilterSettings.LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED.getValue());
        hosterFilterTable = new QuickFilterHosterTable(hosterFilter, table);
        hosterFilterTable.setVisible(LinkFilterSettings.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED.getValue());
        filetypeFilterTable = new QuickFilterTypeTable(filetypeFilter, table);
        filetypeFilterTable.setVisible(LinkFilterSettings.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED.getValue());

        quicksettings = new MigPanel("ins 0,wrap 1", "[grow,fill]", "[]0[]0[]");
        if (c >= 0) {
            quicksettings.setBackground(new Color(c));
            quicksettings.setOpaque(true);
        }
        quicksettings.add(new Checkbox(LinkFilterSettings.LINKGRABBER_ADD_AT_TOP, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.LINKGRABBER_AUTO_CONFIRM_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt()));
        quicksettings.add(new Checkbox(LinkFilterSettings.LINKGRABBER_AUTO_START_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt()));

        quicksettings.add(new Checkbox(LinkFilterSettings.LINK_FILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter(), _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_globfilter_tt()));

        // disable auto confirm if user closed sidebar
        GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (GraphicalUserInterfaceSettings.LINKGRABBER_SIDEBAR_ENABLED.getValue()) {
                    LinkFilterSettings.LINKGRABBER_AUTO_CONFIRM_ENABLED.setValue(LinkFilterSettings.LINKGRABBER_AUTO_CONFIRM_ENABLED.getDefaultValue());
                }
            }
        });
        LinkFilterSettings.LINKGRABBER_QUICK_SETTINGS_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                quicksettings.setVisible(newValue);
            }

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });

        add(exceptions, "gaptop 7,hidemode 3");
        add(exceptionsFilterTable, "hidemode 3");
        add(filetypeFilter, "gaptop 7,hidemode 3");
        add(filetypeFilterTable, "hidemode 3");
        add(hosterFilter, "gaptop 7,hidemode 3");
        add(hosterFilterTable, "hidemode 3");

        add(Box.createGlue(), "pushy,growy");
        add(quickSettingsHeader, "gaptop 7");
        add(quicksettings, "hidemode 3");

    }
}
