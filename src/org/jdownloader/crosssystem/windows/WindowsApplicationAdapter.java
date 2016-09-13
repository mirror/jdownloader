package org.jdownloader.crosssystem.windows;

import java.awt.BasicStroke;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicReference;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.JDownloaderMainFrame;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.extmanager.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.controlling.AggregatedNumbers;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.WindowsTaskBarProgressDisplay;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class WindowsApplicationAdapter {
    private static final WindowsApplicationAdapter INSTANCE = new WindowsApplicationAdapter();

    public static WindowsApplicationAdapter getInstance() {
        return WindowsApplicationAdapter.INSTANCE;
    }

    private WindowsApplicationAdapter() {
        initWin7PlusTaskbarExtension();
    }

    private void initWin7PlusTaskbarExtension() {
        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_7)) {
            SecondLevelLaunch.INIT_COMPLETE.executeWhenReached(new Runnable() {

                @Override
                public void run() {
                    CFG_GUI.WINDOWS_TASKBAR_PROGRESS_DISPLAY.getEventSender().addListener(new GenericConfigEventListener<Enum>() {

                        @Override
                        public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
                        }

                        @Override
                        public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                            if (WindowsTaskBarProgressDisplay.NOTHING.equals(newValue)) {
                                stopDockUpdater();
                            } else {
                                startDockUpdater();
                            }
                        }
                    });
                    final DownloadWatchdogListener listener;
                    DownloadWatchDog.getInstance().getEventSender().addListener(listener = new DownloadWatchdogListener() {

                        @Override
                        public void onDownloadWatchdogStateIsStopping() {
                        }

                        @Override
                        public void onDownloadWatchdogStateIsStopped() {
                        }

                        @Override
                        public void onDownloadWatchdogStateIsRunning() {
                            startDockUpdater();
                        }

                        @Override
                        public void onDownloadWatchdogStateIsPause() {
                            startDockUpdater();
                        }

                        @Override
                        public void onDownloadWatchdogStateIsIdle() {
                        }

                        @Override
                        public void onDownloadWatchdogDataUpdate() {
                        }

                        @Override
                        public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
                        }

                        @Override
                        public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {
                        }

                        @Override
                        public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
                        }
                    });
                    DownloadWatchDog.getInstance().notifyCurrentState(listener);
                }
            });
        }
    }

    private final AtomicReference<Thread> thread = new AtomicReference<Thread>(null);

    private void startDockUpdater() {
        if (CFG_GUI.CFG.getWindowsTaskbarProgressDisplay() != WindowsTaskBarProgressDisplay.NOTHING) {
            final Thread thread = new Thread("WindowsTaskbarProgress") {
                {
                    setDaemon(true);
                }

                public void run() {
                    double lastPercent = -1;
                    try {
                        final JDownloaderMainFrame mainFrame = JDGui.getInstance().getMainFrame();
                        while (WindowsApplicationAdapter.this.thread.get() == Thread.currentThread() && DownloadWatchDog.getInstance().isRunning()) {
                            final AggregatedNumbers aggn = new AggregatedNumbers(DownloadController.getInstance().getSelectionInfo());
                            final double percent;
                            final long totalBytes = aggn.getEnabledUnfinishedTotalBytes();
                            if (totalBytes > 0) {
                                final long loadedBytes = Math.max(0, aggn.getEnabledUnfinishedLoadedBytes());
                                percent = ((double) loadedBytes) / totalBytes;
                            } else {
                                percent = 0;
                            }
                            if (lastPercent != percent) {
                                lastPercent = percent;
                                if (CFG_GUI.CFG.getWindowsTaskbarProgressDisplay() == WindowsTaskBarProgressDisplay.TOTAL_PROGRESS_AND_CONNECTIONS) {
                                    final BufferedImage image = createImage(DownloadWatchDog.getInstance().getActiveDownloads());
                                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    Win7TaskBar.writeTransparentIcoImageWithSanselan(image, baos);
                                    final Object icon = Win7TaskBar.createIcon(baos.toByteArray());
                                    if (icon != null) {
                                        Win7TaskBar.setOverlayIcon(mainFrame, icon, true);
                                    }
                                }
                                Win7TaskBar.setProgress(mainFrame, lastPercent, true);
                            }
                            Thread.sleep(1000);
                        }
                    } catch (Throwable er) {
                        Log.log(er);
                    } finally {
                        if (WindowsApplicationAdapter.this.thread.compareAndSet(Thread.currentThread(), null)) {
                            reset();
                        }
                    }
                }
            };
            this.thread.set(thread);
            thread.start();
        }
    }

    protected BufferedImage createImage(int speed) {
        final String speedString = Integer.toString(speed);
        final Color fg = Color.WHITE;
        final Color bg = Color.GREEN.darker();
        final Font font = new Font(ImageProvider.getDrawFontName(), Font.BOLD, 24);
        final Canvas c = new Canvas();
        final FontMetrics fm = c.getFontMetrics(font);
        int stringwidth = Math.max(fm.getAscent(), fm.stringWidth(speedString)) + 6;
        final int w = stringwidth;
        final int h = stringwidth;
        final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(bg);
        g.fillOval(0, 0, w, h);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2));
        g.drawOval(0, 0, w - 1, h - 1);
        g.setColor(fg);
        final Rectangle2D bounds = g.getFontMetrics().getStringBounds(speedString, g);
        g.drawString(speedString, (int) (w - bounds.getWidth()) / 2, (int) (-bounds.getY() + (h - bounds.getHeight()) / 2));
        g.dispose();
        return image;
    }

    private void stopDockUpdater() {
        this.thread.set(null);
    }

    private void reset() {
        try {
            Win7TaskBar.setOverlayIcon(JDGui.getInstance().getMainFrame(), null, true);
            Win7TaskBar.hideProgress(JDGui.getInstance().getMainFrame());
        } catch (Throwable er) {
            Log.log(er);
        }
    }

}
