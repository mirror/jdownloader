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

package jd.controlling.downloadcontroller;

import java.io.File;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import jd.config.Configuration;
import jd.controlling.IOEQ;
import jd.controlling.JDLogger;
import jd.controlling.packagecontroller.PackageController;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.exceptions.WTFException;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;

public class DownloadController extends PackageController<FilePackage, DownloadLink> {

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

    private transient Eventsender<DownloadControllerListener, DownloadControllerEvent> broadcaster = new Eventsender<DownloadControllerListener, DownloadControllerEvent>() {

                                                                                                       @Override
                                                                                                       protected void fireEvent(final DownloadControllerListener listener, final DownloadControllerEvent event) {
                                                                                                           listener.onDownloadControllerEvent(event);
                                                                                                       };
                                                                                                   };

    private DelayedRunnable                                                            asyncSaving = null;
    private boolean                                                                    allowSave   = false;

    public boolean isAllowSave() {
        return allowSave;
    }

    public void setAllowSave(boolean allowSave) {
        this.allowSave = allowSave;
    }

    private boolean allowLoad = true;

    public boolean isAllowLoad() {
        return allowLoad;
    }

    public void setAllowLoad(boolean allowLoad) {
        this.allowLoad = allowLoad;
    }

    private static DownloadController INSTANCE = new DownloadController();

    /**
     * darf erst nachdem der JDController init wurde, aufgerufen werden
     */
    public static DownloadController getInstance() {
        return INSTANCE;
    }

    private DownloadController() {
        // initDownloadLinks();
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                saveDownloadLinks();
            }

