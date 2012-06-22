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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import jd.Launcher;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.gui.config.ExtractionConfigPanel;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.Unix;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.ExtractionTranslation;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.menu.eventsender.MenuFactoryListener;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberTablePropertiesContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig, ExtractionTranslation> implements FileCreationListener, MenuFactoryListener {

    private ExtractionQueue              extractionQueue   = new ExtractionQueue();

    private ExtractionEventSender        broadcaster       = new ExtractionEventSender();

    private final ArrayList<IExtraction> extractors        = new ArrayList<IExtraction>();

    private final ArrayList<Archive>     archives          = new ArrayList<Archive>();

    private ExtractionConfigPanel        configPanel;

    private static ExtractionExtension   INSTANCE;

    private ExtractionListenerIcon       statusbarListener = null;

    private AbstractToolbarAction        extractFileAction;

    private AppAction                    menuAction;

    private ShutdownVetoListener         listener          = null;

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
        setExtractor(new Unix());
        setExtractor(new XtreamSplit());
        setExtractor(new HJSplit());
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
        extractor.setLogger(LogController.CL());
    }

    /**
     * Checks if there is supported extractor.
     * 
     * @param file
     *            Path of the packed file
     * @return True if a extractor was found
     */
    public final boolean isLinkSupported(ArchiveFactory factory) {
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory)) { return true; }
        }

        return false;
    }

    public boolean isMultiPartArchive(ArchiveFactory factory) {
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory)) { return extractor.isMultiPartArchive(factory);

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
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory)) { return extractor.createID(factory);

            }
        }

        return null;
    }

    public String getArchiveName(ArchiveFactory factory) {
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(factory)) { return extractor.getArchiveName(factory);

            }
        }
        return null;
    }

    /**
     * Adds an archive to the extraction queue.
     */
    public synchronized ExtractionController addToQueue(final Archive archive) {
        // check if we have this archive already in queue.

        for (ExtractionController ec : extractionQueue.getJobs()) {

            if (ec.getArchiv() == archive) return ec;
        }

        IExtraction extractor = archive.getExtractor();

        if (!archive.getFirstArchiveFile().exists()) {
            LogController.CL().info("First File does not exist " + archive.getFirstArchiveFile());
            return null;
        }
        archives.add(archive);

        archive.getFactory().fireArchiveAddedToQueue(archive);

        archive.setOverwriteFiles(getSettings().isOverwriteExistingFilesEnabled());

        ExtractionController controller = new ExtractionController(this, archive);

        controller.setRemoveAfterExtract(getSettings().isDeleteArchiveFilesAfterExtraction());
        controller.setRemoveDownloadLinksAfterExtraction(getSettings().isDeleteArchiveDownloadlinksAfterExtraction());

        archive.setActive(true);
        extractor.setConfig(getSettings());

        extractionQueue.addAsynch(controller);
        fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.QUEUED));
        return controller;
    }

    /**
     * Bestimmt den Pfad in den das Archiv entpackt werden soll
     * 
     * @param archiveFactory
     * 
     * @param archive
     *            .getFactory()
     * @return
     */
    File getExtractToPath(ArchiveFactory archiveFactory, Archive archive) {
        // if extract folder is already set, use it.
        if (archive != null && archive.getExtractTo() != null) return archive.getExtractTo();
        String path = archiveFactory.getExtractPath(archive);
        if (path == null && getSettings().isCustomExtractionPathEnabled()) {
            path = getSettings().getCustomExtractionPath();
        }
        if (path == null) {
            path = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
        }
        return new File(path);
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
    private synchronized Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        IExtraction extrctor = getExtractorByFactory(link);
        if (extrctor == null) {
            //
            return null;
        }
        for (Archive archive : archives) {
            if (archive.contains(link)) {

            return archive;

            }
        }

        Archive archive = extrctor.buildArchive(link);
        LogSource logger = LogController.CL();
        logger.info("Created Archive: " + archive);
        // Log.L.info("Created Archive: " + archive);
        LogController.CL().info("Files: " + archive.getArchiveFiles());

        return archive;
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
     * Sets the extractionpath with subpahts.
     * 
     * @param controller
     */
    void assignRealDownloadDir(ExtractionController controller) {

        Boolean usesub = getSettings().isSubpathEnabled();
        if (usesub) {
            if (getSettings().getSubPathFilesTreshhold() > controller.getArchiv().getContentView().getFileCount() + controller.getArchiv().getContentView().getDirectoryCount()) { return; }
            if (getSettings().isSubpathEnabledIfAllFilesAreInAFolder()) {

                if (controller.getArchiv().getContentView().getFileCount() == 0) { return;

                }
            }

            String path = getSettings().getSubPath();

            path = controller.getArchiv().getFactory().createExtractSubPath(path, controller.getArchiv());
            if (path != null) {
                controller.getArchiv().setExtractTo(new File(controller.getArchiv().getExtractTo(), path));
            }
        }

    }

    /**
     * Returns the extractor for the {@link DownloadLink}.
     * 
     * @param link
     * @return
     */
    public IExtraction getExtractorByFactory(ArchiveFactory factory) {
        for (IExtraction extractor : extractors) {
            try {
                if (extractor.isArchivSupported(factory)) { return extractor.getClass().newInstance(); }
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
    void removeArchive(Archive archive) {
        archives.remove(archive);
    }

    // @SuppressWarnings({ "unchecked", "deprecation" })
    // public void controlEvent(ControlEvent event) {
    // DownloadLink link;
    // switch (event.getEventID()) {
    //
    // case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:

    @Override
    protected void stop() throws StopException {
        LinkCollector.getInstance().setArchiver(null);
        ShutdownController.getInstance().removeShutdownVetoListener(listener);
        MenuFactoryEventSender.getInstance().removeListener(this);
        FileCreationManager.getInstance().getEventSender().removeListener(this);
        Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (statusbarListener != null) {
                            statusbarListener.cleanup();
                            broadcaster.removeListener(statusbarListener);
                        }
                    }
                };
            }
        });

    }

    @Override
    protected void start() throws StartException {
        LinkCollector.getInstance().setArchiver(this);
        MenuFactoryEventSender.getInstance().addListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
        Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (statusbarListener != null) statusbarListener.cleanup();
                        broadcaster.addListener(statusbarListener = new ExtractionListenerIcon(ExtractionExtension.this));
                    }
                };
            }
        });
        ShutdownController.getInstance().addShutdownVetoListener(listener = new ShutdownVetoListener() {

            @Override
            public void onShutdownVeto(ShutdownVetoException[] vetos) {
            }

            @Override
            public void onShutdownVetoRequest(ShutdownVetoException[] vetos) throws ShutdownVetoException {
                if (vetos.length > 0) {
                    /* we already abort shutdown, no need to ask again */
                    return;
                }
                if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) {
                    try {
                        NewUIO.I().showConfirmDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | Dialog.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.Extraction_onShutdownRequest_(), _JDT._.Extraction_onShutdownRequest_msg(), NewTheme.I().getIcon("unpack", 32), _JDT._.literally_yes(), null);
                        return;
                    } catch (DialogNoAnswerException e) {
                        e.printStackTrace();
                    }
                    throw new ShutdownVetoException("ExtractionExtension is still running", this);
                }
            }

            @Override
            public void onShutdown(boolean silent) {
            }

            @Override
            public void onSilentShutdownVetoRequest(ShutdownVetoException[] shutdownVetoExceptions) throws ShutdownVetoException {
                if (shutdownVetoExceptions.length > 0) {
                    /* we already abort shutdown, no need to ask again */
                    return;
                }
                if (!extractionQueue.isEmpty() || extractionQueue.getCurrentQueueEntry() != null) { throw new ShutdownVetoException("ExtractionExtension is still running", this); }
            }

        });
    }

    void fireEvent(ExtractionEvent event) {
        broadcaster.fireEvent(event);
    }

    @Override
    protected void initExtension() throws StartException {
        /* import old passwordlist */
        boolean oldPWListImported = false;
        try {
            if ((oldPWListImported = getSettings().isOldPWListImported()) == false) {
                SubConfiguration oldConfig = SubConfiguration.getConfig("PASSWORDLIST", true);
                Object oldList = oldConfig.getProperties().get("LIST2");
                ArrayList<String> currentList = getSettings().getPasswordList();
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
            LogController.CL().log(e);
        } finally {
            if (oldPWListImported == false) {
                getSettings().setOldPWListImported(true);
            }
        }
        extractFileAction = new AbstractToolbarAction() {

            /**
             * 
             */
            private static final long serialVersionUID = 6674118067384677739L;

            public void actionPerformed(ActionEvent e) {
                menuAction.actionPerformed(e);

            }

            @Override
            protected void doInit() {
            }

            @Override
            protected String createTooltip() {
                return "Extraction";
            }

            @Override
            protected String createMnemonic() {
                return null;
            }

            @Override
            public String createIconKey() {
                return "unpack";
            }

            @Override
            protected String createAccelerator() {
                return null;
            }
        };

        initExtractors();

        // addListener(new ExtractionListenerFile());
        broadcaster.addListener(new ExtractionListenerList());

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                LogController.CL().severe("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                it.remove();
            }
        }

        if (menuAction == null) menuAction = new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 1612595219577059496L;

            {
                setName(_.menu_tools_extract_files());
                setIconKey(ExtractionExtension.this.getIconKey());
                this.setEnabled(true);
            }

            public void actionPerformed(ActionEvent e) {
                FileFilter ff = new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.isDirectory()) return true;

                        for (IExtraction extractor : extractors) {
                            if (extractor.isArchivSupported(new FileArchiveFactory(pathname))) { return true; }
                        }

                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return _.plugins_optional_extraction_filefilter();
                    }
                };

                File[] files = UserIO.getInstance().requestFileChooser("_EXTRATION_", null, UserIO.FILES_ONLY, ff, true, null, null);
                if (files == null) return;

                for (final File archiveStartFile : files) {

                    try {
                        final Archive archive = buildArchive(new FileArchiveFactory(archiveStartFile));
                        if (getSettings().isCustomExtractionPathEnabled()) {

                            File path = DownloadFolderChooserDialog.open(new File(getSettings().getCustomExtractionPath()), false, "Extract To");
                            archive.setExtractTo(path);
                        } else {
                            File path = DownloadFolderChooserDialog.open(archiveStartFile.getParentFile(), false, "Extract To");
                            archive.setExtractTo(path);
                        }

                        new Thread() {
                            @Override
                            public void run() {

                                if (archive.isComplete()) {
                                    final ExtractionController controller = addToQueue(archive);
                                    if (controller != null) {
                                        final ExtractionListener listener = new ExtractionListener() {

                                            @Override
                                            public void onExtractionEvent(ExtractionEvent event) {
                                                if (event.getCaller() == controller) {
                                                    switch (event.getType()) {
                                                    case CLEANUP:
                                                        broadcaster.removeListener(this);
                                                        break;
                                                    case EXTRACTION_FAILED:
                                                    case EXTRACTION_FAILED_CRC:

                                                        if (controller.getException() != null) {
                                                            Dialog.getInstance().showExceptionDialog(_.extraction_failed(archiveStartFile.getName()), controller.getException().getLocalizedMessage(), controller.getException());
                                                        } else {
                                                            Dialog.getInstance().showErrorDialog(_.extraction_failed(archiveStartFile.getName()));
                                                        }
                                                        break;
                                                    }

                                                }
                                            }

                                        };
                                        broadcaster.addListener(listener);
                                    }
                                } else {

                                    new ValidateArchiveAction(ExtractionExtension.this, archive).actionPerformed(null);
                                }
                            }
                        }.start();
                    } catch (ArchiveException e1) {
                        LogController.CL(ExtractionExtension.class).log(e1);
                    } catch (DialogNoAnswerException e1) {
                    }
                }
            }
        };
        configPanel = new ExtractionConfigPanel(this);
    }

    @Override
    public boolean hasConfigPanel() {
        return true;
    }

    @Override
    public String getAuthor() {
        return null;
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
    public java.util.ArrayList<JMenuItem> getMenuAction() {
        ArrayList<JMenuItem> ret = new java.util.ArrayList<JMenuItem>();
        ret.add(new JMenuItem(menuAction));
        return ret;
    }

    @Override
    public ExtensionConfigPanel<ExtractionExtension> getConfigPanel() {
        return configPanel;
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

    public void onNewFile(Object caller, File[] fileList) {
        LogSource logger = LogController.CL();
        try {
            logger.info("New File by " + caller);
            if (caller instanceof SingleDownloadController) {
                DownloadLink link = ((SingleDownloadController) caller).getDownloadLink();

                logger.info("postprocess " + link.getFilePackage().isPostProcessing());
                logger.info("link supported  " + isLinkSupported(new DownloadLinkArchiveFactory(link)));
                if (link.getFilePackage().isPostProcessing() && isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    Archive archive;
                    try {
                        archive = buildArchive(new DownloadLinkArchiveFactory(link));
                        logger.info("archive active " + archive.isActive());
                        logger.info("archive size " + archive.getArchiveFiles().size());
                        logger.info("archive complete " + archive.isComplete());
                        if (!archive.isActive() && archive.getArchiveFiles().size() > 0 && archive.isComplete()) {
                            this.addToQueue(archive);
                        }
                    } catch (ArchiveException e) {
                        logger.log(e);
                    }
                }
            } else if (caller instanceof ExtractionController && getSettings().isDeepExtractionEnabled()) {
                try {

                    for (File archiveStartFile : fileList) {
                        FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                        if (isLinkSupported(fac)) {
                            Archive ar = buildArchive(fac);
                            if (ar.isActive() || ar.getArchiveFiles().size() < 1 || !ar.isComplete()) continue;
                            addToQueue(ar);
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
            } else {
                try {

                    for (File archiveStartFile : fileList) {
                        FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                        if (isLinkSupported(fac)) {
                            Archive ar = buildArchive(fac);
                            if (ar.isActive() || ar.getArchiveFiles().size() < 1 || !ar.isComplete()) continue;
                            addToQueue(ar);
                        }
                    }
                } catch (Exception e) {
                    logger.log(e);
                }
            }
        } finally {
            logger.close();
        }
    }

    public void onExtendPopupMenu(MenuContext<?> unspecifiedContext) {

        if (unspecifiedContext instanceof DownloadTableContext) {

            onExtendPopupMenuDownloadTable((DownloadTableContext) unspecifiedContext);
        } else if (unspecifiedContext instanceof LinkgrabberTableContext) {
            onExtendPopupMenuLinkgrabberTable((LinkgrabberTableContext) unspecifiedContext);
            // p.add(new
            // ValidateArchiveAction(selection).toContextMenuAction());
        } else if (unspecifiedContext instanceof LinkgrabberTablePropertiesContext) {
            onExtendPropertiesMenuLinkgrabberTable((LinkgrabberTablePropertiesContext) unspecifiedContext);
        }
    }

    private void onExtendPropertiesMenuLinkgrabberTable(final LinkgrabberTablePropertiesContext context) {

        // JMenu menu = new JMenu(_.contextmenu_main()) {
        // protected JMenuItem createActionComponent(Action a) {
        // if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
        // return super.createActionComponent(a);
        // }
        // };
        // menu.setIcon(getIcon(18));
        // context.getMenu().add(menu);

    }

    private void onExtendPopupMenuLinkgrabberTable(LinkgrabberTableContext context) {
        boolean isLinkContext = context.getSelectionInfo().isLinkContext();
        boolean isShift = context.getSelectionInfo().isShiftDown();
        boolean isPkgContext = context.getSelectionInfo().getRawContext() instanceof CrawledPackage;
        CrawledLink link = isLinkContext ? (CrawledLink) context.getSelectionInfo().getRawContext() : null;
        CrawledPackage pkg = isPkgContext ? (CrawledPackage) context.getSelectionInfo().getRawContext() : null;
        JMenu menu = new JMenu(_.contextmenu_main()) {
            /**
             * 
             */
            private static final long serialVersionUID = -5156294142768994122L;

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };

        context.getMenu().add(menu);
        menu.setIcon(getIcon(18));
        menu.setEnabled(false);
        ValidateArchiveAction<CrawledPackage, CrawledLink> validation = new ValidateArchiveAction<CrawledPackage, CrawledLink>(this, context.getSelectionInfo());

        for (final Archive a : validation.getArchives()) {
            menu.setEnabled(true);
            JMenu parent = menu;
            if (validation.getArchives().size() > 1) {
                parent = new JMenu(a.getName()) {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -4742306843688611883L;

                    protected JMenuItem createActionComponent(Action a) {
                        if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                        return super.createActionComponent(a);
                    }
                };
                menu.add(parent);
            }

            parent.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 4876283003885620243L;
                {
                    setName(_.auto_extract_enabled());
                    setSelected(((CrawledLinkArchiveFile) a.getArchiveFiles().get(0)).getLink().getParentNode().isAutoExtractionEnabled());
                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    ((CrawledLinkArchiveFile) a.getArchiveFiles().get(0)).getLink().getParentNode().setAutoExtractionEnabled(isSelected());
                }
            });
            parent.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = -1357609236577941787L;

                {
                    setName(_.contextmenu_set_password());
                    setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("password", 12), 0, 0, 10, 10)));

                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        HashSet<String> passwords = ((CrawledLinkArchiveFile) a.getArchiveFiles().get(0)).getLink().getParentNode().getExtractionPasswords();
                        String def = null;
                        if (passwords.size() > 0) {
                            def = passwords.iterator().next();
                        }
                        String newPassword = Dialog.getInstance().showInputDialog(0, _.password(a.getName()), "", def, NewTheme.I().getIcon("password", 32), null, null);
                        if (def != null && def.equals(newPassword)) return;
                        passwords.clear();
                        passwords.add(newPassword);
                    } catch (DialogNoAnswerException e1) {
                    }
                }
            });
            parent.add(new ValidateArchiveAction(this, a));

        }

    }

    protected void onExtendPopupMenuDownloadTable(final DownloadTableContext context) {
        JMenu menu = new JMenu(_.contextmenu_main()) {
            /**
             * 
             */
            private static final long serialVersionUID = -4895713293724938465L;

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };

        if (context.getSelectionInfo().getRawContext() instanceof DownloadLink) {
            final DownloadLink link = (DownloadLink) context.getSelectionInfo().getRawContext();
            final ValidateArchiveAction<FilePackage, DownloadLink> validateAction = new ValidateArchiveAction<FilePackage, DownloadLink>(this, context.getSelectionInfo());
            final DownloadLinkArchiveFactory dlAF = new DownloadLinkArchiveFactory(link);
            final boolean fileExists = new File(link.getFileOutput()).exists();
            final boolean isLinkSupported = isLinkSupported(dlAF);
            menu.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 1375146705091555054L;

                {
                    setName(_.contextmenu_extract());
                    Image front = NewTheme.I().getImage("media-playback-start", 20, true);
                    setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));
                    setEnabled(isLinkSupported && validateAction.getArchives().size() > 0 && fileExists);
                }

                public void actionPerformed(ActionEvent e) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            final HashSet<Archive> archiveHistory = new HashSet<Archive>();
                            for (AbstractNode link : context.getSelectionInfo().getSelectedChildren()) {
                                if (link instanceof DownloadLink) {
                                    for (Archive archive : archiveHistory) {
                                        if (archive.getArchiveFiles().contains(link)) continue;
                                    }
                                    try {
                                        final Archive archive = buildArchive(new DownloadLinkArchiveFactory((DownloadLink) link));
                                        if (!archiveHistory.add(archive)) continue;
                                        if (archive.isComplete()) {
                                            addToQueue(archive);
                                        } else {
                                            Dialog.getInstance().showMessageDialog(_.cannot_extract_incopmplete(archive.getName()));
                                        }
                                    } catch (final ArchiveException e) {
                                        LogController.CL(ExtractionExtension.class).log(e);
                                    }
                                }

                            }
                        }
                    };
                    thread.setName("Extract Context: extract");
                    thread.setDaemon(true);
                    thread.start();
                }
            });

            if (validateAction.getArchives().size() == 1) {
                menu.add(validateAction.toContextMenuAction());
            } else if (validateAction.getArchives().size() > 1) {
                JMenu validate = new JMenu(_.contextmenu_validate_parent());
                validate.setIcon(validateAction.getSmallIcon());
                for (Archive a : validateAction.getArchives()) {
                    validate.add(new ValidateArchiveAction(this, a));
                }
                menu.add(validate);
            } else {
                menu.setEnabled(false);
            }
            menu.add(new JSeparator());

            menu.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 2539573738898436840L;

                {
                    setName(_.contextmenu_autoextract());
                    setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("refresh", 12), 0, 0, 10, 10)));
                    setSelected(link.getFilePackage().isPostProcessing());
                }

                public void actionPerformed(ActionEvent e) {
                    for (AbstractNode link : context.getSelectionInfo().getSelectedChildren()) {
                        if (link instanceof DownloadLink) {
                            ((DownloadLink) link).getFilePackage().setPostProcessing(isSelected());
                        }
                    }
                }
            });
            menu.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 3224128311203557582L;

                {
                    setName(_.contextmenu_extract_to());
                    setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("folder", 20), NewTheme.I().getImage("edit", 12), 0, 0, 10, 10)));
                    setEnabled(isLinkSupported);
                }

                public void actionPerformed(ActionEvent e) {
                    if (link == null) { return; }
                    FileFilter ff = new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            if (pathname.isDirectory()) return true;
                            return false;
                        }

                        @Override
                        public String getDescription() {
                            return _.plugins_optional_extraction_filefilter_extractto();
                        }
                    };
                    try {
                        final Archive archive = buildArchive(dlAF);

                        File extractto = getExtractToPath(dlAF, archive);
                        while (extractto != null && !extractto.isDirectory()) {
                            extractto = extractto.getParentFile();
                        }

                        File[] files = UserIO.getInstance().requestFileChooser("_EXTRACTION_", null, UserIO.DIRECTORIES_ONLY, ff, null, extractto, JFileChooser.SAVE_DIALOG);
                        if (files == null || files.length == 0) return;
                        for (AbstractNode link : context.getSelectionInfo().getSelectedChildren()) {
                            if (link instanceof DownloadLink) {
                                ((DownloadLink) link).setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTTOPATH, files[0].getAbsolutePath());
                            }
                        }
                    } catch (ArchiveException e1) {
                        LogController.CL().log(e1);

                        Dialog.getInstance().showMessageDialog(T._.ExtractionExtension_onExtendPopupMenuDownloadTable_unsupported_title(), T._.ExtractionExtension_onExtendPopupMenuDownloadTable_unsupported_message());
                    }
                }
            });

            menu.add(new AppAction() {
                /**
                 * 
                 */
                private static final long serialVersionUID = 4071826060416621911L;

                {
                    setName(_.contextmenu_openextract_folder());
                    setIconKey("folder");
                    setEnabled(isLinkSupported && CrossSystem.isOpenFileSupported() && fileExists);
                }

                public void actionPerformed(ActionEvent e) {
                    if (link == null) { return; }
                    /* first check for property with already extracted file */
                    String path = link.getStringProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH);
                    if (path == null) {
                        /* check wished extraction destination */
                        path = link.getStringProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTTOPATH);
                    }
                    if (path == null) {
                        /* else it will be the folder where the files are in */
                        path = link.getFilePackage().getDownloadDirectory();
                    }
                    File file = new File(path);
                    if (!file.exists()) {
                        UserIO.getInstance().requestMessageDialog(_.plugins_optional_extraction_messages(path));
                    } else {
                        CrossSystem.openFile(file);
                    }

                }
            });
        } else {
            final FilePackage fp = (FilePackage) context.getSelectionInfo().getRawContext();
            if (fp != null) {
                menu.add(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -2143313495286757118L;
                    {
                        setName(_.contextmenu_extract());
                        Image front = NewTheme.I().getImage("media-playback-start", 20, true);
                        setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));
                    }

                    public void actionPerformed(ActionEvent e) {
                        final List<DownloadLink> links = context.getSelectionInfo().getSelectedChildren();
                        if (links.size() == 0) return;
                        Thread thread = new Thread() {
                            @Override
                            public void run() {
                                final HashSet<Archive> archiveHistory = new HashSet<Archive>();
                                for (DownloadLink link0 : links) {
                                    for (Archive archive : archiveHistory) {
                                        if (archive.getArchiveFiles().contains(link0)) continue;
                                    }
                                    try {
                                        final Archive archive = buildArchive(new DownloadLinkArchiveFactory((DownloadLink) link0));
                                        if (!archiveHistory.add(archive)) continue;
                                        if (archive.isComplete()) {
                                            addToQueue(archive);
                                        }
                                    } catch (final ArchiveException e) {
                                        LogController.CL(ExtractionExtension.class).log(e);
                                    }
                                }

                            }
                        };
                        thread.setName("Extract Context: extract");
                        thread.setDaemon(true);
                        thread.start();
                    };
                });

                ValidateArchiveAction<FilePackage, DownloadLink> validateAction = new ValidateArchiveAction<FilePackage, DownloadLink>(this, context.getSelectionInfo());
                if (validateAction.getArchives().size() == 1) {
                    menu.add(validateAction.toContextMenuAction());
                } else if (validateAction.getArchives().size() > 1) {
                    JMenu validate = new JMenu(_.contextmenu_validate_parent());

                    validate.setIcon(validateAction.getSmallIcon());
                    for (Archive a : validateAction.getArchives()) {
                        validate.add(new ValidateArchiveAction<FilePackage, DownloadLink>(this, a));
                    }
                    menu.add(validate);

                } else {
                    menu.setEnabled(false);
                }
                menu.add(new JSeparator());
                menu.add(new AppAction() {
                    /**
                     * 
                     */
                    private static final long serialVersionUID = -3673003200717488605L;
                    {
                        setName(_.contextmenu_auto_extract_package());
                        setIconKey(ExtractionExtension.this.getIconKey());
                        setSelected(fp.isPostProcessing());
                    }

                    public void actionPerformed(ActionEvent e) {
                        for (AbstractNode link : context.getSelectionInfo().getSelectedChildren()) {
                            if (link instanceof FilePackage) {
                                ((FilePackage) link).setPostProcessing(isSelected());
                            }
                        }
                    }
                });

            }
        }
        context.getMenu().add(menu);
        menu.setIcon(getIcon(18));

    }

    @Override
    public void onRemoveFile(Object caller, File[] fileList) {
    }

}