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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;

import jd.config.NoOldJDDataBaseFoundException;
import jd.controlling.IOEQ;
import jd.controlling.packagecontroller.AbstractNode;
import jd.controlling.packagecontroller.PackageController;
import jd.parser.Regex;
import jd.plugins.DeleteTo;
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
import org.appwork.uio.IconDialog;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.event.Eventsender;
import org.appwork.utils.event.queue.Queue.QueuePriority;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.OKCancelCloseUserIODefinition.CloseReason;
import org.appwork.utils.zip.ZipIOReader;
import org.appwork.utils.zip.ZipIOWriter;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.gui.views.downloads.action.ConfirmDeleteLinksDialog;
import org.jdownloader.gui.views.downloads.action.ConfirmDeleteLinksDialogInterface;
import org.jdownloader.gui.views.downloads.overviewpanel.AggregatedNumbers;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.CleanAfterDownloadAction;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.utils.JDFileUtils;

public class DownloadController extends PackageController<FilePackage, DownloadLink> {

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
                System.out.println("ASYNCH SAVE");
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
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.ADD_CONTENT, pkg));
    }

    @Override
    protected void _controllerPackageNodeRemoved(FilePackage pkg, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, pkg));
    }

    @Override
    protected void _controllerParentlessLinks(final List<DownloadLink> links, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.REMOVE_CONTENT, links.toArray()));
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

    public void addListener(final DownloadControllerListener l, boolean weak) {
        broadcaster.addListener(l, weak);
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

    private ArrayList<File> getAvailableDownloadLists() {
        File[] filesInCfg = Application.getResource("cfg/").listFiles();
        ArrayList<Long> sortedAvailable = new ArrayList<Long>();
        ArrayList<File> ret = new ArrayList<File>();
        if (filesInCfg != null) {
            for (File downloadList : filesInCfg) {
                if (downloadList.isFile() && downloadList.getName().startsWith("downloadList")) {
                    String counter = new Regex(downloadList.getName(), "downloadList(\\d+)\\.zip").getMatch(0);
                    if (counter != null) sortedAvailable.add(Long.parseLong(counter));
                }
            }
            Collections.sort(sortedAvailable, Collections.reverseOrder());
        }
        for (Long loadOrder : sortedAvailable) {
            ret.add(Application.getResource("cfg/downloadList" + loadOrder + ".zip"));
        }
        if (Application.getResource("cfg/downloadList.zip").exists()) {
            ret.add(Application.getResource("cfg/downloadList.zip"));
        }
        return ret;
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
        for (File downloadList : getAvailableDownloadLists()) {
            try {
                lpackages = load(downloadList);
                if (lpackages != null) {
                    break;
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
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
                    broadcaster.fireEvent(new DownloadControllerEvent(DownloadController.this, DownloadControllerEvent.TYPE.DOWNLOADLINKS_LOADED));
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
                    /* lets restore the FilePackages from Json */
                    HashMap<Integer, FilePackage> map = new HashMap<Integer, FilePackage>();
                    DownloadControllerStorable dcs = null;
                    InputStream is = null;
                    for (ZipEntry entry : zip.getZipFiles()) {
                        try {
                            if (entry.getName().matches("^\\d+$")) {
                                int packageIndex = Integer.parseInt(entry.getName());
                                is = zip.getInputStream(entry);
                                byte[] bytes = IO.readStream((int) entry.getSize(), is);
                                String json = new String(bytes, "UTF-8");
                                bytes = null;
                                FilePackageStorable storable = JSonStorage.stringToObject(json, new TypeRef<FilePackageStorable>() {
                                }, null);
                                json = null;
                                if (storable != null) {
                                    map.put(packageIndex, storable._getFilePackage());
                                } else {
                                    throw new WTFException("restored a null FilePackageStorable");
                                }
                            } else if ("extraInfo".equalsIgnoreCase(entry.getName())) {
                                is = zip.getInputStream(entry);
                                byte[] bytes = IO.readStream((int) entry.getSize(), is);
                                String json = new String(bytes, "UTF-8");
                                bytes = null;
                                dcs = JSonStorage.stringToObject(json, new TypeRef<DownloadControllerStorable>() {
                                }, null);
                                json = null;
                            }
                        } finally {
                            try {
                                is.close();
                            } catch (final Throwable e) {
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
                    if (dcs != null && JsonConfig.create(GeneralSettings.class).isConvertRelativePathesJDRoot()) {
                        try {
                            String oldRootPath = dcs.getRootPath();
                            if (!StringUtils.isEmpty(oldRootPath)) {
                                String newRoot = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
                                /*
                                 * convert pathes relative to JDownloader root,only in jared version
                                 */
                                for (FilePackage pkg : ret2) {
                                    if (!CrossSystem.isAbsolutePath(pkg.getDownloadDirectory())) {
                                        /* no need to convert relative pathes */
                                        continue;
                                    }
                                    String pkgPath = LinkTreeUtils.getDownloadDirectory(pkg).getAbsolutePath();
                                    if (pkgPath.startsWith(oldRootPath + "/")) {
                                        /*
                                         * folder is inside JDRoot, lets update it
                                         */
                                        String restPath = pkgPath.substring(oldRootPath.length());
                                        String newPath = new File(newRoot, restPath).getAbsolutePath();
                                        pkg.setDownloadDirectory(newPath);
                                    }
                                }
                            }
                        } catch (final Throwable e) {
                            /* this method can throw exceptions, eg in SVN */
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
    private boolean save(java.util.List<FilePackage> packages, File file) throws IOException {
        synchronized (SAVELOADLOCK) {
            List<File> availableDownloadLists = null;
            if (file == null) {
                availableDownloadLists = getAvailableDownloadLists();
                if (availableDownloadLists.size() > 0) {
                    String counter = new Regex(availableDownloadLists.get(0).getName(), "downloadList(\\d+)\\.zip").getMatch(0);
                    long count = 1;
                    if (counter != null) {
                        count = Long.parseLong(counter) + 1;
                    }
                    file = Application.getResource("cfg/downloadList" + count + ".zip");
                }
                if (file == null) file = Application.getResource("cfg/downloadList.zip");
            }
            if (packages != null && file != null) {
                if (file.exists()) {
                    if (file.isDirectory()) throw new IOException("File " + file + " is a directory");
                    if (file.delete() == false) throw new IOException("Could not delete file " + file);
                } else {
                    if (file.getParentFile().exists() == false && file.getParentFile().mkdirs() == false) throw new IOException("Could not create parentFolder for file " + file);
                }
                /* prepare formatter(001,0001...) for package filenames in zipfiles */
                String format = "%02d";
                if (packages.size() >= 10) {
                    format = String.format("%%0%dd", (int) Math.log10(packages.size()) + 1);
                }
                boolean deleteFile = true;
                ZipIOWriter zip = null;
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    zip = new ZipIOWriter(new BufferedOutputStream(fos, 32767));
                    int index = 0;
                    for (FilePackage pkg : packages) {
                        /* convert FilePackage to JSon */
                        FilePackageStorable storable = new FilePackageStorable(pkg);
                        String string = JSonStorage.toString(storable);
                        storable = null;
                        byte[] bytes = string.getBytes("UTF-8");
                        string = null;
                        zip.addByteArry(bytes, true, "", String.format(format, (index++)));
                    }
                    DownloadControllerStorable dcs = new DownloadControllerStorable();
                    try {
                        /*
                         * set current RootPath of JDownloader, so we can update it when user moves JDownloader folder
                         */
                        dcs.setRootPath(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath());
                    } catch (final Throwable e) {
                        /* the method above can throw exceptions, eg in SVN */
                        logger.log(e);
                    }
                    zip.addByteArry(JSonStorage.toString(dcs).getBytes("UTF-8"), true, "", "extraInfo");
                    /* close ZipIOWriter */
                    zip.close();
                    deleteFile = false;
                    try {
                        int keepXOld = Math.max(JsonConfig.create(GeneralSettings.class).getKeepXOldLists(), 0);
                        if (availableDownloadLists != null && availableDownloadLists.size() > keepXOld) {
                            availableDownloadLists = availableDownloadLists.subList(keepXOld, availableDownloadLists.size());
                            for (File oldDownloadList : availableDownloadLists) {
                                logger.info("Delete outdated DownloadList: " + oldDownloadList + " " + oldDownloadList.delete());
                            }
                        }
                    } catch (final Throwable e) {
                        logger.log(e);
                    }
                    return true;
                } catch (final Throwable e) {
                    logger.log(e);
                } finally {
                    try {
                        fos.close();
                    } catch (final Throwable e) {
                    }
                    if (deleteFile && file.exists()) {
                        file.delete();
                    }
                }
            }
            return false;
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
        try {
            save(packages, null);
        } catch (IOException e) {
            logger.log(e);
        }
    }

    public void setLoadAllowed(boolean allowLoad) {
        this.allowLoad = allowLoad;
    }

    public void setSaveAllowed(boolean allowSave) {
        this.allowSave = allowSave;
    }

    @Override
    public void nodeUpdated(AbstractNode source, jd.controlling.packagecontroller.AbstractNodeNotifier.NOTIFY notify, Object param) {
        super.nodeUpdated(source, notify, param);
        if (source instanceof DownloadLink) {
            switch (notify) {
            case STRUCTURE_CHANCE:
                broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE, new Object[] { source, param }));
                break;
            case PROPERTY_CHANCE:
                broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_CONTENT, new Object[] { source, param }));
                break;
            }
        }
    }

    @Override
    protected void _controllerPackageNodeStructureChanged(FilePackage pkg, QueuePriority priority) {
        broadcaster.fireEvent(new DownloadControllerEvent(this, DownloadControllerEvent.TYPE.REFRESH_STRUCTURE, pkg));
    }

    public void set(DownloadLinkWalker filter) {

        final boolean readL = readLock();
        try {
            for (final FilePackage fp : packages) {
                if (!filter.accept(fp)) continue;
                synchronized (fp) {
                    for (DownloadLink dl : fp.getChildren()) {
                        if (filter.accept(dl)) {
                            filter.handle(dl);
                        }
                    }
                }
            }
        } finally {
            readUnlock(readL);
        }
    }

    public static void deleteLinksRequest(final SelectionInfo<FilePackage, DownloadLink> si, String msg) {
        AggregatedNumbers agg = new AggregatedNumbers(si);
        if (agg.getLinkCount() == 0) {

            UIOManager.I().show(MessageDialogInterface.class, new IconDialog(0, _GUI._.lit_ups_something_is_wrong(), _GUI._.DownloadController_deleteLinksRequest_nolinks(), NewTheme.I().getIcon("robot_sos", 256), null));
            return;
        }
        boolean confirmed = si.isShiftDown();

        ConfirmDeleteLinksDialog dialog = new ConfirmDeleteLinksDialog(msg + "\r\n" + _GUI._.DeleteSelectionAction_actionPerformed_affected(agg.getLinkCount(), agg.getLoadedBytesString(), DownloadController.getInstance().getChildrenCount() - agg.getLinkCount()), agg.getLoadedBytes());
        dialog.setRecycleSupported(JDFileUtils.isTrashSupported());

        dialog.setDeleteFilesFromDiskEnabled(si.isShiftDown());
        final ConfirmDeleteLinksDialogInterface d = si.isAvoidRlyEnabled() ? new ConfirmDeleteLinksDialogInterface() {

            @Override
            public String getMessage() {
                return null;
            }

            @Override
            public String getTitle() {
                return null;
            }

            @Override
            public CloseReason getCloseReason() {
                return CloseReason.OK;
            }

            @Override
            public boolean isDeleteFilesFromDiskEnabled() {
                // Fehlerhafte zurücksetzen
                return si.isShiftDown();
            }

            @Override
            public boolean isDeleteFilesToRecycle() {
                return false;
            }

            @Override
            public boolean isRecycleSupported() {
                return false;
            }
        } : UIOManager.I().show(ConfirmDeleteLinksDialogInterface.class, dialog);
        final boolean deleteFiles = d.isDeleteFilesFromDiskEnabled();
        confirmed = d.getCloseReason() == CloseReason.OK;

        final boolean toRecycle = d.isDeleteFilesToRecycle();

        if (confirmed) {
            IOEQ.add(new Runnable() {

                public void run() {

                    DownloadController.getInstance().removeChildren(si.getChildren());

                    if (deleteFiles) {
                        for (DownloadLink dl : si.getChildren()) {
                            dl.deleteFile(toRecycle ? DeleteTo.RECYCLE : DeleteTo.NULL, true, true);

                        }
                    }
                }

            }, true);
        }
    }
}