            @Override
            public String toString() {
                return "save downloadlist...";
            }
        });
        asyncSaving = new DelayedRunnable(IOEQ.TIMINGQUEUE, 5000l, 60000l) {

            @Override
            public void delayedrun() {
                saveDownloadLinks();
            }

        };

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
    public synchronized void initDownloadLinks() {
        if (isAllowLoad() == false) {
            /* loading is not allowed */
            return;
        }
        LinkedList<FilePackage> lpackages;
        load(getDownloadListFile());
        try {
            lpackages = loadDownloadLinks();
        } catch (final Throwable e) {
            JDLogger.getLogger().severe(Exceptions.getStackTrace(e));
            lpackages = new LinkedList<FilePackage>();
        }
        final LinkedList<FilePackage> lpackages2 = lpackages;
        for (final FilePackage filePackage : lpackages2) {
            filePackage.setControlledBy(this);
        }
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (isAllowLoad() == true) {
                    writeLock();
                    try {
                        packages.addAll(0, lpackages2);
                    } finally {
                        /* loaded, we no longer allow loading */
                        setAllowLoad(false);
                        /* now we allow saving */
                        setAllowSave(true);
                        writeUnlock();
                    }
                    broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
                }
                return null;
            }
        });
        return;
    }

    private File getDownloadListFile() {
        return Application.getResource("cfg/downloadList.zip");
    }

    private String getCheckFileName() {
        return "check.info";
    }

    /**
     * save the current FilePackages/DownloadLinks controlled by this
     * DownloadController
     */
    public void saveDownloadLinks() {
        if (allowSave == false) return;
        ArrayList<FilePackage> packages = null;
        final boolean readL = this.readLock();
        try {
            packages = new ArrayList<FilePackage>(this.packages);
        } finally {
            readUnlock(readL);
        }
        if (packages != null) {
            JDUtilities.getDatabaseConnector().saveLinks(packages);
            JDUtilities.getDatabaseConnector().save();
        }
        /* save as new Json ZipFile */
        save(packages, getDownloadListFile());
    }

    /**
     * saves List of FilePackages to given File as ZippedJSon
     * 
     * @param packages
     * @param file
     */
    synchronized private boolean save(ArrayList<FilePackage> packages, File file) {
        boolean ret = false;
        if (packages != null && file != null) {
            /* prepare tmp file */
            final File tmpfile = new File(file.getAbsolutePath() + ".tmp");
            tmpfile.getParentFile().mkdirs();
            tmpfile.delete();
            ZipIOWriter zip = null;
            int index = 0;
            /* prepare formatter for package filenames in zipfiles */
            String format = "%02d";
            if (packages.size() >= 10) {
                format = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
            }
            try {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                zip = new ZipIOWriter(tmpfile, true);
                for (FilePackage pkg : packages) {
                    /* convert FilePackage to JSon */
                    FilePackageStorable storable = new FilePackageStorable(pkg);
                    String string = JSonStorage.toString(storable);
                    storable = null;
                    byte[] bytes = string.getBytes("UTF-8");
                    string = null;
                    md.update(bytes);
                    zip.addByteArry(bytes, true, "", String.format(format, (index++)));
                }
                String check = System.currentTimeMillis() + ":" + packages.size() + ":" + HexFormatter.byteArrayToHex(md.digest());
                zip.addByteArry(check.getBytes("UTF-8"), true, "", getCheckFileName());
                /* close ZipIOWriter, so we can rename tmp file now */
                try {
                    zip.close();
                } catch (final Throwable e) {
                }
                /* try to delete destination file if it already exists */
                if (file.exists()) {
                    if (!file.delete()) {
                        Log.exception(new WTFException("Could not delete: " + file.getAbsolutePath()));
                        return false;
                    }
                }
                /* rename tmpfile to destination file */
                if (!tmpfile.renameTo(file)) {
                    Log.exception(new WTFException("Could not rename file: " + tmpfile + " to " + file));
                    return false;
                }
                return true;
            } catch (final Throwable e) {
                Log.exception(e);
            } finally {
                try {
                    zip.close();
                } catch (final Throwable e) {
                }
            }
        }
        return ret;
    }

    synchronized private ArrayList<FilePackage> load(File file) {
        ArrayList<FilePackage> ret = null;
        if (file != null) {
            ZipIOReader zip = null;
            try {
                zip = new ZipIOReader(file);
                ZipEntry check = zip.getZipFile(getCheckFileName());
                String checkString = null;
                if (check != null) {
                    /* parse checkFile if it exists */
                    InputStream checkIS = null;
                    try {
                        checkIS = zip.getInputStream(check);
                        byte[] checkbyte = IO.readStream(1024, checkIS);
                        checkString = new String(checkbyte, "UTF-8");
                        checkbyte = null;
                    } finally {
                        try {
                            checkIS.close();
                        } catch (final Throwable e) {
                        }
                    }
                    check = null;
                }
                if (checkString != null) {
                    /* checkFile exists, lets verify */
                    MessageDigest md = MessageDigest.getInstance("SHA1");
                    byte[] buffer = new byte[1024];
                    int found = 0;
                    for (ZipEntry entry : zip.getZipFiles()) {
                        if (entry.getName().matches("^\\d+$")) {
                            found++;
                            DigestInputStream checkIS = null;
                            try {
                                checkIS = new DigestInputStream(zip.getInputStream(entry), md);
                                while (checkIS.read(buffer) >= 0) {
                                }
                            } finally {
                                try {
                                    checkIS.close();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    }
                    String hash = HexFormatter.byteArrayToHex(md.digest());
                    String time = new Regex(checkString, "(\\d+)").getMatch(0);
                    String numberCheck = new Regex(checkString, ".*?:(\\d+)").getMatch(0);
                    String hashCheck = new Regex(checkString, ".*?:.*?:(.+)").getMatch(0);
                    boolean numberOk = (numberCheck != null && Integer.parseInt(numberCheck) == found);
                    boolean hashOk = (hashCheck != null && hashCheck.equalsIgnoreCase(hash));
                    Log.L.info("DownloadListVerify: TimeStamp(" + time + ")|numberOfPackages(" + found + "):" + numberOk + "|hash:" + hashOk);
                }
                /* lets restore the FilePackages from Json */
                HashMap<Integer, FilePackage> map = new HashMap<Integer, FilePackage>();
                for (ZipEntry entry : zip.getZipFiles()) {
                    if (entry.getName().matches("^\\d+$")) {
                        int packageIndex = Integer.parseInt(entry.getName());
                        InputStream is = null;
                        try {
                            is = zip.getInputStream(entry);
                            byte[] bytes = IO.readStream((int) entry.getSize(), is);
                            String json = new String(bytes, "UTF-8");
                            bytes = null;
                            FilePackageStorable storable = JSonStorage.restoreFromString(json, new TypeRef<FilePackageStorable>() {
                            }, null);
                            json = null;
                            map.put(packageIndex, storable._getFilePackage());
                        } finally {
                            try {
                                is.close();
                            } catch (final Throwable e) {
                            }
                        }
                    }
                }
                /* sort positions */
                ArrayList<Integer> positions = new ArrayList<Integer>(map.keySet());
                Collections.sort(positions);
                /* build final ArrayList of FilePackages */
                ArrayList<FilePackage> ret2 = new ArrayList<FilePackage>(positions.size());
                for (Integer position : positions) {
                    ret2.add(map.get(position));
                }
                map = null;
                positions = null;
                ret = ret2;
            } catch (final Throwable e) {
                Log.exception(e);
            } finally {
                try {
                    zip.close();
                } catch (final Throwable e) {
                }
            }
        }
        return ret;
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
            Iterator<DownloadLink> it;
            FilePackage fp;
            while (iterator.hasNext()) {
                fp = iterator.next();
                if (fp.getChildren().size() == 0) {
                    iterator.remove();
                    continue;
                }
                it = fp.getChildren().iterator();
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
                        if (pluginForHost == null) {
                            try {
                                pluginForHost = replacePluginForHost(localLink);
                            } catch (final Throwable e) {
                                Log.exception(e);
                            }
                            if (pluginForHost != null) {
                                Log.L.info("plugin " + pluginForHost.getHost() + " now handles " + localLink.getName());
                            } else {
                                Log.L.severe("could not find plugin " + localLink.getHost() + " for " + localLink.getName());
                            }
                        }
                        if (pluginForHost != null) {
                            /*
                             * we set default plugin here, this plugin MUST NOT
                             * be used for downloading
                             */
                            localLink.setDefaultPlugin(pluginForHost);
                        }
                    }
                }
            }
            return new LinkedList<FilePackage>(packages);
        }
        throw new Exception("Linklist incompatible");
    }

    private PluginForHost replacePluginForHost(DownloadLink localLink) {
        for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
            if (p.getPrototype().rewriteHost(localLink)) return p.getPrototype();
        }
        return null;
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
        this.addmovePackageAt(fp, 0);
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
                        addmovePackageAt(fp, counter++);
                    }
                    return null;
                }
            });
        }
    }

    /**
     * return a list of all DownloadLinks controlled by this DownloadController
     * 
     * @return
     */
    public ArrayList<DownloadLink> getAllDownloadLinks() {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final boolean readL = readLock();
        try {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    ret.addAll(fp.getChildren());
                }
            }
        } finally {
            readUnlock(readL);
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
            synchronized (fp) {
                for (DownloadLink dl : fp.getChildren()) {
                    if (dl.getLinkStatus().hasStatus(status)) {
                        ret.add(dl);
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
        final boolean readL = readLock();
        try {
            LinkStatus linkStatus;
            boolean isEnabled;
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    ds.addPackages(1);
                    ds.addDownloadLinks(fp.size());
                    for (final DownloadLink l : fp.getChildren()) {
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
        } finally {
            readUnlock(readL);
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
            final boolean readL = readLock();
            try {
                for (final FilePackage fp : packages) {
                    synchronized (fp) {
                        for (DownloadLink dl : fp.getChildren()) {
                            if (correctUrl.equalsIgnoreCase(dl.getDownloadURL())) return true;
                        }
                    }
                }
            } finally {
                readUnlock(readL);
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
        final boolean readL = readLock();
        try {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (DownloadLink nextDownloadLink : fp.getChildren()) {
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
        } finally {
            readUnlock(readL);
        }
        return null;
    }

    public void move(final Object src2, final Object dst, final MOVE mode) {
        /* TODO: rewrite */
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

    public void fireDataUpdate(Object o) {
        if (o != null) {
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_DATA, o));
        } else {
            broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_DATA));
        }
    }

    public void fireDataUpdate() {
        fireDataUpdate(null);
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
        final boolean readL = readLock();
        try {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (final DownloadLink nextDownloadLink : fp.getChildren()) {
                        if (pat.matcher(nextDownloadLink.getName()).matches()) {
                            ret.add(nextDownloadLink);
                        }
                    }
                }
            }
        } finally {
            readUnlock(readL);
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
        final boolean readL = readLock();
        try {
            for (final FilePackage fp : packages) {
                synchronized (fp) {
                    for (final DownloadLink nextDownloadLink : fp.getChildren()) {
                        if (pat.matcher(nextDownloadLink.getFileOutput()).matches()) {
                            ret.add(nextDownloadLink);
                        }
                    }
                }
            }
        } finally {
            readUnlock(readL);
        }
        return ret;
    }

    @Override
    protected void _controllerParentlessLinks(List<DownloadLink> links, QueuePriority priority) {
        asyncSaving.run();
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, links));
        if (links != null) {
            /* we stop all parentless downloads */
            for (DownloadLink link : links) {
                link.setAborted(true);
            }
        }
    }

    @Override
    protected void _controllerPackageNodeRemoved(FilePackage pkg, QueuePriority priority) {
        asyncSaving.run();
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, pkg));
    }

    @Override
    protected void _controllerStructureChanged(QueuePriority priority) {
        asyncSaving.run();
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
    }

    @Override
    protected void _controllerPackageNodeAdded(FilePackage pkg, QueuePriority priority) {
        asyncSaving.run();
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
    }

}
