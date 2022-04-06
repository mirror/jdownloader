package org.jdownloader.controlling.ffmpeg;

import java.io.File;
import java.util.Locale;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.UpdateController;

public class FFMpegInstallThread extends Thread {
    private static enum ExtensionPackages {
        MAC_BIG_SUR_AS("ffmpeg_as_11+") {
            @Override
            protected boolean isSupported() {
                // no pre compiled binaries available yet
                return false && CrossSystem.getOS().isMinimum(OperatingSystem.MAC_BIG_SUR) && CrossSystem.ARCHFamily.ARM.equals(CrossSystem.getARCHFamily());
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                return Application.getResource("tools/mac/" + extensionID + "/" + binaryName);
            }
        },
        MAC_YOSEMITE("ffmpeg_10.10+") {
            @Override
            protected boolean isSupported() {
                // intel package is compatible to apple silicon because of rosetta 2 support
                return CrossSystem.getOS().isMinimum(OperatingSystem.MAC_YOSEMITE);
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                return Application.getResource("tools/mac/" + extensionID + "/" + binaryName);
            }
        },
        MAC_SNOW_LEOPOARD("ffmpeg_10.6+") {
            @Override
            protected boolean isSupported() {
                return CrossSystem.getOS().isMinimum(OperatingSystem.MAC_SNOW_LEOPOARD);
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                return Application.getResource("tools/mac/" + extensionID + "/" + binaryName);
            }
        },
        MAC_LEOPOARD("ffmpeg_10.5.x-") {
            @Override
            protected boolean isSupported() {
                return CrossSystem.isMac() && ((!CrossSystem.getOS().isMinimum(OperatingSystem.MAC_SNOW_LEOPOARD) || !CrossSystem.is64BitOperatingSystem()));
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                return Application.getResource("tools/mac/" + extensionID + "/" + binaryName);
            }
        },
        LINUX("ffmpeg") {
            @Override
            protected boolean isSupported() {
                return CrossSystem.isLinux() && CrossSystem.ARCHFamily.X86.equals(CrossSystem.getARCHFamily());
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                return GENERIC.getBundledBinaryPath(binary);
            }
        },
        WINDOWS("ffmpeg") {
            @Override
            protected boolean isSupported() {
                return CrossSystem.isWindows() && CrossSystem.ARCHFamily.X86.equals(CrossSystem.getARCHFamily());
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                switch (CrossSystem.getARCHFamily()) {
                case X86:
                    if (CrossSystem.is64BitOperatingSystem()) {
                        final File x64Path = Application.getResource("tools/Windows/ffmpeg/x64/" + binaryName + ".exe");
                        if (x64Path.isFile()) {
                            return x64Path;
                        }
                    }
                    return Application.getResource("tools/Windows/ffmpeg/i386/" + binaryName + ".exe");
                default:
                    return null;
                }
            }
        },
        GENERIC(null) {
            @Override
            protected boolean isSupported() {
                return CrossSystem.isLinux() || CrossSystem.isBSD();
            }

            @Override
            protected File getBundledBinaryPath(BINARY binary) {
                final String binaryName = binary.name().toLowerCase(Locale.ENGLISH);
                if (CrossSystem.isLinux() || CrossSystem.isBSD()) {
                    final String os;
                    if (CrossSystem.isLinux()) {
                        os = "linux";
                    } else {
                        os = "bsd";
                    }
                    switch (CrossSystem.getARCHFamily()) {
                    case X86:
                        if (CrossSystem.is64BitOperatingSystem()) {
                            return Application.getResource("tools/" + os + "/ffmpeg/x64/" + binaryName);
                        } else {
                            return Application.getResource("tools/" + os + "/ffmpeg/i386/" + binaryName);
                        }
                    case ARM:
                        if (CrossSystem.is64BitOperatingSystem()) {
                            return Application.getResource("tools/" + os + "/ffmpeg/arm64/" + binaryName);
                        } else {
                            return Application.getResource("tools/" + os + "/ffmpeg/arm/" + binaryName);
                        }
                    case PPC:
                        if (CrossSystem.is64BitOperatingSystem()) {
                            return Application.getResource("tools/" + os + "/ffmpeg/ppc64/" + binaryName);
                        } else {
                            return Application.getResource("tools/" + os + "/ffmpeg/ppc/" + binaryName);
                        }
                    case RISCV:
                        if (CrossSystem.is64BitOperatingSystem()) {
                            return Application.getResource("tools/" + os + "/ffmpeg/riscv64/" + binaryName);
                        } else {
                            return Application.getResource("tools/" + os + "/ffmpeg/riscv32/" + binaryName);
                        }
                    default:
                        return null;
                    }
                }
                return null;
            }
        };
        protected abstract boolean isSupported();

