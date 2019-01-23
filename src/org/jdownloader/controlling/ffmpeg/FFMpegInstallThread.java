package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.Locale;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.updatev2.UpdateController;

public class FFMpegInstallThread extends Thread {
    public static final String   FFMPEG_GENRIC        = "ffmpeg";
    public static final String   FFMPEG_MAC_MIN_10_10 = "ffmpeg_10.10+";
    public static final String   FFMPEG_MAC_MIN_10_6  = "ffmpeg_10.6+";
    public static final String   FFMPEG_MAC_10_5_X    = "ffmpeg_10.5.x-";
    /**
     *
     */
    private final FFmpegProvider fFmpegProvider;
    private volatile long        progress             = -1;
    private volatile boolean     success              = false;
    private final String         task;

    public static enum BINARY {
        FFMPEG,
        FFPROBE
    }

    public FFMpegInstallThread(FFmpegProvider fFmpegProvider, String task) {
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
        try {
            File ffmpeg = getBundledBinaryPath(BINARY.FFMPEG);
            if (ffmpeg != null && !ffmpeg.exists()) {
                ffmpeg = null;
            }
            File ffprobe = getBundledBinaryPath(BINARY.FFPROBE);
            if (ffprobe != null && !ffprobe.exists()) {
                ffprobe = null;
            }
            if (ffmpeg == null || ffprobe == null) {
                switch (CrossSystem.getARCHFamily()) {
                case ARM:
                case IA64:
                case NA:
                case PPC:
                case SPARC:
                    // no pre compiled binaries available yet
                    return;
                default:
                    break;
                }
                switch (CrossSystem.getOSFamily()) {
                case BSD:
                case OS2:
                case OTHERS:
                    // no pre compiled binaries available yet
                    return;
                default:
                    break;
                }
                final ConfirmDialogInterface res = UIOManager.I().show(ConfirmDialogInterface.class, new FFMpegInstallTypeChooserDialog(task));
                if (res.getCloseReason() == CloseReason.OK) {
                    UpdateController.getInstance().setGuiVisible(true);
                    try {
                        UpdateController.getInstance().runExtensionInstallation(getFFmpegExtensionName());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            ffmpeg = getBundledBinaryPath(BINARY.FFMPEG);
            if (ffmpeg != null && ffmpeg.exists()) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPath(ffmpeg.getAbsolutePath());
            }
            ffprobe = getBundledBinaryPath(BINARY.FFPROBE);
            if (ffprobe != null && ffprobe.exists()) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPathProbe(ffprobe.getAbsolutePath());
            }
            if (ffmpeg != null && ffprobe != null && ffprobe.exists() && ffmpeg.exists()) {
                success = true;
            }
        } finally {
            synchronized (this.fFmpegProvider) {
                this.fFmpegProvider.installThread = null;
            }
        }
    }

    public static File getBundledBinaryPath(BINARY binary) {
        final String name = binary.name().toLowerCase(Locale.ENGLISH);
        if (CrossSystem.isWindows()) {
            if (CrossSystem.is64BitOperatingSystem()) {
                final File x64Path = Application.getResource("tools/Windows/ffmpeg/x64/" + name + ".exe");
                if (!x64Path.exists()) {
                    final File x32Path = Application.getResource("tools/Windows/ffmpeg/i386/" + name + ".exe");
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
            if (!CrossSystem.getOS().isMinimum(OperatingSystem.MAC_SNOW_LEOPOARD) || !CrossSystem.is64BitOperatingSystem()) {
                return Application.getResource("tools/mac/ffmpeg_10.5.x-/" + name);
            } else {
                if (CrossSystem.getOS().isMinimum(OperatingSystem.MAC_YOSEMITE)) {
                    return Application.getResource("tools/mac/ffmpeg_10.10+/" + name);
                } else {
                    return Application.getResource("tools/mac/ffmpeg_10.6+/" + name);
                }
            }
        } else if (CrossSystem.isLinux() || CrossSystem.isBSD()) {
            final String os;
            if (CrossSystem.isLinux()) {
                os = "linux";
            } else {
                os = "bsd";
            }
            switch (CrossSystem.getARCHFamily()) {
            case X86:
                if (CrossSystem.is64BitOperatingSystem()) {
                    return Application.getResource("tools/" + os + "/ffmpeg/x64/" + name);
                } else {
                    return Application.getResource("tools/" + os + "/ffmpeg/i386/" + name);
                }
            case ARM:
                if (CrossSystem.is64BitOperatingSystem()) {
                    return Application.getResource("tools/" + os + "/ffmpeg/arm64/" + name);
                } else {
                    return Application.getResource("tools/" + os + "/ffmpeg/arm/" + name);
                }
            case PPC:
                if (CrossSystem.is64BitOperatingSystem()) {
                    return Application.getResource("tools/" + os + "/ffmpeg/ppc64/" + name);
                } else {
                    return Application.getResource("tools/" + os + "/ffmpeg/ppc/" + name);
                }
            default:
                break;
            }
        }
        return null;
    }

    public static String getFFmpegExtensionName() {
        switch (CrossSystem.getOSFamily()) {
        case MAC:
            if (!CrossSystem.getOS().isMinimum(OperatingSystem.MAC_SNOW_LEOPOARD) || !CrossSystem.is64BitOperatingSystem()) {
                return FFMPEG_MAC_10_5_X;
            } else {
                if (CrossSystem.getOS().isMinimum(OperatingSystem.MAC_YOSEMITE)) {
                    return FFMPEG_MAC_MIN_10_10;
                } else {
                    return FFMPEG_MAC_MIN_10_6;
                }
            }
        default:
            return FFMPEG_GENRIC;
        }
    }
}