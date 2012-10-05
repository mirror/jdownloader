package org.jdownloader.jdserv.stats;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

public class StatsManager {
    private static final StatsManager INSTANCE = new StatsManager();

    /**
     * get the only existing instance of StatsManager. This is a singleton
     * 
     * @return
     */
    public static StatsManager I() {
        return StatsManager.INSTANCE;
    }

    private StatisticsInterface remote;
    private StatsManagerConfig  config;
    private Queue               queue;

    private long                startTime;
    private LogSource           logger;

    /**
     * Create a new instance of StatsManager. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private StatsManager() {
        remote = new LoggerRemoteClient(org.jdownloader.jdserv.JD_SERV_CONSTANTS.CLIENT).create(StatisticsInterface.class);
        config = JsonConfig.create(StatsManagerConfig.class);
        logger = LogController.getInstance().getLogger(StatsManager.class.getName());
        queue = new Queue("StatsManager Queue") {
        };

        logStart();
        boolean fresh = false;
        if (fresh) {
            logFreshInstall();
        }
        startTime = System.currentTimeMillis();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                logExit();
            }
        });

    }

    protected void logExit() {

    }

    private void logFreshInstall() {

    }

    public void logAction(String key) {

    }

    private void logStart() {

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

    public void onFileDownloaded(File outputCompleteFile, final DownloadLink downloadLink) {
        if (!isEnabled()) return;
        try {
            final String fp = getFingerprint(outputCompleteFile);
            PluginForHost plg = downloadLink.getLivePlugin();
            final long size = outputCompleteFile.length();
            final boolean accountUsed = downloadLink.getDownloadLinkController().getAccount() != null;
            final long plgVersion = plg.getVersion();
            final String plgHost = plg.getHost();
            final String linkHost = downloadLink.getDomainInfo().getTld();
            queue.add(new AsynchLogger() {
                @Override
                public void doRemoteCall() {
                    if (!isEnabled()) return;
                    String nID = remote.onDownload(plgHost, linkHost, plgVersion, accountUsed, size, fp);
                }

            });
        } catch (Throwable e) {

        }
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

}
