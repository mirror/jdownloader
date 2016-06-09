package org.jdownloader.phantomjs.installation;

import java.io.File;

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
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.http.download.DownloadClient;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.phantomjs.PhantomJS;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.http.Browser;

public class InstallThread extends Thread {

    private static final String PHANTOM_JS_INSTALL_CHECK = "PHANTOM_JS_INSTALL_CHECK";

    private boolean             success                  = false;

    private String              task;

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
            installing = false;
            downloading = true;

            br = new DownloadClient(new Browser());
            br.setProgressCallback(this.downloadProgress = new DownloadProgress());
            File file = Application.getTempResource("download/phantomjs_" + CrossSystem.getOS().name() + "_.zip");
            file.getParentFile().mkdirs();

            br.setOutputFile(file);
            boolean tryIt = true;
            boolean tryToResume = file.exists();
            File extractTo = new File(file.getAbsolutePath() + "_extracted");
            String url = null;
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-windows.zip";

                break;
            case MAC:
                url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-macosx.zip";
                new ZipIOReader(file).extractTo(extractTo);
                break;
            case LINUX:
                if (CrossSystem.is64BitArch()) {
                    url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-x86_64.tar.bz2";
                } else {
                    url = "https://bitbucket.org/ariya/phantomjs/downloads/phantomjs-2.1.1-linux-i686.tar.bz2";
                }
                break;
            }
            if (url == null) {
                return;
            }
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
            downloading = false;
            installing = true;
            File dest = new PhantomJS().getBinaryPath();
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                new ZipIOReader(file).extractTo(extractTo);
                File exe = new File(extractTo, "\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");

                dest.getParentFile().mkdirs();
                exe.renameTo(dest);
                break;
            case MAC:
                new ZipIOReader(file).extractTo(extractTo);
                File app = new File(extractTo, "\\phantomjs-2.1.1-windows\\bin\\phantomjs.exe");

                dest.getParentFile().mkdirs();
                app.renameTo(dest);
                break;
            case LINUX:

                break;
            }

            Files.deleteRecursiv(extractTo);
            file.delete();
            installing = false;

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
        if (downloading && getTotalProgress() >= 0) {
            return _GUI.T.phantom_downloading_status(SizeFormatter.formatBytes(br.getSpeedInBps()), getTotalProgress());

        }
        if (installing) {
            return _GUI.T.phantom_installation_status();
        }
        return null;
    }

}