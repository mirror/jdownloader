package org.jdownloader.swt.browser;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.appwork.utils.os.CrossSystem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * A simple canvas that encapsulates a SWT Browser instance. Add it to a AWT or Swing container and call "connect()" after the container has
 * been made visible.
 */
public class SWTBrowser extends Canvas {
    static {
        if (CrossSystem.isWindows()) {
            org.eclipse.swt.internal.win32.OS.CoInternetSetFeatureEnabled(org.eclipse.swt.internal.win32.OS.FEATURE_DISABLE_NAVIGATION_SOUNDS, org.eclipse.swt.internal.win32.OS.SET_FEATURE_ON_PROCESS, true);
        }
        if (CrossSystem.isLinux()) {

            System.setProperty("sun.awt.xembedserver", "true");
        }
    }
    private Thread  swtThread;
    private Browser swtBrowser;

    /**
     * Connect this canvas to a SWT shell with a Browser component and starts a background thread to handle SWT events. This method waits
     * until the browser component is ready.
     * 
     * @throws Exception
     */
    public void connect() throws Exception {
        if (this.swtThread == null) {
            final Canvas canvas = this;
            // final Thread thisThread = Thread.currentThread();
            final AtomicReference<Exception> exception = new AtomicReference<Exception>();
            this.swtThread = new Thread("SWT Dispatcher") {

                @Override
                public void run() {
                    try {
                        Display display = new Display();
                        Shell shell = SWT_AWT.new_Shell(display, canvas);
                        shell.setLayout(new FillLayout());

                        synchronized (this) {
                            swtBrowser = new Browser(shell, SWT.NONE);
                            this.notifyAll();
                        }

                        shell.open();
                        while (!isInterrupted() && !shell.isDisposed()) {
                            if (!display.readAndDispatch()) {
                                display.sleep();
                            }
                        }
                        shell.dispose();
                        display.dispose();
                    } catch (Exception e) {
                        exception.set(e);

                    }
                }
            };
            this.swtThread.start();

            // Wait for the Browser instance to become ready
            synchronized (this.swtThread) {
                while (this.swtBrowser == null && exception.get() == null) {
                    try {
                        this.swtThread.wait(100);
                    } catch (InterruptedException e) {
                        this.swtBrowser = null;
                        this.swtThread = null;
                        break;
                    }
                }
            }
            if (exception.get() != null) {
                throw exception.get();
            }
        }
    }

    /**
     * Returns the Browser instance. Will return "null" before "connect()" or after "disconnect()" has been called.
     */
    public Browser getBrowser() {
        return this.swtBrowser;
    }

    /**
     * Stops the swt background thread.
     */
    public void disconnect() {
        if (swtThread != null) {
            swtBrowser = null;
            swtThread.interrupt();
            swtThread = null;
        }
    }

    /**
     * Ensures that the SWT background thread is stopped if this canvas is removed from it's parent component (e.g. because the frame has
     * been disposed).
     */
    @Override
    public void removeNotify() {
        super.removeNotify();
        disconnect();
    }

    /**
     * Opens a new JFrame with BrowserCanvas in it
     */
    public static void main(String[] args) {
        // Required for Linux systems
        System.setProperty("sun.awt.xembedserver", "true");

        // Create container canvas. Note that the browser
        // widget will not be created, yet.
        final SWTBrowser browserCanvas = new SWTBrowser();
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
        try {
            browserCanvas.connect();

            // Now we can open a webpage, but remember that we have
            // to use the SWT thread for this.
            browserCanvas.getBrowser().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    browserCanvas.getBrowser().setUrl("http://installer.jdownloader.org/ads.html");
                    browserCanvas.doLayout();
                    browserCanvas.setVisible(true);
                    browserCanvas.setSize(browserCanvas.getSize());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}