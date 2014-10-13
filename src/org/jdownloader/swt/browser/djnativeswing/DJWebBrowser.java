package org.jdownloader.swt.browser.djnativeswing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.logging.LogController;
import org.jdownloader.swt.browser.djnativeswing.event.DJWebBrowserAdapter;
import org.jdownloader.swt.browser.djnativeswing.event.DJWebBrowserEvent;
import org.jdownloader.swt.browser.djnativeswing.event.DJWebBrowserEventSender;
import org.jdownloader.swt.browser.djnativeswing.event.DJWebBrowserListener;
import org.jdownloader.swt.browser.djnativeswing.event.JavaScriptEventListener;

import chrriis.common.WebServer;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserAdapter;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserCommandEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserFunction;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserListener;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserNavigationEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowOpeningEvent;
import chrriis.dj.nativeswing.swtimpl.components.WebBrowserWindowWillOpenEvent;

public class DJWebBrowser extends MigPanel implements WebBrowserListener {

    private DelayedRunnable         delayer;
    private DJWebBrowserEventSender eventSender;
    private JWebBrowser             browser;
    private LogSource               logger;

    public void addJavaScriptEventListener(final String functionName) {
        addJavaScriptEventListener(functionName, null);

    }

    public void addJavaScriptEventListener(final String functionName, final JavaScriptEventListener callback) {
        browser.registerFunction(new WebBrowserFunction(functionName) {

            @Override
            public Object invoke(JWebBrowser webBrowser, final Object... arguments) {
                eventSender.fireEvent(new DJWebBrowserEvent() {

                    @Override
                    public void sendTo(DJWebBrowserListener listener) {
                        listener.onJavaScriptCallback(DJWebBrowser.this, functionName, arguments);
                    }

                });
                if (callback != null) {
                    return callback.onJavaScriptCallback(DJWebBrowser.this, functionName, arguments);
                } else {
                    return null;
                }

            }
        });

    }

    @Override
    public void addNotify() {
        super.addNotify();

    }

