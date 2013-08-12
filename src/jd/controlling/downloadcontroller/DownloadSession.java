package jd.controlling.downloadcontroller;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.NullsafeAtomicReference;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.settings.IfFileExistsAction;

public class DownloadSession {

    public static enum STOPMARK {
        /* no stopmark is set */
        NONE,
        /*
         * stopmark is set but no longer visible, eg link/package removed from list
         */
        HIDDEN,
        /* to set a random stopmark */
        RANDOM;
    }

    private long                                  created;
    private HashMap<Object, IfFileExistsAction>   fileExistsActions;
    private FileAccessManager                     fileAccessManager;
    private static final FileAccessManager        FILE_ACCESS_MANAGER = new FileAccessManager();

    private final NullsafeAtomicReference<Object> stopMark            = new NullsafeAtomicReference<Object>(STOPMARK.NONE);
    private final AtomicInteger                   downloadsStarted    = new AtomicInteger(0);
    private final AtomicInteger                   skipCounter         = new AtomicInteger(0);
    private final AtomicBoolean                   activateForcedOnly  = new AtomicBoolean(true);

    public AtomicBoolean getActivateForcedOnly() {
        return activateForcedOnly;
    }

    public AtomicInteger getSkipCounter() {
        return skipCounter;
    }

    public NullsafeAtomicReference<Integer> getSpeedLimitBeforePause() {
        return speedLimitBeforePause;
    }

    public NullsafeAtomicReference<Boolean> getSpeedLimitedBeforePause() {
        return speedLimitedBeforePause;
    }

    private final AtomicBoolean                      stopAfterForcedLinks    = new AtomicBoolean(false);

    private final NullsafeAtomicReference<Integer>   speedLimitBeforePause   = new NullsafeAtomicReference<Integer>(null);
    private final NullsafeAtomicReference<Boolean>   speedLimitedBeforePause = new NullsafeAtomicReference<Boolean>(null);

    private final CopyOnWriteArrayList<DownloadLink> forcedLinks             = new CopyOnWriteArrayList<DownloadLink>();
    private CopyOnWriteArrayList<DownloadLink>       activationRequests      = new CopyOnWriteArrayList<DownloadLink>();

    public void setActivationRequests(CopyOnWriteArrayList<DownloadLink> activationRequests) {
        this.activationRequests = activationRequests;
    }

    private final HashMap<String, PluginForHost> activationPluginCache = new HashMap<String, PluginForHost>();

    public HashMap<String, PluginForHost> getActivationPluginCache() {
        return activationPluginCache;
    }

    private final CopyOnWriteArrayList<SingleDownloadController> controllers = new CopyOnWriteArrayList<SingleDownloadController>() {
                                                                                 /**
         * 
         */
                                                                                 private static final long serialVersionUID = -3897088297641777499L;

                                                                                 public boolean add(SingleDownloadController e) {
                                                                                     downloadsStarted.incrementAndGet();
                                                                                     e.getDownloadLink().setDownloadLinkController(e);
                                                                                     return super.add(e);
                                                                                 };

                                                                                 @Override
                                                                                 public boolean remove(Object e) {
                                                                                     boolean ret = super.remove(e);
                                                                                     if (ret) {
                                                                                         getFileAccessManager().unlockAllHeldby(e);
                                                                                         if (e instanceof SingleDownloadController) {
                                                                                             ((SingleDownloadController) e).getDownloadLink().setDownloadLinkController(null);
                                                                                         }
                                                                                     }
                                                                                     return ret;
                                                                                 };
                                                                             };

    public int getActiveDownloadsFromHost(String host) {
        if (host == null) return 0;
        int ret = 0;
        for (SingleDownloadController con : controllers) {
            if (con.isActive() && con.getDownloadLink().getHost().equals(host)) {
                ret++;
            }
        }
        return ret;
    }

