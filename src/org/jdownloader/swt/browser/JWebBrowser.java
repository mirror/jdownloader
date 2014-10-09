package org.jdownloader.swt.browser;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.OpenWindowListener;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.browser.VisibilityWindowListener;
import org.eclipse.swt.browser.WindowEvent;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.events.DragDetectListener;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.jdownloader.swt.browser.events.JWebBrowserAdapter;
import org.jdownloader.swt.browser.events.JWebBrowserEvent;
import org.jdownloader.swt.browser.events.JWebBrowserEventSender;
import org.jdownloader.swt.browser.events.JWebBrowserListener;

public class JWebBrowser extends MigPanel implements ProgressListener, LocationListener, OpenWindowListener, VisibilityWindowListener, MenuDetectListener, DragDetectListener {
    static {
        if (CrossSystem.isWindows()) {
            org.eclipse.swt.internal.win32.OS.CoInternetSetFeatureEnabled(org.eclipse.swt.internal.win32.OS.FEATURE_DISABLE_NAVIGATION_SOUNDS, org.eclipse.swt.internal.win32.OS.SET_FEATURE_ON_PROCESS, true);
        }
        if (CrossSystem.isLinux()) {

            System.setProperty("sun.awt.xembedserver", "true");
        }
    }

    private DelayedRunnable        delayer;
    private JWebBrowserEventSender eventSender;
    private Browser                swtBrowser;
    private SwtBrowserCanvas       canvas;

