//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import jd.config.Configuration;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.PluginsC;
import jd.utils.JDUtilities;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.utils.Exceptions;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.QueueAction;

class DownloadControllerBroadcaster extends Eventsender<DownloadControllerListener, DownloadControllerEvent> {

    @Override
    protected void fireEvent(final DownloadControllerListener listener, final DownloadControllerEvent event) {
        listener.onDownloadControllerEvent(event);
    }
}

public class DownloadController implements DownloadControllerListener, DownloadControllerInterface {

    private final AtomicLong structureChanged = new AtomicLong(0);

    public static enum MOVE {
        BEFORE,
        AFTER,
        BEGIN,
        END,
        TOP,
        BOTTOM,
        UP,
        DOWN
    }

    private LinkedList<FilePackage>                 packages       = new LinkedList<FilePackage>();

    private transient DownloadControllerBroadcaster broadcaster    = new DownloadControllerBroadcaster();

    public static final Object                      ACCESSLOCK     = new Object();
    private ScheduledFuture<?>                      asyncSaveTimer = null;
    private static DownloadController               INSTANCE       = new DownloadController();

    /**
     * darf erst nachdem der JDController init wurde, aufgerufen werden
     */
    public static DownloadController getInstance() {
        return INSTANCE;
    }

    private DownloadController() {
        initDownloadLinks();
        broadcaster.addListener(this);
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                saveDownloadLinksSyncnonThread();
            }