    public PluginForHost getPlugin(String host) {
        if (StringUtils.isEmpty(host)) return null;
        PluginForHost plugin = getActivationPluginCache().get(host);
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost(host);
            getActivationPluginCache().put(host, plugin);
        }
        return plugin;
    }

    public CopyOnWriteArrayList<DownloadLink> getForcedLinks() {
        return forcedLinks;
    }

    public void toggleStopMark(Object entry) {
        if (entry == null || stopMark.get() == entry || entry == STOPMARK.NONE) {
            /* no stopmark OR toggle current set stopmark */
            stopMark.set(STOPMARK.NONE);
        } else {
            /* set new stopmark */
            stopMark.set(entry);
            DownloadsTableModel.getInstance().setStopSignColumnVisible(true);
        }
    }

    public boolean forcedLinksWaiting() {
        return forcedLinks.size() > 0;
    }

    public AtomicBoolean getStopAfterForcedLinks() {
        return stopAfterForcedLinks;
    }

    public long getCreated() {
        return created;
    }

    public DownloadSession() {
        created = System.currentTimeMillis();
        fileAccessManager = FILE_ACCESS_MANAGER;
    }

    public boolean isStopMark(final Object item) {
        return stopMark.get() == item;
    }

    public boolean isStopMarkSet() {
        return stopMark.get() != STOPMARK.NONE;
    }

    protected boolean isStopMarkReached() {
        Object stop = stopMark.get();
        if (stop == STOPMARK.HIDDEN) { return true; }
        /* TODO */
        // if (stop instanceof DownloadLink) {
        // synchronized (downloadControlHistory) {
        // if (downloadControlHistory.get(stop) != null) {
        // /*
        // * we already started this download in current session, so stopmark reached
        // */
        // return true;
        // }
        // }
        // final DownloadLink dl = (DownloadLink) stop;
        // if (dl.isSkipped()) return true;
        // if (!dl.isEnabled()) { return true; }
        // if (dl.getLinkStatus().isFinished()) { return true; }
        // return false;
        // }
        // if (stop instanceof FilePackage) {
        // boolean readL = ((FilePackage) stop).getModifyLock().readLock();
        // try {
        // for (final DownloadLink dl : ((FilePackage) stop).getChildren()) {
        // synchronized (downloadControlHistory) {
        // if (downloadControlHistory.get(dl) != null) {
        // /*
        // * we already started this download in current session, so stopmark reached
        // */
        // continue;
        // }
        // }
        // if ((!dl.isSkipped() && dl.isEnabled()) && dl.getLinkStatus().isFinished()) {
        // continue;
        // }
        // return false;
        // }
        // } finally {
        // ((FilePackage) stop).getModifyLock().readUnlock(readL);
        // }
        // return true;
        // }
        return false;
    }

    public void setStopMark(final Object stopEntry) {
        Object entry = stopEntry;
        if (entry == null || entry == STOPMARK.NONE) {
            entry = STOPMARK.NONE;
        }
        if (entry == STOPMARK.RANDOM) {
            /* user wants to set a random stopmark */
            Iterator<SingleDownloadController> it = controllers.iterator();
            if (it.hasNext()) {
                entry = it.next();
            } else {
                entry = STOPMARK.NONE;
            }
        }
        stopMark.set(entry);
    }

    public IfFileExistsAction getOnFileExistsAction(FilePackage filePackage) {
        return fileExistsActions == null ? null : fileExistsActions.get(filePackage.getUniqueID().toString());
    }

    public synchronized void setOnFileExistsAction(FilePackage filePackage, IfFileExistsAction doAction) {
        if (fileExistsActions == null) fileExistsActions = new HashMap<Object, IfFileExistsAction>();
        if (doAction == null) {
            fileExistsActions.remove(filePackage.getUniqueID().toString());
        } else {
            // let's use the unique id. else this map would hold a reference to the filepackage and avoid gc
            fileExistsActions.put(filePackage.getUniqueID().toString(), doAction);
        }
    }

    public FileAccessManager getFileAccessManager() {
        return fileAccessManager;
    }

    public Object getStopMark() {
        return stopMark.get();
    }

    /**
     * @return the downloadsStarted
     */
    public AtomicInteger getDownloadsStarted() {
        return downloadsStarted;
    }

    /**
     * @return the controllers
     */
    public CopyOnWriteArrayList<SingleDownloadController> getControllers() {
        return controllers;
    }

    /**
     * @return the activationLinks
     */
    public CopyOnWriteArrayList<DownloadLink> getActivationRequests() {
        return activationRequests;
    }

}
