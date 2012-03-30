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
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileFilter;

import jd.Launcher;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.SingleDownloadController;
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
import org.appwork.utils.logging.Log;
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
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.multi.ArchiveException;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.Unix;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.menu.MenuContext;
import org.jdownloader.gui.menu.eventsender.MenuFactoryEventSender;
import org.jdownloader.gui.menu.eventsender.MenuFactoryListener;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.gui.views.downloads.table.DownloadTableContext;
import org.jdownloader.gui.views.linkgrabber.contextmenu.LinkgrabberTableContext;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig> implements FileCreationListener, MenuFactoryListener {

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
        super(T._.name());
        INSTANCE = this;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    public static ExtractionExtension getIntance() {
        return INSTANCE;
    }

    public void addListener(ExtractionListener listener) {
        broadcaster.addListener(listener);
    }

    public void removeListener(ExtractionListener listener) {
        broadcaster.removeListener(listener);
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
        extractor.setLogger(getLogger());
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
    public synchronized void addToQueue(final Archive archive) {
        // check if we have this archive already in queue.

        for (ExtractionController ec : extractionQueue.getJobs()) {

            if (ec.getArchiv() == archive) return;
        }

        IExtraction extractor = archive.getExtractor();

        if (!archive.getFirstArchiveFile().exists()) return;
        archive.getFactory().fireArchiveAddedToQueue(archive);

        archive.setOverwriteFiles(getSettings().isOverwriteExistingFilesEnabled());

        ExtractionController controller = new ExtractionController(archive, logger);

        controller.setRemoveAfterExtract(getSettings().isDeleteArchiveFilesAfterExtraction());

        archive.setActive(true);
        extractor.setConfig(getSettings());

        extractionQueue.addAsynch(controller);
        fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.QUEUED));
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
        String path = archiveFactory.getExtractPath(archive);

        if (getSettings().isCustomExtractionPathEnabled()) {

            path = getSettings().getCustomExtractionPath();
            if (path == null) {
                path = org.appwork.storage.config.JsonConfig.create(GeneralSettings.class).getDefaultDownloadFolder();
            }
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
        Log.L.info("Created Archive: " + archive);
        Log.L.info("Files: " + archive.getArchiveFiles());
        archives.add(archive);

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
            if (getSettings().getSubPathFilesTreshhold() > controller.getArchiv().getNumberOfFiles()) { return; }
            if (getSettings().isSubpathEnabledIfAllFilesAreInAFolder()) {
                if (controller.getArchiv().isNoFolder()) { return; }
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
                            removeListener(statusbarListener);
                        }
                    }
                };
            }
        });

    }

    @Override
    protected void start() throws StartException {
        MenuFactoryEventSender.getInstance().addListener(this);
        FileCreationManager.getInstance().getEventSender().addListener(this);
        Launcher.GUI_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        if (statusbarListener != null) statusbarListener.cleanup();
                        addListener(statusbarListener = new ExtractionListenerIcon(ExtractionExtension.this));
                    }
                };
            }
        });
        ShutdownController.getInstance().addShutdownVetoListener(listener = new ShutdownVetoListener() {

            @Override
            public void onShutdownVeto(ArrayList<ShutdownVetoException> vetos) {
            }

            @Override
            public void onShutdownRequest(int vetos) throws ShutdownVetoException {
                if (vetos > 0) {
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
                    throw new ShutdownVetoException("ExtractionExtension is still running");
                }
            }

            @Override
            public void onShutdown() {
            }
        });
    }

    void fireEvent(ExtractionEvent event) {
        synchronized (broadcaster) {
            broadcaster.fireEvent(event);
        }
    }

    @Override
    protected void initExtension() throws StartException {
        extractFileAction = new AbstractToolbarAction() {

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
        addListener(new ExtractionListenerList());

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                logger.severe("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                it.remove();
            }
        }

        if (menuAction == null) menuAction = new AppAction() {
            {
                setName("Extract Files");
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
                        return T._.plugins_optional_extraction_filefilter();
                    }
                };

                File[] files = UserIO.getInstance().requestFileChooser("_EXTRATION_", null, UserIO.FILES_ONLY, ff, true, null, null);
                if (files == null) return;

                for (File archiveStartFile : files) {

                    try {
                        final Archive archive = buildArchive(new FileArchiveFactory(archiveStartFile));

                        new Thread() {
                            @Override
                            public void run() {
                                if (archive.isComplete()) addToQueue(archive);
                            }
                        }.start();
                    } catch (ArchiveException e1) {
                        Log.exception(e1);
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
    public String getConfigID() {
        return "extraction";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return T._.description();
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
        getJobQueue().remove(activeValue);
        fireEvent(new ExtractionEvent(activeValue, ExtractionEvent.Type.CLEANUP));
    }

    public void onNewFile(Object caller, File[] fileList) {

        if (caller instanceof SingleDownloadController) {
            DownloadLink link = ((SingleDownloadController) caller).getDownloadLink();
            if (link.getFilePackage().isPostProcessing() && this.getPluginConfig().getBooleanProperty("ACTIVATED", true) && isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                Archive archive;
                try {
                    archive = buildArchive(new DownloadLinkArchiveFactory(link));

                    if (!archive.isActive() && archive.getArchiveFiles().size() > 0 && archive.isComplete()) {
                        this.addToQueue(archive);
                    }
                } catch (ArchiveException e) {
                    Log.exception(e);
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
                Log.exception(e);
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
                Log.exception(e);
            }
        }
    }

    public void onExtendPopupMenu(MenuContext<?> unspecifiedContext) {

        if (unspecifiedContext instanceof DownloadTableContext) {

            onExtendPopupMenuDownloadTable((DownloadTableContext) unspecifiedContext);
        } else if (unspecifiedContext instanceof LinkgrabberTableContext) {
            onExtendPopupMenuLinkgrabberTable((LinkgrabberTableContext) unspecifiedContext);
            // p.add(new
            // ValidateArchiveAction(selection).toContextMenuAction());
        }
    }

    private void onExtendPopupMenuLinkgrabberTable(LinkgrabberTableContext context) {
        boolean isLinkContext = context.getClickedObject() instanceof CrawledLink;
        boolean isShift = context.getMouseEvent().isShiftDown();
        boolean isPkgContext = context.getClickedObject() instanceof CrawledPackage;
        CrawledLink link = isLinkContext ? (CrawledLink) context.getClickedObject() : null;
        CrawledPackage pkg = isPkgContext ? (CrawledPackage) context.getClickedObject() : null;

        context.getMenu().add(new ValidateArchiveAction(this, context.getSelectedObjects()).toContextMenuAction());
    }

    protected void onExtendPopupMenuDownloadTable(final DownloadTableContext context) {
        JMenu menu = new JMenu(T._.contextmenu_main()) {
            protected JMenuItem createActionComponent(Action a) {
                if (((AppAction) a).isToggle()) { return new JCheckBoxMenuItem(a); }
                return super.createActionComponent(a);
            }
        };

        if (context.getClickedObject() instanceof DownloadLink) {
            final DownloadLink link = (DownloadLink) context.getClickedObject();
            final ValidateArchiveAction validateAction = new ValidateArchiveAction(this, context.getSelectedObjects());
            menu.add(new AppAction() {
                {
                    setName(T._.contextmenu_extract());
                    Image front = NewTheme.I().getImage("media-playback-start", 20, true);

                    setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));
                    setEnabled(new File(link.getFileOutput()).exists() && isLinkSupported(new DownloadLinkArchiveFactory(link)) && validateAction.getArchives().size() > 0);
                }

                public void actionPerformed(ActionEvent e) {
                    boolean found = false;
                    for (AbstractNode link : context.getSelectedObjects()) {
                        if (link instanceof DownloadLink) {
                            try {
                                final Archive archive = buildArchive(new DownloadLinkArchiveFactory((DownloadLink) link));
                                if (archive != null) {
                                    found = true;
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            if (archive.isComplete()) {
                                                addToQueue(archive);
                                            } else {
                                                Dialog.getInstance().showMessageDialog(T._.cannot_extract_incopmplete(archive.getName()));
                                            }
                                        }
                                    }.start();
                                }
                            } catch (ArchiveException e1) {
                                Log.exception(e1);
                            }
                        }
                    }

                }
            });

            if (validateAction.getArchives().size() == 1) {
                menu.add(validateAction.toContextMenuAction());
            } else if (validateAction.getArchives().size() > 1) {
                JMenu validate = new JMenu(T._.contextmenu_validate_parent());
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
                {
                    setName(T._.contextmenu_autoextract());
                    setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("unpack", 20), NewTheme.I().getImage("refresh", 12), 0, 0, 10, 10)));
                    setSelected(link.getFilePackage().isPostProcessing());
                }

                public void actionPerformed(ActionEvent e) {

                    for (AbstractNode link : context.getSelectedObjects()) {
                        if (link instanceof DownloadLink) {
                            ((DownloadLink) link).getFilePackage().setPostProcessing(isSelected());
                        }
                    }
                }
            });

            menu.add(new AppAction() {
                {
                    setName(T._.contextmenu_extract_to());
                    setSmallIcon(new ImageIcon(ImageProvider.merge(NewTheme.I().getImage("folder", 20), NewTheme.I().getImage("edit", 12), 0, 0, 10, 10)));

                    setEnabled(new File(link.getFileOutput()).exists() && isLinkSupported(new DownloadLinkArchiveFactory(link)));
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
                            return T._.plugins_optional_extraction_filefilter_extractto();
                        }
                    };

                    File extractto = getExtractToPath(new DownloadLinkArchiveFactory(link), null);
                    while (extractto != null && !extractto.isDirectory()) {
                        extractto = extractto.getParentFile();
                    }

                    File[] files = UserIO.getInstance().requestFileChooser("_EXTRACTION_", null, UserIO.DIRECTORIES_ONLY, ff, null, extractto, null);
                    if (files == null) return;

                    for (AbstractNode link : context.getSelectedObjects()) {
                        if (link instanceof DownloadLink) {
                            try {
                                Archive archive0 = buildArchive(new DownloadLinkArchiveFactory((DownloadLink) link));
                                for (ArchiveFile l : archive0.getArchiveFiles()) {

                                    ((DownloadLinkArchiveFile) l).getDownloadLink().setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTTOPATH, files[0]);
                                }
                            } catch (ArchiveException e1) {
                                Log.exception(e1);
                            }
                        }
                    }
                }
            });

            File dir = this.getExtractToPath(new DownloadLinkArchiveFactory(link), null);
            while (dir != null && !dir.exists()) {
                if (dir.getParentFile() == null) break;
                dir = dir.getParentFile();
            }
            if (dir == null) return;

            menu.add(new AppAction() {
                {
                    setName(T._.contextmenu_openextract_folder());
                    setIconKey("folder");
                    setEnabled(new File(link.getFileOutput()).exists() && isLinkSupported(new DownloadLinkArchiveFactory(link)));
                }

                public void actionPerformed(ActionEvent e) {

                    if (link == null) { return; }
                    String path = link.getStringProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH);

                    if (!new File(path).exists()) {
                        UserIO.getInstance().requestMessageDialog(T._.plugins_optional_extraction_messages(path));
                    } else {
                        CrossSystem.openFile(new File(path));
                    }

                }
            });

            //
            // link.setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH
            // + "2", dir.getAbsolutePath());
            // m.setProperty("LINK", link);
        } else {
            final FilePackage fp = (FilePackage) context.getClickedObject();
            if (fp != null) {

                menu.add(new AppAction() {
                    {
                        setName(T._.contextmenu_extract());
                        Image front = NewTheme.I().getImage("media-playback-start", 20, true);

                        setSmallIcon(new ImageIcon(ImageProvider.merge(getIcon(20).getImage(), front, 0, 0, 5, 5)));

                    }

                    public void actionPerformed(ActionEvent e) {

                        ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                        final boolean readL = DownloadController.getInstance().readLock();
                        try {
                            for (AbstractNode link : context.getSelectedObjects()) {
                                if (link instanceof FilePackage) {
                                    synchronized (fp) {
                                        for (DownloadLink l : fp.getChildren()) {
                                            if (l.getLinkStatus().isFinished()) {
                                                links.add(l);
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            DownloadController.getInstance().readUnlock(readL);
                        }
                        if (links.size() == 0) return;
                        for (DownloadLink link0 : links) {
                            try {
                                final Archive archive0 = buildArchive(new DownloadLinkArchiveFactory(link0));
                                new Thread() {
                                    @Override
                                    public void run() {
                                        if (archive0.isComplete()) addToQueue(archive0);
                                    }
                                }.start();
                            } catch (ArchiveException e1) {
                                Log.exception(e1);
                            }
                        }
                    }
                });

                ValidateArchiveAction validateAction = new ValidateArchiveAction(this, context.getSelectedObjects());
                if (validateAction.getArchives().size() == 1) {
                    menu.add(validateAction.toContextMenuAction());
                } else if (validateAction.getArchives().size() > 1) {
                    JMenu validate = new JMenu(T._.contextmenu_validate_parent());

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
                    {
                        setName(T._.contextmenu_auto_extract_package());
                        setIconKey(ExtractionExtension.this.getIconKey());
                        setSelected(fp.isPostProcessing());
                        setEnabled(getPluginConfig().getBooleanProperty("ACTIVATED", true));
                    }

                    public void actionPerformed(ActionEvent e) {

                        for (AbstractNode link : context.getSelectedObjects()) {
                            if (link instanceof FilePackage) {

                                ((FilePackage) link).setPostProcessing(isSelected());
                            }
                        }
                    }
                });

            }
        }
        context.getMenu().add(menu);
        menu.setIcon(getIcon(20));

    }

}