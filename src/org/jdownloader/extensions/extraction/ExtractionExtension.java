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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;

import javax.swing.JViewport;
import javax.swing.filechooser.FileFilter;

import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.downloads.DownloadLinksPanel;
import jd.gui.swing.jdgui.views.downloads.DownloadTable;
import jd.nutils.jobber.Jobber;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.multi.Multi;
import org.jdownloader.extensions.extraction.split.HJSplit;
import org.jdownloader.extensions.extraction.split.Unix;
import org.jdownloader.extensions.extraction.split.XtreamSplit;
import org.jdownloader.extensions.extraction.translate.T;

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

    private Jobber                       queue;

    private Timer                        update                  = null;

    private final ArrayList<IExtraction> extractors              = new ArrayList<IExtraction>();

    private final ArrayList<Archive>     archives                = new ArrayList<Archive>();

    private ExtractionConfigPanel        configPanel;

    public ExtractionExtension() throws StartException {
        super(T._.name());
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
    private final boolean isLinkSupported(String file) {
        for (IExtraction extractor : extractors) {
            if (extractor.isArchivSupported(file)) { return true; }
        }

        return false;
    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private void addToQueue(final Archive archive) {
        IExtraction extractor = archive.getExtractor();

        if (!new File(archive.getFirstDownloadLink().getFileOutput()).exists()) return;
        archive.getFirstDownloadLink().getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
        archive.getFirstDownloadLink().getLinkStatus().setErrorMessage(null);
        File dl = this.getExtractToPath(archive.getFirstDownloadLink());

        archive.setOverwriteFiles(getSettings().isOverwriteExistingFilesEnabled());
        archive.setExtractTo(dl);

        ExtractionController controller = new ExtractionController(archive, logger);

        if (archive.getFirstDownloadLink() instanceof DummyDownloadLink) {
            ProgressController progress = new ProgressController(T._.plugins_optional_extraction_progress_extractfile(archive.getFirstDownloadLink().getFileOutput()), 100, getIconKey());
            controller.setProgressController(progress);

            controller.addExtractionListener(new ExtractionListenerFile(this));
        } else {
            controller.addExtractionListener(new ExtractionListenerList(this));
        }

        controller.setRemoveAfterExtract(getSettings().isDeleteArchiveFilesAfterExtraction());

        archive.setActive(true);
        extractor.setConfig(getSettings());

        controller.fireEvent(ExtractionConstants.WRAPPER_STARTED);
        queue.add(controller);

        if (update == null) {
            update = new Timer("Extraction display update");
        }
        controller.setTimer(update);

        queue.start();

        for (DownloadLink link1 : archive.getDownloadLinks()) {
            link1.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
        }
    }

    /**
     * Bestimmt den Pfad in den das Archiv entpackt werden soll
     * 
     * @param link
     * @return
     */
    private File getExtractToPath(DownloadLink link) {
        if (link.getProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) return (File) link.getProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH);
        if (link instanceof DummyDownloadLink) return new File(link.getFileOutput()).getParentFile();
        String path;

        if (!getSettings().isCustomExtractionPathEnabled()) {
            path = new File(link.getFileOutput()).getParent();
        } else {
            path = getSettings().getCustomExtractionPath();
            if (path == null) {
                path = JDUtilities.getDefaultDownloadDirectory();
            }
        }

        return new File(path);
    }

    @Override
    public String getIconKey() {
        return "gui.images.addons.unrar";
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
    private Archive buildArchive(DownloadLink link) {
        for (Archive archive : archives) {
            if (archive.getDownloadLinks().contains(link)) { return archive; }
        }

        Archive archive = getExtractor(link).buildArchive(link);

        archives.add(archive);

        return archive;
    }

    /**
     * Builds an dummy archive for an file.
     * 
     * @param file
     * @return
     */
    private Archive buildDummyArchive(File file) {
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
        if (link == null) {
            /* link no longer in list */
            DummyDownloadLink link0 = new DummyDownloadLink(file.getName());
            link0.setFile(file);
            return buildArchive(link0);
        }

        return buildArchive(link);
    }

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
                final Archive archive = buildArchive(link);
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
            for (FilePackage fp : fps) {
                for (DownloadLink l : fp.getDownloadLinkList()) {
                    if (l.getLinkStatus().isFinished()) {
                        links.add(l);
                    }
                }
            }
            if (links.size() == 0) return;
            for (DownloadLink link0 : links) {
                final Archive archive0 = buildArchive(link0);
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
            String path = link.getStringProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2");

            if (!new File(path).exists()) {
                UserIO.getInstance().requestMessageDialog(T._.plugins_optional_extraction_messages(path));
            } else {
                JDUtilities.openExplorer(new File(path));
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

            File extractto = this.getExtractToPath(link0);
            while (extractto != null && !extractto.isDirectory()) {
                extractto = extractto.getParentFile();
            }

            File[] files = UserIO.getInstance().requestFileChooser("_EXTRACTION_", null, UserIO.DIRECTORIES_ONLY, ff, null, extractto, null);
            if (files == null) return;

            for (DownloadLink ll : dlinks) {
                Archive archive0 = buildArchive(ll);
                for (DownloadLink l : archive0.getDownloadLinks()) {
                    l.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, files[0]);
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
            if (getSettings().getSubPathFilesTreshold() > controller.getArchiv().getNumberOfFiles()) { return; }
            if (getSettings().isSubpathEnabledIfAllFilesAreInAFolder()) {
                if (controller.getArchiv().isNoFolder()) { return; }
            }

            String path = getSettings().getSubPath();
            DownloadLink link = controller.getArchiv().getFirstDownloadLink();

            try {
                if (link.getFilePackage().getName() != null) {
                    path = path.replace("%PACKAGENAME%", link.getFilePackage().getName());
                } else {
                    path = path.replace("%PACKAGENAME%", "");
                    logger.severe("Could not set packagename for " + controller.getArchiv().getFirstDownloadLink().getFileOutput());
                }

                if (controller.getArchiv().getExtractor().getArchiveName(link) != null) {
                    path = path.replace("%ARCHIVENAME%", controller.getArchiv().getExtractor().getArchiveName(link));
                } else {
                    path = path.replace("%ARCHIVENAME%", "");
                    logger.severe("Could not set archivename for " + controller.getArchiv().getFirstDownloadLink().getFileOutput());
                }

                if (link.getHost() != null) {
                    path = path.replace("%HOSTER%", link.getHost());
                } else {
                    path = path.replace("%HOSTER%", "");
                    logger.severe("Could not set hoster for " + controller.getArchiv().getFirstDownloadLink().getFileOutput());
                }

                if (path.contains("$DATE:")) {
                    int start = path.indexOf("$DATE:");
                    int end = start + 6;

                    while (end < path.length() && path.charAt(end) != '$') {
                        end++;
                    }

                    try {
                        SimpleDateFormat format = new SimpleDateFormat(path.substring(start + 6, end));
                        path = path.replace(path.substring(start, end + 1), format.format(new Date()));
                    } catch (Exception e) {
                        path = path.replace(path.substring(start, end + 1), "");
                        logger.severe("Could not set extraction date. Maybe pattern is wrong. For " + controller.getArchiv().getFirstDownloadLink().getFileOutput());
                    }
                }

                String dif = new File(JDUtilities.getDefaultDownloadDirectory()).getAbsolutePath().replace(new File(link.getFileOutput()).getParent(), "");
                if (new File(dif).isAbsolute()) {
                    dif = "";
                }
                path = path.replace("%SUBFOLDER%", dif);

                path = path.replaceAll("[/]+", "\\\\");
                path = path.replaceAll("[\\\\]+", "\\\\");

                controller.getArchiv().setExtractTo(new File(controller.getArchiv().getExtractTo(), path));
            } catch (Exception e) {
                JDLogger.exception(e);
            }

            for (DownloadLink l : controller.getArchiv().getDownloadLinks()) {
                if (l == null) continue;
                l.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, controller.getArchiv().getExtractTo().getAbsolutePath());
            }
        }
    }

    /**
     * Returns the extractor for the {@link DownloadLink}.
     * 
     * @param link
     * @return
     */
    private IExtraction getExtractor(DownloadLink link) {
        for (IExtraction extractor : extractors) {
            try {
                if (extractor.isArchivSupported(link.getFileOutput())) { return extractor.getClass().newInstance(); }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // @Override
    // public Object interact(String command, Object parameter) {
    // if (command.equals("isWorking")) {
    // return queue.isAlive();
    // } else {
    // return null;
    // }
    // }

    /**
     * Finishes the extraction process.
     * 
     * @param controller
     */
    void onFinished(ExtractionController controller) {
        controller.getArchiv().getFirstDownloadLink().setPluginProgress(null);
        if (controller.getProgressController() != null) {
            controller.getProgressController().doFinalize(8000);
        }
    }

    /**
     * Removes an {@link Archive} from the list.
     * 
     * @param archive
     */
    void removeArchive(Archive archive) {
        archives.remove(archive);
    }

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        DownloadLink link;
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getCaller() instanceof PluginForHost)) return;
            link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (link.getFilePackage().isPostProcessing() && this.getPluginConfig().getBooleanProperty("ACTIVATED", true) && isLinkSupported(link.getFileOutput())) {
                Archive archive = buildArchive(link);

                if (!archive.isActive() && archive.getDownloadLinks().size() > 0 && archive.isComplete()) {
                    this.addToQueue(archive);
                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            if (getSettings().isDeepExtractionEnabled()) {
                try {
                    File[] list = (File[]) event.getParameter();
                    for (File archiveStartFile : list) {
                        if (isLinkSupported(archiveStartFile.getAbsolutePath())) {
                            Archive ar = buildDummyArchive(archiveStartFile);
                            if (ar.isActive() || ar.getDownloadLinks().size() < 1 || !ar.isComplete()) continue;
                            addToQueue(ar);
                        }
                    }
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
            break;
        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            MenuAction m;
            MenuAction container = new MenuAction("optional.extraction.linkmenu.container", 0);
            container.setIcon(getIconKey());
            items.add(container);
            if (event.getCaller() instanceof DownloadLink) {
                link = (DownloadLink) event.getCaller();

                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.extract", EXTRACT_LINK));
                m.setIcon(getIconKey());
                m.setActionListener(this);

                boolean isLocalyAvailable = new File(link.getFileOutput()).exists() || new File(link.getStringProperty(DownloadLink.STATIC_OUTPUTFILE, link.getFileOutput())).exists();

                if (isLocalyAvailable && isLinkSupported(link.getFileOutput())) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                m.setProperty(MENU_LINKS, ((DownloadTable) ((JViewport) DownloadLinksPanel.getDownloadLinksPanel().getScrollPane().getComponent(0)).getComponent(0)).getSelectedDownloadLinks());
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.autoextract", SET_LINK_AUTOEXTRACT));
                m.setActionListener(this);
                m.setSelected(link.getFilePackage().isPostProcessing());
                if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                    m.setEnabled(false);
                }
                m.setProperty(MENU_LINKS, ((DownloadTable) ((JViewport) DownloadLinksPanel.getDownloadLinksPanel().getScrollPane().getComponent(0)).getComponent(0)).getSelectedDownloadLinks());
                container.addMenuItem(new MenuAction(Types.SEPARATOR));
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.setextract", SET_EXTRACT_TO));
                m.setActionListener(this);

                if (isLinkSupported(link.getFileOutput())) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                m.setProperty("LINK", link);
                m.setProperty(MENU_LINKS, ((DownloadTable) ((JViewport) DownloadLinksPanel.getDownloadLinksPanel().getScrollPane().getComponent(0)).getComponent(0)).getSelectedDownloadLinks());
                File dir = this.getExtractToPath(link);
                while (dir != null && !dir.exists()) {
                    if (dir.getParentFile() == null) break;
                    dir = dir.getParentFile();
                }
                if (dir == null) break;
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.openextract3", OPEN_EXTRACT));
                m.setActionListener(this);

                if (isLinkSupported(link.getFileOutput())) {
                    m.setEnabled(true);
                } else {
                    m.setEnabled(false);
                }

                link.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2", dir.getAbsolutePath());
                m.setProperty("LINK", link);
            } else {
                FilePackage fp = (FilePackage) event.getCaller();

                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.package.extract", EXTRACT_PACKAGE));
                m.setProperty(MENU_PACKAGES, ((DownloadTable) ((JViewport) DownloadLinksPanel.getDownloadLinksPanel().getScrollPane().getComponent(0)).getComponent(0)).getSelectedFilePackages());
                m.setIcon(getIconKey());
                m.setActionListener(this);
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.package.autoextract", SET_PACKAGE_AUTOEXTRACT));
                m.setProperty(MENU_PACKAGES, ((DownloadTable) ((JViewport) DownloadLinksPanel.getDownloadLinksPanel().getScrollPane().getComponent(0)).getComponent(0)).getSelectedFilePackages());
                m.setSelected(fp.isPostProcessing());
                m.setActionListener(this);
                if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                    m.setEnabled(false);
                }
            }
            break;
        }
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        JDController.getInstance().addControlListener(this);
    }

    @Override
    protected void initExtension() throws StartException {
        // we will remove them soon anway
        // ArrayList<PluginOptional> pluginsOptional = new
        // ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        // Collections.sort(pluginsOptional);
        //
        // for (OptionalPluginWrapper pow : pluginsOptional) {
        // if (pow.getAnnotation().id().equals("unrar") ||
        // pow.getAnnotation().id().equals("hjsplit")) {
        // if (pow.isEnabled()) {
        // logger.warning("Disable unrar and hjsplit to use this plugin");
        // UserIO.getInstance().requestMessageDialog(0,
        // "Disable unrar and hjsplit to use this plugin");
        // return false;
        // }
        // }
        // }

        initExtractors();

        Iterator<IExtraction> it = extractors.iterator();
        while (it.hasNext()) {
            IExtraction extractor = it.next();
            if (!extractor.checkCommand()) {
                logger.warning("Extractor " + extractor.getClass().getName() + " plugin could not be initialized");
                it.remove();
            }
        }

        this.queue = new Jobber(1);

        if (menuAction == null) menuAction = new MenuAction("optional.extraction.menu.extract.singlefiles", getIconKey()) {
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
                            if (extractor.isArchivSupportedFileFilter(pathname.getAbsolutePath())) { return true; }
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
                    final Archive archive = buildDummyArchive(archiveStartFile);
                    new Thread() {
                        @Override
                        public void run() {
                            if (archive.isComplete()) addToQueue(archive);
                        }
                    }.start();
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
    public AddonPanel getGUI() {
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