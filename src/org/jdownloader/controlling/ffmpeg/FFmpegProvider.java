package org.jdownloader.controlling.ffmpeg;

import java.io.File;

import jd.gui.swing.laf.LookAndFeelController;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;

public class FFmpegProvider {
    private static final FFmpegProvider INSTANCE = new FFmpegProvider();

    /**
     * get the only existing instance of FFmpegProvider. This is a singleton
     * 
     * @return
     */
    public static FFmpegProvider getInstance() {
        return FFmpegProvider.INSTANCE;
    }

    private boolean       installing = false;
    private InstallThread installThread;

    /**
     * Create a new instance of FFmpegProvider. This is a singleton class. Access the only existing instance by using {@link #getInstance()}
     * .
     */
    private FFmpegProvider() {

    }

    public class InstallThread extends Thread {

        private long    progress = -1;

        private boolean success  = false;

        public boolean isSuccessFul() {
            return success;
        }

        public long getProgress() {
            return progress;
        }

        @Override
        public void run() {

            File path = null;
            // FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("tmp").getParentFile(), JDGui.getInstance() == null ?
            // null : JDGui.getInstance().getMainFrame(), true);

            if (path == null) {
                FFMPegInstallTypeChooserDialogInterface res = UIOManager.I().show(FFMPegInstallTypeChooserDialogInterface.class, new FFMpegInstallTypeChooserDialog());
                path = new File(res.getFFmpegBinaryPath());
            }
            FFmpeg ff = new FFmpeg();
            ff.setPath(path.getAbsolutePath());
            if (ff.validateBinary()) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPath(path.getAbsolutePath());
                success = true;
            }
            synchronized (FFmpegProvider.this) {
                installThread = null;
            }

        }
    }

    public static void main(String[] args) {
        try {
            Application.setApplication(".jd_home");
            LookAndFeelController.getInstance().init();
            getInstance().install(new FFMpegInstallProgress());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void install(FFMpegInstallProgress progress) throws InterruptedException {

        synchronized (this) {
            if (installThread == null) {
                installThread = new InstallThread();
                installThread.start();
            }
        }

        while (installThread != null && installThread.isAlive()) {

            progress.updateValues(installThread.getProgress(), 100);
            Thread.sleep(1000);
        }

    }
}
