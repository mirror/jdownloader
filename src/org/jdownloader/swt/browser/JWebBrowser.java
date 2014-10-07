package org.jdownloader.swt.browser;

import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.HierarchyBoundsListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
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
import org.jdownloader.swt.browser.events.JWebBrowserEvent;
import org.jdownloader.swt.browser.events.JWebBrowserEventSender;
import org.jdownloader.swt.browser.events.JWebBrowserListener;

public class JWebBrowser extends MigPanel implements ProgressListener, LocationListener, OpenWindowListener, VisibilityWindowListener, MenuDetectListener, DragDetectListener {

    private SWTBrowser             swtbrowser;
    private boolean                connected;
    private Exception              connnectException;
    private DelayedRunnable        delayer;
    private JWebBrowserEventSender eventSender;

    public JWebBrowser() {
        super("ins 0", "[grow,fill]", "[grow,fill]");
        swtbrowser = new SWTBrowser();
        SwingUtils.setOpaque(this, false);
        connected = false;
        add(swtbrowser);
        eventSender = new JWebBrowserEventSender();
        delayer = new DelayedRunnable(250) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        setVisible(true);
                    }
                };

            }
        };

        // connect after the parent window has been set visible
        addHierarchyListener(new HierarchyListener() {

            @Override
            public void hierarchyChanged(HierarchyEvent e) {

                Window window = SwingUtilities.getWindowAncestor(JWebBrowser.this);
                if (window != null) {
                    connect();

                    removeHierarchyListener(this);

                }
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

    public synchronized void connect() {
        if (!connected) {
            connected = true;
            new Thread("Connect Thread") {
                @Override
                public void run() {
                    try {
                        swtbrowser.connect();
                    } catch (Exception e) {
                        JWebBrowser.this.connnectException = e;
                    }
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {

                            // Hide Browser on resize
                            addHierarchyBoundsListener(new HierarchyBoundsListener() {

                                @Override
                                public void ancestorResized(HierarchyEvent e) {
                                    if (e.getChanged() == SwingUtilities.getWindowAncestor(JWebBrowser.this)) {
                                        if (isVisible() || delayer.isDelayerActive()) {
                                            setVisible(false);
                                            delayer.resetAndStart();
                                        }
                                    }
                                }

                                @Override
                                public void ancestorMoved(HierarchyEvent e) {
                                    if (e.getChanged() == SwingUtilities.getWindowAncestor(JWebBrowser.this)) {
                                        if (isVisible() || delayer.isDelayerActive()) {
                                            setVisible(false);
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
                                            setVisible(false);
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

                }
            }.start();

        }
    }

    private void init() {

        Browser swtb = swtbrowser.getBrowser();
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
                ref.set(swtbrowser.getBrowser().getUrl());
            }
        });
        return ref.get();
    }

    public String getHtmlText() {
        final AtomicReference<String> ref = new AtomicReference<String>();
        syncExec(new Runnable() {

            @Override
            public void run() {
                ref.set(swtbrowser.getBrowser().getText());
            }
        });
        return ref.get();
    }

    public Object executeJavaScript(final String js) {
        final AtomicReference<Object> ref = new AtomicReference<Object>();
        syncExec(new Runnable() {

            @Override
            public void run() {

                ref.set(swtbrowser.getBrowser().evaluate(js));
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
                // swtbrowser.doLayout();
                swtbrowser.getBrowser().setRedraw(false);
                swtbrowser.getBrowser().setVisible(false);
                // swtbrowser.setSize(swtbrowser.getSize());
                swtbrowser.getBrowser().setUrl(url);
                swtbrowser.getBrowser().setVisible(true);
                swtbrowser.getBrowser().setRedraw(true);

            }
        });

    }

    public void stop() {
        syncExec(new Runnable() {

            @Override
            public void run() {
                swtbrowser.getBrowser().stop();
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
                swtbrowser.getBrowser().setText(html, trusted);
            }
        });

    }

    public void setJavaScriptEnabled(final boolean b) {
        syncExec(new Runnable() {

            @Override
            public void run() {
                swtbrowser.getBrowser().setJavascriptEnabled(b);
            }
        });
    }

    public boolean isJavaScriptEnabled() {
        final AtomicBoolean ret = new AtomicBoolean(false);
        syncExec(new Runnable() {

            @Override
            public void run() {
                ret.set(swtbrowser.getBrowser().getJavascriptEnabled());
            }
        });
        return ret.get();
    }

    private void syncExec(Runnable runnable) {
        swtbrowser.getBrowser().getDisplay().syncExec(runnable);
    }

    private void asyncExec(Runnable runnable) {
        swtbrowser.getBrowser().getDisplay().asyncExec(runnable);
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
        final Browser newBrowser = new Browser(swtbrowser.getBrowser().getParent(), SWT.NONE);

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

}
