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
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
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
    private JWebBrowser             browserCanvas;

    public void addJavaScriptEventListener(final String functionName) {
        addJavaScriptEventListener(functionName, null);

    }

    public void addJavaScriptEventListener(final String functionName, final JavaScriptEventListener callback) {
        browserCanvas.registerFunction(new WebBrowserFunction(functionName) {

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

        SwingUtils.setOpaque(this, false);
        setFocusable(false);
        eventSender = new DJWebBrowserEventSender();
        delayer = new DelayedRunnable(500) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        browserCanvas.setVisible(isVisible());
                        resizing = false;
                    }
                };

            }
        };
        browserCanvas = new JWebBrowser();
        add(browserCanvas);
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
                            if (browserCanvas.isVisible() || delayer.isDelayerActive()) {
                                browserCanvas.setVisible(false);
                                delayer.resetAndStart();
                                resizing = true;
                            }
                        }
                    }

                    @Override
                    public void ancestorMoved(HierarchyEvent e) {
                        if (e.getChanged() == SwingUtilities.getWindowAncestor(DJWebBrowser.this)) {
                            if (browserCanvas.isVisible() || delayer.isDelayerActive()) {
                                browserCanvas.setVisible(false);
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
                            if (browserCanvas.isVisible() || delayer.isDelayerActive()) {
                                browserCanvas.setVisible(false);
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
            browserCanvas.setVisible(aFlag);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    protected void init() {
        browserCanvas.setBarsVisible(false);
        browserCanvas.setButtonBarVisible(false);
        browserCanvas.setDefaultPopupMenuRegistered(false);
        browserCanvas.setLocationBarVisible(false);
        browserCanvas.setMenuBarVisible(false);
        browserCanvas.setStatusBarVisible(false);

        browserCanvas.addWebBrowserListener(this);
        browserCanvas.setJavascriptEnabled(true);

    }

    public String getUrl() {
        return browserCanvas.getResourceLocation();
    }

    public String getHtmlText() {
        return browserCanvas.getHTMLContent();
    }

    public Object executeJavaScript(final String js) {
        return browserCanvas.executeJavascriptWithResult(js);
    }

    public String getDocument() {

        Object ret = executeJavaScript("return document.documentElement.innerHTML");
        if (ret == null || !(ret instanceof String)) {
            return null;
        }
        return ret.toString();

    }

    public void getPage(final String url) {
        browserCanvas.navigate(url);

    }

    public void stop() {
        browserCanvas.stopLoading();
    }

    public void setCookie(final String url, final String value) {

        JWebBrowser.setCookie(url, value);

    }

    public void setHtmlText(final String html) {
        browserCanvas.setHTMLContent(html);

    }

    public void setJavaScriptEnabled(final boolean b) {
        browserCanvas.setJavascriptEnabled(b);
    }

    public boolean isJavaScriptEnabled() {
        return browserCanvas.isJavascriptEnabled();
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

        System.out.println(e);
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
        System.out.println(e);

    }

    @Override
    public void windowClosing(WebBrowserEvent e) {
        System.out.println(e);

    }

    @Override
    public void locationChanging(final WebBrowserNavigationEvent e) {
        eventSender.fireEvent(new DJWebBrowserEvent() {

            @Override
            public void sendTo(DJWebBrowserListener listener) {
                listener.onUrlChanging(DJWebBrowser.this, e.getNewResourceLocation());
            }

        });
    }

    @Override
    public void locationChanged(final WebBrowserNavigationEvent e) {
        System.out.println(e);
    }

    @Override
    public void locationChangeCanceled(WebBrowserNavigationEvent e) {
        System.out.println(e);
    }

    @Override
    public void loadingProgressChanged(final WebBrowserEvent e) {
        System.out.println(e);

        eventSender.fireEvent(new DJWebBrowserEvent() {

            @Override
            public void sendTo(DJWebBrowserListener listener) {
                listener.onLoading(DJWebBrowser.this, browserCanvas.getLoadingProgress() / 100d);
            }

        });
        if (browserCanvas.getLoadingProgress() >= 100) {
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
    }

    @Override
    public void statusChanged(WebBrowserEvent e) {
    }

    @Override
    public void commandReceived(WebBrowserCommandEvent e) {
    }
}
