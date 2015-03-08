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
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchive;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFile;
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
import org.jdownloader.extensions.extraction.split.HachaSplit;
import org.jdownloader.extensions.extraction.split.UnixSplit;
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
        setTitle(_.name());
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
        for (ExtractionController ec : extractionQueue.getJobs()) {
            if (ec.getArchiv() == archive || StringUtils.equals(ec.getArchiv().getArchiveID(), archive.getArchiveID())) {
                return ec;
            }
        }
        final ExtractionController currentController = extractionQueue.getCurrentQueueEntry();
        if (currentController != null) {
            if (currentController.getArchiv() == archive || StringUtils.equals(currentController.getArchiv().getArchiveID(), archive.getArchiveID())) {
                return currentController;
            }
        }
        if (archive.getFirstArchiveFile() == null || !archive.getFirstArchiveFile().isComplete()) {
            logger.info("Archive is not complete: " + archive.getName());
            return null;
        }
        final IExtraction extractor = getExtractorInstanceByFactory(archive.getFactory());
        if (extractor == null) {
            return null;
        }
        archive.getFactory().fireArchiveAddedToQueue(archive);
        final ExtractionController controller = new ExtractionController(this, archive, extractor);
        controller.setAskForUnknownPassword(forceAskForUnknownPassword);
        controller.setIfFileExistsAction(getIfFileExistsAction(archive));
        controller.setRemoveAfterExtract(getRemoveFilesAfterExtractAction(archive));
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

    public FileCreationManager.DeleteOption getRemoveFilesAfterExtractAction(Archive archive) {
        switch (archive.getSettings().getRemoveFilesAfterExtraction()) {
        case FALSE:
            return FileCreationManager.DeleteOption.NO_DELETE;
        case TRUE:
            switch (getSettings().getDeleteArchiveFilesAfterExtractionAction()) {
            case NO_DELETE:
            case RECYCLE:
                return FileCreationManager.DeleteOption.RECYCLE;
            case NULL:
                return FileCreationManager.DeleteOption.NULL;
            }
        }
        return getSettings().getDeleteArchiveFilesAfterExtractionAction();
    }

    @Override
    public String getIconKey() {
        return org.jdownloader.gui.IconKey.ICON_COMPRESS;
    }

    /**
     * Builds an archive for an {@link DownloadLink}.
     *
     * @param link
     * @return
     * @throws ArchiveException
     */
    public Archive buildArchive(final ArchiveFactory factory) throws ArchiveException {
        final Archive existing = getExistingArchive(factory);
        if (existing == null) {
            final boolean deepInspection = !(factory instanceof CrawledLinkFactory);
            for (IExtraction extractor : extractors) {
                try {
                    if (!Boolean.FALSE.equals(extractor.isSupported(factory, deepInspection))) {
                        final Archive archive = extractor.buildArchive(factory, deepInspection);
                        if (archive != null) {
                            factory.onArchiveFinished(archive);
                            return archive;
                        }
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                }
            }
        }
        return existing;
    }

    public DummyArchive createDummyArchive(final Archive archive) throws CheckException {
        final ArchiveFactory factory = archive.getFactory();
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
        return null;
    }

    /**
     * Finishes the extraction process.
     *
     * @param controller
     */
    void onFinished(ExtractionController controller) {
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
                            bubbleSupport = new ExtractionBubbleSupport(T._.bubbletype(), CFG_EXTRACTION.BUBBLE_ENABLED_IF_ARCHIVE_EXTRACTION_IS_IN_PROGRESS);
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

                        if (UIOManager.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.Extraction_onShutdownRequest_(), _JDT._.Extraction_onShutdownRequest_msg(), NewTheme.I().getIcon(org.jdownloader.gui.IconKey.ICON_COMPRESS, 32), _JDT._.literally_yes(), null)) {
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
                        if (!org.appwork.utils.Application.isHeadless()) {
                            /* currently disabled as headless does not support log upload */
                            if (StringUtils.isNotEmpty(latestLog)) {
                                ExceptionDialog ed = new ExceptionDialog(0, T._.crash_title(), T._.crash_message(), null, null, null);
                                ed.setMore(latestLog);
                                UIOManager.I().show(ExceptionDialogInterface.class, ed);
                                LogAction la = new LogAction();
                                la.actionPerformed(null);
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
            if (!extractor.isAvailable()) {
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
                        }
                        for (Object item : (List<?>) oldList) {
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
        return _.description();
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
    public boolean cancel(ExtractionController activeValue) {
        boolean wasInProgress = getJobQueue().isInProgress(activeValue);
        boolean ret = getJobQueue().remove(activeValue);
        if (wasInProgress) {
            fireEvent(new ExtractionEvent(activeValue, ExtractionEvent.Type.CLEANUP));
        }
        return ret;
    }

    private boolean onNewArchive(Object caller, Archive archive) {
        if (archive == null) {
            logger.info("Archive seems not supported!");
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
        final boolean complete = isComplete(archive);
        if (complete) {
            logger.info("Archive:" + archive.getName() + "|Complete|Size:" + archive.getArchiveFiles().size());
            return true;
        } else {
            logger.info("Archive:" + archive.getName() + "|Incomplete");
            return false;
        }
    }

    private synchronized Archive getExistingArchive(final Object archiveFactory) {
        for (ExtractionController ec : extractionQueue.getJobs()) {
            final Archive archive = ec.getArchiv();
            if (archive.contains(archiveFactory)) {
                return archive;
            }
        }
        final ExtractionController currentController = extractionQueue.getCurrentQueueEntry();
        if (currentController != null) {
            final Archive archive = currentController.getArchiv();
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
                            final String usedPassword = con.getArchiv().getFinalPassword();
                            if (StringUtils.isNotEmpty(usedPassword)) {
                                knownPasswords.add(usedPassword);
                            }
                            final List<String> archiveSettingsPasswords = con.getArchiv().getSettings().getPasswords();
                            if (archiveSettingsPasswords != null) {
                                knownPasswords.addAll(archiveSettingsPasswords);
                            }
                            DownloadLinkArchive previousDownloadLinkArchive = null;
                            Archive previousArchive = con.getArchiv();
                            while (previousArchive != null) {
                                if (con.getArchiv() instanceof DownloadLinkArchive) {
                                    previousDownloadLinkArchive = (DownloadLinkArchive) con.getArchiv();
                                }
                                previousArchive = previousArchive.getPreviousArchive();
                            }
                            final List<ArchiveFactory> archiveFactories = new ArrayList<ArchiveFactory>(files.size());
                            for (File archiveStartFile : files) {
                                archiveFactories.add(new FileArchiveFactory(archiveStartFile, previousDownloadLinkArchive));
                            }
                            final List<Archive> newArchives = ArchiveValidator.getArchivesFromPackageChildren(archiveFactories);
                            for (Archive newArchive : newArchives) {
                                if (onNewArchive(caller, newArchive)) {
                                    final ArchiveFile firstArchiveFile = newArchive.getFirstArchiveFile();
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
                    path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME);
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
            path = PackagizerController.replaceDynamicTags(path, ArchiveFactory.PACKAGENAME);
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
            root.add(new SeparatorData());
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

            root.add(new SeparatorData());
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
                    ExtractionController ec = (ExtractionController) asker;
                    if (ec.getArchiv() == archive || StringUtils.equals(ec.getArchiv().getArchiveID(), archive.getArchiveID())) {
                        ret.add(dlink);
                        continue;
                    }
                }
                logger.info("Link is in active Archive do not remove: " + archive);
            } else {
                ret.add(dlink);
            }
        }
        return ret;
    }

}