        protected abstract File getBundledBinaryPath(BINARY binary);

        public static ExtensionPackages getExtension() {
            for (final ExtensionPackages extension : values()) {
                if (extension.isSupported()) {
                    return extension;
                }
            }
            return null;
        }

        protected final String extensionID;

        private ExtensionPackages(final String extensionID) {
            this.extensionID = extensionID;
        }
    }

    /**
     *
     */
    private final FFmpegProvider fFmpegProvider;
    private volatile long        progress = -1;
    private volatile boolean     success  = false;
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

    private boolean isFile(File file) {
        if (CrossSystem.isMac()) {
            // File.isFile may fail on MacOS
            return file != null && file.exists() && !file.isDirectory();
        } else {
            return file != null && file.isFile();
        }
    }

    public static String getFFmpegExtensionName() {
        final ExtensionPackages extension = ExtensionPackages.getExtension();
        return extension != null ? extension.extensionID : null;
    }

    @Override
    public void run() {
        try {
            final ExtensionPackages extension = ExtensionPackages.getExtension();
            File ffmpeg = extension != null ? extension.getBundledBinaryPath(BINARY.FFMPEG) : null;
            if (!isFile(ffmpeg)) {
                ffmpeg = null;
            }
            File ffprobe = extension != null ? extension.getBundledBinaryPath(BINARY.FFPROBE) : null;
            if (!isFile(ffprobe)) {
                ffprobe = null;
            }
            if (ffmpeg == null || ffprobe == null) {
                if (extension != null) {
                    final LogInterface logger = LogController.CL();
                    if (logger != null) {
                        logger.info("FFMpegInstallThread:" + extension + "|ExtensionID:" + extension.extensionID + "|FFmpeg:" + ffmpeg + "|FFprobe:" + ffprobe);
                    }
                    final String extensionID = extension.extensionID;
                    if (extensionID != null) {
                        final ConfirmDialogInterface res = UIOManager.I().show(ConfirmDialogInterface.class, new FFMpegInstallTypeChooserDialog(task));
                        if (res.getCloseReason() == CloseReason.OK) {
                            UpdateController.getInstance().setGuiVisible(true);
                            try {
                                if (UpdateController.getInstance().isExtensionInstalled(extensionID)) {
                                    UpdateController.getInstance().runExtensionUnInstallation(extensionID);
                                }
                                UpdateController.getInstance().runExtensionInstallation(extensionID);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            ffmpeg = extension != null ? extension.getBundledBinaryPath(BINARY.FFMPEG) : null;
            if (isFile(ffmpeg)) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPath(ffmpeg.getAbsolutePath());
            } else {
                ffmpeg = null;
            }
            ffprobe = extension != null ? extension.getBundledBinaryPath(BINARY.FFPROBE) : null;
            if (isFile(ffprobe)) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPathProbe(ffprobe.getAbsolutePath());
            } else {
                ffprobe = null;
            }
            success = ffmpeg != null && ffprobe != null;
        } finally {
            if (this.fFmpegProvider != null) {
                synchronized (this.fFmpegProvider) {
                    this.fFmpegProvider.installThread = null;
                }
            }
        }
    }
}