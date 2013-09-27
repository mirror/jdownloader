package org.jdownloader.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.event.queue.Queue;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.jdserv.stats.StatsManagerConfig;
import org.jdownloader.logging.LogController;
import org.jdownloader.remotecall.RemoteClient;
import org.jdownloader.statistics.interfaces.StatisticsInterface;

public class StatsManager implements GenericConfigEventListener<Object> {
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
     * Create a new instance of StatsManager. This is a singleton class. Access the only existing instance by using {@link #link()}.
     */
    private StatsManager() {
        remote = new LoggerRemoteClient(new RemoteClient("update3.jdownloader.org/stats")).create(StatisticsInterface.class);
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
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                logExit();
            }
        });
        logEnabled();
        config._getStorageHandler().getKeyHandler("enabled").getEventSender().addListener(this);
    }

    private synchronized void logEnabled() {

        queue.add(new AsynchLogger() {

            @Override
            public void doRemoteCall() {

                remote.enabled(isEnabled());

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
        return false;
        // return config.isEnabled();
    }

    public void onFileDownloaded(File outputCompleteFile, final DownloadLink downloadLink, final long speedBytePS, final long startDelay, final int chunks) {
        if (!isEnabled()) return;
        try {
            final String fp = getFingerprint(outputCompleteFile);
            PluginForHost plg = downloadLink.getLivePlugin();
            final long size = outputCompleteFile.length();
            final boolean accountUsed = downloadLink.getDownloadLinkController().getAccount() != null;
            final long plgVersion = plg.getVersion();
            final String plgHost = plg.getHost();
            final String linkHost = downloadLink.getDomainInfo(true).getTld();

            queue.add(new AsynchLogger() {
                @Override
                public void doRemoteCall() {
                    if (!isEnabled()) return;

                    remote.onDownload(plgHost, linkHost, plgVersion, accountUsed, size, fp, speedBytePS, chunks);

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

    @Override
    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {

        logEnabled();
    }

    public void trackAdvancedOptionChange(final KeyHandler<?> keyHandler) {

        queue.add(new AsynchLogger() {
            @Override
            public void doRemoteCall() {
                if (!isEnabled()) return;
                if (Clazz.isEnum(keyHandler.getRawType())) {
                    remote.onAdvancedOptionUpdate(keyHandler.getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + "." + keyHandler.getKey(), keyHandler.getValue() + "");
                } else if (Clazz.isString(keyHandler.getRawType())) {
                    remote.onAdvancedOptionUpdate(keyHandler.getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + "." + keyHandler.getKey(), null);
                } else if (Clazz.isPrimitive(keyHandler.getRawType()) || Clazz.isPrimitiveWrapper(keyHandler.getRawType())) {
                    remote.onAdvancedOptionUpdate(keyHandler.getStorageHandler().getConfigInterface().getSimpleName().replace("Config", "") + "." + keyHandler.getKey(), keyHandler.getValue() + "");
                }

            }

        });
    }

}