    public static void main(String[] args) throws Exception {
        // Required for Linux systems
        System.setProperty("sun.awt.xembedserver", "true");
        // start Dispatcher
        SWTDummyDisplayDispatcher.getInstance().start();

        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // Create container canvas. Note that the browser
                // widget JWebBrowser not be created, yet.
                final JWebBrowser browserCanvas = new JWebBrowser();
                browserCanvas.getEventSender().addListener(new JWebBrowserAdapter() {
                    @Override
                    public void onInitComplete(JWebBrowser jWebBrowser) {
                        jWebBrowser.getPage("http://installer.jdownloader.org/flash.html");
                    }
                });

                browserCanvas.setPreferredSize(new Dimension(800, 600));
                JPanel panel = new JPanel(new BorderLayout());
                panel.add(browserCanvas, BorderLayout.CENTER);

                // Add container to Frame
                JFrame frame = new JFrame("My SWT Browser");
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(panel);

                frame.pack();

                // This is VERY important: Make the frame visible BEFORE
                // connecting the SWT Shell and starting the event loop!
                frame.setVisible(true);
            }
        };

    }

    @Override
    public void addNotify() {
        super.addNotify();

    }

    public JWebBrowser() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        SWTDummyDisplayDispatcher.getInstance().ensureRunning();

        SwingUtils.setOpaque(this, false);

        eventSender = new JWebBrowserEventSender();
        delayer = new DelayedRunnable(250) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        canvas.setVisible(true);
                    }
                };

            }
        };

        addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(HierarchyEvent e) {

                Window window = SwingUtilities.getWindowAncestor(JWebBrowser.this);
                if (window != null && window.isVisible()) {
                    if (canvas == null) {
                        canvas = SWTDummyDisplayDispatcher.getInstance().createBrowser();

                        add(canvas);
                        canvas.connect();
                        swtBrowser = canvas.getBrowser();
                        initHideOnResizeOrMove();
                        asyncExec(new Runnable() {
                            public void run() {
                                init();
                                eventSender.fireEvent(new JWebBrowserEvent() {

                                    @Override
                                    public void sendTo(JWebBrowserListener listener) {
                                        listener.onInitComplete(JWebBrowser.this);
                                    }

                                });
                                onConnected();
                            }

                        });
                        initHideOnResizeOrMove();
                    }

                    removeHierarchyListener(this);

                }
            }
        });

    }

    private void initHideOnResizeOrMove() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {

                // Hide Browser on resize
                addHierarchyBoundsListener(new HierarchyBoundsListener() {

                    @Override
                    public void ancestorResized(HierarchyEvent e) {
                        if (e.getChanged() == SwingUtilities.getWindowAncestor(JWebBrowser.this)) {
                            if (isVisible() || delayer.isDelayerActive()) {
                                canvas.setVisible(false);
                                delayer.resetAndStart();
                            }
                        }
                    }

                    @Override
                    public void ancestorMoved(HierarchyEvent e) {
                        if (e.getChanged() == SwingUtilities.getWindowAncestor(JWebBrowser.this)) {
                            if (isVisible() || delayer.isDelayerActive()) {
                                canvas.setVisible(false);
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
                        if (e.getAncestor() == SwingUtilities.getWindowAncestor(JWebBrowser.this)) {
                            if (isVisible() || delayer.isDelayerActive()) {
                                canvas.setVisible(false);
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

    public JWebBrowserEventSender getEventSender() {
        return eventSender;
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
    }

    protected void onConnected() {
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);
    }

    private void init() {

        Browser swtb = swtBrowser;
        swtb.addProgressListener(JWebBrowser.this);
        swtb.setJavascriptEnabled(true);
        swtb.addDragDetectListener(this);
        swtb.addLocationListener(this);
        swtb.addMenuDetectListener(this);
        swtb.addOpenWindowListener(this);
        swtb.addVisibilityWindowListener(this);

    }

    public String getUrl() {
        final AtomicReference<String> ref = new AtomicReference<String>();
        syncExec(new Runnable() {

            @Override
            public void run() {
                ref.set(swtBrowser.getUrl());
            }
        });
        return ref.get();
    }

    public String getHtmlText() {
        final AtomicReference<String> ref = new AtomicReference<String>();
        syncExec(new Runnable() {

            @Override
            public void run() {
                ref.set(swtBrowser.getText());
            }
        });
        return ref.get();
    }

    public Object executeJavaScript(final String js) {
        final AtomicReference<Object> ref = new AtomicReference<Object>();
        syncExec(new Runnable() {

            @Override
            public void run() {

                ref.set(swtBrowser.evaluate(js));
            }
        });
        return ref.get();
    }

    public String getDocument() {

        Object ret = executeJavaScript("return document.documentElement.innerHTML");
        if (ret == null || !(ret instanceof String)) {
            return null;
        }
        return ret.toString();

    }

    public void getPage(final String url) {
        syncExec(new Runnable() {

            @Override
            public void run() {

                swtBrowser.setUrl(url);
                canvas.getShell().setFullScreen(true);
                canvas.getShell().pack();
            }
        });

    }

    public void stop() {
        syncExec(new Runnable() {

            @Override
            public void run() {
                swtBrowser.stop();
            }
        });
    }

    public void setCookie(final String url, final String value) {

        syncExec(new Runnable() {

            @Override
            public void run() {
                Browser.setCookie(value, url);
            }
        });

    }

    public void setHtmlText(final String html, final boolean trusted) {
        syncExec(new Runnable() {

            @Override
            public void run() {
                swtBrowser.setText(html, trusted);
            }
        });

    }

    public void setJavaScriptEnabled(final boolean b) {
        syncExec(new Runnable() {

            @Override
            public void run() {
                swtBrowser.setJavascriptEnabled(b);
            }
        });
    }

    public boolean isJavaScriptEnabled() {
        final AtomicBoolean ret = new AtomicBoolean(false);
        syncExec(new Runnable() {

            @Override
            public void run() {
                ret.set(swtBrowser.getJavascriptEnabled());
            }
        });
        return ret.get();
    }

    private void syncExec(Runnable runnable) {
        swtBrowser.getDisplay().syncExec(runnable);
    }

    private void asyncExec(Runnable runnable) {
        swtBrowser.getDisplay().asyncExec(runnable);
    }

    @Override
    public void changed(final ProgressEvent event) {
        eventSender.fireEvent(new JWebBrowserEvent() {

            @Override
            public void sendTo(JWebBrowserListener listener) {
                listener.onLoading(JWebBrowser.this, event.current / (double) event.total);
            }

        });
    }

    @Override
    public void completed(ProgressEvent event) {
        eventSender.fireEvent(new JWebBrowserEvent() {

            @Override
            public void sendTo(JWebBrowserListener listener) {
                listener.onLoadingComplete(JWebBrowser.this);
            }

        });
    }

    @Override
    public void changing(final LocationEvent event) {

        eventSender.fireEvent(new JWebBrowserEvent() {

            @Override
            public void sendTo(JWebBrowserListener listener) {
                listener.onUrlChanging(JWebBrowser.this, event.location);
            }

        });
    }

    @Override
    public void changed(final LocationEvent event) {
        eventSender.fireEvent(new JWebBrowserEvent() {

            @Override
            public void sendTo(JWebBrowserListener listener) {
                listener.onUrlChanged(JWebBrowser.this, event.location);
            }

        });
    }

    @Override
    public void open(final WindowEvent event) {
        final Browser newBrowser = new Browser(swtBrowser.getParent(), SWT.NONE);

        newBrowser.addLocationListener(new LocationListener() {

            @Override
            public void changing(final LocationEvent event) {

                eventSender.fireEvent(new JWebBrowserEvent() {

                    @Override
                    public void sendTo(JWebBrowserListener listener) {
                        listener.onBrowserWindowRequested(JWebBrowser.this, event.location);
                    }

                });
                newBrowser.removeLocationListener(this);
                newBrowser.dispose();

            }

            @Override
            public void changed(LocationEvent event) {
                System.out.println(event);
            }
        });
        event.browser = newBrowser;
    }

    @Override
    public void hide(WindowEvent event) {
        System.out.println(event);
    }

    @Override
    public void show(WindowEvent event) {
        System.out.println(event);
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

    @Override
    public void menuDetected(MenuDetectEvent e) {
        e.doit = isContextMenuEnabled();

    }

    @Override
    public void dragDetected(DragDetectEvent e) {

    }

    public void runSizeCheck() {
        System.out.println("This " + getSize());
        System.out.println("Browser " + getSize());

    }
}
