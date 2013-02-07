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

import java.awt.Cursor;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import jd.Launcher;
import jd.config.SubConfiguration;
import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.FileCreationListener;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.ArchiveSettings.BooleanStatus;
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
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberTablePropertiesContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig, ExtractionTranslation> implements FileCreationListener, MenuFactoryListener {

    private ExtractionQueue                   extractionQueue   = new ExtractionQueue();

    private ExtractionEventSender             broadcaster       = new ExtractionEventSender();

    private final java.util.List<IExtraction> extractors        = new ArrayList<IExtraction>();

    private final java.util.List<Archive>     archives          = new ArrayList<Archive>();

    private ExtractionConfigPanel             configPanel;

    private static ExtractionExtension        INSTANCE;

    private ExtractionListenerIcon            statusbarListener = null;

    private AbstractToolbarAction             extractFileAction;

    private AppAction                         menuAction;

    private ShutdownVetoListener              listener          = null;

    private LogSource                         logger;

    public ExtractionExtension() throws StartException {
        super();
        setTitle(_.name());
        INSTANCE = this;
        logger = LogController.getInstance().getLogger("ExtractionExtension");
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
            logger.info("First File does not exist " + archive.getFirstArchiveFile());
            return null;
        }
        archives.add(archive);

        archive.getFactory().fireArchiveAddedToQueue(archive);

        ExtractionController controller = new ExtractionController(this, archive);
        controller.setOverwriteFiles(isOverwriteFiles(archive));
        controller.setRemoveAfterExtract(isRemoveFilesAfterExtractEnabled(archive));
        controller.setRemoveDownloadLinksAfterExtraction(isRemoveDownloadLinksAfterExtractEnabled(archive));

        archive.setActive(true);
        extractor.setConfig(getSettings());

        extractionQueue.addAsynch(controller);
        fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.QUEUED));
        return controller;
    }

    private boolean isRemoveDownloadLinksAfterExtractEnabled(Archive archive) {

        switch (archive.getSettings().getRemoveDownloadLinksAfterExtraction()) {
        case FALSE:
            return false;
        case TRUE:
            return true;

        }
        return getSettings().isDeleteArchiveDownloadlinksAfterExtraction();
    }

    private boolean isRemoveFilesAfterExtractEnabled(Archive archive) {

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
    private synchronized Archive buildArchive(ArchiveFactory link) throws ArchiveException {
        IExtraction extrctor = getExtractorByFactory(link);
        if (extrctor == null) {
            //
            return null;
        }
        for (Archive archive : archives) {
            if (archive.contains(link)) {
                logger.info("Found Archive: " + archive);
                return archive;

            }
        }

        Archive archive = extrctor.buildArchive(link);
        link.onArchiveFinished(archive);
        logger.info("Created Archive: " + archive);
        // Log.L.info("Created Archive: " + archive);
        logger.info("Files: " + archive.getArchiveFiles());

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

            @Override
            public long getShutdownVetoPriority() {
                return 0;
            }

        });
    }

    void fireEvent(ExtractionEvent event) {
        broadcaster.fireEvent(event);
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
            public String createIconKey() {

                return "unpack";
            }

            @Override
            protected String createAccelerator() {
                return ShortcutController._.getExtractionUnpackFilesAction();
            }
        };

        initExtractors();

        // addListener(new ExtractionListenerFile());
        broadcaster.addListener(new ExtractionListenerList());

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                logger.severe("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
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
                            archive.getSettings().setExtractPath(path.getAbsolutePath());
                        } else {
                            File path = DownloadFolderChooserDialog.open(archiveStartFile.getParentFile(), false, "Extract To");
                            archive.getSettings().setExtractPath(path.getAbsolutePath());
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
                        logger.log(e1);
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
    public String getDescription() {
        return _.description();
    }

    @Override
    public AddonPanel<ExtractionExtension> getGUI() {
        return null;
    }

    @Override
    public List<JMenuItem> getMenuAction() {
        java.util.List<JMenuItem> ret = new java.util.ArrayList<JMenuItem>();
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

        try {
            logger.info("New File by " + caller);
            if (caller instanceof SingleDownloadController) {
                DownloadLink link = ((SingleDownloadController) caller).getDownloadLink();

                logger.info("link supported  " + isLinkSupported(new DownloadLinkArchiveFactory(link)));
                if (isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    Archive archive;
                    archive = buildArchive(new DownloadLinkArchiveFactory(link));
                    logger.info("postprocess \r\n" + archive.getSettings());
                    logger.info("archive active " + archive.isActive());
                    logger.info("archive size " + archive.getArchiveFiles().size());
                    logger.info("archive complete " + archive.isComplete());
                    if (archive.isActive() || archive.getArchiveFiles().size() < 1 || !archive.isComplete() || !isAutoExtractEnabled(archive)) return;
                    this.addToQueue(archive);
                }
                //

                //
            } else if (caller instanceof ExtractionController && getSettings().isDeepExtractionEnabled()) {
                try {
                    for (File archiveStartFile : fileList) {
                        FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                        if (isLinkSupported(fac)) {
                            Archive ar = buildArchive(fac);
                            if (ar.isActive() || ar.getArchiveFiles().size() < 1 || !ar.isComplete() || !isAutoExtractEnabled(ar)) continue;
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
                            if (ar.isActive() || ar.getArchiveFiles().size() < 1 || !ar.isComplete() || !isAutoExtractEnabled(ar)) continue;
                            addToQueue(ar);
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

    private void onExtendPopupMenuLinkgrabberTable(final LinkgrabberTableContext context) {

        final JMenu menu = new JMenu(_.contextmenu_main()) {
            /**
             * 
             */
            private static final long serialVersionUID = -5156294142768994122L;

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };

        // menu.setEnabled(false);
        context.getMenu().add(menu);

        menu.setIcon(getIcon(18));
        menu.setEnabled(false);
        IOEQ.add(new Runnable() {

            public void run() {

                final String org = menu.getText();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        menu.setText(_.contextmenu_loading(org));
                        menu.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    }

                };
                final ValidateArchiveAction<CrawledPackage, CrawledLink> validation = new ValidateArchiveAction<CrawledPackage, CrawledLink>(ExtractionExtension.this, context.getSelectionInfo());

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (validation.getArchives().size() > 0) {
                            createMenu(menu, false, validation.getArchives().toArray(new Archive[] {}));
                            menu.setEnabled(true);
                        }
                    }
                };
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        menu.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        menu.setText(org);
                    }
                };

            }
        });

    }

    protected void onExtendPopupMenuDownloadTable(final DownloadTableContext context) {

        final JMenu menu = new JMenu(_.contextmenu_main()) {
            /**
             * 
             */
            private static final long serialVersionUID = -5156294142768994122L;

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };

        // menu.setEnabled(false);
        context.getMenu().add(menu);

        menu.setIcon(getIcon(18));
        menu.setEnabled(false);
        IOEQ.add(new Runnable() {

            @Override
            public void run() {
                final String org = menu.getText();
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        menu.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        menu.setText(_.contextmenu_loading(org));
                    }

                };
                final ValidateArchiveAction<FilePackage, DownloadLink> validation = new ValidateArchiveAction<FilePackage, DownloadLink>(ExtractionExtension.this, context.getSelectionInfo());
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {

                        if (validation.getArchives().size() > 0) {
                            createMenu(menu, true, validation.getArchives().toArray(new Archive[] {}));
                            menu.setEnabled(true);
                        }
                    }
                };
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        menu.setText(org);
                        menu.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                };
            }

        });

    }

    private void createMenu(JMenu menu, boolean startExtract, final Archive... archives) {

        // final boolean fileExists = new File(link.getFileOutput()).exists();
        // final boolean isLinkSupported = isLinkSupported(dlAF);
        if (startExtract) {

            menu.add(new AppAction() {
                /**
             * 
             */
                private static final long serialVersionUID = 1375146705091555054L;

                {
                    setName(_.contextmenu_extract());
                    Image front = NewTheme.I().getImage("media-playback-start", 20, true);
                    setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));
                    setEnabled(true);
                }

                public void actionPerformed(ActionEvent e) {
                    Thread thread = new Thread() {
                        @Override
                        public void run() {
                            for (Archive archive : archives) {
                                if (archive.isComplete()) {
                                    addToQueue(archive);
                                } else {
                                    Dialog.getInstance().showMessageDialog(_.cannot_extract_incopmplete(archive.getName()));
                                }
                            }

                        }
                    };
                    thread.setName("Extract Context: extract");
                    thread.setDaemon(true);
                    thread.start();
                }
            });
        }
        menu.add(new ValidateArchiveAction(this, archives));
        menu.add(new JSeparator());

        menu.add(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 2539573738898436840L;

            {
                setName(_.contextmenu_autoextract());
                setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("refresh", 12), 0, 0, 10, 10)));
                setSelected(isAutoExtractEnabled(archives[0]));
            }

            public void actionPerformed(ActionEvent e) {
                for (Archive archive : archives) {
                    archive.getSettings().setAutoExtract(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
                }
                Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoextract_true() : _.set_autoextract_false());
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

            }

            public void actionPerformed(ActionEvent e) {

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
                ArchiveFactory dlAF = archives[0].getFactory();
                File extractto = getFinalExtractToFolder(archives[0]);

                while (extractto != null && !extractto.isDirectory()) {
                    extractto = extractto.getParentFile();
                }
                try {
                    File path = DownloadFolderChooserDialog.open(extractto, false, T._.extract_to2());

                    if (path == null) return;
                    for (Archive archive : archives) {
                        archive.getSettings().setExtractPath(path.getAbsolutePath());
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }
        });

        menu.add(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 3224128311203557582L;

            {
                setName(_.contextmenu_password());
                setSmallIcon(NewTheme.I().getIcon("password", 20));

            }

            public void actionPerformed(ActionEvent e) {

                try {
                    StringBuilder sb = new StringBuilder();
                    HashSet<String> list = new HashSet<String>();

                    for (Archive archive : archives) {
                        HashSet<String> pws = archive.getSettings().getPasswords();
                        if (pws != null) list.addAll(pws);
                    }

                    if (list != null && list.size() > 0) {
                        for (String s : list) {
                            if (sb.length() > 0) sb.append("\r\n");
                            sb.append(s);
                        }
                    }
                    String pw = null;

                    if (archives.length > 1) {
                        pw = Dialog.getInstance().showInputDialog(0, _.context_password(), (list == null || list.size() == 0) ? _.context_password_msg_multi() : _.context_password_msg2_multi(sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

                    } else {
                        pw = Dialog.getInstance().showInputDialog(0, _.context_password(), (list == null || list.size() == 0) ? _.context_password_msg(archives[0].getName()) : _.context_password_msg2(archives[0].getName(), sb.toString()), null, NewTheme.getInstance().getIcon("password", 32), null, null);

                    }
                    if (!StringUtils.isEmpty(pw)) {

                        list.add(pw);
                        for (Archive archive : archives) {
                            archive.getSettings().setPasswords(list);
                        }
                    }

                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }
        });
        JMenu cleanup;
        menu.add(cleanup = new JMenu(_.context_cleanup()) {

            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        });
        cleanup.setIcon(NewTheme.I().getIcon("clear", 20));
        cleanup.add(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 2539573738898436840L;

            {
                setName(_.contextmenu_autodeletefiles());
                setSmallIcon(NewTheme.I().getIcon("file", 20));
                setSelected(isRemoveFilesAfterExtractEnabled(archives[0]));
            }

            public void actionPerformed(ActionEvent e) {
                for (Archive archive : archives) {
                    archive.getSettings().setRemoveFilesAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
                }
                Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoremovefiles_true() : _.set_autoremovefiles_false());
            }

        });

        cleanup.add(new AppAction() {
            /**
             * 
             */
            private static final long serialVersionUID = 2539573738898436840L;

            {
                setName(_.contextmenu_autodeletelinks());
                setSmallIcon(NewTheme.I().getIcon("link", 20));
                setSelected(isRemoveDownloadLinksAfterExtractEnabled(archives[0]));
            }

            public void actionPerformed(ActionEvent e) {
                for (Archive archive : archives) {
                    archive.getSettings().setRemoveDownloadLinksAfterExtraction(isSelected() ? BooleanStatus.TRUE : BooleanStatus.FALSE);
                }
                Dialog.getInstance().showMessageDialog(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, isSelected() ? _.set_autoremovelinks_true() : _.set_autoremovelinks_false());
            }

        });

    }

    public boolean isAutoExtractEnabled(Archive archive) {
        switch (archive.getSettings().getAutoExtract()) {
        case FALSE:
            return false;
        case TRUE:
            return true;
        case UNSET:
            return getSettings().isEnabled();
        }
        return false;

    }

    @Override
    public void onRemoveFile(Object caller, File[] fileList) {
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
        if (archive.getSettings().getExtractionInfo() != null) {
            /* archive already extracted, use extracttofolder */
            path = archive.getSettings().getExtractionInfo().getExtractToFolder();
        }
        if (StringUtils.isEmpty(path)) {
            /* use customized extracttofolder */
            path = archive.getSettings().getExtractPath();
        }
        if (!StringUtils.isEmpty(path)) { return new File(path); }
        if (getSettings().isCustomExtractionPathEnabled()) {
            /* customized extractpath is enabled */
            path = getSettings().getCustomExtractionPath();
        }
        if (StringUtils.isEmpty(path)) {
            /* extractpath is still emptry, create default one */
            path = archive.getFactory().createDefaultExtractToPath(archive);
        }
        File ret = new File(path);
        if (getSettings().isSubpathEnabled()) {
            if (getSettings().getSubPathFilesTreshhold() > archive.getContentView().getFileCount() + archive.getContentView().getDirectoryCount()) return ret;
            if (getSettings().isSubpathEnabledIfAllFilesAreInAFolder() && archive.getContentView().getFileCount() == 0) return ret;
            String sub = getSettings().getSubPath();
            if (!StringUtils.isEmpty(sub)) {
                sub = archive.getFactory().createExtractSubPath(sub, archive);
                if (!StringUtils.isEmpty(sub)) ret = new File(ret, sub);
            }
        }
        return ret;
    }

}