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
import java.util.zip.ZipEntry;

import jd.config.NoOldJDDataBaseFoundException;
import jd.controlling.IOEQ;
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
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;

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

    private transient Eventsender<DownloadControllerListener, DownloadControllerEvent> broadcaster  = new Eventsender<DownloadControllerListener, DownloadControllerEvent>() {

                                                                                                        @Override
                                                                                                        protected void fireEvent(final DownloadControllerListener listener, final DownloadControllerEvent event) {
                                                                                                            listener.onDownloadControllerEvent(event);
                                                                                                        };
                                                                                                    };

    private DelayedRunnable                                                            asyncSaving  = null;
    private boolean                                                                    allowSave    = false;

    private boolean                                                                    allowLoad    = true;

    private static DownloadController                                                  INSTANCE     = new DownloadController();

    private static Object                                                              SAVELOADLOCK = new Object();

    /**
     * darf erst nachdem der JDController init wurde, aufgerufen werden
     */
    public static DownloadController getInstance() {
        return INSTANCE;
    }

    private DownloadController() {
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {

            @Override
            public void run() {
                int retry = 10;
                while (retry > 0) {
                    if (DownloadWatchDog.getInstance().getStateMachine().isFinal() || DownloadWatchDog.getInstance().getStateMachine().isStartState()) {
                        /*
                         * we wait till the DownloadWatchDog is finished or max 10 secs
                         */
                        break;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        break;
                    }
                    retry--;
                }
                saveDownloadLinks();
                setSaveAllowed(false);
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
        this.broadcaster.addListener(new DownloadControllerListener() {

            public void onDownloadControllerEvent(DownloadControllerEvent event) {
                asyncSaving.run();
            }
        });

    }

    public void requestSaving(boolean async) {
        if (async) {
            asyncSaving.run();
        } else {
            saveDownloadLinks();
        }
    }

    @Override
    protected void _controllerPackageNodeAdded(FilePackage pkg, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
    }

    @Override
    protected void _controllerPackageNodeRemoved(FilePackage pkg, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, pkg));
    }

    @Override
    protected void _controllerParentlessLinks(final List<DownloadLink> links, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, links));
        if (links != null) {
            for (DownloadLink link : links) {
                /* disabling the link will also abort an ongoing download */
                link.setEnabled(false);
            }
        }
    }

    @Override
    protected void _controllerStructureChanged(QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
    }

    /**
     * add all given FilePackages to this DownloadController at the beginning
     * 
     * @param fps
     */
    public void addAll(final java.util.List<FilePackage> fps) {
        addAllAt(fps, 0);
    }

    /**
     * add/move all given FilePackages at given Position
     * 
     * @param fp
     * @param index
     * @param repos
     * @return
     */
    public void addAllAt(final java.util.List<FilePackage> fps, final int index) {
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

    public void addListener(final DownloadControllerListener l) {
        broadcaster.addListener(l);
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
     * return a list of all DownloadLinks controlled by this DownloadController
     * 
     * @return
     */
    public java.util.List<DownloadLink> getAllDownloadLinks() {
        final java.util.List<DownloadLink> ret = new ArrayList<DownloadLink>();
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

    private String getCheckFileName() {
        return "check.info";
    }

    private String getJDRootFileName() {
        return "jdroot.path";
    }

    /**
     * return a list of all DownloadLinks from a given FilePackage with status
     * 
     * @param fp
     * @param status
     * @return
     */
    public java.util.List<DownloadLink> getDownloadLinksbyStatus(FilePackage fp, int status) {
        final java.util.List<DownloadLink> ret = new ArrayList<DownloadLink>();
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

    private File getDownloadListFile() {
        return Application.getResource("cfg/downloadList.zip");
    }

    /**
     * fill given DownloadInformations with current details of this DownloadController
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
                                 * fertige datei sollte auch auf der platte sein und nicht nur als fertig in der liste
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

    public LinkedList<FilePackage> getPackages() {
        return packages;
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
     * load all FilePackages/DownloadLinks from Database
     */
    public synchronized void initDownloadLinks() {
        if (isLoadAllowed() == false) {
            /* loading is not allowed */
            return;
        }
        LinkedList<FilePackage> lpackages = null;
        try {
            /* try fallback to load tmp file */
            lpackages = load(new File(getDownloadListFile().getAbsolutePath() + ".tmp"));
        } catch (final Throwable e) {
            logger.log(e);
        }
        try {
            /* load from new json zip */
            if (lpackages == null) lpackages = load(getDownloadListFile());
        } catch (final Throwable e) {
            logger.log(e);
        }
        try {
            /* try fallback to load tmp file */
            if (lpackages == null) lpackages = load(new File(getDownloadListFile().getAbsolutePath() + ".bak"));
        } catch (final Throwable e) {
            logger.log(e);
        }
        try {
            /* fallback to old hsqldb */
            if (lpackages == null) lpackages = loadDownloadLinks();
        } catch (final Throwable e) {
            logger.log(e);
        }
        if (lpackages == null) lpackages = new LinkedList<FilePackage>();
        postInit(lpackages);
        final LinkedList<FilePackage> lpackages2 = lpackages;
        IOEQ.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                if (isLoadAllowed() == true) {
                    writeLock();
                    /* add loaded Packages to this controller */
                    try {
                        for (final FilePackage filePackage : lpackages2) {
                            filePackage.setControlledBy(DownloadController.this);
                        }
                        packages.addAll(0, lpackages2);
                    } finally {
                        /* loaded, we no longer allow loading */
                        setLoadAllowed(false);
                        /* now we allow saving */
                        setSaveAllowed(true);
                        writeUnlock();
                    }
                    broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE));
                }
                return null;
            }
        });
        return;
    }

    public boolean isLoadAllowed() {
        return allowLoad;
    }

    public boolean isSaveAllowed() {
        return allowSave;
    }

    private LinkedList<FilePackage> load(File file) {
        synchronized (SAVELOADLOCK) {
            LinkedList<FilePackage> ret = null;
            if (file != null && file.exists()) {
                ZipIOReader zip = null;
                try {
                    zip = new ZipIOReader(file);
                    ZipEntry check = zip.getZipFile(getCheckFileName());
                    if (check != null) {
                        /* parse checkFile if it exists */
                        String checkString = null;
                        {
                            /* own scope so we can reuse checkIS */
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
                            logger.info("DownloadListVerify: TimeStamp(" + time + ")|numberOfPackages(" + found + "):" + numberOk + "|hash:" + hashOk);
                        }
                        check = null;
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
                                if (storable != null) {
                                    map.put(packageIndex, storable._getFilePackage());
                                }
                            } finally {
                                try {
                                    is.close();
                                } catch (final Throwable e) {
                                }
                            }
                        }
                    }
                    /* sort positions */
                    java.util.List<Integer> positions = new ArrayList<Integer>(map.keySet());
                    Collections.sort(positions);
                    /* build final ArrayList of FilePackages */
                    java.util.List<FilePackage> ret2 = new ArrayList<FilePackage>(positions.size());
                    for (Integer position : positions) {
                        ret2.add(map.get(position));
                    }
                    if (JsonConfig.create(GeneralSettings.class).isConvertRelativePathesJDRoot()) {
                        try {
                            ZipEntry jdRoot = zip.getZipFile(getJDRootFileName());
                            String oldJDRoot = null;
                            if (jdRoot != null) {
                                /* parse jdRoot.path if it exists */
                                InputStream checkIS = null;
                                try {
                                    checkIS = zip.getInputStream(jdRoot);
                                    byte[] checkbyte = IO.readStream(1024, checkIS);
                                    oldJDRoot = new String(checkbyte, "UTF-8");
                                    checkbyte = null;
                                } finally {
                                    try {
                                        checkIS.close();
                                    } catch (final Throwable e) {
                                    }
                                }
                                jdRoot = null;
                            }
                            if (!StringUtils.isEmpty(oldJDRoot)) {
                                String newRoot = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
                                /*
                                 * convert pathes relative to JDownloader root,only in jared version
                                 */
                                for (FilePackage pkg : ret2) {
                                    if (!CrossSystem.isAbsolutePath(pkg.getDownloadDirectory())) {
                                        /* no need to convert relative pathes */
                                        continue;
                                    }
                                    String pkgPath = LinkTreeUtils.getDownloadDirectory(pkg).toString();
                                    if (pkgPath.startsWith(oldJDRoot)) {
                                        /*
                                         * folder is inside JDRoot, lets update it
                                         */
                                        String restPath = pkgPath.substring(oldJDRoot.length());
                                        String newPath = new File(newRoot, restPath).toString();
                                        pkg.setDownloadDirectory(newPath);
                                    }
                                }
                            }
                        } catch (final Throwable e) {
                            logger.log(e);
                        }
                    }
                    map = null;
                    positions = null;
                    ret = new LinkedList<FilePackage>(ret2);
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    try {
                        zip.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            return ret;
        }
    }

    /**
     * load FilePackages and DownloadLinks from database
     * 
     * @return
     * @throws Exception
     */
    private LinkedList<FilePackage> loadDownloadLinks() throws Exception {
        Object obj = null;
        try {
            obj = JDUtilities.getDatabaseConnector().getLinks();
        } catch (final NoOldJDDataBaseFoundException e) {
            return null;
        }
        if (obj != null && obj instanceof ArrayList && (((java.util.List<?>) obj).size() == 0 || ((java.util.List<?>) obj).size() > 0 && ((java.util.List<?>) obj).get(0) instanceof FilePackage)) { return new LinkedList<FilePackage>((java.util.List<FilePackage>) obj); }
        throw new Exception("Linklist incompatible");
    }

    private void postInit(LinkedList<FilePackage> fps) {
        if (fps == null || fps.size() == 0) return;
        final Iterator<FilePackage> iterator = fps.iterator();
        DownloadLink localLink;
        PluginForHost pluginForHost = null;
        Iterator<DownloadLink> it;
        FilePackage fp;
        HashMap<String, PluginForHost> fixWith = new HashMap<String, PluginForHost>();
        while (iterator.hasNext()) {
            fp = iterator.next();
            java.util.List<DownloadLink> removeList = new ArrayList<DownloadLink>();
            it = fp.getChildren().iterator();
            while (it.hasNext()) {
                localLink = it.next();
                if (CleanAfterDownloadAction.CLEANUP_ONCE_AT_STARTUP.equals(org.jdownloader.settings.staticreferences.CFG_GENERAL.CFG.getCleanupAfterDownloadAction()) && localLink.getLinkStatus().isFinished()) {
                    logger.info("Remove " + localLink.getName() + " because Finished and CleanupOnStartup!");
                    removeList.add(localLink);
                    continue;
                }
                /*
                 * reset not if already exist, offline or finished. plugin errors will be reset here because plugin can be fixed again
                 */
                localLink.getLinkStatus().resetStatus(LinkStatus.ERROR_ALREADYEXISTS, LinkStatus.ERROR_FILE_NOT_FOUND, LinkStatus.FINISHED, LinkStatus.ERROR_FATAL);

                /* assign defaultPlugin matching the hostname */
                try {
                    pluginForHost = null;
                    LazyHostPlugin hPlugin = HostPluginController.getInstance().get(localLink.getHost());
                    if (hPlugin != null) {
                        pluginForHost = hPlugin.getPrototype(null);
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
                if (pluginForHost == null) {
                    try {
                        if (fixWith.containsKey(localLink.getHost()) == false) {
                            for (LazyHostPlugin p : HostPluginController.getInstance().list()) {
                                try {
                                    if (p.getPrototype(null).rewriteHost(localLink)) {
                                        pluginForHost = p.getPrototype(null);
                                        break;
                                    }
                                } catch (final Throwable e) {
                                    logger.log(e);
                                }
                            }
                        } else {
                            PluginForHost rewriteWith = fixWith.get(localLink.getHost());
                            if (rewriteWith != null) {
                                rewriteWith.rewriteHost(localLink);
                                pluginForHost = rewriteWith;
                            }
                        }
                        if (pluginForHost != null) {
                            logger.info("Plugin " + pluginForHost.getHost() + " now handles " + localLink.getName());
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    fixWith.put(localLink.getHost(), pluginForHost);
                }
                if (pluginForHost != null) {
                    localLink.setDefaultPlugin(pluginForHost);
                } else {
                    logger.severe("Could not find plugin: " + localLink.getHost() + " for " + localLink.getName());
                }
            }
            if (removeList.size() > 0) {
                fp.getChildren().removeAll(removeList);
            }
            if (fp.getChildren().size() == 0) {
                /* remove empty packages */
                iterator.remove();
                continue;
            }
        }
    }

    public void removeListener(final DownloadControllerListener l) {
        broadcaster.removeListener(l);
    }

    /**
     * saves List of FilePackages to given File as ZippedJSon
     * 
     * @param packages
     * @param file
     */
    private boolean save(java.util.List<FilePackage> packages, File file) {
        synchronized (SAVELOADLOCK) {
            boolean ret = false;
            if (packages != null && file != null) {
                /* prepare tmp file */
                final File tmpfile = new File(file.getAbsolutePath() + ".tmp");
                final File bakfile = new File(file.getAbsolutePath() + ".bak");
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
                    try {
                        /*
                         * add current JDRoot directory to savefile so we can convert pathes if needed
                         */
                        String currentROOT = JDUtilities.getJDHomeDirectoryFromEnvironment().toString();
                        zip.addByteArry(currentROOT.getBytes("UTF-8"), true, "", getJDRootFileName());
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    /* close ZipIOWriter, so we can rename tmp file now */
                    try {
                        zip.close();
                    } catch (final Throwable e) {
                        return false;
                    }
                    /* backup existing file list to .bak */
                    if (file.exists()) {
                        if (bakfile.exists() == false || bakfile.delete() == true) {
                            if (!file.renameTo(bakfile)) {
                                logger.log(new WTFException("Could not backup old List: " + file.getAbsolutePath()));
                                return false;
                            }
                        } else {
                            logger.log(new WTFException("Could delete existing backup old List: " + bakfile.getAbsolutePath()));
                            return false;
                        }
                    }
                    /* rename tmpfile to destination file */
                    if (!tmpfile.renameTo(file)) {
                        logger.log(new WTFException("Could not rename file: " + tmpfile + " to " + file));
                        if (bakfile.exists() && bakfile.renameTo(file) == false) {
                            logger.log(new WTFException("Could not restore file: " + bakfile + " to " + file));
                        }
                        return false;
                    } else {
                        if (bakfile.exists() && bakfile.delete() == false) {
                            logger.log(new WTFException("Could delete backup old List: " + bakfile.getAbsolutePath()));
                        }
                        return true;
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    try {
                        zip.close();
                    } catch (final Throwable e) {
                    }
                }
            }
            return ret;
        }
    }

    /**
     * save the current FilePackages/DownloadLinks controlled by this DownloadController
     */
    public void saveDownloadLinks() {
        if (isSaveAllowed() == false) return;
        java.util.List<FilePackage> packages = null;
        final boolean readL = this.readLock();
        try {
            packages = new ArrayList<FilePackage>(this.packages);
        } finally {
            readUnlock(readL);
        }
        /* save as new Json ZipFile */
        save(packages, getDownloadListFile());
    }

    public void setLoadAllowed(boolean allowLoad) {
        this.allowLoad = allowLoad;
    }

    public void setSaveAllowed(boolean allowSave) {
        this.allowSave = allowSave;
    }

}
