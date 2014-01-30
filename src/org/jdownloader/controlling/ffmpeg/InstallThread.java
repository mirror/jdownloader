package org.jdownloader.controlling.ffmpeg;

import java.io.File;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.updatev2.UpdateController;

public class InstallThread extends Thread {

    /**
     * 
     */
    private final FFmpegProvider fFmpegProvider;

    private long                 progress = -1;

    private boolean              success  = false;

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

        File path = scan();

        if (path == null) {
            ConfirmDialogInterface res = UIOManager.I().show(ConfirmDialogInterface.class, new FFMpegInstallTypeChooserDialog(task));
            if (res.getCloseReason() == CloseReason.OK) {
                UpdateController.getInstance().setGuiVisible(true);
                try {
                    UpdateController.getInstance().runExtensionInstallation("ffmpeg");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        path = scan();
        if (path != null) {
            FFmpeg ff = new FFmpeg();
            ff.setPath(path.getAbsolutePath());
            if (ff.validateBinary()) {
                JsonConfig.create(FFmpegSetup.class).setBinaryPath(path.getAbsolutePath());
                success = true;
            }
            synchronized (this.fFmpegProvider) {
                this.fFmpegProvider.installThread = null;
            }
        }

    }

    protected File scan() {
        File path = null;
        if (CrossSystem.isWindows()) {
            path = FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("tools/Windows/ffmpeg"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
            if (path == null) {
                path = FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("ffmpeg.exe"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
            }
            if (path == null) {

                String systemroot = System.getenv("SYSTEMROOT");
                if (StringUtils.isNotEmpty(systemroot)) {
                    path = FFMpegInstallTypeChooserDialog.searchFileIn(new File(systemroot, "System32/ffmpeg.exe"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
                    if (path == null && CrossSystem.is64BitOperatingSystem()) {
                        path = FFMpegInstallTypeChooserDialog.searchFileIn(new File(systemroot, "Syswow64/ffmpeg.exe"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);

                    }
                }

            }
            if (path == null) {
                String systemroot = System.getenv("WINDIR");
                if (StringUtils.isNotEmpty(systemroot)) {
                    path = FFMpegInstallTypeChooserDialog.searchFileIn(new File(systemroot, "System32/ffmpeg.exe"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
                    if (path == null && CrossSystem.is64BitOperatingSystem()) {
                        path = FFMpegInstallTypeChooserDialog.searchFileIn(new File(systemroot, "Syswow64/ffmpeg.exe"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);

                    }
                }

            }

        } else if (CrossSystem.isMac()) {
            // different ffmpeg version for 10.6-
            if (CrossSystem.getMacOSVersion() < 10600000) {
                path = FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("tools/mac/ffmpeg_10.5.x-/"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
            } else {
                path = FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("tools/mac/ffmpeg_10.6+/"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
            }

        } else {
            path = FFMpegInstallTypeChooserDialog.searchFileIn(Application.getResource("tools/linux/ffmpeg/"), JDGui.getInstance() == null ? null : JDGui.getInstance().getMainFrame(), true);
        }
        return path;
    }
}