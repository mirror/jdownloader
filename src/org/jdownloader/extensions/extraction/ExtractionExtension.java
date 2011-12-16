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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.filechooser.FileFilter;

import jd.Main;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFactory;
import org.jdownloader.extensions.extraction.bindings.downloadlink.DownloadLinkArchiveFile;
import org.jdownloader.extensions.extraction.bindings.file.FileArchiveFactory;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.Unix;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.views.downloads.table.DownloadsTable;
import org.jdownloader.settings.GeneralSettings;

public class ExtractionExtension extends AbstractExtension<ExtractionConfig> implements ControlListener, ActionListener {
    private static final int             EXTRACT_LINK            = 1000;

    private static final int             EXTRACT_PACKAGE         = 1001;

    private static final int             OPEN_EXTRACT            = 1002;

    private static final int             SET_EXTRACT_TO          = 1003;

    private static final int             SET_LINK_AUTOEXTRACT    = 1005;

    private static final int             SET_PACKAGE_AUTOEXTRACT = 1006;

    private static final String          MENU_PACKAGES           = "MENU_EXTRACT_PACKAGE";

    private static final String          MENU_LINKS              = "MENU_EXTRACT_LINK";

    private static MenuAction            menuAction              = null;

    private ExtractionQueue              ExtractionQueue         = new ExtractionQueue();

    private ExtractionEventSender        broadcaster             = new ExtractionEventSender();

    private final ArrayList<IExtraction> extractors              = new ArrayList<IExtraction>();

    private final ArrayList<Archive>     archives                = new ArrayList<Archive>();

    private ExtractionConfigPanel        configPanel;

    private static ExtractionExtension   INSTANCE;

