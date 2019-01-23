package org.jdownloader.controlling.ffmpeg;

import jd.controlling.downloadcontroller.DownloadWatchDog;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class FFmpegProvider {
    public static final String          FFMPEG_INSTALL_CHECK = "FFMPEG_INSTALL_CHECK";
    private static final FFmpegProvider INSTANCE             = new FFmpegProvider();

    /**
     * get the only existing instance of FFmpegProvider. This is a singleton
     *
     * @return
     */
    public static FFmpegProvider getInstance() {
        return FFmpegProvider.INSTANCE;
    }

    protected volatile FFMpegInstallThread installThread;
    private final LogSource                logger;

    /**
     * Create a new instance of FFmpegProvider. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     */
    private FFmpegProvider() {
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
    }

    private final static Object LOCK = new Object();

    public void install(final FFMpegInstallProgress progress, String task) throws InterruptedException {
        synchronized (LOCK) {
            // we do not want to ask twice on one session
            if (DownloadWatchDog.getInstance().getSession().getBooleanProperty(FFMPEG_INSTALL_CHECK, false)) {
                return;
            } else {
                DownloadWatchDog.getInstance().getSession().setProperty(FFMPEG_INSTALL_CHECK, true);
                FFMpegInstallThread thread = installThread;
                if (thread == null || !thread.isAlive()) {
                    thread = new FFMpegInstallThread(this, task);
                    this.installThread = thread;
                    thread.start();
                    logger.info("Started Install process");
                }
                while (true) {
                    thread = installThread;
                    if (thread == null || !thread.isAlive()) {
                        break;
                    }
                    if (progress != null) {
                        progress.updateValues(thread.getProgress(), 100);
                    }
                    Thread.sleep(1000);
                }
                logger.info("Ended Install process");
            }
        }
    }
}
