package org.jdownloader.phantomjs.installation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
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
import org.jdownloader.logging.LogController;
import org.jdownloader.phantomjs.PhantomJS;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.statistics.StatsManager.CollectionName;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.http.Browser;
import jd.plugins.Plugin;

public class InstallThread extends Thread {

    private static final String PHANTOM_JS_INSTALL_CHECK = "PHANTOM_JS_INSTALL_CHECK";

    private boolean             success                  = false;

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
    }

    public boolean isSuccessFul() {
        return success;
    }

    @Override
    public void run() {
        try {
            StatsManager.I().track("installing/start", CollectionName.PJS);
            br = new DownloadClient(new Browser());
            br.setProgressCallback(this.downloadProgress = new DownloadProgress());
            final String url;
            final String binaryPath;
            final String sha256;
            final long size;
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-windows.zip";
                binaryPath = "\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe";
                sha256 = "d9fb05623d6b26d3654d008eab3adafd1f6350433dfd16138c46161f42c7dcc8";
                size = 18193653;
                break;
            case MAC:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-macosx.zip";
                binaryPath = "/phantomjs-2.1.1-macosx/bin/phantomjs";
                sha256 = "538cf488219ab27e309eafc629e2bcee9976990fe90b1ec334f541779150f8c1";
                size = 17148816;
                break;
            case LINUX:
                switch (CrossSystem.getARCHFamily()) {
                case X86:
                    if (CrossSystem.is64BitArch()) {
                        url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2";
                        binaryPath = "/phantomjs-2.1.1-linux-x86_64/bin/phantomjs";
                        sha256 = "86dd9a4bf4aee45f1a84c9f61cf1947c1d6dce9b9e8d2a907105da7852460d2f";
                        size = 23415665;
                    } else {
                        url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-i686.tar.bz2";
                        binaryPath = "/phantomjs-2.1.1-linux-i686/bin/phantomjs";
                        sha256 = "80e03cfeb22cc4dfe4e73b68ab81c9fdd7c78968cfd5358e6af33960464f15e3";
                        size = 24144933;
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
                StatsManager.I().track("installing/downloadstart", CollectionName.PJS);
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
            StatsManager.I().track("installing/downloadend", CollectionName.PJS);
            try {
                if (file.length() != size) {
                    throw new IOException("File size missmatch! (" + file.length() + "!=" + size + ")");
                }
                final String fileSha256 = Hash.getSHA256(file);
                if (!sha256.equalsIgnoreCase(fileSha256)) {
                    throw new IOException("File sha256 missmatch!");
                }
                installing = true;
                StatsManager.I().track("installing/extractStart", CollectionName.PJS);
                final File dest = new PhantomJS().getBinaryPath();
                dest.getParentFile().mkdirs();
                dest.delete();
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
                        extraction.addToQueue(archive, false);
                        try {
                            boolean wait = true;
                            while (wait) {
                                wait = false;
                                for (final ExtractionController job : extraction.getJobQueue().getJobs()) {
                                    if (archive == job.getArchive()) {
                                        wait = true;
                                        break;
                                    } else if (archive == job.getArchive().getParentArchive()) {
                                        wait = true;
                                        break;
                                    }
                                }
                                if (!wait) {
                                    Thread.sleep(1000);
                                    for (final ExtractionController job : extraction.getJobQueue().getJobs()) {
                                        if (archive == job.getArchive()) {
                                            wait = true;
                                            break;
                                        } else if (archive == job.getArchive().getParentArchive()) {
                                            wait = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            throw e;
                        }
                    }
                    break;
                }
                if (binarySource.exists()) {
                    if (binarySource.renameTo(dest)) {
                        dest.setExecutable(true);
                    }
                }
            } finally {
                StatsManager.I().track("installing/extractEnd", CollectionName.PJS);
                StatsManager.I().track("installing/result/" + new PhantomJS().isAvailable(), CollectionName.PJS);
                installing = false;
                file.delete();
                if (extractTo.exists()) {
                    Files.deleteRecursiv(extractTo);
                }
            }
        } catch (Throwable e) {
            LogController.CL(false).log(e);
            StatsManager.I().trackException(0, null, e, "installing/", CollectionName.PJS);
        }
    }

    public static synchronized void install(final InstallProgress progress, String task) throws InterruptedException {

        switch (CrossSystem.getOSFamily()) {
        case LINUX:
            switch (CrossSystem.getARCHFamily()) {
            case ARM:
            case PPC:
            case SPARC:
                HashMap<String, String> infos = new HashMap<String, String>();
                infos.put("arch", CrossSystem.getARCHFamily().name());
                StatsManager.I().track(0, null, "install/osfail", infos, CollectionName.PJS);

                return;
            }
        }
        final InstallThread thread = new InstallThread(task);
        if (DownloadWatchDog.getInstance().getSession().getBooleanProperty(PHANTOM_JS_INSTALL_CHECK, false)) {
            return;
        }
        StatsManager.I().track("install/started", CollectionName.PJS);
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
                StatsManager.I().track("install/success", CollectionName.PJS);
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