package org.jdownloader.gui.views.linkgrabber;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.JComponent;

import jd.gui.swing.jdgui.interfaces.View;
import jd.http.Browser;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.MigPanel;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.event.GUIListener;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.Header;
import org.jdownloader.gui.views.linkgrabber.quickfilter.CustomFilterHeader;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterExceptionsTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterHosterTable;
import org.jdownloader.gui.views.linkgrabber.quickfilter.QuickFilterTypeTable;
import org.jdownloader.updatev2.SimpleHttpInterface;
import org.jdownloader.updatev2.SimpleHttpResponse;
import org.jdownloader.updatev2.SponsoringPanelInterface;
import org.jdownloader.updatev2.gui.LAFOptions;

public class LinkGrabberSidebar extends MigPanel {

    /**
     * 
     */
    private static final long          serialVersionUID = 4006309139115917564L;

    private QuickFilterHosterTable     hosterFilterTable;
    private QuickFilterTypeTable       filetypeFilterTable;
    private Header                     hosterFilter;
    private Header                     filetypeFilter;

    private CustomFilterHeader         exceptions;
    private QuickFilterExceptionsTable exceptionsFilterTable;

    private SponsoringPanelInterface   panel;

    private JComponent                 sponsoringPanel;

    public LinkGrabberSidebar(LinkGrabberTable table) {
        super("ins 0,wrap 1", "[grow,fill]", "[]");
        LAFOptions.getInstance().applyPanelBackground(this);
        // header
        hosterFilter = new Header(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter());
        exceptions = new CustomFilterHeader();
        filetypeFilter = new Header(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED, _GUI._.LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter());

        //
        exceptionsFilterTable = new QuickFilterExceptionsTable(exceptions, table);
        exceptionsFilterTable.setVisible(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_EXCEPTIONS_QUICKFILTER_ENABLED.getValue());
        hosterFilterTable = new QuickFilterHosterTable(hosterFilter, table);
        hosterFilterTable.setVisible(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_HOSTER_QUICKFILTER_ENABLED.getValue());
        filetypeFilterTable = new QuickFilterTypeTable(filetypeFilter, table);
        filetypeFilterTable.setVisible(org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINKGRABBER_FILETYPE_QUICKFILTER_ENABLED.getValue());

        // disable auto confirm if user closed sidebar
        org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }

            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                if (org.jdownloader.settings.staticreferences.CFG_GUI.LINKGRABBER_SIDEBAR_VISIBLE.getValue()) {
                    org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.setValue(org.jdownloader.settings.staticreferences.CFG_LINKGRABBER.LINKGRABBER_AUTO_CONFIRM_ENABLED.getDefaultValue());
                }
            }
        });

        add(exceptions, "gaptop 7,hidemode 3");
        add(exceptionsFilterTable, "hidemode 3");
        add(filetypeFilter, "gaptop 7,hidemode 3");
        add(filetypeFilterTable, "hidemode 3");
        add(hosterFilter, "gaptop 7,hidemode 3");
        add(hosterFilterTable, "hidemode 3");

        if (System.getProperty("nativeswing") != null) {
            try {
                panel = (SponsoringPanelInterface) Class.forName("org.jdownloader.sponsor.bt.DJBTSponsoringPanel").newInstance();
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
                add(Box.createVerticalGlue(), "pushy,growy");

                add(sponsoringPanel = panel.getPanel(), "hidemode 3");
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
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

    }

    public JComponent getSponsoringPanel() {
        return sponsoringPanel;
    }

}