    private ExtractionListenerIcon       statusbarListener       = null;

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
     * Startet das abwarbeiten der extractqueue
     */
    public void addToQueue(final Archive archive) {
        IExtraction extractor = archive.getExtractor();

        if (!archive.getFirstArchiveFile().exists()) return;
        archive.getFactory().fireArchiveAddedToQueue(archive);

        archive.setOverwriteFiles(getSettings().isOverwriteExistingFilesEnabled());

        ExtractionController controller = new ExtractionController(archive, logger);

        controller.setRemoveAfterExtract(getSettings().isDeleteArchiveFilesAfterExtraction());

        archive.setActive(true);
        extractor.setConfig(getSettings());

        fireEvent(new ExtractionEvent(controller, ExtractionEvent.Type.QUEUED));
        ExtractionQueue.addAsynch(controller);
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

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuAction) {
            actionPerformedOnMenuItem((MenuAction) e.getSource());
        }
    }

    /**
     * Builds an archive for an {@link DownloadLink}.
     * 
     * @param link
     * @return
     */
    private Archive buildArchive(ArchiveFactory link) {
        for (Archive archive : archives) {
            if (archive.contains(link)) { return archive; }
        }

        Archive archive = getExtractorByFactory(link).buildArchive(link);
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
     * ActionListener for the menuitems.
     * 
     * @param source
     */
    @SuppressWarnings("unchecked")
    private void actionPerformedOnMenuItem(MenuAction source) {
        ArrayList<DownloadLink> dlinks = (ArrayList<DownloadLink>) source.getProperty(MENU_LINKS);
        ArrayList<FilePackage> fps = (ArrayList<FilePackage>) source.getProperty(MENU_PACKAGES);
        switch (source.getActionID()) {

        case EXTRACT_LINK:
            if (dlinks == null) return;
            for (DownloadLink link : dlinks) {
                final Archive archive = buildArchive(new DownloadLinkArchiveFactory(link));
                new Thread() {
                    @Override
                    public void run() {
                        if (archive.isComplete()) addToQueue(archive);
                    }
                }.start();
            }
            break;
        case EXTRACT_PACKAGE:
            if (fps == null) return;
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (FilePackage fp : fps) {
                    synchronized (fp) {
                        for (DownloadLink l : fp.getChildren()) {
                            if (l.getLinkStatus().isFinished()) {
                                links.add(l);
                            }
                        }
                    }
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }
            if (links.size() == 0) return;
            for (DownloadLink link0 : links) {
                final Archive archive0 = buildArchive(new DownloadLinkArchiveFactory(link0));
                new Thread() {
                    @Override
                    public void run() {
                        if (archive0.isComplete()) addToQueue(archive0);
                    }
                }.start();
            }
            break;
        case OPEN_EXTRACT:
            DownloadLink link = (DownloadLink) source.getProperty("LINK");
            if (link == null) { return; }
            String path = link.getStringProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2");

            if (!new File(path).exists()) {
                UserIO.getInstance().requestMessageDialog(T._.plugins_optional_extraction_messages(path));
            } else {
                CrossSystem.openFile(new File(path));
            }

            break;
        case SET_EXTRACT_TO:
            DownloadLink link0 = (DownloadLink) source.getProperty("LINK");
            if (link0 == null) { return; }
            if (dlinks == null) return;

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

            File extractto = this.getExtractToPath(new DownloadLinkArchiveFactory(link0), null);
            while (extractto != null && !extractto.isDirectory()) {
                extractto = extractto.getParentFile();
            }

            File[] files = UserIO.getInstance().requestFileChooser("_EXTRACTION_", null, UserIO.DIRECTORIES_ONLY, ff, null, extractto, null);
            if (files == null) return;

            for (DownloadLink ll : dlinks) {
                Archive archive0 = buildArchive(new DownloadLinkArchiveFactory(ll));
                for (ArchiveFile l : archive0.getArchiveFiles()) {

                    ((DownloadLinkArchiveFile) l).getDownloadLink().setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTTOPATH, files[0]);
                }
            }
            break;
        case SET_LINK_AUTOEXTRACT:
            if (dlinks == null) { return; }
            for (DownloadLink l : dlinks) {
                l.getFilePackage().setPostProcessing(!l.getFilePackage().isPostProcessing());
            }
            break;
        case SET_PACKAGE_AUTOEXTRACT:
            if (fps == null) { return; }
            for (FilePackage fp : fps) {
                if (fp == null) continue;
                fp.setPostProcessing(!fp.isPostProcessing());
            }
            break;
        }
    }

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

    @SuppressWarnings({ "unchecked", "deprecation" })
    public void controlEvent(ControlEvent event) {
        DownloadLink link;
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_DOWNLOAD_FINISHED:
            if (event.getCaller() instanceof SingleDownloadController) {
                link = ((SingleDownloadController) event.getCaller()).getDownloadLink();
                if (link.getFilePackage().isPostProcessing() && this.getPluginConfig().getBooleanProperty("ACTIVATED", true) && isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    Archive archive = buildArchive(new DownloadLinkArchiveFactory(link));

                    if (!archive.isActive() && archive.getArchiveFiles().size() > 0 && archive.isComplete()) {
                        this.addToQueue(archive);
                    }
                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            if (getSettings().isDeepExtractionEnabled()) {
                try {
                    File[] list = (File[]) event.getParameter();
                    for (File archiveStartFile : list) {
                        FileArchiveFactory fac = new FileArchiveFactory(archiveStartFile);
                        if (isLinkSupported(fac)) {
                            Archive ar = buildArchive(fac);
                            if (ar.isActive() || ar.getArchiveFiles().size() < 1 || !ar.isComplete()) continue;
                            addToQueue(ar);
                        }
                    }
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
            break;
        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            Object source = event.getParameter2();
            if (source == null || !(source instanceof DownloadsTable)) break;
            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            MenuAction m;
            MenuAction container = new MenuAction("Extract", "optional.extraction.linkmenu.container", 0) {
                private static final long serialVersionUID = 6976229914297269865L;

                @Override
                protected String createMnemonic() {
                    return null;
                }

                @Override
                protected String createAccelerator() {
                    return null;
                }

                @Override
                protected String createTooltip() {
                    return null;
                }
            };
            container.setIcon(getIconKey());
            items.add(container);
            if (event.getCaller() instanceof DownloadLink) {
                link = (DownloadLink) event.getCaller();

                container.addMenuItem(m = new MenuAction("Extract Link", "optional.extraction.linkmenu.extract", EXTRACT_LINK) {
                    private static final long serialVersionUID = 8648569972779344623L;

                    @Override
                    protected String createMnemonic() {
                        return null;
                    }

                    @Override
                    protected String createAccelerator() {
                        return null;
                    }

                    @Override
                    protected String createTooltip() {
                        return null;
                    }
                });
                m.setIcon(getIconKey());
                m.setActionListener(this);

                boolean isLocalyAvailable = new File(link.getFileOutput()).exists();

                if (isLocalyAvailable && isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                m.setProperty(MENU_LINKS, ((DownloadsTable) source).getSelectedChildren());
                container.addMenuItem(m = new MenuAction("Autoextract", "optional.extraction.linkmenu.autoextract", SET_LINK_AUTOEXTRACT) {
                    private static final long serialVersionUID = -6049435417230844732L;

                    @Override
                    protected String createMnemonic() {
                        return null;
                    }

                    @Override
                    protected String createAccelerator() {
                        return null;
                    }

                    @Override
                    protected String createTooltip() {
                        return null;
                    }
                });
                m.setActionListener(this);
                m.setSelected(link.getFilePackage().isPostProcessing());
                if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                    m.setEnabled(false);
                }
                m.setProperty(MENU_LINKS, ((DownloadsTable) source).getSelectedChildren());
                container.addMenuItem(new MenuAction(Types.SEPARATOR) {
                    private static final long serialVersionUID = -8990247058181516475L;

                    @Override
                    protected String createMnemonic() {
                        return null;
                    }

                    @Override
                    protected String createAccelerator() {
                        return null;
                    }

                    @Override
                    protected String createTooltip() {
                        return null;
                    }
                });
                container.addMenuItem(m = new MenuAction("Extract To", "optional.extraction.linkmenu.setextract", SET_EXTRACT_TO) {
                    private static final long serialVersionUID = -7772646110444791318L;

                    @Override
                    protected String createMnemonic() {
                        return null;
                    }

                    @Override
                    protected String createAccelerator() {
                        return null;
                    }

                    @Override
                    protected String createTooltip() {
                        return null;
                    }
                });
                m.setActionListener(this);

                if (isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                m.setProperty("LINK", link);
                m.setProperty(MENU_LINKS, ((DownloadsTable) source).getSelectedChildren());
                File dir = this.getExtractToPath(new DownloadLinkArchiveFactory(link), null);
                while (dir != null && !dir.exists()) {
                    if (dir.getParentFile() == null) break;
                    dir = dir.getParentFile();
                }
                if (dir == null) break;
                container.addMenuItem(m = new MenuAction("Open Extract folder", "optional.extraction.linkmenu.openextract3", OPEN_EXTRACT) {
                    private static final long serialVersionUID = 212186274511234351L;

                    @Override
                    protected String createMnemonic() {
                        return null;
                    }

                    @Override
                    protected String createAccelerator() {
                        return null;
                    }

                    @Override
                    protected String createTooltip() {
                        return null;
                    }
                });
                m.setActionListener(this);

                if (isLinkSupported(new DownloadLinkArchiveFactory(link))) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                link.setProperty(DownloadLinkArchiveFactory.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2", dir.getAbsolutePath());
                m.setProperty("LINK", link);
            } else {
                FilePackage fp = (FilePackage) event.getCaller();
                if (fp != null) {
                    container.addMenuItem(m = new MenuAction("Extract Package", "optional.extraction.linkmenu.package.extract", EXTRACT_PACKAGE) {
                        private static final long serialVersionUID = 5329598452469482639L;

                        @Override
                        protected String createMnemonic() {
                            return null;
                        }

                        @Override
                        protected String createAccelerator() {
                            return null;
                        }

                        @Override
                        protected String createTooltip() {
                            return null;
                        }
                    });
                    m.setIcon(getIconKey());
                    m.setActionListener(this);
                    container.addMenuItem(m = new MenuAction("Auto Extract Package", "optional.extraction.linkmenu.package.autoextract", SET_PACKAGE_AUTOEXTRACT) {
                        private static final long serialVersionUID = 8363153410685078199L;

                        @Override
                        protected String createMnemonic() {
                            return null;
                        }

                        @Override
                        protected String createAccelerator() {
                            return null;
                        }

                        @Override
                        protected String createTooltip() {
                            return null;
                        }
                    });
                    m.setSelected(fp.isPostProcessing());
                    m.setActionListener(this);
                    if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                        m.setEnabled(false);
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);

        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {

            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                JDGui.getInstance().getStatusBar().getExtractionIndicator().setVisible(false);
                            }
                        };
                        if (statusbarListener != null) removeListener(statusbarListener);
                    }
                };
            }
        });

    }

    @Override
    protected void start() throws StartException {
        JDController.getInstance().addControlListener(this);
        Main.GUI_COMPLETE.executeWhenReached(new Runnable() {
            public void run() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                JDGui.getInstance().getStatusBar().getExtractionIndicator().setVisible(true);
                                JDGui.getInstance().getStatusBar().getExtractionIndicator().setDescription("No Job active");
                            }
                        };
                        addListener(statusbarListener = new ExtractionListenerIcon());
                    }
                };
            }
        });
    }

    void fireEvent(ExtractionEvent event) {
        broadcaster.fireEvent(event);
    }

    @Override
    protected void initExtension() throws StartException {
        new ToolBarAction("extraction", "extraction", getIconKey()) { //
            // TODO: remove
            private static final long serialVersionUID = 1L;

            @Override
            protected String createMnemonic() {
                return null;
            }

            @Override
            protected String createAccelerator() {
                return null;
            }

            @Override
            protected String createTooltip() {
                return "Extraction";
            }

            @Override
            public void initDefaults() {
            }

            @Override
            public void onAction(final ActionEvent e) {
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
                    final Archive archive = buildArchive(new FileArchiveFactory(archiveStartFile));
                    new Thread() {
                        @Override
                        public void run() {
                            if (archive.isComplete()) addToQueue(archive);
                        }
                    }.start();
                }
            }
        };

        initExtractors();

        // addListener(new ExtractionListenerFile());
        addListener(new ExtractionListenerList());

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                logger.warning("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                it.remove();
            }
        }

        if (menuAction == null) menuAction = new MenuAction("Extract Files", "optional.extraction.menu.extract.singlefiles", getIconKey()) {
            private static final long serialVersionUID = -7569522709162921624L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
            }

            @Override
            public void onAction(ActionEvent e) {
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
                    final Archive archive = buildArchive(new FileArchiveFactory(archiveStartFile));
                    new Thread() {
                        @Override
                        public void run() {
                            if (archive.isComplete()) addToQueue(archive);
                        }
                    }.start();
                }
            }

            @Override
            protected String createMnemonic() {
                return null;
            }

            @Override
            protected String createAccelerator() {
                return null;
            }

            @Override
            protected String createTooltip() {
                return null;
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
    public ArrayList<MenuAction> getMenuAction() {

        return null;
    }

    @Override
    public ExtensionConfigPanel<ExtractionExtension> getConfigPanel() {
        return configPanel;
    }

}