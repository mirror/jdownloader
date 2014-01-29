package org.jdownloader.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.util.List;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.plugins.DownloadLink;
import jd.plugins.download.raf.HashResult;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.jdserv.stats.StatsManagerConfig;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.tasks.PluginSubTask;
import org.jdownloader.remotecall.RemoteClient;
import org.jdownloader.statistics.interfaces.PluginStatsInterface;

public class StatsManager implements GenericConfigEventListener<Object>, DownloadWatchdogListener {
    private static final StatsManager INSTANCE = new StatsManager();

    /**
     * get the only existing instance of StatsManager. This is a singleton
     * 
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private PluginStatsInterface remote;
    private StatsManagerConfig   config;
    private Queue                queue;

    private long                 startTime;
    private LogSource            logger;

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access the only existing instance by using {@link #link()}.
     */
    private StatsManager() {
        remote = new LoggerRemoteClient(new RemoteClient("update3.jdownloader.org/stats")).create(PluginStatsInterface.class);
        config = JsonConfig.create(StatsManagerConfig.class);
        logger = LogController.getInstance().getLogger(StatsManager.class.getName());
        queue = new Queue("StatsManager Queue") {
        };
        DownloadWatchDog.getInstance().getEventSender().addListener(this);
        config._getStorageHandler().getKeyHandler("enabled").getEventSender().addListener(this);
    }

    /**
     * this setter does not set the config flag. Can be used to disable the logger for THIS session.
     * 
     * @param b
     */
    public void setEnabled(boolean b) {
        config.setEnabled(b);
    }

    public boolean isEnabled() {

        return config.isEnabled();
    }

    public static String getFingerprint(final File arg) {
        if (arg == null || !arg.exists() || arg.isDirectory()) { return null; }
        FileInputStream fis = null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
            // if (true) { throw new IOException("Any IOEXCeption"); }
            final byte[] b = new byte[32767];

            fis = new FileInputStream(arg);
            int n = 0;
            long total = 0;
            while ((n = fis.read(b)) >= 0 && total < 2 * 1024 * 1024) {
                if (n > 0) {
                    md.update(b, 0, n);
                }
                total += n;
            }
            // add random number. We cannot reference back from a pseudo fingerprint to a certain file this way.
            //
            md.update((byte) (Math.random() * 3));
        } catch (final Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                fis.close();
            } catch (final Throwable e) {
            }
        }
        final byte[] digest = md.digest();
        return HexFormatter.byteArrayToHex(digest);
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {

    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController) {
        try {
            DownloadLogEntry dl = new DownloadLogEntry();
            HTTPProxy proxy = downloadController.getCurrentProxy();
            HashResult hashResult = downloadController.getHashResult();
            long startedAt = downloadController.getStartTimestamp();
            DownloadLink link = downloadController.getDownloadLink();
            long downloadTime = link.getView().getDownloadTime();
            List<PluginSubTask> tasks = downloadController.getTasks();
            System.out.println(tasks);
            // DownloadInterface instance = link.getDownloadLinkController().getDownloadInstance();
            log(dl);
        } catch (Throwable e) {

        }

    }

    private void log(DownloadLogEntry dl) {
    }

}