    public DJWebBrowser() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        logger = LogController.getInstance().getLogger(DJWebBrowser.class.getName());
        SwingUtils.setOpaque(this, false);
        setFocusable(false);
        eventSender = new DJWebBrowserEventSender();
        delayer = new DelayedRunnable(500) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        browser.setVisible(isVisible());
                        resizing = false;
                    }
                };

            }
        };
        browser = new JWebBrowser();
        browser.setFocusable(false);
        add(browser);
        initHideOnResizeOrMove();
        init();

    }

    private boolean resizing = false;

    private void initHideOnResizeOrMove() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                // Hide Browser on resize
                addHierarchyBoundsListener(new HierarchyBoundsListener() {

                    @Override
                    public void ancestorResized(HierarchyEvent e) {
                        if (e.getChanged() == SwingUtilities.getWindowAncestor(DJWebBrowser.this)) {
                            if (browser.isVisible() || delayer.isDelayerActive()) {
                                browser.setVisible(false);
                                delayer.resetAndStart();
                                resizing = true;
                            }
                        }
                    }

                    @Override
                    public void ancestorMoved(HierarchyEvent e) {
                        if (e.getChanged() == SwingUtilities.getWindowAncestor(DJWebBrowser.this)) {
                            if (browser.isVisible() || delayer.isDelayerActive()) {
                                browser.setVisible(false);
                                resizing = true;
                                delayer.resetAndStart();
                            }
                        }
                    }
                });

                // Hide Browser on resize
                addAncestorListener(new AncestorListener() {

                    @Override
                    public void ancestorRemoved(AncestorEvent event) {

                    }

                    @Override
                    public void ancestorMoved(AncestorEvent e) {
                        if (e.getAncestor() == SwingUtilities.getWindowAncestor(DJWebBrowser.this)) {
                            if (browser.isVisible() || delayer.isDelayerActive()) {
                                browser.setVisible(false);
                                resizing = true;
                                delayer.resetAndStart();
                            }
                        }
                    }

                    @Override
                    public void ancestorAdded(AncestorEvent event) {

                    }
                });
            }
        });
    }

    public DJWebBrowserEventSender getEventSender() {
        return eventSender;
    }

    @Override
    public void setVisible(boolean aFlag) {
        System.out.println("Browser Visible: " + aFlag);
        super.setVisible(aFlag);
        if (!resizing) {
            browser.setVisible(aFlag);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    protected void init() {
        browser.setBarsVisible(false);
        browser.setButtonBarVisible(false);
        browser.setDefaultPopupMenuRegistered(false);
        browser.setLocationBarVisible(false);
        browser.setMenuBarVisible(false);
        browser.setStatusBarVisible(false);

        browser.addWebBrowserListener(this);
        browser.setJavascriptEnabled(true);

    }

    public String getUrl() {
        return browser.getResourceLocation();
    }

    public String getHtmlText() {
        return browser.getHTMLContent();
    }

    public Object executeJavaScript(final String js) {
        return browser.executeJavascriptWithResult(js);
    }

    public String getDocument() {

        Object ret = executeJavaScript("return document.documentElement.innerHTML");
        if (ret == null || !(ret instanceof String)) {
            return null;
        }
        return ret.toString();

    }

    public void getPage(final String url) {
        browser.navigate(url);

    }

    public void stop() {
        browser.stopLoading();
    }

    public void setCookie(final String url, final String value) {

        JWebBrowser.setCookie(url, value);

    }

    public void setHtmlText(final String html) {
        browser.setHTMLContent(html);

    }

    public void setJavaScriptEnabled(final boolean b) {
        browser.setJavascriptEnabled(b);
    }

    public boolean isJavaScriptEnabled() {
        return browser.isJavascriptEnabled();
    }

    private boolean dragAndDropEnabled = false;

    public boolean isDragAndDropEnabled() {
        return dragAndDropEnabled;
    }

    public void setDragAndDropEnabled(boolean dragAndDropEnabled) {
        this.dragAndDropEnabled = dragAndDropEnabled;
    }

    private boolean contextMenuEnabled = false;

    public boolean isContextMenuEnabled() {
        return contextMenuEnabled;
    }

    public void setContextMenuEnabled(boolean contextMenuEnabled) {
        this.contextMenuEnabled = contextMenuEnabled;
    }

    public static void main(String[] args) throws Exception {
        // start Dispatcher
        long start = System.currentTimeMillis();
        DJWebBrowser.ensureOpenPeer();

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // Create container canvas. Note that the browser
                // widget JWebBrowser not be created, yet.
                final DJWebBrowser browserCanvas = new DJWebBrowser();

                // final JWebBrowser browserCanvas = new JWebBrowser();
                browserCanvas.getEventSender().addListener(new DJWebBrowserAdapter() {

                    @Override
                    public void onJavaScriptCallback(DJWebBrowser jWebBrowser, String functionName, Object[] arguments) {
                        System.out.println(functionName + " - " + Arrays.toString(arguments));
                    }

                });
                browserCanvas.getPage("https://www.whatismybrowser.com/");
                browserCanvas.setPreferredSize(new Dimension(800, 600));
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(browserCanvas, BorderLayout.CENTER);

                // Add container to Frame
                JFrame frame = new JFrame("whatismybrowser.com");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(panel);

                frame.pack();

                // This is VERY important: Make the frame visible BEFORE
                // connecting the SWT Shell and starting the event loop!
                frame.setVisible(true);
            }
        };

        try {
            chrriis.dj.nativeswing.swtimpl.NativeInterface.runEventPump();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void ensureOpenPeer() {
        long start = System.currentTimeMillis();
        try {
            WebServer.getDefaultWebServer().stop();
            chrriis.dj.nativeswing.swtimpl.NativeInterface.open();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println("Native Swing init took " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    @Override
    public void windowWillOpen(WebBrowserWindowWillOpenEvent e) {

        logger.info("Window will Opening");
        // We let the window to be created, but we will check the first location that is set on it.
        e.getNewWebBrowser().addWebBrowserListener(new WebBrowserAdapter() {
            @Override
            public void locationChanging(final WebBrowserNavigationEvent e) {
                final JWebBrowser webBrowser = e.getWebBrowser();
                webBrowser.removeWebBrowserListener(this);

                eventSender.fireEvent(new DJWebBrowserEvent() {

                    @Override
                    public void sendTo(DJWebBrowserListener listener) {
                        listener.onBrowserWindowRequested(DJWebBrowser.this, e.getNewResourceLocation());
                    }

                });
                e.consume();
                // The URL Changing event is special: it is synchronous so disposal must be deferred.
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        webBrowser.getWebBrowserWindow().dispose();
                    }
                });

            }
        });

    }

    @Override
    public void windowOpening(WebBrowserWindowOpeningEvent e) {
        logger.info("Window Opening");

    }

    @Override
    public void windowClosing(WebBrowserEvent e) {
        logger.info("Window closing ");

    }

    @Override
    public void locationChanging(final WebBrowserNavigationEvent e) {

        logger.info("Location Changing to " + e.getNewResourceLocation());
        eventSender.fireEvent(new DJWebBrowserEvent() {

            @Override
            public void sendTo(DJWebBrowserListener listener) {
                listener.onUrlChanging(DJWebBrowser.this, e.getNewResourceLocation());
            }

        });
    }

    @Override
    public void locationChanged(final WebBrowserNavigationEvent e) {
        logger.info("Location changed to " + e.getNewResourceLocation());
    }

    @Override
    public void locationChangeCanceled(WebBrowserNavigationEvent e) {
        logger.info("Location Change Canceled");
    }

    @Override
    public void loadingProgressChanged(final WebBrowserEvent e) {
        logger.info("Loading Progress: " + browser.getLoadingProgress());

        eventSender.fireEvent(new DJWebBrowserEvent() {

            @Override
            public void sendTo(DJWebBrowserListener listener) {
                listener.onLoading(DJWebBrowser.this, browser.getLoadingProgress() / 100d);
            }

        });
        if (browser.getLoadingProgress() >= 100) {
            eventSender.fireEvent(new DJWebBrowserEvent() {

                @Override
                public void sendTo(DJWebBrowserListener listener) {
                    listener.onLoadingComplete(DJWebBrowser.this);
                }

            });
        }
    }

    @Override
    public void titleChanged(WebBrowserEvent e) {
        logger.info("Title: " + e.getWebBrowser().getPageTitle());
    }

    @Override
    public void statusChanged(WebBrowserEvent e) {
        logger.info("Status: " + e.getWebBrowser().getStatusText());
    }

    @Override
    public void commandReceived(WebBrowserCommandEvent e) {

        logger.info(e + "");
        logger.info("Command: " + e.getCommand());
        logger.info("Paramaters: " + Arrays.toString(e.getParameters()));
    }
}
