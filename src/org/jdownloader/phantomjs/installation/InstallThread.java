package org.jdownloader.phantomjs.installation;

import java.io.File;
import java.net.URL;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.net.DownloadProgress;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.MessageDialogImpl;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.appwork.utils.swing.dialog.ProgressInterface;
import org.appwork.utils.zip.ZipIOReader;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.http.download.DownloadClient;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.phantomjs.PhantomJS;

public class InstallThread extends Thread {

    private static final String PHANTOM_JS_INSTALL_CHECK = "PHANTOM_JS_INSTALL_CHECK";

    private boolean             success                  = false;

    private final String        task;

    private boolean             downloading;

    public boolean isDownloading() {
        return downloading;
    }

    public boolean isInstalling() {
        return installing;
    }

    public DownloadProgress getDownloadProgress() {
        return downloadProgress;
    }

    private boolean          installing;

    private DownloadProgress downloadProgress;

    private DownloadClient   br;

    public InstallThread(String task) {
        this.task = task;
    }

    public boolean isSuccessFul() {
        return success;
    }

    @Override
    public void run() {
        try {
            br = new DownloadClient(new Browser());
            br.setProgressCallback(this.downloadProgress = new DownloadProgress());
            final String url;
            final String binaryPath;
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-windows.zip";
                binaryPath = "\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe";
                break;
            case MAC:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-macosx.zip";
                binaryPath = "/phantomjs-2.1.1-macosx/bin/phantomjs";
                break;
            case LINUX:
                switch (CrossSystem.getARCHFamily()) {
                case X86:
                    if (CrossSystem.is64BitArch()) {
                        url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2";
                        binaryPath = "/phantomjs-2.1.1-linux-x86_64/bin/phantomjs";
                    } else {
                        url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-i686.tar.bz2";
                        binaryPath = "/phantomjs-2.1.1-linux-i686/bin/phantomjs";
                    }
                    break;
                default:
                    return;
                }
                break;
            default:
                return;
            }
            final String fileName = Plugin.getFileNameFromURL(new URL(url));
            final File file = Application.getTempResource("download/" + fileName);
            file.getParentFile().mkdirs();
            final File extractTo = new File(file.getAbsolutePath() + "_extracted");
            try {
                br.setOutputFile(file);
                boolean tryIt = true;
                boolean tryToResume = file.exists();
                downloading = true;
                while (tryToResume || tryIt) {
                    try {
                        tryIt = false;
                        if (!file.exists() || file.length() == 0) {
                            tryToResume = false;
                        }
                        br.download(url);
                        break;
                    } catch (Throwable e) {
                        e.printStackTrace();
                        if (tryToResume) {
                            tryToResume = true;
                            br.getOutputStream().close();
                            file.delete();
                            br.setOutputFile(file);
                        }
                    }
                }
            } finally {
                downloading = false;
            }
            try {
                installing = true;
                final File dest = new PhantomJS().getBinaryPath();
                dest.getParentFile().mkdirs();
                final File binarySource = new File(extractTo, binaryPath);
                if (extractTo.exists()) {
                    Files.deleteRecursiv(extractTo);
                }
                switch (CrossSystem.getOSFamily()) {
                case WINDOWS:
                case MAC:
                    new ZipIOReader(file).extractTo(extractTo);
                    break;
                case LINUX:
                    final LazyExtension extension = ExtensionController.getInstance().getExtension(ExtractionExtension.class);
                    if (extension != null && extension._isEnabled()) {
                        final ExtractionExtension extraction = (ExtractionExtension) extension._getExtension();
                        final Archive archive = extraction.buildArchive(new FileArchiveFactory(file));
                        archive.getSettings().setExtractPath(extractTo.getAbsolutePath());
                        final ExtractionController controller = extraction.addToQueue(archive, false);
                        try {
                            while (!controller.isFinished()) {
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            controller.kill();
                            throw e;
                        }
                    }
                    break;
                }
                if (binarySource.exists()) {
                    if (binarySource.renameTo(dest)) {
                        dest.setExecutable(true);
                    }
                    dest.setExecutable(true);
                }
            } finally {
                installing = false;
                file.delete();
                Files.deleteRecursiv(extractTo);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static void install(final InstallProgress progress, String task) throws InterruptedException {
        switch (CrossSystem.getOSFamily()) {
        case LINUX:

            switch (CrossSystem.getARCHFamily()) {
            case ARM:
            case PPC:
            case SPARC:
                return;

            }
        }
        final InstallThread thread = new InstallThread(task);
        synchronized (DownloadWatchDog.getInstance()) {
            if (DownloadWatchDog.getInstance().getSession().getBooleanProperty(PHANTOM_JS_INSTALL_CHECK, false)) {
                return;
            }
            DownloadWatchDog.getInstance().getSession().setProperty(PHANTOM_JS_INSTALL_CHECK, true);
            try {
                UIOManager.I().show(ConfirmDialogInterface.class, new InstallTypeChooserDialog(_GUI.T.phantomjs_usage())).throwCloseExceptions();
                ProgressGetter pg = new org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter() {

                    @Override
                    public int getProgress() {
                        return (int) thread.getTotalProgress();
                    }

                    @Override
                    public String getString() {
                        return thread.getStatusString();
                    }

                    @Override
                    public void run() throws Exception {
                        try {
                            thread.start();

                            while (thread != null && thread.isAlive()) {
                                if (progress != null) {

                                    progress.updateValues(thread.getTotalProgress(), 100);
                                }
                                thread.join(1000);
                            }
                        } finally {
                            System.out.println(1);
                        }
                    }

                    @Override
                    public String getLabelString() {
                        return thread.getStatusString();
                    }

                };
                ProgressDialog p = new ProgressDialog(pg, 0, _GUI.T.lit_installation(), _GUI.T.phantomjs_installation_message(), new AbstractIcon(IconKey.ICON_LOGO_PHANTOMJS_LOGO, 32), null, null);
                UIOManager.I().show(ProgressInterface.class, p);
                if (new PhantomJS().isAvailable()) {
                    UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(0, _GUI.T.lit_installation(), _GUI.T.phantomjs_installation_message_success(), new AbstractIcon(IconKey.ICON_LOGO_PHANTOMJS_LOGO, 32), _GUI.T.lit_continue()));
                } else {
                    UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(0, _GUI.T.lit_installation(), _GUI.T.phantomjs_installation_message_failed(), new AbstractIcon(IconKey.ICON_ERROR, 32), _GUI.T.lit_continue()));

                }
            } catch (DialogClosedException e) {
                e.printStackTrace();
            } catch (DialogCanceledException e) {
                e.printStackTrace();
            }
        }

    }

    protected long getTotalProgress() {
        if (isDownloading() && getDownloadProgress().getTotal() > 0) {
            return (int) ((getDownloadProgress().getLoaded() * 100) / getDownloadProgress().getTotal()) - 1;
        }
        return -1;
    }

    protected String getStatusString() {
        if (isDownloading()) {
            final long progress = getTotalProgress();
            if (progress >= 0) {
                return _GUI.T.phantom_downloading_status(SizeFormatter.formatBytes(br.getSpeedInBps()), progress);
            } else {
                return null;
            }
        } else if (isInstalling()) {
            return _GUI.T.phantom_installation_status();
        } else {
            return null;
        }
    }

}