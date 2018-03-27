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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

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
import org.appwork.uio.ExceptionDialogInterface;
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
import org.jdownloader.controlling.contextmenu.SeparatorData;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.actions.ExtractAction;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
import org.jdownloader.extensions.extraction.contextmenu.ArchivesSubMenu;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.CleanupSubMenu;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.action.AbortExtractionAction;
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
import org.jdownloader.extensions.extraction.multi.Zip4J;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.HachaSplit;
import org.jdownloader.extensions.extraction.split.UnixSplit;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
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
import org.jdownloader.settings.IfFileExistsAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;
import org.jdownloader.translate._JDT;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig, ExtractionTranslation> implements FileCreationListener, MenuExtenderHandler, PackageControllerModifyVetoListener<FilePackage, DownloadLink> {

    private ExtractionQueue       extractionQueue = new ExtractionQueue();

    private ExtractionEventSender eventSender     = new ExtractionEventSender();

    public ExtractionEventSender getEventSender() {
        return eventSender;
    }

    private final Set<IExtraction>     extractors        = new CopyOnWriteArraySet<IExtraction>();

    private ExtractionConfigPanel      configPanel;

    private static ExtractionExtension INSTANCE;

    private ExtractionListenerIcon     statusbarListener = null;
    private ShutdownVetoListener       listener          = null;
    private boolean                    lazyInitOnStart   = false;
    private final Object               PWLOCK            = new Object();

    public ExtractionExtension() throws StartException {
        super();
        setTitle(T.name());
        INSTANCE = this;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    public static ExtractionExtension getInstance() {
        return INSTANCE;
    }

    /**
     * Adds all internal extraction plugins.
     */
    private void initExtractors() {
        /* the order is important because hjsplit and multi listen to same patterns (xy.001, because 7zip can have that pattern as well) */
        setExtractor(new UnixSplit());
        setExtractor(new XtreamSplit());
        setExtractor(new HachaSplit());
        setExtractor(new HJSplit());
        setExtractor(new Zip4J());
        /* must be last one! */
        setExtractor(new Multi());
    }

    /**
     * Adds an extraction plugin to the framework.
     *
     * @param extractor
     *            The extractor.
     */
    public void setExtractor(IExtraction extractor) {
        extractors.add(extractor);
        extractor.setLogger(logger);
    }

    /**
     * Adds an archive to the extraction queue.
     */
    public synchronized ExtractionController addToQueue(final Archive archive, boolean forceAskForUnknownPassword) {
        // check if we have this archive already in queue.
        for (final ExtractionController ec : extractionQueue.getJobs()) {
            final Archive eca = ec.getArchive();
            if (eca == archive || StringUtils.equals(eca.getArchiveID(), archive.getArchiveID())) {
                return ec;
            }
        }
        final ExtractionController currentController = extractionQueue.getCurrentQueueEntry();
        if (currentController != null) {
            final Archive ca = currentController.getArchive();
            if (ca == archive || StringUtils.equals(ca.getArchiveID(), archive.getArchiveID())) {
                return currentController;
            }
        }
        if (archive.getArchiveFiles().size() == 0) {
            logger.info("Archive:" + archive.getName() + "|Empty");
            return null;
        }
        if (isComplete(archive) == false) {
            logger.info("Archive:" + archive.getName() + "|Incomplete");
            return null;
        }
        final IExtraction extractor = getExtractorInstanceByFactory(archive.getFactory());
        if (extractor == null) {
            logger.info("Archive:" + archive.getName() + "|Unsupported");
            return null;
        }
        archive.getFactory().fireArchiveAddedToQueue(archive);
        final ExtractionController controller = new ExtractionController(this, archive, extractor);
        controller.setAskForUnknownPassword(forceAskForUnknownPassword);
        controller.setIfFileExistsAction(getIfFileExistsAction(archive));
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
        default:
            return getSettings().isDeleteArchiveDownloadlinksAfterExtraction();
        }
    }

    public FileCreationManager.DeleteOption getRemoveFilesAfterExtractAction(Archive archive) {
        switch (archive.getSettings().getRemoveFilesAfterExtraction()) {
        case FALSE:
            return FileCreationManager.DeleteOption.NO_DELETE;
        case TRUE:
            switch (getSettings().getDeleteArchiveFilesAfterExtractionAction()) {
            case NO_DELETE:
            case RECYCLE:
                return FileCreationManager.DeleteOption.RECYCLE;
            default:
                return FileCreationManager.DeleteOption.NULL;
            }
        default:
            return getSettings().getDeleteArchiveFilesAfterExtractionAction();
        }
    }

    @Override
    public String getIconKey() {
        return org.jdownloader.gui.IconKey.ICON_EXTRACT;
    }

    /**
     * Builds an archive for an {@link DownloadLink}.
     *
     * @param link
     * @return
     * @throws ArchiveException
     */
    public Archive buildArchive(final ArchiveFactory factory) throws ArchiveException {
        if (!Boolean.FALSE.equals(factory.isPartOfAnArchive())) {
            final Archive existing = getExistingArchive(factory);
            if (existing == null) {
                Throwable throwable = null;
                final boolean deepInspection = !(factory instanceof CrawledLinkFactory);
                for (IExtraction extractor : extractors) {
                    try {
                        if (!Boolean.FALSE.equals(extractor.isSupported(factory, deepInspection))) {
                            final Archive archive = extractor.buildArchive(factory, deepInspection);
                            if (archive != null) {
                                for (final ArchiveFile archiveFile : archive.getArchiveFiles()) {
                                    archiveFile.setArchive(archive);
                                }
                                return archive;
                            }
                        }
                    } catch (final Throwable e) {
                        throwable = e;
                        logger.log(e);
                    }
                }
                if (throwable == null) {
                    factory.setPartOfAnArchive(false);
                }
            }
            return existing;
        }
        return null;
    }

    public DummyArchive createDummyArchive(final Archive archive) throws CheckException {
        final ArchiveFactory factory = archive.getFactory();
        if (!Boolean.FALSE.equals(factory.isPartOfAnArchive())) {
            final boolean deepInspection = !(factory instanceof CrawledLinkFactory);
            for (IExtraction extractor : extractors) {
                try {
                    if (!Boolean.FALSE.equals(extractor.isSupported(factory, deepInspection))) {
                        final DummyArchive dummyArchive = extractor.checkComplete(archive);
                        if (dummyArchive != null) {
                            return dummyArchive;
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
        return null;
    }

    public boolean isComplete(Archive archive) {
        try {
            final DummyArchive ret = createDummyArchive(archive);
            return ret != null && ret.isComplete();
        } catch (CheckException e) {
            LogController.CL().log(e);
        }
        return false;
    }

    /**
     * Returns the extractor for the {@link DownloadLink}.
     *
     * @param link
     * @return
     */
    private IExtraction getExtractorInstanceByFactory(final ArchiveFactory factory) {
        if (!Boolean.FALSE.equals(factory.isPartOfAnArchive())) {
            for (IExtraction extractor : extractors) {
                try {
                    if (Boolean.TRUE.equals(extractor.isSupported(factory, true))) {
                        final IExtraction ret = extractor.getClass().newInstance();
                        ret.setLogger(extractor.logger);
                        return ret;
                    }
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }
        return null;
    }

    @Override
    protected void stop() throws StopException {
        ShutdownController.getInstance().removeShutdownVetoListener(listener);
        LinkCollector.getInstance().setArchiver(null);
        MenuManagerDownloadTableContext.getInstance().unregisterExtender(this);
        MenuManagerLinkgrabberTableContext.getInstance().unregisterExtender(this);
        DownloadController.getInstance().removeVetoListener(this);
        FileCreationManager.getInstance().getEventSender().removeListener(this);
        if (!org.appwork.utils.Application.isHeadless()) {
            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {

                public void run() {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {

                            if (statusbarListener != null) {
                                statusbarListener.cleanup();
                                eventSender.removeListener(statusbarListener);
                            }
                            if (bubbleSupport != null) {
                                eventSender.removeListener(bubbleSupport);
                                BubbleNotify.getInstance().unregisterTypes(bubbleSupport);
                            }
                        }
                    };
                }
            });
        }
    }

    public void addPassword(String pw) {
        if (StringUtils.isEmpty(pw)) {
            return;
        }
        synchronized (PWLOCK) {
            java.util.List<String> pwList = getSettings().getPasswordList();
            if (pwList == null) {
                pwList = new ArrayList<String>();
            }
            /* avoid duplicates */
            pwList.remove(pw);
            pwList.add(0, pw);
            getSettings().setPasswordList(pwList);
        }
    }

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    private ExtractionBubbleSupport bubbleSupport;

    @Override
    protected void start() throws StartException {
        lazyInitOnceOnStart();
        if (!org.appwork.utils.Application.isHeadless()) {
            MenuManagerDownloadTableContext.getInstance().registerExtender(this);
            MenuManagerLinkgrabberTableContext.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
            MenuManagerMainToolbar.getInstance().registerExtender(this);
        }
        LinkCollector.getInstance().setArchiver(this);
        DownloadController.getInstance().addVetoListener(this);

        FileCreationManager.getInstance().getEventSender().addListener(this);
        if (!org.appwork.utils.Application.isHeadless()) {
            SecondLevelLaunch.GUI_COMPLETE.executeWhenReached(new Runnable() {
                public void run() {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (statusbarListener != null) {
                                statusbarListener.cleanup();
                            }
                            eventSender.addListener(statusbarListener = new ExtractionListenerIcon(ExtractionExtension.this));
                            bubbleSupport = new ExtractionBubbleSupport(T.bubbletype(), CFG_EXTRACTION.BUBBLE_ENABLED_IF_ARCHIVE_EXTRACTION_IS_IN_PROGRESS);
                            eventSender.addListener(bubbleSupport, true);
                            BubbleNotify.getInstance().registerType(bubbleSupport);
                        }
                    };
                }
            });
        }
        ShutdownController.getInstance().addShutdownVetoListener(listener = new ShutdownVetoListener() {

            @Override
            public void onShutdownVetoRequest(ShutdownRequest request) throws ShutdownVetoException {
                if (request.hasVetos()) {
                    /* we already abort shutdown, no need to ask again */
                    return;
                }
                if (request.isSilent()) {
                    if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) {
                        throw new ShutdownVetoException("ExtractionExtension is still running", this);
                    }
                } else {
                    if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) {

                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT.T.Extraction_onShutdownRequest_(), _JDT.T.Extraction_onShutdownRequest_msg(), NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_EXTRACT, 32), _JDT.T.literally_yes(), null)) {
                            return;
                        }
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
                    final File[] files = Application.getResource("logs/extracting/open").listFiles();
                    if (files != null) {
                        String latestLog = null;
                        for (final File f : files) {
                            if (f.getName().matches("\\w+\\.txt")) {
                                getLogger().log(new Exception("Extraction Crashlog found! " + f.getName()));
                                int i = 1;
                                File renamedTo = new File(f.getParentFile().getParentFile(), "crashed_" + i + "_" + f.getName());
                                while (renamedTo.exists()) {
                                    i++;
                                    renamedTo = new File(f.getParentFile().getParentFile(), "crashed_" + i + "_" + f.getName());
                                }
                                f.renameTo(renamedTo);
                                final byte[] bytes = IO.readFile(renamedTo, 512 * 1024);
                                latestLog = new String(bytes, "UTF-8");
                                getLogger().info(latestLog);
                            }
                        }
                        if (!org.appwork.utils.Application.isHeadless()) {
                            /* currently disabled as headless does not support log upload */
                            if (StringUtils.isNotEmpty(latestLog)) {
                                final ExceptionDialog ed = new ExceptionDialog(0, T.crash_title(), T.crash_message(), null, null, null);
                                ed.setMore(latestLog);
                                final ExceptionDialogInterface dialog = UIOManager.I().show(ExceptionDialogInterface.class, ed);
                                dialog.throwCloseExceptions();
                                new EDTRunner() {

                                    @Override
                                    protected void runInEDT() {
                                        final LogAction la = new LogAction();
                                        la.actionPerformed(null);
                                    }
                                };
                            }
                        }
                    }
                } catch (Throwable e) {
                    getLogger().log(e);
                }
            }
        }.start();
    }

    private void lazyInitOnceOnStart() {
        if (lazyInitOnStart) {
            return;
        }
        lazyInitOnStart = true;
        initExtractors();

        // addListener(new ExtractionListenerFile());
        eventSender.addListener(new ExtractionListenerList());

        final Iterator<IExtraction> it = extractors.iterator();
        final List<IExtraction> remove = new ArrayList<IExtraction>();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.isAvailable(this)) {
                logger.severe("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                remove.add(extractor);
            }
        }
        extractors.removeAll(remove);
    }

    void fireEvent(ExtractionEvent event) {
        eventSender.fireEvent(event);
    }

    @Override
    public void handleCommand(String command, String... parameters) {
        if (command.equalsIgnoreCase("add-passwords") || command.equalsIgnoreCase("add-passwords") || command.equalsIgnoreCase("p")) {
            synchronized (PWLOCK) {
                List<String> lst = getSettings().getPasswordList();
                ArrayList<String> ret = new ArrayList<String>();
                if (lst != null) {
                    ret.addAll(lst);
                }
                Collection<String> newPws = Arrays.asList(parameters);
                ret.removeAll(newPws);
                ret.addAll(0, newPws);
                getSettings().setPasswordList(ret);
                logger.info("Added Passwords: " + newPws + " New List Size: " + ret.size());
            }
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
                final SubConfiguration oldConfig = SubConfiguration.getConfig("PASSWORDLIST", true);
                if (oldConfig.getProperties() != null) {
                    final Object oldList = oldConfig.getProperties().get("LIST2");
                    if (oldList != null && oldList instanceof List) {
                        final HashSet<String> dups = new HashSet<String>();
                        List<String> currentList = getSettings().getPasswordList();
                        if (currentList == null) {
                            currentList = new ArrayList<String>();
                        } else {
                            for (final String pw : currentList) {
                                dups.add(pw);
                            }
                        }
                        for (final Object item : (List<?>) oldList) {
                            if (item != null && item instanceof String) {
                                final String pw = (String) item;
                                if (dups.add(pw)) {
                                    currentList.add(pw);
                                }
                            }
                        }
                        getSettings().setPasswordList(currentList);
                    }
                }
            }
        } catch (final Throwable e) {
            logger.info("Could not Restore old Database");
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
        return T.description();
    }

    @Override
    public AddonPanel<ExtractionExtension> getGUI() {
        return null;
    }

    @Override
    public ExtensionConfigPanel<ExtractionExtension> getConfigPanel() {
        if (configPanel != null) {
            return configPanel;
        }
        return new EDTHelper<ExtractionConfigPanel>() {
            @Override
            public ExtractionConfigPanel edtRun() {
                if (configPanel != null) {
                    return configPanel;
                }
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
    public boolean cancel(final ExtractionController controller) {
        final boolean wasInProgress = getJobQueue().isInProgress(controller);
        final boolean ret;
        if (wasInProgress) {
            ret = true;
            if (!controller.isFinished() && !controller.gotKilled()) {
                controller.kill();
            }
        } else {
            ret = getJobQueue().remove(controller);
        }
        if (wasInProgress && ret) {
            fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.CLEANUP));
        }
        return ret;
    }

    private boolean onNewArchive(Object caller, Archive archive) {
        if (archive == null) {
            return false;
        }
        if (archive.getArchiveFiles().size() == 0) {
            logger.info("Caller:" + caller + "Archive:" + archive.getName() + "|Empty");
            return false;
        }
        if (!isAutoExtractEnabled(archive)) {
            logger.info("Caller:" + caller + "Archive:" + archive.getName() + "|AutoExtractionDisabled");
            return false;
        }
        final boolean complete = isComplete(archive);
        if (complete) {
            logger.info("Caller:" + caller + "Archive:" + archive.getName() + "|Complete|Size:" + archive.getArchiveFiles().size());
            return true;
        } else {
            logger.info("Caller:" + caller + "Archive:" + archive.getName() + "|Incomplete");
            return false;
        }
    }

    private synchronized Archive getExistingArchive(final Object archiveFactory) {
        for (ExtractionController ec : extractionQueue.getJobs()) {
            final Archive archive = ec.getArchive();
            if (archive.contains(archiveFactory)) {
                return archive;
            }
        }
        final ExtractionController currentController = extractionQueue.getCurrentQueueEntry();
        if (currentController != null) {
            final Archive archive = currentController.getArchive();
            if (archive.contains(archiveFactory)) {
                return archive;
            }
        }
        return null;
    }

    private boolean matchesDeepExtractionBlacklist(File[] fileList) {
        final String[] patternStrings = getSettings().getDeepExtractionBlacklistPatterns();
        if (fileList != null && fileList.length > 0 && patternStrings != null && patternStrings.length > 0) {
            final ArrayList<Pattern> patterns = new ArrayList<Pattern>();
            for (final String patternString : patternStrings) {
                try {
                    if (StringUtils.isNotEmpty(patternString) && !patternString.startsWith("##")) {
                        patterns.add(Pattern.compile(patternString));
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
            if (patterns.size() > 0) {
                for (File file : fileList) {
                    for (Pattern pattern : patterns) {
                        final String path = file.getAbsolutePath();
                        if (pattern.matcher(path).matches()) {
                            logger.info("Skip deep extraction: " + pattern.toString() + " matches file " + path);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void onNewFile(final Object caller, final File[] fileList) {
        if (fileList != null && fileList.length > 0) {
            try {
                if (caller instanceof SingleDownloadController) {
                    final DownloadLink link = ((SingleDownloadController) caller).getDownloadLink();
                    if (getExistingArchive(link) == null) {
                        final Archive archive = buildArchive(new DownloadLinkArchiveFactory(link));
                        if (onNewArchive(caller, archive)) {
                            addToQueue(archive, false);
                        }
                    }
                } else if (caller instanceof ExtractionController) {
                    if (getSettings().isDeepExtractionEnabled() && matchesDeepExtractionBlacklist(fileList) == false) {
                        final List<File> files = new ArrayList<File>(fileList.length);
                        for (File file : fileList) {
                            if (getExistingArchive(file) == null) {
                                files.add(file);
                            }
                        }
                        if (files.size() > 0) {
                            final ExtractionController con = (ExtractionController) caller;
                            final ArrayList<String> knownPasswords = new ArrayList<String>();
                            final String usedPassword = con.getArchive().getFinalPassword();
                            if (StringUtils.isNotEmpty(usedPassword)) {
                                knownPasswords.add(usedPassword);
                            }
                            final List<String> archiveSettingsPasswords = con.getArchive().getSettings().getPasswords();
                            if (archiveSettingsPasswords != null) {
                                knownPasswords.addAll(archiveSettingsPasswords);
                            }
                            final List<ArchiveFactory> archiveFactories = new ArrayList<ArchiveFactory>(files.size());
                            for (File archiveStartFile : files) {
                                archiveFactories.add(new FileArchiveFactory(archiveStartFile, con.getArchive()));
                            }
                            final List<Archive> newArchives = ArchiveValidator.getArchivesFromPackageChildren(archiveFactories);
                            for (Archive newArchive : newArchives) {
                                if (onNewArchive(caller, newArchive)) {
                                    final ArchiveFile firstArchiveFile = newArchive.getArchiveFiles().get(0);
                                    if (firstArchiveFile instanceof FileArchiveFile) {
                                        newArchive.getSettings().setExtractPath(((FileArchiveFile) firstArchiveFile).getFile().getParent());
                                    }
                                    newArchive.getSettings().setPasswords(knownPasswords);
                                    addToQueue(newArchive, false);
                                }
                            }
                        }
                    }
                } else {
                    final List<File> files = new ArrayList<File>(fileList.length);
                    for (File file : fileList) {
                        if (getExistingArchive(file) == null) {
                            files.add(file);
                        }
                    }
                    final List<Archive> newArchives = ArchiveValidator.getArchivesFromPackageChildren(files);
                    for (Archive newArchive : newArchives) {
                        if (onNewArchive(caller, newArchive)) {
                            addToQueue(newArchive, false);
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(e);
            }
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

    public IfFileExistsAction getIfFileExistsAction(Archive archive) {
        IfFileExistsAction ret = archive.getSettings()._getIfFileExistsAction();
        if (ret != null) {
            return ret;
        }
        return getSettings().getIfFileExistsAction();

    }

    public File getFinalExtractToFolder(Archive archive, boolean raw) {
        String path = null;
        if (StringUtils.isEmpty(path)) {
            path = archive.getSettings().getExtractPath();
            if (!StringUtils.isEmpty(path)) {
                /* use customized extracttofolder */
                if (!raw) {
                    path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME, null);
                }

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
        if (StringUtils.isEmpty(path)) {
            return null;
        }
        if (!raw) {
            path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME, null);
        }
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

    public Set<IExtraction> getExtractors() {
        return extractors;
    }

    @Override
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        if (manager instanceof MenuManagerMainToolbar) {
            return updateMainToolbar(mr);
        } else if (manager instanceof MenuManagerMainmenu) {
            return updateMainMenu(mr);
        } else if (manager instanceof MenuManagerLinkgrabberTableContext) {
            int addonLinkIndex = -1;
            for (int i = 0; i < mr.getItems().size(); i++) {
                if (mr.getItems().get(i) instanceof LinkGrabberMoreSubMenu) {
                    addonLinkIndex = i;
                    break;
                }
            }

            final ArchivesSubMenu root = new ArchivesSubMenu();
            root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));
            root.add(new SeparatorData());
            root.add(new MenuItemData(new ActionData(AutoExtractEnabledToggleAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));

            final CleanupSubMenu cleanup = new CleanupSubMenu();
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesEnabledToggleAction.class)));
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksEnabledToggleAction.class)));

            root.add(cleanup);
            if (addonLinkIndex != -1) {
                mr.getItems().add(addonLinkIndex, root);
            } else {
                mr.getItems().add(root);
            }
            return null;
        } else if (manager instanceof MenuManagerDownloadTableContext) {
            int addonLinkIndex = 0;
            for (int i = 0; i < mr.getItems().size(); i++) {
                if (mr.getItems().get(i) instanceof MoreMenuContainer) {
                    addonLinkIndex = i;
                    break;
                }
            }
            final ArchivesSubMenu root = new ArchivesSubMenu();
            root.add(new MenuItemData(new ActionData(ExtractArchiveNowAction.class)));
            root.add(new MenuItemData(new ActionData(AbortExtractionAction.class)));
            root.add(new MenuItemData(new ActionData(ShowExtractionResultAction.class)));
            root.add(new MenuItemData(new ActionData(ValidateArchivesAction.class)));
            root.add(new SeparatorData());
            root.add(new MenuItemData(new ActionData(AutoExtractEnabledToggleAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractToAction.class)));
            root.add(new MenuItemData(new ActionData(SetExtractPasswordAction.class)));

            final CleanupSubMenu cleanup = new CleanupSubMenu();
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteFilesEnabledToggleAction.class)));
            cleanup.add(new MenuItemData(new ActionData(CleanupAutoDeleteLinksEnabledToggleAction.class)));

            root.add(cleanup);
            if (addonLinkIndex != -1) {
                mr.getItems().add(addonLinkIndex, root);
            } else {
                mr.getItems().add(root);
            }
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
    public List<DownloadLink> onAskToRemovePackage(Object asker, FilePackage pkg, List<DownloadLink> children) {
        return onAskToRemoveChildren(asker, children);
    }

    @Override
    public List<DownloadLink> onAskToRemoveChildren(final Object asker, final List<DownloadLink> children) {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>(children.size());
        for (final DownloadLink dlink : children) {
            final Archive archive = getExistingArchive(dlink);
            if (archive != null) {
                if (asker instanceof ExtractionController) {
                    final ExtractionController ec = (ExtractionController) asker;
                    if (ec.getArchive() == archive || StringUtils.equals(ec.getArchive().getArchiveID(), archive.getArchiveID())) {
                        ret.add(dlink);
                        continue;
                    }
                }
                logger.info("Link (" + dlink.toString() + ") is in active Archive do not remove: " + archive.getArchiveID());
            } else {
                ret.add(dlink);
            }
        }
        return ret;
    }

    @Override
    public void onNewFolder(Object caller, File folder) {
    }

}