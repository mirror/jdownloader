//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSE the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://gnu.org/licenses/>.

package org.jdownloader.extensions.extraction;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import jd.SecondLevelLaunch;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.packagecontroller.PackageControllerModifyVetoListener;
import jd.gui.swing.jdgui.menu.actions.sendlogs.LogAction;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ExceptionDialog;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.SeperatorData;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.actions.ExtractAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.contextmenu.ArchivesSubMenu;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.CleanupSubMenu;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.AutoExtractEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.CleanupAutoDeleteFilesEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.CleanupAutoDeleteLinksEnabledToggleAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ExtractArchiveNowAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.SetExtractPasswordAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.SetExtractToAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ShowExtractionResultAction;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.ValidateArchivesAction;
import org.jdownloader.extensions.extraction.gui.bubble.ExtractionBubbleSupport;
import org.jdownloader.extensions.extraction.gui.config.ExtractionConfigPanel;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.CheckException;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.Unix;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.mainmenu.container.OptionalContainer;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.gui.views.downloads.context.submenu.MoreMenuContainer;
import org.jdownloader.gui.views.downloads.contextmenumanager.MenuManagerDownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkGrabberMoreSubMenu;
import org.jdownloader.gui.views.linkgrabber.contextmenu.MenuManagerLinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig, ExtractionTranslation> implements FileCreationListener, MenuExtenderHandler, PackageControllerModifyVetoListener<FilePackage, DownloadLink> {

    private ExtractionQueue       extractionQueue = new ExtractionQueue();

    private ExtractionEventSender eventSender     = new ExtractionEventSender();

    public ExtractionEventSender getEventSender() {
        return eventSender;
    }

    private final java.util.List<IExtraction> extractors        = new ArrayList<IExtraction>();

    private final java.util.List<Archive>     archives          = new ArrayList<Archive>();

    private ExtractionConfigPanel             configPanel;

    private static ExtractionExtension        INSTANCE;

    private ExtractionListenerIcon            statusbarListener = null;

    private ShutdownVetoListener              listener          = null;

    private boolean                           lazyInitOnStart   = false;
    private static final Object               LOCK              = new Object();

    public ExtractionExtension() throws StartException {
        super();
        setTitle(_.name());
        INSTANCE = this;

    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    public static ExtractionExtension getIntance() {
        return INSTANCE;
    }

    /**
     * Adds all internal extraction plugins.
     */
    private void initExtractors() {
        /* the order is important because hjsplit and multi listen to same patterns (xy.001, because 7zip can have that pattern as well) */
        setExtractor(new Unix());
        setExtractor(new XtreamSplit());
        setExtractor(new HJSplit());
        /* must be last one! */
        setExtractor(new Multi());
    }

    /**
     * Adds an ectraction plugin to the framework.
     * 
     * @param extractor
     *            The exractor.
     */
    public void setExtractor(IExtraction extractor) {
        extractors.add(extractor);
        extractor.setLogger(logger);
    }

    /**
     * Checks if there is supported extractor.
     * 
     * @param file
     *            Path of the packed file
     * @return True if a extractor was found
     */
    public final boolean isLinkSupported(ArchiveFactory factory) {
        boolean deepInspection = !(factory instanceof CrawledLinkFactory);
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory, deepInspection)) { return true; }
        }

        return false;
    }

    public boolean isMultiPartArchive(ArchiveFactory factory) {
        boolean deepInspection = !(factory instanceof CrawledLinkFactory);
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory, deepInspection)) { return extractor.isMultiPartArchive(factory);

            }
        }
        return false;
    }

    /**
     * CReates and returns an id for the archive filenames belongs to.
     * 
     * @param factory
     *            TODO
     * 
     * @return
     */
    public String createArchiveID(ArchiveFactory factory) {
        boolean deepInspection = !(factory instanceof CrawledLinkFactory);
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory, deepInspection)) { return extractor.createID(factory);

            }
        }

        return null;
    }

    public String getArchiveName(ArchiveFactory factory) {
        boolean deepInspection = !(factory instanceof CrawledLinkFactory);
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory, deepInspection)) { return extractor.getArchiveName(factory);

            }
        }
        return null;
    }

    /**
     * Adds an archive to the extraction queue.
     */
    public synchronized ExtractionController addToQueue(final Archive archive, boolean forceAskForUnknownPassword) {
        // check if we have this archive already in
        // queue.
        for (ExtractionController ec : extractionQueue.getJobs()) {
            if (ec.getArchiv() == archive) return ec;
        }
        if (!archive.getFirstArchiveFile().isComplete()) {
            logger.info("First File is not complete: " + archive.getFirstArchiveFile());
            return null;
        }
        IExtraction extractor = getExtractorByFactory(archive.getFactory());
        if (extractor == null) return null;
        synchronized (archives) {
            archives.add(archive);
        }
        archive.getFactory().fireArchiveAddedToQueue(archive);
        ExtractionController controller = new ExtractionController(this, archive, extractor);
        controller.setAskForUnknownPassword(forceAskForUnknownPassword);
        controller.setOverwriteFiles(isOverwriteFiles(archive));
        controller.setRemoveAfterExtract(isRemoveFilesAfterExtractEnabled(archive));
        controller.setRemoveDownloadLinksAfterExtraction(isRemoveDownloadLinksAfterExtractEnabled(archive));
        archive.setActive(true);
        extractor.setConfig(getSettings());
        extractionQueue.addAsynch(controller);
        fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.QUEUED));
        return controller;
    }

    public boolean isRemoveDownloadLinksAfterExtractEnabled(Archive archive) {
        switch (archive.getSettings().getRemoveDownloadLinksAfterExtraction()) {
        case FALSE:
            return false;
        case TRUE:
            return true;

        }
        return getSettings().isDeleteArchiveDownloadlinksAfterExtraction();
    }

    public boolean isRemoveFilesAfterExtractEnabled(Archive archive) {
        switch (archive.getSettings().getRemoveFilesAfterExtraction()) {
        case FALSE:
            return false;
        case TRUE:
            return true;
        }
        return getSettings().isDeleteArchiveFilesAfterExtraction();
    }

    @Override
    public String getIconKey() {
        return "unpack";
    }

    /**
     * Builds an archive for an {@link DownloadLink}.
     * 
     * @param link
     * @return
     * @throws ArchiveException
     */
    public synchronized Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        synchronized (archives) {

            for (Archive archive : archives) {
                if (archive.contains(link)) {
                    // logger.info("Found Archive: " + archive);
                    return archive;

                }
            }
        }
        IExtraction extrctor = getExtractorByFactory(link);
        if (extrctor == null) {
            //
            return null;
        }
        Archive archive = extrctor.buildArchive(link);
        if (archive != null) {
            link.onArchiveFinished(archive);
            // logger.info("Created Archive: " + archive);
            // Log.L.info("Created Archive: " + archive);
            // logger.info("Files: " + archive.getArchiveFiles());
        }
        return archive;
    }

    public DummyArchive createDummyArchive(Archive archive) throws CheckException {
        IExtraction extrctor = getExtractorByFactory(archive.getFactory());
        if (extrctor != null) return extrctor.checkComplete(archive);
        return null;
    }

    public boolean isComplete(Archive archive) {
        try {
            DummyArchive ret = createDummyArchive(archive);
            if (ret != null) return ret.isComplete();
        } catch (CheckException e) {
            LogController.CL().log(e);
        }
        return false;
    }

    // /**
    // * Builds an dummy archive for an file.
    // *
    // * @param file
    // * @return
    // */
    // private Archive buildDummyArchive(final File file) {
    // final String lfile = file.getAbsolutePath();
    // List<DownloadLink> links =
    // DownloadController.getInstance().getChildrenByFilter(new
    // AbstractPackageChildrenNodeFilter<DownloadLink>() {
    //
    // public boolean isChildrenNodeFiltered(DownloadLink node) {
    // if (node.getFileOutput().equals(lfile)) {
    // if (node.getLinkStatus().hasStatus(LinkStatus.FINISHED)) return true;
    // }
    // return false;
    // }
    //
    // public int returnMaxResults() {
    // return 1;
    // }
    // });
    // if (links == null || links.size() == 0) {
    // /* link no longer in list */
    // DummyDownloadLink link0 = new DummyDownloadLink(file.getName());
    // link0.setFile(file);
    // return buildArchive(link0);
    // }
    // return buildArchive(links.get(0));
    // }

    /**
     * Returns the extractor for the {@link DownloadLink}.
     * 
     * @param link
     * @return
     */
    public IExtraction getExtractorByFactory(ArchiveFactory factory) {
        for (IExtraction extractor : extractors) {
            try {
                if (extractor.isArchivSupported(factory, true)) {
                    IExtraction ret = extractor.getClass().newInstance();
                    ret.setLogger(extractor.logger);
                    return ret;
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Finishes the extraction process.
     * 
     * @param controller
     */
    void onFinished(ExtractionController controller) {
        // controller.getArchiv().
        // getFgetFirstDownloadLink().setPluginProgress(null);
    }

    /**
     * Removes an {@link Archive} from the list.
     * 
     * @param archive
     */
    synchronized void removeArchive(Archive archive) {
        synchronized (archives) {
            archives.remove(archive);
        }
    }

    // @SuppressWarnings({ "unchecked", "deprecation" })
    // public void controlEvent(ControlEvent event) {
    // DownloadLink link;
    // switch (event.getEventID()) {
    //
    // case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:

    @Override
    protected void stop() throws StopException {
        ShutdownController.getInstance().removeShutdownVetoListener(listener);
        LinkCollector.getInstance().setArchiver(null);
        MenuManagerDownloadTableContext.getInstance().unregisterExtender(this);
        MenuManagerLinkgrabberTableContext.getInstance().unregisterExtender(this);
        DownloadController.getInstance().removeVetoListener(this);
        FileCreationManager.getInstance().getEventSender().removeListener(this);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (statusbarListener != null) {
                            statusbarListener.cleanup();
                            eventSender.removeListener(statusbarListener);
                        }
                    }
                };
            }
        });

    }

    public void addPassword(String pw) {
        if (StringUtils.isEmpty(pw)) return;
        synchronized (LOCK) {
            java.util.List<String> pwList = getSettings().getPasswordList();
            if (pwList == null) pwList = new ArrayList<String>();
            /* avoid duplicates */
            pwList.remove(pw);
            pwList.add(0, pw);
            getSettings().setPasswordList(pwList);
        }
    }

    @Override
    protected void start() throws StartException {
        lazyInitOnceOnStart();
        MenuManagerDownloadTableContext.getInstance().registerExtender(this);
        MenuManagerLinkgrabberTableContext.getInstance().registerExtender(this);
        MenuManagerMainmenu.getInstance().registerExtender(this);
        MenuManagerMainToolbar.getInstance().registerExtender(this);
        LinkCollector.getInstance().setArchiver(this);
        DownloadController.getInstance().addVetoListener(this);

        FileCreationManager.getInstance().getEventSender().addListener(this);
        SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (statusbarListener != null) statusbarListener.cleanup();
                        eventSender.addListener(statusbarListener = new ExtractionListenerIcon(ExtractionExtension.this));
                        ExtractionBubbleSupport bubbleSupport = new ExtractionBubbleSupport(T._.bubbletype(), CFG_EXTRACTION.BUBBLE_ENABLED_IF_ARCHIVE_EXTRACTION_IS_IN_PROGRESS);
                        eventSender.addListener(bubbleSupport);
                        BubbleNotify.getInstance().registerType(bubbleSupport);
                    }
                };
            }
        });
        ShutdownController.getInstance().addShutdownVetoListener(listener = new ShutdownVetoListener() {

            @Override
            public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
                if (request.hasVetos()) {
                    /* we already abort shutdown, no need to ask again */
                    return;
                }
                if (request.isSilent()) {
                    if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) { throw new ShutdownVetoException("ExtractionExtension is still running", this); }
                } else {
                    if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) {

                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.Extraction_onShutdownRequest_(), _JDT._.Extraction_onShutdownRequest_msg(), NewTheme.I().getIcon("unpack", 32), _JDT._.literally_yes(), null)) { return; }
                        throw new ShutdownVetoException("ExtractionExtension is still running", this);
                    }
                }
            }

            @Override
            public long getShutdownVetoPriority() {
                return 0;
            }

            @Override
            public void onShutdown(ShutdownRequest request) {
            }

            @Override
            public void onShutdownVeto(ShutdownRequest request) {
            }

        });
        new Thread() {
            public void run() {
                try {
                    FileCreationManager.getInstance().mkdir(Application.getResource("logs/extracting/open"));
                    File[] files = Application.getResource("logs/extracting/open").listFiles();
                    if (files != null) {
                        String latestLog = null;
                        for (File f : files) {
                            if (f.getName().matches("\\w+\\.txt")) {
                                getLogger().log(new Exception("Extraction Crashlog found! " + f.getName()));
                                int i = 1;
                                File renamedTo = new File(f.getParentFile().getParentFile(), "crashed_" + i + "_" + f.getName());

                                while (renamedTo.exists()) {
                                    i++;
                                    renamedTo = new File(f.getParentFile().getParentFile(), "crashed_" + i + "_" + f.getName());
                                }
                                f.renameTo(renamedTo);
                                byte[] bytes = IO.readFile(renamedTo, 512 * 1024);
                                latestLog = new String(bytes, "UTF-8");
                                bytes = null;
                                getLogger().info(latestLog);
                            }
                        }
                        if (StringUtils.isNotEmpty(latestLog)) {
                            ExceptionDialog ed = new ExceptionDialog(0, T._.crash_title(), T._.crash_message(), null, null, null);
                            ed.setMore(latestLog);
                            Dialog.getInstance().showDialog(ed);
                            LogAction la = new LogAction();
                            la.actionPerformed(null);
                        }
                    }
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }.start();
    }

    private void lazyInitOnceOnStart() {
        if (lazyInitOnStart) return;
        lazyInitOnStart = true;
        initExtractors();

        // addListener(new ExtractionListenerFile());
        eventSender.addListener(new ExtractionListenerList());

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                logger.severe("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                it.remove();
            }
        }

    }

    void fireEvent(ExtractionEvent event) {
        eventSender.fireEvent(event);
    }

    @Override
    public void handleCommand(String command, String... parameters) {

        if (command.equalsIgnoreCase("add-passwords") || command.equalsIgnoreCase("add-passwords") || command.equalsIgnoreCase("p")) {
            List<String> lst = getSettings().getPasswordList();
            ArrayList<String> ret = new ArrayList<String>();
            if (lst != null) ret.addAll(lst);
            Collection<String> newPws = Arrays.asList(parameters);
            ret.removeAll(newPws);
            ret.addAll(0, newPws);
            getSettings().setPasswordList(ret);
            logger.info("Added Passwords: " + newPws + " New List Size: " + ret.size());

        }

    }

    // public void handleStartupParameters(ParameterParser parameters) {
    // +#
    // cs=parameters.getCommandSwitch(")
    //
    // ;
    // }
    @Override
    protected void initExtension() throws StartException {
        /* import old passwordlist */
        boolean oldPWListImported = false;
        ArchiveValidator.EXTENSION = this;

        try {
            if ((oldPWListImported = getSettings().isOldPWListImported()) == false) {
                SubConfiguration oldConfig = SubConfiguration.getConfig("PASSWORDLIST", true);
                Object oldList = oldConfig.getProperties().get("LIST2");
                java.util.List<String> currentList = getSettings().getPasswordList();
                if (currentList == null) currentList = new ArrayList<String>();
                if (oldList != null && oldList instanceof List) {
                    for (Object item : (List<?>) oldList) {
                        if (item != null && item instanceof String) {
                            String pw = (String) item;
                            currentList.remove(pw);
                            currentList.add(pw);
                        }
                    }
                }
                getSettings().setPasswordList(currentList);
            }
        } catch (final Throwable e) {
            logger.log(e);
        } finally {
            if (oldPWListImported == false) {
                getSettings().setOldPWListImported(true);
            }
        }

    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getDescription() {
        return _.description();
    }

    @Override
    public AddonPanel<ExtractionExtension> getGUI() {
        return null;
    }

    @Override
    public ExtensionConfigPanel<ExtractionExtension> getConfigPanel() {
        if (configPanel != null) return configPanel;
        return new EDTHelper<ExtractionConfigPanel>() {
            @Override
            public ExtractionConfigPanel edtRun() {
                if (configPanel != null) return configPanel;
                configPanel = new ExtractionConfigPanel(ExtractionExtension.this);
                return configPanel;
            }
        }.getReturnValue();
    }

    public ExtractionQueue getJobQueue() {
        return extractionQueue;
    }

    /**
     * Cancels a job
     * 
     * @param activeValue
     */
    public void cancel(ExtractionController activeValue) {
        boolean wasInProgress = getJobQueue().isInProgress(activeValue);
        getJobQueue().remove(activeValue);
        if (wasInProgress) fireEvent(new ExtractionEvent(activeValue, ExtractionEvent.Type.CLEANUP));
    }

    private boolean onNewFile(Archive archive) {
        if (archive == null) {
            logger.info("Archive not supported!");
            return false;
        }
        if (archive.getArchiveFiles().size() < 1) {
            logger.info("Archive:" + archive.getName() + "|Empty");
            return false;
        }
        if (archive.isActive()) {
            logger.info("Archive:" + archive.getName() + "|Active");
            return false;
        }
        if (!isAutoExtractEnabled(archive)) {
            logger.info("Archive:" + archive.getName() + "|AutoExtractionDisabled");
            return false;
        }
        boolean complete = isComplete(archive);
        if (complete) {
            logger.info("Archive:" + archive.getName() + "|Complete|Size:" + archive.getArchiveFiles().size());
            return true;
        } else {
            logger.info("Archive:" + archive.getName() + "|Incomplete");
            return false;
        }
    }

    public void onNewFile(Object caller, File[] fileList) {
        try {
            if (caller instanceof SingleDownloadController) {
                DownloadLink link = ((SingleDownloadController) caller).getDownloadLink();
                DownloadLinkArchiveFactory dlFactory = new DownloadLinkArchiveFactory(link);
                if (isLinkSupported(dlFactory)) {
                    Archive archive = buildArchive(dlFactory);
                    if (onNewFile(archive)) {
                        addToQueue(archive, false);
                    }
                }
            } else if (caller instanceof ExtractionController) {
                if (getSettings().isDeepExtractionEnabled()) {
                    try {
                        for (File archiveStartFile : fileList) {
                            FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                            if (isLinkSupported(fac)) {
                                Archive archive = buildArchive(fac);
                                if (onNewFile(archive)) {
                                    archive.getSettings().setExtractPath(archiveStartFile.getParent());
                                    addToQueue(archive, false);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.log(e);
                    }
                }
            } else {
                try {
                    for (File archiveStartFile : fileList) {
                        FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                        if (isLinkSupported(fac)) {
                            Archive archive = buildArchive(fac);
                            if (onNewFile(archive)) {
                                addToQueue(archive, false);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }

    public boolean isAutoExtractEnabled(Archive archive) {
        switch (archive.getSettings().getAutoExtract()) {
        case FALSE:
            return false;
        case TRUE:
            return true;
        case UNSET:
            return CFG_LINKGRABBER.AUTO_EXTRACTION_ENABLED.isEnabled();
        }
        return false;

    }

    public Archive getArchiveByFactory(ArchiveFactory clf) {

        try {
            return buildArchive(clf);
        } catch (ArchiveException e) {
            e.printStackTrace();
            return null;
        }

    }

    public boolean isOverwriteFiles(Archive archive) {
        switch (archive.getSettings().getOverwriteFiles()) {
        case FALSE:
            return false;
        case TRUE:
            return true;
        case UNSET:
            return getSettings().isOverwriteExistingFilesEnabled();
        }
        return false;
    }

    public File getFinalExtractToFolder(Archive archive) {
        String path = null;

        if (StringUtils.isEmpty(path)) {
            path = archive.getSettings().getExtractPath();
            if (!StringUtils.isEmpty(path)) {
                /* use customized extracttofolder */
                path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME);

                path = archive.getFactory().createExtractSubPath(path, archive);
                File ret = new File(path);
                ret = appendSubFolder(archive, ret);
                return ret;
            }
        }
        if (getSettings().isCustomExtractionPathEnabled()) {
            /* customized extractpath is enabled */
            path = getSettings().getCustomExtractionPath();
        }
        if (StringUtils.isEmpty(path)) {
            /* extractpath is still emptry, create default one */
            path = archive.getFactory().createDefaultExtractToPath(archive);
        }
        if (StringUtils.isEmpty(path)) return null;

        path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME);

        path = archive.getFactory().createExtractSubPath(path, archive);
        File ret = new File(path);
        ret = appendSubFolder(archive, ret);

        return ret;
    }

    /**
     * @param archive
     * @param ret
     * @return
     */
    protected File appendSubFolder(Archive archive, File ret) {
        if (getSettings().isSubpathEnabled()) {

            if (archive.getContentView().getFileCount() < getSettings().getSubPathMinFilesTreshhold()) {
                logger.info("No Subfolder because Root contains only " + archive.getContentView().getFileCount() + " files");
                return ret;
            }
            if (archive.getContentView().getDirectoryCount() < getSettings().getSubPathMinFoldersTreshhold()) {
                logger.info("No Subfolder because Root contains only " + archive.getContentView().getDirectoryCount() + " folders");
                return ret;
            }
            if (archive.getContentView().getDirectoryCount() + archive.getContentView().getFileCount() < getSettings().getSubPathMinFilesOrFoldersTreshhold()) {
                logger.info("No Subfolder because Root contains only " + (archive.getContentView().getDirectoryCount() + archive.getContentView().getFileCount()) + " files and folders");

                return ret;
            }

            String sub = getSettings().getSubPath();
            if (!StringUtils.isEmpty(sub)) {
                sub = archive.getFactory().createExtractSubPath(sub, archive);

                if (!StringUtils.isEmpty(sub)) {
                    sub = sub.trim();
                    ret = new File(ret, sub);
                }
            }
        }
        return ret;
    }

    public java.util.List<IExtraction> getExtractors() {
        return extractors;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {

        if (manager instanceof MenuManagerMainToolbar) {
            return updateMainToolbar(mr);
        } else if (manager instanceof MenuManagerMainmenu) {
            return updateMainMenu(mr);
        } else if (manager instanceof MenuManagerLinkgrabberTableContext) {
            int addonLinkIndex = 0;
            for (int i = 0; i < mr.getItems().size(); i++) {
                if (mr.getItems().get(i) instanceof LinkGrabberMoreSubMenu) {
                    addonLinkIndex = i;
                    break;
                }
            }

            ArchivesSubMenu root;
            mr.getItems().add(addonLinkIndex, root = new ArchivesSubMenu());
            root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));
            root.add(new SeperatorData());
            root.add(new MenuItemData(new ActionData(AutoExtractEnabledToggleAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));
            CleanupSubMenu cleanup = new CleanupSubMenu();
            root.add(cleanup);
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesEnabledToggleAction.class)));
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksEnabledToggleAction.class)));
            return null;

        } else if (manager instanceof MenuManagerDownloadTableContext) {
            int addonLinkIndex = 0;
            for (int i = 0; i < mr.getItems().size(); i++) {
                if (mr.getItems().get(i) instanceof MoreMenuContainer) {
                    addonLinkIndex = i;
                    break;
                }
            }

            ArchivesSubMenu root;
            mr.getItems().add(addonLinkIndex, root = new ArchivesSubMenu());
            root.add(new MenuItemData(new ActionData(ExtractArchiveNowAction.class)));
            root.add(new MenuItemData(new ActionData(ShowExtractionResultAction.class)));
            root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));

            root.add(new SeperatorData());
            root.add(new MenuItemData(new ActionData(AutoExtractEnabledToggleAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));
            CleanupSubMenu cleanup = new CleanupSubMenu();
            root.add(cleanup);
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesEnabledToggleAction.class)));
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksEnabledToggleAction.class)));
            return null;
        }
        return null;
    }

    private MenuItemData updateMainMenu(MenuContainerRoot mr) {
        ExtensionsMenuContainer container = new ExtensionsMenuContainer();
        container.add(ExtractAction.class);
        return container;

    }

    private MenuItemData updateMainToolbar(MenuContainerRoot mr) {
        OptionalContainer opt = new OptionalContainer(false);
        opt.add(ExtractAction.class);
        return opt;
    }

    @Override
    public boolean onAskToRemovePackage(FilePackage pkg) {
        boolean readL = pkg.getModifyLock().readLock();
        List<DownloadLink> copy = null;
        try {
            copy = new ArrayList<DownloadLink>(pkg.getChildren());
        } finally {
            pkg.getModifyLock().readUnlock(readL);
        }
        return onAskToRemoveChildren(copy);

    }

    @Override
    public boolean onAskToRemoveChildren(List<DownloadLink> children) {
        synchronized (archives) {
            for (DownloadLink dlink : children) {
                DownloadLinkArchiveFactory link = new DownloadLinkArchiveFactory(dlink);
                for (Archive archive : archives) {
                    if (archive.contains(link)) {
                        logger.info("Link is in active Archive do not remove: " + archive);
                        return false;

                    }
                }
            }
        }
        return true;
    }

}