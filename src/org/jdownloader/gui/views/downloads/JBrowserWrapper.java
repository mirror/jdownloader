package org.jdownloader.gui.views.downloads;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.appwork.swing.MigPanel;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.updatev2.gui.LAFOptions;

import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser.WebBrowserDecoratorFactory;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserAdapter;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserCommandEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserDecorator;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserListener;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserNavigationEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowOpeningEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowWillOpenEvent;

public class JBrowserWrapper {

    public static HeaderScrollPane createView() {

        final MigPanel loverView = new MigPanel("ins 0", "[grow,fill]0[]0[grow,fill]", "[]");
        loverView.setBackground(Color.WHITE);
        final JWebBrowser webBrowser = new JWebBrowser(JWebBrowser.destroyOnFinalization()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(Math.min(728, loverView.getWidth() - 2), 90);
            }
        };
        webBrowser.setButtonBarVisible(false);
        webBrowser.setDefaultPopupMenuRegistered(false);
        webBrowser.setJavascriptEnabled(true);
        webBrowser.setFocusable(false);
        webBrowser.setLocationBarVisible(false);
        webBrowser.setMenuBarVisible(false);
        webBrowser.setStatusBarVisible(false);

        webBrowser.addWebBrowserListener(new WebBrowserListener() {

            @Override
            public void windowWillOpen(final WebBrowserWindowWillOpenEvent e) {
                System.out.println(e);
                final JWebBrowser newBrowser = e.getNewWebBrowser();
                newBrowser.addWebBrowserListener(new WebBrowserAdapter() {
                    @Override
                    public void locationChanging(WebBrowserNavigationEvent e) {
                        e.consume();
                        CrossSystem.openURL(e.getNewResourceLocation());
                        // immediately close the new swing window
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                newBrowser.getWebBrowserWindow().dispose();
                            }
                        });
                    }
                });
            }

            @Override
            public void windowOpening(WebBrowserWindowOpeningEvent paramWebBrowserWindowOpeningEvent) {
                System.out.println(paramWebBrowserWindowOpeningEvent);
            }

            @Override
            public void windowClosing(WebBrowserEvent paramWebBrowserEvent) {
                System.out.println(paramWebBrowserEvent);
            }

            @Override
            public void titleChanged(WebBrowserEvent paramWebBrowserEvent) {
                System.out.println("Title Changed to " + paramWebBrowserEvent.getWebBrowser().getPageTitle());
            }

            @Override
            public void statusChanged(WebBrowserEvent paramWebBrowserEvent) {
                // System.out.println("Status Text: " + paramWebBrowserEvent.getWebBrowser().getStatusText());

            }

            @Override
            public void locationChanging(WebBrowserNavigationEvent event) {
                // System.out.println(event);
                System.out.println("Location is Changing to " + event.getNewResourceLocation() + " Same Broswer: " + (event.getSource() == event.getWebBrowser()));

            }

            @Override
            public void locationChanged(WebBrowserNavigationEvent paramWebBrowserNavigationEvent) {
                System.out.println(paramWebBrowserNavigationEvent);
            }

            @Override
            public void locationChangeCanceled(WebBrowserNavigationEvent paramWebBrowserNavigationEvent) {
                System.out.println(paramWebBrowserNavigationEvent);
            }

            @Override
            public void loadingProgressChanged(WebBrowserEvent paramWebBrowserEvent) {
                System.out.println("WebBrowser Loading: " + paramWebBrowserEvent.getWebBrowser().getLoadingProgress());

                if (paramWebBrowserEvent.getWebBrowser().getLoadingProgress() == 100) {
                    // NativeComponent nComp = webBrowser.getNativeComponent();
                    //
                    // nComp.setSize(new Dimension(728, 90));
                }
            }

            @Override
            public void commandReceived(WebBrowserCommandEvent paramWebBrowserCommandEvent) {
                System.out.println(paramWebBrowserCommandEvent);
            }
        });
        JWebBrowser.setWebBrowserDecoratorFactory(new WebBrowserDecoratorFactory() {

            @Override
            public WebBrowserDecorator createWebBrowserDecorator(JWebBrowser paramJWebBrowser, Component paramComponent) {
                return null;
            }
        });

        webBrowser.navigate("http://installer.jdownloader.org/ads.html");
        webBrowser.setPreferredSize(new Dimension(728, 90));
        webBrowser.executeJavascript("");
        loverView.add(Box.createHorizontalGlue());
        loverView.add(webBrowser, "");
        loverView.add(Box.createHorizontalGlue());
        final OverviewHeaderScrollPane propertiesScrollPane = new OverviewHeaderScrollPane(loverView);
        propertiesScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        propertiesScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        propertiesScrollPane.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0), propertiesScrollPane.getBorder()));

        LAFOptions.getInstance().applyPanelBackground(propertiesScrollPane);

        return propertiesScrollPane;

    }

}