            @Override
            public String toString() {
                return "save downloadlist...";
            }
        });
    }

    public void addListener(final DownloadControllerListener l) {
        broadcaster.addListener(l);
    }

    public void removeListener(final DownloadControllerListener l) {
        broadcaster.removeListener(l);
    }

    /**
     * load all FilePackages/DownloadLinks from Database
     */
    private void initDownloadLinks() {
        try {
            packages = loadDownloadLinks();
        } catch (final Throwable e) {
            JDLogger.getLogger().severe(Exceptions.getStackTrace(e));
            packages = new LinkedList<FilePackage>();
        }
        for (final FilePackage filePackage : packages) {
            filePackage.setControlledby(this);
        }
        refreshListOrderIDS();
        return;
    }

    /**
     * save the current FilePackages/DownloadLinks controlled by this
     * DownloadController
     */
    public void saveDownloadLinksSyncnonThread() {
        synchronized (ACCESSLOCK) {
            asyncSaveTimer = null;
            ArrayList<FilePackage> packages = new ArrayList<FilePackage>(this.packages);
            JDUtilities.getDatabaseConnector().saveLinks(packages);
        }
    }

    /**
     * request saving of current FilePackages/DownloadLinks with a 20 sec delay
     */
    public void saveDownloadLinksAsync() {
        synchronized (ACCESSLOCK) {
            if (asyncSaveTimer != null) asyncSaveTimer.cancel(false);
            asyncSaveTimer = IOEQ.TIMINGQUEUE.schedule(new Runnable() {

                public void run() {
                    final String id = JDController.requestDelayExit("downloadcontroller");
                    try {
                        saveDownloadLinksSyncnonThread();
                    } finally {
                        JDController.releaseDelayExit(id);
                    }
                }

            }, 20, TimeUnit.SECONDS);
        }
    }

    /**
     * load FilePackages and DownloadLinks from database
     * 
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    private LinkedList<FilePackage> loadDownloadLinks() throws Exception {
        final Object obj = JDUtilities.getDatabaseConnector().getLinks();
        if (obj != null && obj instanceof ArrayList && (((ArrayList<?>) obj).size() == 0 || ((ArrayList<?>) obj).size() > 0 && ((ArrayList<?>) obj).get(0) instanceof FilePackage)) {
            final ArrayList<FilePackage> packages = (ArrayList<FilePackage>) obj;
            final Iterator<FilePackage> iterator = packages.iterator();
            DownloadLink localLink;
            PluginForHost pluginForHost = null;
            PluginsC pluginForContainer = null;
            // String tmp2 = null;
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.getControlledDownloadLinks().size() == 0) {
                    iterator.remove();
                    continue;
                }
                it = fp.getControlledDownloadLinks().iterator();
                while (it.hasNext()) {
                    localLink = it.next();
                    /*
                     * reset not if already exist, offline or finished. plugin
                     * errors will be reset here because plugin can be fixed
                     * again
                     */
                    localLink.getLinkStatus().resetStatus(LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED, LinkStatus.ERROR_FATAL);

                    if (localLink.getLinkStatus().isFinished() && JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_FINISHED_DOWNLOADS_ACTION, 3) == 1) {
                        it.remove();
                        if (fp.size() == 0) {
                            iterator.remove();
                            continue;
                        }
                    } else {
                        // Anhand des Hostnamens aus dem DownloadLink
                        // wird ein passendes Plugin gesucht
                        try {
                            pluginForHost = null;
                            pluginForHost = JDUtilities.getPluginForHost(localLink.getHost());
                        } catch (final Throwable e) {
                            JDLogger.exception(e);
                        }
                        // Gibt es einen Names für ein Containerformat,
                        // wird ein passendes Plugin gesucht
                        try {
                            if (localLink.getContainer() != null) {
                                pluginForContainer = JDUtilities.getPluginForContainer(localLink.getContainer());
                                if (pluginForContainer == null) {
                                    localLink.setEnabled(false);
                                }
                            }
                        } catch (final NullPointerException e) {
                            JDLogger.exception(e);
                        }
                        if (pluginForHost == null) {
                            try {
                                pluginForHost = JDUtilities.replacePluginForHost(localLink);
                            } catch (final Throwable e) {
                                JDLogger.exception(e);
                            }
                            if (pluginForHost != null) {
                                JDLogger.getLogger().info("plugin " + pluginForHost.getHost() + " now handles " + localLink.getName());
                            } else {
                                JDLogger.getLogger().severe("could not find plugin " + localLink.getHost() + " for " + localLink.getName());
                            }
                        }
                        if (pluginForHost != null) {
                            /*
                             * we set default plugin here, this plugin MUST NOT
                             * be used for downloading
                             */
                            localLink.setDefaultPlugin(pluginForHost);
                        }
                        if (pluginForContainer != null) {
                            localLink.setLoadedPluginForContainer(pluginForContainer);
                        }

                    }
                }
            }
            return new LinkedList<FilePackage>(packages);
        }
        throw new Exception("Linklist incompatible");
    }

    /**
     * refresh all FilePackage and DownloadLink ListOrderIDs
     */
    private void refreshListOrderIDS() {
        synchronized (ACCESSLOCK) {
            int id = 0;
            Iterator<FilePackage> it = packages.iterator();
            while (it.hasNext()) {
                FilePackage fp = it.next();
                fp.setListOrderID(id++);
                synchronized (fp) {
                    Iterator<DownloadLink> it2 = fp.getControlledDownloadLinks().iterator();
                    while (it2.hasNext()) {
                        DownloadLink dl = it2.next();
                        dl.setListOrderID(id++);
                    }
                }
            }
        }
    }

    public LinkedList<FilePackage> getPackages() {
        return packages;
    }

    /**
     * add all given FilePackages to this DownloadController at the beginning
     * 
     * @param fps
     */
    public void addAll(final ArrayList<FilePackage> fps) {
        addAllAt(fps, 0);
    }

    /**
     * add given FilePackage to this DownloadController at the beginning
     * 
     * @param fp
     */
    public void addPackage(final FilePackage fp) {
        addPackageAt(fp, 0);
    }

    /**
     * add/move given FilePackage at given Position
     * 
     * @param fp
     * @param index
     * @param repos
     * @return
     */
    public void addPackageAt(final FilePackage fp, final int index) {
        if (fp != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    boolean isNew = true;
                    synchronized (ACCESSLOCK) {
                        /**
                         * iterate through all packages, remove the existing one
                         * and add at given position
                         */
                        ListIterator<FilePackage> li = packages.listIterator();
                        int counter = 0;
                        boolean need2Remove = fp.getControlledby() != null;
                        boolean done = false;
                        while (li.hasNext()) {
                            if (done && !need2Remove) break;
                            FilePackage c = li.next();
                            if (counter == index) {
                                li.add(fp);
                                done = true;
                            }
                            if (c == fp) {
                                li.remove();
                                isNew = false;
                                need2Remove = false;
                            }
                            counter++;
                        }
                        if (!done) {
                            /**
                             * index > packages.size , then add at end
                             */
                            packages.addLast(fp);
                        }
                        fp.setControlledby(DownloadController.this);
                    }
                    structureChanged.incrementAndGet();
                    if (isNew) {
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.ADD_FILEPACKAGE, fp));
                    } else {
                        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
                    }
                    return null;
                }
            });
        }
    }

    /**
     * add/move all given FilePackages at given Position
     * 
     * @param fp
     * @param index
     * @param repos
     * @return
     */
    public void addAllAt(final ArrayList<FilePackage> fps, final int index) {
        if (fps != null && fps.size() > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    int counter = index;
                    for (FilePackage fp : fps) {
                        addPackageAt(fp, counter++);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * removes the given FilePackage from this DownloadController
     * 
     * @param fp
     */
    public void removePackage(final FilePackage fp) {
        if (fp != null) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    boolean removed = false;
                    ArrayList<DownloadLink> stop = null;
                    synchronized (ACCESSLOCK) {
                        if (fp.getControlledby() != null) {
                            fp.setControlledby(null);
                            removed = packages.remove(fp);
                        }
                        synchronized (fp) {
                            stop = new ArrayList<DownloadLink>(fp.getControlledDownloadLinks());
                        }
                    }
                    if (stop != null) {
                        for (DownloadLink dl : stop) {
                            dl.setAborted(true);
                        }
                    }
                    if (removed) {
                        structureChanged.incrementAndGet();
                        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.REMOVE_FILPACKAGE, fp));
                    }
                    return null;
                }
            });
        }
    }

    /**
     * return how many FilePackages are controlled by this DownloadController
     * 
     * @return
     */
    public int size() {
        synchronized (ACCESSLOCK) {
            return packages.size();
        }
    }

    /**
     * return a list of all DownloadLinks controlled by this DownloadController
     * 
     * @return
     */
    public ArrayList<DownloadLink> getAllDownloadLinks() {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        synchronized (ACCESSLOCK) {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    ret.addAll(fp.getControlledDownloadLinks());
                }
            }
        }
        return ret;
    }

    /**
     * return a list of all DownloadLinks from a given FilePackage with status
     * 
     * @param fp
     * @param status
     * @return
     */
    public ArrayList<DownloadLink> getDownloadLinksbyStatus(FilePackage fp, int status) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        if (fp != null) {
            synchronized (ACCESSLOCK) {
                synchronized (fp) {
                    for (DownloadLink dl : fp.getControlledDownloadLinks()) {
                        if (dl.getLinkStatus().hasStatus(status)) {
                            ret.add(dl);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * fill given DownloadInformations with current details of this
     * DownloadController
     */
    protected void getDownloadStatus(final DownloadInformations ds) {
        ds.reset();
        ds.addRunningDownloads(DownloadWatchDog.getInstance().getActiveDownloads());
        synchronized (ACCESSLOCK) {
            LinkStatus linkStatus;
            boolean isEnabled;
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    ds.addPackages(1);
                    ds.addDownloadLinks(fp.size());
                    for (final DownloadLink l : fp.getControlledDownloadLinks()) {
                        linkStatus = l.getLinkStatus();
                        isEnabled = l.isEnabled();
                        if (!linkStatus.hasStatus(LinkStatus.ERROR_ALREADYEXISTS) && isEnabled) {
                            ds.addTotalDownloadSize(l.getDownloadSize());
                            ds.addCurrentDownloadSize(l.getDownloadCurrent());
                        }
                        if (linkStatus.hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
                            ds.addDuplicateDownloads(1);
                        } else if (!isEnabled) {
                            ds.addDisabledDownloads(1);
                        } else if (linkStatus.hasStatus(LinkStatus.FINISHED)) {
                            ds.addFinishedDownloads(1);
                        }
                    }
                }
            }
        }
    }

    /**
     * checks if this DownloadController contains a DownloadLink with given url
     * 
     * @param url
     * @return
     */
    public boolean hasDownloadLinkwithURL(final String url) {
        if (url != null) {
            final String correctUrl = url.trim();
            synchronized (ACCESSLOCK) {
                for (final FilePackage fp : packages) {
                    synchronized (fp) {
                        for (DownloadLink dl : fp.getControlledDownloadLinks()) {
                            if (correctUrl.equalsIgnoreCase(dl.getDownloadURL())) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * return the first DownloadLink that does block given DownloadLink
     * 
     * @param link
     * @return
     */
    public DownloadLink getFirstLinkThatBlocks(final DownloadLink link) {
        if (link == null) return null;
        synchronized (ACCESSLOCK) {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (DownloadLink nextDownloadLink : fp.getControlledDownloadLinks()) {
                        if (nextDownloadLink == link) continue;
                        if ((nextDownloadLink.getLinkStatus().hasStatus(LinkStatus.FINISHED)) && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) {
                            if (new File(nextDownloadLink.getFileOutput()).exists()) {
                                /*
                                 * fertige datei sollte auch auf der platte sein
                                 * und nicht nur als fertig in der liste
                                 */
                                return nextDownloadLink;
                            }
                        }
                        if (nextDownloadLink.getLinkStatus().isPluginInProgress() && nextDownloadLink.getFileOutput().equalsIgnoreCase(link.getFileOutput())) return nextDownloadLink;
                    }
                }
            }
        }
        return null;
    }

    /**
     * remove this as soon as possible and rewrite move
     * 
     * @param fp
     * @return
     */
    @Deprecated
    private int indexOf(FilePackage fp) {
        return packages.indexOf(fp);
    }

    @SuppressWarnings("unchecked")
    public void move(final Object src2, final Object dst, final MOVE mode) {
        // boolean type = false; /* false=downloadLink,true=filepackage */
        // Object src = null;
        // FilePackage fp = null;
        // if (src2 instanceof ArrayList<?>) {
        // if (((ArrayList<?>) src2).isEmpty()) return;
        // final Object check = ((ArrayList<?>) src2).get(0);
        // if (check == null) {
        // JDLogger.getLogger().warning("Null src, cannot move!");
        // return;
        // }
        // if (check instanceof DownloadLink) {
        // src = src2;
        // type = false;
        // } else if (check instanceof FilePackage) {
        // src = src2;
        // type = true;
        // }
        // } else if (src2 instanceof DownloadLink) {
        // type = false;
        // src = new ArrayList<DownloadLink>();
        // ((ArrayList<DownloadLink>) src).add((DownloadLink) src2);
        // } else if (src2 instanceof FilePackage) {
        // type = true;
        // src = new ArrayList<FilePackage>();
        // ((ArrayList<FilePackage>) src).add((FilePackage) src2);
        // }
        // if (src == null) {
        // JDLogger.getLogger().warning("Unknown src, cannot move!");
        // return;
        // }
        // synchronized (DownloadController.ACCESSLOCK) {
        // if (dst != null) {
        // if (!type) {
        // if (dst instanceof FilePackage) {
        // /* src:DownloadLinks dst:filepackage */
        // switch (mode) {
        // case BEGIN:
        // fp = ((FilePackage) dst);
        // fp.addLinksAt((ArrayList<DownloadLink>) src, 0);
        // return;
        // case END:
        // fp = ((FilePackage) dst);
        // fp.addLinksAt((ArrayList<DownloadLink>) src, fp.size());
        // return;
        // default:
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // } else if (dst instanceof DownloadLink) {
        // /* src:DownloadLinks dst:DownloadLinks */
        // switch (mode) {
        // case BEFORE:
        // fp = ((DownloadLink) dst).getFilePackage();
        // fp.addLinksAt((ArrayList<DownloadLink>) src,
        // fp.indexOf((DownloadLink) dst));
        // return;
        // case AFTER:
        // fp = ((DownloadLink) dst).getFilePackage();
        // fp.addLinksAt((ArrayList<DownloadLink>) src,
        // fp.indexOf((DownloadLink) dst) + 1);
        // return;
        // default:
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // } else {
        // JDLogger.getLogger().warning("Unsupported dst, cannot move!");
        // return;
        // }
        // } else {
        // if (dst instanceof FilePackage) {
        // /* src:FilePackages dst:filepackage */
        // switch (mode) {
        // case BEFORE:
        // addAllAt((ArrayList<FilePackage>) src, indexOf((FilePackage) dst));
        // return;
        // case AFTER:
        // addAllAt((ArrayList<FilePackage>) src, indexOf((FilePackage) dst) +
        // 1);
        // return;
        // default:
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // } else if (dst instanceof DownloadLink) {
        // /* src:FilePackages dst:DownloadLinks */
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // }
        // } else {
        // /* dst==null, global moving */
        // if (type) {
        // /* src:FilePackages */
        // switch (mode) {
        // case UP: {
        // int curpos = 0;
        // for (final FilePackage item : (ArrayList<FilePackage>) src) {
        // curpos = indexOf(item);
        // addPackageAt(item, curpos - 1);
        // }
        // }
        // return;
        // case DOWN: {
        // int curpos = 0;
        // final ArrayList<FilePackage> fps = ((ArrayList<FilePackage>) src);
        // for (int i = fps.size() - 1; i >= 0; i--) {
        // curpos = indexOf(fps.get(i));
        // addPackageAt(fps.get(i), curpos + 2);
        // }
        // }
        // return;
        // case TOP:
        // addAllAt((ArrayList<FilePackage>) src, 0);
        // return;
        // case BOTTOM:
        // addAllAt((ArrayList<FilePackage>) src, size() + 1);
        // return;
        // default:
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // } else {
        // /* src:DownloadLinks */
        // switch (mode) {
        // case UP: {
        // int curpos = 0;
        // for (final DownloadLink item : (ArrayList<DownloadLink>) src) {
        // final FilePackage filePackage = item.getFilePackage();
        // curpos = filePackage.indexOf(item);
        // filePackage.add(curpos - 1, item, 0);
        // if (curpos == 0) {
        // curpos = indexOf(filePackage);
        // addPackageAt(filePackage, curpos - 1);
        // }
        // }
        // }
        // return;
        // case DOWN: {
        // int curpos = 0;
        // final ArrayList<DownloadLink> links = ((ArrayList<DownloadLink>)
        // src);
        // for (int i = links.size() - 1; i >= 0; i--) {
        // final DownloadLink link = links.get(i);
        // final FilePackage filePackage = link.getFilePackage();
        // curpos = filePackage.indexOf(link);
        // filePackage.add(curpos + 2, link, 0);
        // if (curpos == filePackage.size() - 1) {
        // curpos = indexOf(filePackage);
        // addPackageAt(filePackage, curpos + 2);
        // }
        // }
        // }
        // return;
        // case TOP: {
        // final ArrayList<ArrayList<DownloadLink>> split =
        // splitByFilePackage((ArrayList<DownloadLink>) src);
        // for (final ArrayList<DownloadLink> links : split) {
        // final FilePackage filePackage = links.get(0).getFilePackage();
        // if (filePackage.indexOf(links.get(0)) == 0) {
        // addPackageAt(filePackage, 0);
        // }
        // filePackage.addLinksAt(links, 0);
        // }
        // }
        // return;
        // case BOTTOM: {
        // final ArrayList<ArrayList<DownloadLink>> split =
        // splitByFilePackage((ArrayList<DownloadLink>) src);
        // for (final ArrayList<DownloadLink> links : split) {
        // final FilePackage filePackage = links.get(0).getFilePackage();
        // if (filePackage.indexOf(links.get(links.size() - 1)) ==
        // filePackage.size() - 1) {
        // addPackageAt(filePackage, size() + 1);
        // }
        // filePackage.addLinksAt(links, filePackage.size() + 1);
        // }
        // }
        // return;
        // default:
        // JDLogger.getLogger().warning("Unsupported mode, cannot move!");
        // return;
        // }
        // }
        // }
        // }
    }

    public static ArrayList<ArrayList<DownloadLink>> splitByFilePackage(final ArrayList<DownloadLink> links) {
        final ArrayList<ArrayList<DownloadLink>> ret = new ArrayList<ArrayList<DownloadLink>>();
        boolean added = false;
        for (final DownloadLink link : links) {
            if (ret.size() == 0) {
                final ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                tmp.add(link);
                ret.add(tmp);
            } else {
                added = false;
                for (final ArrayList<DownloadLink> check : ret) {
                    if (link.getFilePackage() == check.get(0).getFilePackage()) {
                        added = true;
                        check.add(link);
                    }
                }
                if (added == false) {
                    final ArrayList<DownloadLink> tmp = new ArrayList<DownloadLink>();
                    tmp.add(link);
                    ret.add(tmp);
                }
            }
        }
        return ret;
    }

    public void fireStructureUpdate() {
        /* speichern der downloadliste + aktuallisierung der gui */
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_STRUCTURE));
    }

    public void fireGlobalUpdate() {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_ALL));
    }

    public void fireDownloadLinkUpdate(final Object param) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.REFRESH_SPECIFIC, param));
    }

    public void onDownloadControllerEvent(final DownloadControllerEvent event) {
        switch (event.getEventID()) {
        case DownloadControllerEvent.ADD_DOWNLOADLINK:
        case DownloadControllerEvent.REMOVE_DOWNLOADLINK:
        case DownloadControllerEvent.ADD_FILEPACKAGE:
        case DownloadControllerEvent.REMOVE_FILPACKAGE:
            fireStructureUpdate();
            break;
        case DownloadControllerEvent.REFRESH_STRUCTURE:
            this.saveDownloadLinksAsync();
            break;
        }
    }

    /**
     * return all DownloadLinks that match given Regex for Filename
     * 
     * @param matcher
     * @return
     */
    public LinkedList<DownloadLink> getDownloadLinksByNamePattern(final String matcher) {
        final LinkedList<DownloadLink> ret = new LinkedList<DownloadLink>();
        if (matcher == null || matcher.length() == 0) return ret;
        Pattern pat = Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
        synchronized (ACCESSLOCK) {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (final DownloadLink nextDownloadLink : fp.getControlledDownloadLinks()) {
                        if (pat.matcher(nextDownloadLink.getName()).matches()) {
                            ret.add(nextDownloadLink);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * return all DownloadLinks that match a given Regex for OutputPath
     * 
     * @param matcher
     * @return
     */
    public LinkedList<DownloadLink> getDownloadLinksByPathPattern(final String matcher) {
        final LinkedList<DownloadLink> ret = new LinkedList<DownloadLink>();
        if (matcher == null || matcher.length() == 0) return ret;
        Pattern pat = Pattern.compile(matcher, Pattern.CASE_INSENSITIVE);
        synchronized (ACCESSLOCK) {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (final DownloadLink nextDownloadLink : fp.getControlledDownloadLinks()) {
                        if (pat.matcher(nextDownloadLink.getFileOutput()).matches()) {
                            ret.add(nextDownloadLink);
                        }
                    }
                }
            }
        }
        return ret;
    }

    /**
     * add DownloadLinks to the FilePackage
     */
    public void addDownloadLinks(final FilePackage fp, final DownloadLink... dls) {
        if (fp != null && dls != null && dls.length > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    if (fp.getControlledby() == null) {
                        DownloadController.this.addPackage(fp);
                    }
                    LinkedList<DownloadLink> links = new LinkedList<DownloadLink>();
                    for (DownloadLink dl : dls) {
                        links.add(dl);
                    }
                    synchronized (ACCESSLOCK) {
                        synchronized (fp) {
                            LinkedList<DownloadLink> list = fp.getControlledDownloadLinks();
                            Iterator<DownloadLink> it = links.iterator();
                            /*
                             * remove DownloadLinks that are already in this
                             * FilePackage
                             */
                            while (it.hasNext()) {
                                DownloadLink dl = it.next();
                                if (list.contains(dl)) {
                                    it.remove();
                                }
                            }
                            /* add the remaining ones to the FilePackage */
                            if (links.size() > 0) {
                                list.addAll(links);
                                for (DownloadLink dl : links) {
                                    dl._setFilePackage(fp);
                                }
                            }
                        }
                    }
                    if (links.size() > 0) {
                        structureChanged.incrementAndGet();
                        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.ADD_DOWNLOADLINK, links));
                    }
                    return null;
                }
            });
        }
    }

    /**
     * remove DownloadLinks from a FilePackage
     */
    public void removeDownloadLinks(final FilePackage fp, final DownloadLink... dls) {
        if (fp != null && dls != null && dls.length > 0) {
            IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

                @Override
                protected Void run() throws RuntimeException {
                    LinkedList<DownloadLink> links = new LinkedList<DownloadLink>();
                    for (DownloadLink dl : dls) {
                        links.add(dl);
                    }
                    synchronized (ACCESSLOCK) {
                        synchronized (fp) {
                            LinkedList<DownloadLink> list = fp.getControlledDownloadLinks();
                            Iterator<DownloadLink> it = links.iterator();
                            while (it.hasNext()) {
                                DownloadLink dl = it.next();
                                if (list.remove(dl)) {
                                    /*
                                     * set FilePackage to null if the link was
                                     * controlled by this FilePackage
                                     */
                                    if (dl.getFilePackage() == fp) dl._setFilePackage(null);
                                } else {
                                    it.remove();
                                }
                            }
                        }
                    }
                    if (links.size() > 0) {
                        structureChanged.incrementAndGet();
                        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.REMOVE_DOWNLOADLINK, links));
                        if (fp.size() == 0) {
                            DownloadController.this.removePackage(fp);
                        }
                    }
                    return null;
                }
            });
        }
    }

}
