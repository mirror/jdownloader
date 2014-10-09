package org.jdownloader.swt.browser;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.utils.logging2.LogSource;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jdownloader.logging.LogController;

public class SWTDummyDisplayDispatcher {
    private static final SWTDummyDisplayDispatcher INSTANCE = new SWTDummyDisplayDispatcher();

    public static SWTDummyDisplayDispatcher getInstance() {
        return INSTANCE;
    }

    volatile private Display display = null;
    private LogSource        logger;

    public SWTDummyDisplayDispatcher() {
        logger = LogController.getInstance().getLogger("SWTDummyDisplayDispatcher");
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void onShutdown(ShutdownRequest shutdownRequest) {
                SWTDummyDisplayDispatcher.this.close();
            }
        });

    }

    protected void close() {
        synchronized (this) {
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();

                }

                thread = null;
            }
        }
    }

    public SwtBrowserCanvas createBrowser() {
        SwtBrowserCanvas ret = new SwtBrowserCanvas() {

            public void connect() {
                final SwtBrowserCanvas canvas = this;
                display.syncExec(new Runnable() {

                    @Override
                    public void run() {
                        final Shell shell = SWT_AWT.new_Shell(display, canvas);
                        shell.setLayout(new FillLayout());
                        Browser browser = new Browser(shell, SWT.NONE);
                        browser.setLayout(new FillLayout());
                        setBrowser(browser);

                        setShell(shell);
                        shell.open();
                    }
                });

            };
        };

        return ret;
    }

    private Thread thread;

    public void start() throws InterruptedException {
        synchronized (this) {

            if (thread == null) {
                thread = new Thread("SWTDispatcherThread") {
                    @Override
                    public void run() {

                        display = new Display();

                        synchronized (thread) {
                            thread.notifyAll();
                        }
                        try {

                            while (!isInterrupted()) {
                                if (!display.readAndDispatch()) {
                                    display.sleep();
                                }
                            }

                        } catch (Exception e) {
                            logger.log(e);
                            synchronized (SWTDummyDisplayDispatcher.this) {
                                thread = null;
                            }

                        }
                    }
                };
                thread.start();
                while (display == null) {

                    synchronized (thread) {

                        if (display == null) {

                            thread.wait(10000);

                        }
                    }
                }

                logger.info("SWTDisplayDispatcher Running");
            }
        }
    }

    public void ensureRunning() {
        try {
            SWTDummyDisplayDispatcher.getInstance().start();
        } catch (InterruptedException e) {
            logger.log(e);
        }
    }

}
