package org.jdownloader.controlling.ffmpeg;

import java.io.File;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.MacOsXVersion;
import org.jdownloader.updatev2.UpdateController;

public class InstallThread extends Thread {

    public static final String   FFMPEG        = "ffmpeg";

    public static final String   FFMPEG_10_6   = "ffmpeg_10.6+";

    public static final String   FFMPEG_10_5_X = "ffmpeg_10.5.x-";

    /**
     * 
     */
    private final FFmpegProvider fFmpegProvider;

    private long                 progress      = -1;

    private boolean              success       = false;

    private String               task;

    public InstallThread(FFmpegProvider fFmpegProvider, String task) {
        this.fFmpegProvider = fFmpegProvider;
        this.task = task;
    }

    public boolean isSuccessFul() {
        return success;
    }

    public long getProgress() {
        return progress;
    }

    @Override
    public void run() {

        File ffmpeg = getFFmpegPath("ffmpeg");
        if (ffmpeg != null && !ffmpeg.exists()) {
            ffmpeg = null;
        }
        File ffprobe = getFFmpegPath("ffprobe");
        if (ffprobe != null && !ffprobe.exists()) {
            ffprobe = null;
        }
        if (ffmpeg == null || ffprobe == null) {
            ConfirmDialogInterface res = UIOManager.I().show(ConfirmDialogInterface.class, new FFMpegInstallTypeChooserDialog(task));
            if (res.getCloseReason() == CloseReason.OK) {
                UpdateController.getInstance().setGuiVisible(true);
                try {

                    UpdateController.getInstance().runExtensionInstallation(getFFmpegExtensionName());

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ffmpeg = getFFmpegPath("ffmpeg");
        if (ffmpeg.exists()) {
            JsonConfig.create(FFmpegSetup.class).setBinaryPath(ffmpeg.toString());
        }
        ffprobe = getFFmpegPath("ffprobe");
        if (ffprobe.exists()) {
            JsonConfig.create(FFmpegSetup.class).setBinaryPathProbe(ffprobe.toString());
        }
        if (ffprobe.exists() && ffmpeg.exists()) {
            success = true;
        }
        synchronized (this.fFmpegProvider) {
            this.fFmpegProvider.installThread = null;
        }

    }

    private File getFFmpegPath(String name) {

        if (CrossSystem.isWindows()) {
            if (CrossSystem.is64BitOperatingSystem()) {
                File x64Path = Application.getResource("tools/Windows/ffmpeg/x64/" + name + ".exe");
                if (!x64Path.exists()) {
                    File x32Path = Application.getResource("tools/Windows/ffmpeg/i386/" + name + ".exe");
                    if (x32Path.exists()) {
                        return x32Path;
                    }
                }
                return x64Path;
            } else {
                return Application.getResource("tools/Windows/ffmpeg/i386/" + name + ".exe");
            }
        } else if (CrossSystem.isMac()) {
            // different ffmpeg version for 10.6-
            if (CrossSystem.getMacOSVersion() < MacOsXVersion.MAC_OSX_10p6_SNOW_LEOPARD.getVersionID() || !CrossSystem.is64BitOperatingSystem()) {
                return Application.getResource("tools/mac/ffmpeg_10.5.x-/" + name + "");
            } else {
                return Application.getResource("tools/mac/ffmpeg_10.6+/" + name + "");
            }

        } else {
            if (CrossSystem.is64BitOperatingSystem()) {
                return Application.getResource("tools/linux/ffmpeg/x64/" + name + "");
            } else {
                return Application.getResource("tools/linux/ffmpeg/i386/" + name + "");
            }

        }

    }

    public static String getFFmpegExtensionName() {
        switch (CrossSystem.getOSFamily()) {

        case MAC:
            if (CrossSystem.getMacOSVersion() < MacOsXVersion.MAC_OSX_10p6_SNOW_LEOPARD.getVersionID()) {
                return FFMPEG_10_5_X;
            } else {
                return FFMPEG_10_6;
            }

        default:
            return FFMPEG;
        }

    }

}