package org.jdownloader.controlling.ffmpeg;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.utils.Application;
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

    private boolean   installing = false;
    InstallThread     installThread;
    private LogSource logger;

    /**
     * Create a new instance of FFmpegProvider. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     */
    private FFmpegProvider() {
        logger = LogController.getInstance().getLogger(FFmpeg.class.getName());
    }

    public static void main(String[] args) {
        try {
            Application.setApplication(".jd_home");
            LookAndFeelController.getInstance().init();
            getInstance().install(new FFMpegInstallProgress(), "test");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void install(FFMpegInstallProgress progress, String task) throws InterruptedException {
        // we do not want to ask twice on one session
        if (DownloadWatchDog.getInstance().getSession().getBooleanProperty(FFMPEG_INSTALL_CHECK, false)) return;
        DownloadWatchDog.getInstance().getSession().setProperty(FFMPEG_INSTALL_CHECK, true);
        synchronized (this) {
            if (installThread == null || !installThread.isAlive()) {
                installThread = new InstallThread(this, task);
                installThread.start();
                logger.info("Started Install process");
            }
        }

        while (installThread != null && installThread.isAlive()) {

            progress.updateValues(installThread.getProgress(), 100);
            Thread.sleep(1000);
        }
        logger.info("Ended Install process");
    }
}
