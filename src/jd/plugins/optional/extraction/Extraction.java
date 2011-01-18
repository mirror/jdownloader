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

package jd.plugins.optional.extraction;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;

import javax.swing.filechooser.FileFilter;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.ProgressController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.jobber.Jobber;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.PluginProgress;
import jd.plugins.optional.extraction.hjsplit.HJSplt;
import jd.plugins.optional.extraction.multi.Multi;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision: 10523 $", defaultEnabled = false, id = "extraction", interfaceversion = 7)
public class Extraction extends PluginOptional implements ControlListener, ExtractionListener, ActionListener {
    private static final String    DUMMY_HOSTER            = "dum.my";

    private static final int       EXTRACT_LINK            = 1000;

    private static final int       EXTRACT_PACKAGE         = 1001;

    private static final int       OPEN_EXTRACT            = 1002;

    private static final int       SET_EXTRACT_TO          = 1003;

    private static final int       SET_LINK_AUTOEXTRACT    = 1005;

    private static final int       SET_PACKAGE_AUTOEXTRACT = 1006;

    private static MenuAction      menuAction              = null;

    private Jobber                 queue;

    private Timer                  update                  = null;

    private ArrayList<IExtraction> extractors              = new ArrayList<IExtraction>();

    private ArrayList<Archive>     archives                = new ArrayList<Archive>();

    public Extraction(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Adds all internal extraction plugins.
     */
    private void initExtractors() {
        setExtractor(new HJSplt());
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
     * das controllevent fängt heruntergeladene file ab und wertet sie aus
     */
    @Override
    public void onControlEvent(ControlEvent event) {
        DownloadLink link;
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getCaller() instanceof PluginForHost)) return;
            link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (link.getFilePackage().isPostProcessing() && this.getPluginConfig().getBooleanProperty("ACTIVATED", true) && isLinkSupported(link.getFileOutput())) {
                Archive archive = buildArchive(link);

                if (archive.isComplete() && !archive.isActive()) {
                    this.addToQueue(archive);
                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            if (this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_DEEP_EXTRACT, true)) {
                try {
                    File[] list = (File[]) event.getParameter();
                    for (File archiveStartFile : list) {
                        if (isLinkSupported(archiveStartFile.getAbsolutePath())) {
                            Archive ar = buildDummyArchive(archiveStartFile);
                            if (ar.isActive()) continue;
                            addToQueue(buildDummyArchive(archiveStartFile));
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

                m.setProperty("LINK", link);
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.autoextract", SET_LINK_AUTOEXTRACT));
                m.setActionListener(this);
                m.setSelected(link.getFilePackage().isPostProcessing());
                if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                    m.setEnabled(false);
                }
                m.setProperty("LINK", link);
                container.addMenuItem(new MenuAction(Types.SEPARATOR));
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.setextract", SET_EXTRACT_TO));
                m.setActionListener(this);
                m.setProperty("LINK", link);
                File dir = this.getExtractToPath(link);
                while (dir != null && !dir.exists()) {
                    if (dir.getParentFile() == null) break;
                    dir = dir.getParentFile();
                }
                if (dir == null) break;
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.openextract3", OPEN_EXTRACT));
                m.setActionListener(this);
                link.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2", dir.getAbsolutePath());
                m.setProperty("LINK", link);
            } else {
                FilePackage fp = (FilePackage) event.getCaller();

                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.package.extract", EXTRACT_PACKAGE));
                m.setIcon(getIconKey());
                m.setActionListener(this);
                m.setProperty("PACKAGE", fp);
                container.addMenuItem(m = new MenuAction("optional.extraction.linkmenu.package.autoextract", SET_PACKAGE_AUTOEXTRACT));
                m.setSelected(fp.isPostProcessing());
                m.setActionListener(this);
                if (!this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                    m.setEnabled(false);
                }
                m.setProperty("PACKAGE", fp);
            }
            break;
        }
    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private void addToQueue(final Archive archive) {
        IExtraction extractor = getExtractor(archive.getFirstDownloadLink());

        if (!new File(archive.getFirstDownloadLink().getFileOutput()).exists()) return;
        archive.getFirstDownloadLink().getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
        archive.getFirstDownloadLink().getLinkStatus().setErrorMessage(null);
        File dl = this.getExtractToPath(archive.getFirstDownloadLink());

        archive.setOverwriteFiles(this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_OVERWRITE, true));
        archive.setExtractTo(dl);

        ExtractionController controller = new ExtractionController(archive, extractor);

        if (archive.getFirstDownloadLink().getHost().equals(DUMMY_HOSTER)) {
            ProgressController progress = new ProgressController(JDL.LF("plugins.optional.extraction.progress.extractfile", "Extract %s", archive.getFirstDownloadLink().getFileOutput()), 100, getIconKey());
            controller.setProgressController(progress);
        }

        controller.addExtractionListener(this);

        controller.setRemoveAfterExtract(this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, false));

        ArrayList<String> pwList = new ArrayList<String>();
        pwList.add(archive.getFirstDownloadLink().getFilePackage().getPassword());
        pwList.addAll(archive.getFirstDownloadLink().getFilePackage().getPasswordAuto());
        String dlpw = archive.getFirstDownloadLink().getStringProperty("pass", null);
        if (dlpw != null) pwList.add(dlpw);
        pwList.addAll(PasswordListController.getInstance().getPasswordList());
        pwList.add(extractor.getArchiveName(archive.getFirstDownloadLink()));
        pwList.add(new File(archive.getFirstDownloadLink().getFileOutput()).getName());
        controller.setPasswordList(pwList);

        archive.setActive(true);

        extractor.setConfig(getPluginConfig());

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
        IExtraction extractor = getExtractor(link);
        if (link.getProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) return (File) link.getProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH);
        if (link.getHost().equals(DUMMY_HOSTER)) return new File(link.getFileOutput()).getParentFile();
        String path;

        if (!getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_USE_EXTRACT_PATH, false)) {
            path = new File(link.getFileOutput()).getParent();
        } else {
            path = this.getPluginConfig().getStringProperty(ExtractionConstants.CONFIG_KEY_UNPACKPATH, JDUtilities.getDefaultDownloadDirectory());
        }

