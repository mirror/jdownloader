package org.jdownloader.gui.views.downloads;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import org.appwork.swing.MigPanel;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.eclipse.swt.browser.LocationEvent;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.components.OverviewHeaderScrollPane;
import org.jdownloader.gui.views.components.HeaderScrollPane;
import org.jdownloader.swt.browser.JWebBrowser;
import org.jdownloader.swt.browser.events.JWebBrowserAdapter;
import org.jdownloader.updatev2.gui.LAFOptions;

public class JBrowserWrapper {

    public static HeaderScrollPane createView() {

        final MigPanel loverView = new MigPanel("ins 0", "[grow,fill]0[]0[grow,fill]", "[]");
        loverView.setBackground(Color.WHITE);

        final JWebBrowser webBrowser = new JWebBrowser() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(Math.min(728, loverView.getWidth() - 2), 90);
            }

            @Override
            public void changing(LocationEvent event) {
                System.out.println(event.location);
                if ("http://installer.jdownloader.org/flash.html".equals(event.location)) {
                    super.changing(event);
                    return;
                }
                if ("http://installer.jdownloader.org/noflash.html".equals(event.location)) {
                    super.changing(event);
                    return;
                }
                if ("about:blank".equals(event.location)) {
                    super.changing(event);
                    return;
                }
                // do not open the new url in our browser window, but in new window instead.
                event.doit = false;
                CrossSystem.openURL(event.location);

            }

        };
        webBrowser.getEventSender().addListener(new JWebBrowserAdapter() {
            @Override
            public void onBrowserWindowRequested(JWebBrowser jWebBrowser, String location) {
                CrossSystem.openURL(location);
            }
        });
        webBrowser.getEventSender().addListener(new JWebBrowserAdapter() {

            @Override
            public void onInitComplete(JWebBrowser jWebBrowser) {
                jWebBrowser.getEventSender().removeListener(this);
                String flashHtml;
                try {
                    flashHtml = IO.readURLToString(JWebBrowser.class.getResource("swfobject.js"));

                    jWebBrowser.getEventSender().addListener(new JWebBrowserAdapter() {
                        @Override
                        public void onLoadingComplete(final JWebBrowser jWebBrowser) {
                            //
                            jWebBrowser.getEventSender().removeListener(this);
                            Object result = jWebBrowser.executeJavaScript("function f(){return swfobject.getFlashPlayerVersion().major;}; return f();");
                            if (result != null) {
                                jWebBrowser.getPage("http://installer.jdownloader.org/flash.html");
                            } else {
                                jWebBrowser.getPage("http://installer.jdownloader.org/noflash.html");
                            }
                            new EDTRunner() {

                                @Override
                                protected void runInEDT() {
                                    jWebBrowser.setVisible(true);
                                }
                            };
                        }
                    });
                    jWebBrowser.setHtmlText("<html><body><script type='text/javascript'>" + flashHtml + "</script></body></html>", false);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        // webBrowser.setButtonBarVisible(false);
        // webBrowser.setDefaultPopupMenuRegistered(false);
        // webBrowser.setJavascriptEnabled(true);
        webBrowser.setFocusable(false);
        webBrowser.setVisible(false);
        loverView.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu p = new JPopupMenu();
                p.add(new AppAction() {
                    {
                        setName("Request Page");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String page;
                        try {
                            page = Dialog.getInstance().showInputDialog(0, "Enter Url", "http://installer.jdownloader.org/ads.html");
                            if (page != null) {
                                webBrowser.executeJavaScript("document.location=\"" + page + "\"");
                                // webBrowser.getPage(page);
                            }
                        } catch (DialogClosedException e1) {
                            e1.printStackTrace();
                        } catch (DialogCanceledException e1) {
                            e1.printStackTrace();
                        }
                    }

                });
                p.add(new AppAction() {
                    {
                        setName("Get Document");
                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        String doc = webBrowser.getDocument();
                        System.out.println(doc);

                    }

                });

                p.show(webBrowser, e.getX(), e.getY());
            }
        });
        // webBrowser.setLocationBarVisible(false);
        // webBrowser.setMenuBarVisible(false);
        // webBrowser.setStatusBarVisible(false);

        // webBrowser.addWebBrowserListener(new WebBrowserListener() {
        //
        // @Override
        // public void windowWillOpen(final WebBrowserWindowWillOpenEvent e) {
        // System.out.println(e);
        // final JWebBrowser newBrowser = e.getNewWebBrowser();
        // newBrowser.addWebBrowserListener(new WebBrowserAdapter() {
        // @Override
        // public void locationChanging(WebBrowserNavigationEvent e) {
        // e.consume();
        // CrossSystem.openURL(e.getNewResourceLocation());
        // // immediately close the new swing window
        // SwingUtilities.invokeLater(new Runnable() {
        //
        // public void run() {
        // newBrowser.getWebBrowserWindow().dispose();
        // }
        // });
        // }
        // });
        // }
        //
        // @Override
        // public void windowOpening(WebBrowserWindowOpeningEvent paramWebBrowserWindowOpeningEvent) {
        // System.out.println(paramWebBrowserWindowOpeningEvent);
        // }
        //
        // @Override
        // public void windowClosing(WebBrowserEvent paramWebBrowserEvent) {
        // System.out.println(paramWebBrowserEvent);
        // }
        //
        // @Override
        // public void titleChanged(WebBrowserEvent paramWebBrowserEvent) {
        // System.out.println("Title Changed to " + paramWebBrowserEvent.getWebBrowser().getPageTitle());
        // }
        //
        // @Override
        // public void statusChanged(WebBrowserEvent paramWebBrowserEvent) {
        // // System.out.println("Status Text: " + paramWebBrowserEvent.getWebBrowser().getStatusText());
        //
        // }
        //
        // @Override
        // public void locationChanging(WebBrowserNavigationEvent event) {
        // // System.out.println(event);
        // System.out.println("Location is Changing to " + event.getNewResourceLocation() + " Same Broswer: " + (event.getSource() ==
        // event.getWebBrowser()));
        //
        // }
        //
        // @Override
        // public void locationChanged(WebBrowserNavigationEvent paramWebBrowserNavigationEvent) {
        // System.out.println(paramWebBrowserNavigationEvent);
        // }
        //
        // @Override
        // public void locationChangeCanceled(WebBrowserNavigationEvent paramWebBrowserNavigationEvent) {
        // System.out.println(paramWebBrowserNavigationEvent);
        // }
        //
        // @Override
        // public void loadingProgressChanged(WebBrowserEvent paramWebBrowserEvent) {
        // System.out.println("WebBrowser Loading: " + paramWebBrowserEvent.getWebBrowser().getLoadingProgress());
        //
        // if (paramWebBrowserEvent.getWebBrowser().getLoadingProgress() == 100) {
        // // NativeComponent nComp = webBrowser.getNativeComponent();
        // //
        // // nComp.setSize(new Dimension(728, 90));
        // }
        // }
        //
        // @Override
        // public void commandReceived(WebBrowserCommandEvent paramWebBrowserCommandEvent) {
        // System.out.println(paramWebBrowserCommandEvent);
        // }
        // });
        // JWebBrowser.setWebBrowserDecoratorFactory(new WebBrowserDecoratorFactory() {
        //
        // @Override
        // public WebBrowserDecorator createWebBrowserDecorator(JWebBrowser paramJWebBrowser, Component paramComponent) {
        // return null;
        // }
        // });

        webBrowser.setPreferredSize(new Dimension(728, 90));
        // webBrowser.executeJavascript("");
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