        File ret = new File(path);
        if (!this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_USE_SUBPATH, false)) return ret;

        path = this.getPluginConfig().getStringProperty(ExtractionConstants.CONFIG_KEY_SUBPATH, "%PACKAGENAME%");

        try {
            if (link.getFilePackage().getName() != null) {
                path = path.replace("%PACKAGENAME%", link.getFilePackage().getName());
            } else {
                path = path.replace("%PACKAGENAME%", "");
                logger.severe("link.getFilePackage().getName() == null");
            }
            if (extractor.getArchiveName(link) != null) {
                path = path.replace("%ARCHIVENAME%", extractor.getArchiveName(link));
            } else {
                logger.severe("getArchiveName(link) == null");
            }
            if (link.getHost() != null) {
                path = path.replace("%HOSTER%", link.getHost());
            } else {
                logger.severe("link.getFilePackage().getName() == null");
            }

            String dif = new File(JDUtilities.getDefaultDownloadDirectory()).getAbsolutePath().replace(new File(link.getFileOutput()).getParent(), "");
            if (new File(dif).isAbsolute()) {
                dif = "";
            }
            path = path.replace("%SUBFOLDER%", dif);

            path = path.replaceAll("[/]+", "\\\\");
            path = path.replaceAll("[\\\\]+", "\\\\");

            return new File(ret, path);
        } catch (Exception e) {
            JDLogger.exception(e);
            return ret;
        }
    }

    @Override
    public String getIconKey() {
        return "gui.images.addons.unrar";
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        MenuAction m;
        menu.add(m = new MenuAction("optional.extraction.menu.toggle", 1));
        m.setActionListener(this);
        m.setSelected(this.getPluginConfig().getBooleanProperty("ACTIVATED", true));
        menu.add(new MenuAction(Types.SEPARATOR));
        menu.add(menuAction);

        return menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuAction) {
            actionPerformedOnMenuItem((MenuAction) e.getSource());
        }
    }

    private Archive buildArchive(DownloadLink link) {
        for (Archive archive : archives) {
            if (archive.getDownloadLinks().contains(link)) { return archive; }
        }

        Archive archive = getExtractor(link).buildArchive(link);

        archives.add(archive);

        return archive;
    }

    private Archive buildDummyArchive(File file) {
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
        if (link == null) {
            /* link no longer in list */
            link = new DownloadLink(null, file.getName(), DUMMY_HOSTER, "", true);
            link.setDownloadSize(file.length());
            FilePackage fp = FilePackage.getInstance();
            fp.setDownloadDirectory(file.getParent());
            link.setFilePackage(fp);
        }

        return buildArchive(link);
    }

    private void actionPerformedOnMenuItem(MenuAction source) {
        SubConfiguration cfg = this.getPluginConfig();
        DownloadLink link;
        switch (source.getActionID()) {
        case 1:
            cfg.setProperty("ACTIVATED", !cfg.getBooleanProperty("ACTIVATED", true));
            cfg.save();
            break;
        case EXTRACT_LINK:
            link = (DownloadLink) source.getProperty("LINK");
            final Archive archive = buildArchive(link);
            new Thread() {
                @Override
                public void run() {
                    addToQueue(archive);
                }
            }.start();
            break;
        case EXTRACT_PACKAGE:
            FilePackage fp = (FilePackage) source.getProperty("PACKAGE");
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            for (DownloadLink l : fp.getDownloadLinkList()) {
                if (l.getLinkStatus().isFinished()) {
                    links.add(l);
                }
            }
            if (links.size() <= 0) return;
            for (DownloadLink link0 : links) {
                final Archive archive0 = buildArchive(link0);
                new Thread() {
                    @Override
                    public void run() {
                        addToQueue(archive0);
                    }
                }.start();
            }
            break;
        case OPEN_EXTRACT:
            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) { return; }
            String path = link.getStringProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2");

            if (!new File(path).exists()) {
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.extraction.messages", "The path %s does not exist.", path));
            } else {
                JDUtilities.openExplorer(new File(path));
            }

            break;
        case SET_EXTRACT_TO:
            link = (DownloadLink) source.getProperty("LINK");
            Archive archive0 = buildArchive(link);

            FileFilter ff = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                @Override
                public String getDescription() {
                    return JDL.L("plugins.optional.extraction.filefilter.extractto", "Extract Directory");
                }
            };

            File extractto = this.getExtractToPath(link);
            while (extractto != null && !extractto.isDirectory()) {
                extractto = extractto.getParentFile();
            }

            File[] files = UserIO.getInstance().requestFileChooser("_EXTRACTION_", null, UserIO.DIRECTORIES_ONLY, ff, null, extractto, null);
            if (files == null) return;

            for (DownloadLink l : archive0.getDownloadLinks()) {
                l.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, files[0]);
            }
            break;
        case SET_LINK_AUTOEXTRACT:
            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) { return; }
            link.getFilePackage().setPostProcessing(!link.getFilePackage().isPostProcessing());
            break;
        case SET_PACKAGE_AUTOEXTRACT:
            fp = (FilePackage) source.getProperty("PACKAGE");
            if (fp == null) { return; }
            fp.setPostProcessing(!fp.isPostProcessing());
            break;
        }
    }

    @Override
    public boolean initAddon() {
        ArrayList<OptionalPluginWrapper> pluginsOptional = new ArrayList<OptionalPluginWrapper>(OptionalPluginWrapper.getOptionalWrapper());
        Collections.sort(pluginsOptional);

        for (OptionalPluginWrapper pow : pluginsOptional) {
            if (pow.getAnnotation().id().equals("unrar") || pow.getAnnotation().id().equals("hjsplit")) {
                if (pow.isEnabled()) {
                    logger.warning("Disable unrar and hjsplit to use this plugin");
                    UserIO.getInstance().requestMessageDialog(0, "Disable unrar and hjsplit to use this plugin");
                    return false;
                }
            }
        }

        this.queue = new Jobber(1);

        initExtractors();
        initConfig();

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
                        return JDL.L("plugins.optional.extraction.filefilter", "All supported formats");
                    }
                };

                File[] files = UserIO.getInstance().requestFileChooser("_EXTRATION_", null, UserIO.FILES_ONLY, ff, true, null, null);
                if (files == null) return;

                for (File archiveStartFile : files) {
                    final Archive archive = buildDummyArchive(archiveStartFile);
                    new Thread() {
                        @Override
                        public void run() {
                            addToQueue(archive);
                        }
                    }.start();
                }
            }
        };
        return true;
    }

    private void initConfig() {
        ConfigEntry ce, conditionEntry;
        final SubConfiguration subConfig = getPluginConfig();

        config.setGroup(new ConfigGroup(getHost(), getIconKey()));

        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_USE_EXTRACT_PATH, JDL.L("gui.config.extraction.use_extractto", "Use customized extract path")).setDefaultValue(false));
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, ExtractionConstants.CONFIG_KEY_UNPACKPATH, JDL.L("gui.config.extraction.path", "Extract to")));
        ce.setDefaultValue(JDUtilities.getDefaultDownloadDirectory());
        ce.setEnabledCondidtion(conditionEntry, true);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, JDL.L("gui.config.extraction.remove_after_extract", "Delete archives after suc. extraction?")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_OVERWRITE, JDL.L("gui.config.extraction.overwrite", "Overwrite existing files?")).setDefaultValue(false));

        config.setGroup(new ConfigGroup(JDL.L("plugins.optional.extraction.config.advanced", "Advanced settings"), getIconKey()));
        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_USE_SUBPATH, JDL.L("gui.config.extraction.use_subpath", "Use subpath")).setDefaultValue(false));
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, ExtractionConstants.CONFIG_KEY_SUBPATH, JDL.L("gui.config.extraction.subpath", "Subpath")));
        ce.setDefaultValue("%PACKAGENAME%");
        ce.setEnabledCondidtion(conditionEntry, true);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, ExtractionConstants.CONFIG_KEY_SUBPATH_MINNUM, JDL.L("gui.config.extraction.subpath_minnum", "Only use subpath if archive contains more than x files"), 1, 600, 1).setDefaultValue(5));
        ce.setEnabledCondidtion(conditionEntry, true);
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, JDL.L("gui.config.extraction.ask_path", "Ask for unknown passwords?")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_DEEP_EXTRACT, JDL.L("gui.config.extraction.deep_extract", "Deep-Extraction")).setDefaultValue(true));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, ExtractionConstants.CONFIG_KEY_REMOVE_INFO_FILE, JDL.L("gui.config.extraction.remove_infofile", "Delete Infofile after extraction")).setDefaultValue(false));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, ExtractionConstants.CONFIG_KEY_ADDITIONAL_SPACE, JDL.L("gui.config.extraction.additional_space", "Leave x MiB additional space after unpacking"), 1, 2048, 1).setDefaultValue(512));

        for (IExtraction extractor : extractors) {
            extractor.initConfig(config, subConfig);
        }
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    public void onExtractionEvent(int id, ExtractionController controller) {
        LinkStatus ls = controller.getArchiv().getFirstDownloadLink().getLinkStatus();
        // Falls der link entfernt wird während dem entpacken
        if (controller.getArchiv().getFirstDownloadLink().getFilePackage() == FilePackage.getDefaultFilePackage() && controller.getProgressController() == null) {
            logger.warning("LINK GOT REMOVED_: " + controller.getArchiv().getFirstDownloadLink());
            ProgressController progress = new ProgressController(JDL.LF("plugins.optional.extraction.progress.extractfile", "Extract %s", controller.getArchiv().getFirstDownloadLink().getFileOutput()), 100, getIconKey());
            controller.setProgressController(progress);
        }

        if (controller.getProgressController() != null) {
            onExtractionDummyEvent(id, controller);
            return;
        }

        switch (id) {
        case ExtractionConstants.WRAPPER_STARTED:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.queued", "Queued for extracting"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.INVALID_BINARY:
            logger.severe("Invalid extraction binary!");
            controller.getArchiv().setActive(false);
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;
                LinkStatus lls = link.getLinkStatus();

                if (controller.getException() != null) {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed: " + controller.getException().getMessage());
                    link.requestGuiUpdate();
                } else {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed");
                    link.requestGuiUpdate();
                }
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();

            if (this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = UserIO.getInstance().requestInputDialog(0, JDL.LF("plugins.optional.extraction.askForPassword", "Password for %s?", controller.getArchiv().getFirstDownloadLink().getName()), null);
                if (pass == null || pass.length() == 0) {
                    ls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    ls.setStatusText(JDL.L("plugins.optional.extraction.status.extractfailedpass", "Extract failed (password)"));
                    this.onFinished(controller);
                    break;
                }
                controller.getArchiv().setPassword(pass);
            }
            break;
        case ExtractionConstants.WRAPPER_CRACK_PASSWORD:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.crackingpass", "Cracking password"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.openingarchive", "Opening archive"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_FOUND:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.passfound", "Password found"));
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            controller.getArchiv().getFirstDownloadLink().setPluginProgress(null);
            break;
        case ExtractionConstants.WRAPPER_PASSWORT_CRACKING:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.crackingpass", "Cracking password"));
            if (controller.getArchiv().getFirstDownloadLink().getPluginProgress() == null) {
                controller.getArchiv().getFirstDownloadLink().setPluginProgress(new PluginProgress(controller.getCrackProgress(), controller.getPasswordList().size(), Color.GREEN.darker()));
            } else {
                controller.getArchiv().getFirstDownloadLink().getPluginProgress().setCurrent(controller.getCrackProgress());
            }
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_ON_PROGRESS:
            controller.getArchiv().getFirstDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.extracting", "Extracting"));
            if (controller.getArchiv().getFirstDownloadLink().getPluginProgress() == null) {
                controller.getArchiv().getFirstDownloadLink().setPluginProgress(new PluginProgress(controller.getArchiv().getExtracted(), controller.getArchiv().getSize(), Color.YELLOW.darker()));
            } else {
                controller.getArchiv().getFirstDownloadLink().getPluginProgress().setCurrent(controller.getArchiv().getExtracted());
            }
            controller.getArchiv().getFirstDownloadLink().requestGuiUpdate();
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            if (controller.getArchiv().getCrcError().size() != 0) {
                for (DownloadLink link : controller.getArchiv().getCrcError()) {
                    if (link == null) continue;
                    link.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                    link.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                    link.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                    link.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                    link.getLinkStatus().setErrorMessage(JDL.LF("plugins.optional.extraction.crcerrorin", "Extract: failed (CRC in %s)", link.getName()));
                    link.requestGuiUpdate();
                }
            } else {
                for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.error.extrfailedcrc", "Extract: failed (CRC in unknown file)"));
                    link.requestGuiUpdate();
                }
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_FINISHED_SUCCESSFULL:
            File[] files = new File[controller.getArchiv().getExtractedFiles().size()];
            int i = 0;
            for (File f : controller.getArchiv().getExtractedFiles()) {
                files[i++] = f;
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;
                link.getLinkStatus().addStatus(LinkStatus.FINISHED);
                link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.extractok", "Extract OK"));
                link.requestGuiUpdate();
            }

            // TODO
            // if
            // (this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_REMOVE_INFO_FILE,
            // false)) {
            // File fileOutput = new
            // File(controller.getArchiv().getFirstDownloadLink().getFileOutput());
            // String packname =
            // controller.getArchiv().getFirstDownloadLink().getFilePackage().getName();
            // File infoFiles = new File(fileOutput.getParentFile(),
            // packname.replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$",
            // "") + ".info");
            // if (infoFiles.exists() && infoFiles.delete()) {
            // logger.info(infoFiles.getName() + " removed");
            // }
            // }
            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        case ExtractionConstants.NOT_ENOUGH_SPACE:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;

                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.notenoughspace", "Not enough space to extract"));
                link.requestGuiUpdate();
            }

            this.onFinished(controller);
            break;
        case ExtractionConstants.REMOVE_ARCHIVE_METADATA:
            archives.remove(controller.getArchiv());
            break;
        case ExtractionConstants.WRAPPER_FILE_NOT_FOUND:
            if (controller.getArchiv().getCrcError().size() != 0) {
                for (DownloadLink link : controller.getArchiv().getCrcError()) {
                    if (link == null) continue;
                    link.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                    link.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                    link.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.filenotfound", "Extract: failed (File not found)"));
                    link.requestGuiUpdate();
                }
            } else {
                for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.extraction.filenotfound", "Extract: failed (File not found)"));
                    link.requestGuiUpdate();
                }
            }

            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        }
    }

    /**
     * Sets the extractionpath with subpahts.
     * 
     * @param controller
     */
    private void assignRealDownloadDir(ExtractionController controller) {
        Boolean usesub = this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_USE_SUBPATH, false);
        if (usesub) {
            int min = this.getPluginConfig().getIntegerProperty(ExtractionConstants.CONFIG_KEY_SUBPATH_MINNUM, 0);
            if (min > controller.getArchiv().getNumberOfFiles()) { return; }

            File dl = this.getExtractToPath(controller.getArchiv().getFirstDownloadLink());
            controller.getArchiv().setExtractTo(dl);

            ArrayList<DownloadLink> linkList = controller.getArchiv().getDownloadLinks();
            for (DownloadLink l : linkList) {
                if (l == null) continue;
                l.setProperty(ExtractionConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
            }
        }
    }

    /**
     * Als Dummy wird ein downloadlink bezeicnet, der nicht ind er downloadliste
     * war, sondern nur angelegt wurde um als container für ein externes archiv
     * zu dienen. Zur Fortschrittsanzeige wird ein progresscontroller verwendet
     * 
     * @param id
     * @param controller
     */
    private void onExtractionDummyEvent(int id, ExtractionController controller) {
        ProgressController pc = controller.getProgressController();
        // int min;
        switch (id) {
        case ExtractionConstants.WRAPPER_STARTED:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.queued", "Queued for extracting"));
            break;
        case ExtractionConstants.INVALID_BINARY:
            logger.severe("Invalid extraction binary!");
            controller.getArchiv().setActive(false);
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED:
            if (controller.getException() != null) {
                pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extractfailed", "Extract failed") + ": " + controller.getException().getMessage());
            } else {
                pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extractfailed", "Extract failed"));
            }

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            this.onFinished(controller);

            break;
        case ExtractionConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extractfailedpass", "Extract failed (password)"));

            if (this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = UserIO.getInstance().requestInputDialog(0, JDL.LF("plugins.optional.extraction.askForPassword", "Password for %s?", controller.getArchiv().getFirstDownloadLink().getName()), null);
                if (pass == null || pass.length() == 0) {
                    this.onFinished(controller);
                    break;
                }
                controller.getArchiv().setPassword(pass);
            }

            break;
        case ExtractionConstants.WRAPPER_PASSWORT_CRACKING:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.crackingpass", "Cracking password"));
            pc.setRange(controller.getPasswordList().size());
            pc.setStatus(controller.getCrackProgress());
            break;
        case ExtractionConstants.WRAPPER_START_OPEN_ARCHIVE:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.openingarchive", "Opening archive"));
            break;
        case ExtractionConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(controller);
            break;
        case ExtractionConstants.WRAPPER_PASSWORD_FOUND:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.passfound", "Password found"));
            break;
        case ExtractionConstants.WRAPPER_ON_PROGRESS:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extracting", "Extracting"));
            pc.setRange(controller.getArchiv().getSize());
            pc.setStatus(controller.getArchiv().getExtracted());
            break;
        case ExtractionConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extractfailedcrc", "Extract failed (CRC error)"));

            for (File f : controller.getArchiv().getExtractedFiles()) {
                if (f.exists()) {
                    if (!f.delete()) {
                        logger.warning("Could not delete file " + f.getAbsolutePath());
                    }
                }
            }

            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        case ExtractionConstants.WRAPPER_FINISHED_SUCCESSFULL:
            File[] files = new File[controller.getArchiv().getExtractedFiles().size()];
            int i = 0;
            for (File f : controller.getArchiv().getExtractedFiles()) {
                files[i++] = f;
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            pc.setStatusText(controller.getArchiv().getFirstDownloadLink().getFileOutput() + ": " + JDL.L("plugins.optional.extraction.status.extractok", "Extract OK"));

            if (this.getPluginConfig().getBooleanProperty(ExtractionConstants.CONFIG_KEY_REMOVE_INFO_FILE, false)) {
                File fileOutput = new File(controller.getArchiv().getFirstDownloadLink().getFileOutput());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
        case ExtractionConstants.NOT_ENOUGH_SPACE:
            for (DownloadLink link : controller.getArchiv().getDownloadLinks()) {
                if (link == null) continue;

                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.extraction.status.notenoughspace", "Not enough space to extract"));
                link.requestGuiUpdate();
            }

            this.onFinished(controller);
            break;
        case ExtractionConstants.REMOVE_ARCHIVE_METADATA:
            archives.remove(controller.getArchiv());
            break;
        case ExtractionConstants.WRAPPER_FILE_NOT_FOUND:
            pc.setStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
            pc.setStatusText(JDL.L("plugins.optional.extraction.filenotfound", "Extract: failed (File not found)"));
            controller.getArchiv().setActive(false);
            this.onFinished(controller);
            break;
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

    /**
     * Finishes the extraction process.
     * 
     * @param controller
     */
    private void onFinished(ExtractionController controller) {
        controller.getArchiv().getFirstDownloadLink().setPluginProgress(null);
        if (controller.getProgressController() != null) {
            controller.getProgressController().doFinalize(8000);
        }
    }

    @Override
    public Object interact(String command, Object parameter) {
        if (command.equals("isWorking")) {
            return queue.isAlive();
        } else {
            return null;
        }
    }
}