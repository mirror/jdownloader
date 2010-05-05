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

package jd.plugins.optional.jdunrar;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigPropertyListener;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.PasswordListController;
import jd.controlling.ProgressController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Executer;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.OSDetector;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.PluginProgress;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "unrar", interfaceversion = 5)
public class JDUnrar extends PluginOptional implements UnrarListener, ActionListener {

    private static final String DUMMY_HOSTER = "dum.my";

    private static MenuAction menuAction = null;

    /**
     * Wird als reihe für anstehende extractjobs verwendet
     */
    private Jobber queue;

    public JDUnrar(PluginWrapper wrapper) {
        super(wrapper);

        this.queue = new Jobber(1);

        checkUnrarCommand();
        initConfig();
    }

    /**
     * das controllevent fängt heruntergeladene file ab und wertet sie aus
     */
    @SuppressWarnings("unchecked")
    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        DownloadLink link;
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) return;
            link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            /* react if JDUnrar is activated or package has flag for autoextract */
            if (this.getPluginConfig().getBooleanProperty("ACTIVATED", true) || link.getFilePackage().isExtractAfterDownload()) {
                link = findStartLink(link);
                if (link == null) return;
                if (link.getLinkStatus().isFinished()) {
                    if (link.getFilePackage().isExtractAfterDownload()) {
                        if (isArchiveComplete(link)) {
                            this.addToQueue(link);
                        }
                    }
                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_DEEP_EXTRACT, true)) {
                try {
                    File[] list = (File[]) event.getParameter();
                    ArrayList<String> processed = new ArrayList<String>();
                    for (File archiveStartFile : list) {
                        /*
                         * skip file if it belongs to an archive which is
                         * already queued
                         */
                        if (processed.contains(archiveStartFile.toString())) continue;
                        if (getArchivePartType(archiveStartFile) == JDUnrarConstants.NO_RAR_ARCHIVE || getArchivePartType(archiveStartFile) == JDUnrarConstants.NO_START_PART) {
                            processed.add(archiveStartFile.toString());
                            continue;
                        }

                        link = findStartLink(archiveStartFile);
                        if (link == null || processed.contains(link.getFileOutput0())) continue;
                        /* find/add all files that belongs to the queued archive */
                        processed.add(link.getFileOutput0());
                        processed.addAll(getFileList(link.getFileOutput0()));
                        addToQueue(link);
                    }
                } catch (Exception e) {
                    JDLogger.exception(e);
                }
            }
            break;

        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            MenuAction m;
            MenuAction container = new MenuAction("optional.jdunrar.linkmenu.container", 0);
            items.add(container);
            if (event.getSource() instanceof DownloadLink) {

                link = (DownloadLink) event.getSource();

                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.extract", 1000));
                m.setActionListener(this);
                m.setEnabled(false);
                boolean isLocalyAvailable = (new File(link.getFileOutput()).exists() || new File(link.getStringProperty(DownloadLink.STATIC_OUTPUTFILE, link.getFileOutput())).exists());
                if (isLocalyAvailable && link.getName().matches(".*rar$")) m.setEnabled(true);
                m.setProperty("LINK", link);
                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.autoextract", 1005));
                m.setActionListener(this);
                m.setSelected(link.getFilePackage().isExtractAfterDownload());
                m.setProperty("LINK", link);
                container.addMenuItem(m = new MenuAction(Types.SEPARATOR));
                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.setextract", 1003));
                m.setActionListener(this);

                m.setProperty("LINK", link);
                File dir = this.getExtractToPath(link);
                while (dir != null && !dir.exists()) {
                    if (dir.getParentFile() == null) break;
                    dir = dir.getParentFile();
                }
                if (dir == null) break;
                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.openextract3", 1002));
                m.setActionListener(this);
                link.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2", dir.getAbsolutePath());
                m.setProperty("LINK", link);

            } else {
                FilePackage fp = (FilePackage) event.getSource();
                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.package.extract", 1001));
                m.setActionListener(this);
                m.setProperty("PACKAGE", fp);
                container.addMenuItem(m = new MenuAction("optional.jdunrar.linkmenu.package.autoextract", 1006));
                m.setSelected(fp.isExtractAfterDownload());
                m.setActionListener(this);
                m.setProperty("PACKAGE", fp);
            }
            break;
        }
    }

    /**
     * Prüft im zugehörigem Filepackage, ob noch downloadlinks vom archiv
     * ungeladen sind.
     * 
     * @param link
     * @return
     */
    private boolean isArchiveComplete(DownloadLink link) {
        String pattern = link.getFileOutput().replaceAll("\\.pa?r?t?\\.?[0-9]+.*?.rar$", "");
        pattern = pattern.replaceAll("\\.rar$", "");
        pattern = pattern.replaceAll("\\.r\\d+$", "");
        pattern = "^" + Regex.escape(pattern) + ".*";
        ArrayList<DownloadLink> matches = JDUtilities.getController().getDownloadLinksByPathPattern(pattern);
        if (matches == null) return false;
        for (DownloadLink l : matches) {
            if (!new File(l.getFileOutput()).exists()) return false;
            if (!l.getLinkStatus().hasStatus(LinkStatus.FINISHED) && l.isEnabled()) return false;
        }
        return true;
    }

    /**
     * prüft um welchen archivtyp es sich handelt. Es wird
     * JDUnrarConstants.MULTIPART_START_PART
     * JDUnrarConstants.SINGLE_PART_ARCHIVE JDUnrarConstants.NO_RAR_ARCHIVE
     * JDUnrarConstants.NO_START_PART
     * 
     * @param link
     * @return
     */
    public static int getArchivePartType(DownloadLink link) {
        return getArchivePartType(new File(link.getFileOutput()));
    }

    public static int getArchivePartType(File file) {
        if (file.getName().matches(".*pa?r?t?\\.?\\d+.rar$")) return JDUnrarConstants.MULTIPART_START_PART;
        if (file.getName().matches(".*.rar$")) {
            String filename = new Regex(file, "(.*)\\.rar$").getMatch(0);
            if ((new File(filename + ".r0")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r00")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r000")).exists()) {
                return JDUnrarConstants.MULTIPART_START_PART_V2;
            } else if ((new File(filename + ".r0000")).exists()) return JDUnrarConstants.MULTIPART_START_PART_V2;
        }
        if (file.getName().matches(".*rar$")) return JDUnrarConstants.SINGLE_PART_ARCHIVE;
        if (!file.getName().matches(".*rar$")) return JDUnrarConstants.NO_RAR_ARCHIVE;
        return JDUnrarConstants.NO_START_PART;
    }

    public static DownloadLink findStartLink(File file) {
        DownloadLink link = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
        if (link == null) {
            /* link no longer in list */
            link = new DownloadLink(null, file.getName(), DUMMY_HOSTER, "", true);
            link.setDownloadSize(file.length());
            FilePackage fp = FilePackage.getInstance();
            fp.setDownloadDirectory(file.getParent());
            link.setFilePackage(fp);
        }
        return findStartLink(link);
    }

    public static DownloadLink findStartLink(DownloadLink link) {
        int type = getArchivePartType(link);
        switch (type) {
        case JDUnrarConstants.MULTIPART_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART_V2:
            break;
        case JDUnrarConstants.SINGLE_PART_ARCHIVE:
            return link;
        case JDUnrarConstants.NO_RAR_ARCHIVE:
            return null;
        }
        File file = null;
        String filename = null;
        if (type == JDUnrarConstants.MULTIPART_START_PART) {
            filename = new Regex(link.getFileOutput(), "(.*)\\.pa?r?t?\\.?[0-9]+.rar$").getMatch(0);
            String partid = new Regex(link.getFileOutput(), "(.*)\\.(pa?r?t?\\.?)[0-9]+.rar$").getMatch(1);
            if ((file = new File(filename + "." + partid + "1.rar")).exists()) {
            } else if ((file = new File(filename + "." + partid + "01.rar")).exists()) {
            } else if ((file = new File(filename + "." + partid + "001.rar")).exists()) {
            } else if ((file = new File(filename + "." + partid + "0001.rar")).exists()) {
            } else if ((file = new File(filename + "." + partid + "000.rar")).exists()) {
            } else {
                return null;
            }
        } else if (type == JDUnrarConstants.MULTIPART_START_PART_V2) {
            filename = new Regex(link.getFileOutput(), "(.*)\\.r(\\d+|ar)$").getMatch(0);
            if (!(file = new File(filename + ".rar")).exists()) { return null; }
        }

        DownloadLink dlink = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
        if (dlink == null) {
            /* in case link is already removed */
            /* we create new package with settings of link that calles jdunrar */
            dlink = new DownloadLink(null, file.getName(), DUMMY_HOSTER, "", true);
            dlink.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, link.getProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, null));
            dlink.getLinkStatus().setStatus(LinkStatus.FINISHED);
            FilePackage fp = FilePackage.getInstance();
            fp.setDownloadDirectory(link.getFilePackage().getDownloadDirectory());
            fp.setExtractAfterDownload(link.getFilePackage().isExtractAfterDownload());
            fp.setPassword(link.getFilePackage().getPassword());
            dlink.setFilePackage(fp);
        }
        return dlink;
    }

    private String getArchiveName(DownloadLink link) {
        return getArchiveName(link.getFileOutput0());
    }

    private String getArchiveName(String link) {
        String match = new Regex(new File(link).getName(), "(.*)\\.pa?r?t?\\.?[0-9]+.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link).getName(), "(.*)\\.rar$").getMatch(0);
        if (match != null) return match;
        match = new Regex(new File(link).getName(), "(.*)\\.r\\d+$").getMatch(0);
        return match;
    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private void addToQueue(final DownloadLink link) {
        if (getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null) == null) {
            logger.warning("JDUnrar: no valid binary found!");
            return;
        }
        if (!new File(link.getFileOutput()).exists()) return;
        link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
        link.getLinkStatus().setErrorMessage(null);
        File dl = this.getExtractToPath(link);

        UnrarWrapper wrapper = new UnrarWrapper(link);

        if (link.getHost().equals(DUMMY_HOSTER)) {
            ProgressController progress = new ProgressController(JDL.LF("plugins.optional.jdunrar.progress.extractfile", "Extract %s", link.getFileOutput()), 100, "gui.images.addons.unrar");
            wrapper.setProgressController(progress);
        }

        wrapper.addUnrarListener(this);
        wrapper.setExtractTo(dl);

        wrapper.setRemoveAfterExtract(this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, false));

        wrapper.setOverwrite(this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_OVERWRITE, true));
        wrapper.setUnrarCommand(getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND));
        ArrayList<String> pwList = new ArrayList<String>();
        pwList.add(link.getFilePackage().getPassword());
        pwList.addAll(link.getFilePackage().getPasswordAuto());
        String dlpw = link.getStringProperty("pass", null);
        if (dlpw != null) pwList.add(dlpw);
        pwList.addAll(PasswordListController.getInstance().getPasswordList());
        // Adds the archive name at the end of the password list
        // (example.part01.rar)
        String archiveName = this.getArchiveName(link);
        pwList.add(archiveName); // example
        pwList.add(archiveName + ".rar"); // example.rar
        pwList.add(new File(link.getFileOutput()).getName()); // example.part01.rar
        wrapper.setPasswordList(pwList);

        queue.add(wrapper);
        queue.start();
        ArrayList<DownloadLink> list = this.getArchiveList(link);
        for (DownloadLink l : list) {
            if (l == null) continue;
            l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
        }
    }

    /**
     * Bestimmt den Pfad in den das Archiv entpackt werden soll
     * 
     * @param link
     * @return
     */
    private File getExtractToPath(DownloadLink link) {

        if (link.getProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH) != null) return (File) link.getProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH);
        if (link.getHost().equals(DUMMY_HOSTER)) return new File(link.getFileOutput()).getParentFile();
        String path;

        if (!getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_EXTRACT_PATH, false)) {
            path = new File(link.getFileOutput()).getParent();
        } else {
            path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARPATH, JDUtilities.getDefaultDownloadDirectory());
        }

        File ret = new File(path);
        if (!this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false)) return ret;

        path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_SUBPATH, "%PACKAGENAME%");

        try {
            if (link.getFilePackage().getName() != null) {
                path = path.replace("%PACKAGENAME%", link.getFilePackage().getName());
            } else {
                path = path.replace("%PACKAGENAME%", "");
                logger.severe("link.getFilePackage().getName() ==null");
            }
            if (getArchiveName(link) != null) {
                path = path.replace("%ARCHIVENAME%", getArchiveName(link));
            } else {
                logger.severe("getArchiveName(link) ==null");
            }
            if (link.getHost() != null) {
                path = path.replace("%HOSTER%", link.getHost());
            } else {
                logger.severe("link.getFilePackage().getName() ==null");
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
        menu.add(m = new MenuAction("optional.jdunrar.menu.toggle", 1));
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

    private void actionPerformedOnMenuItem(MenuAction source) {
        SubConfiguration cfg = this.getPluginConfig();
        DownloadLink link;
        switch (source.getActionID()) {
        case 1:
            cfg.setProperty("ACTIVATED", !cfg.getBooleanProperty("ACTIVATED", true));
            cfg.save();
            break;
        case 1000:
            link = findStartLink((DownloadLink) source.getProperty("LINK"));
            if (link == null) return;
            final DownloadLink finalLink = link;
            System.out.print("queued to extract: " + link);
            new Thread() {
                @Override
                public void run() {
                    addToQueue(finalLink);
                }
            }.start();
            break;
        case 1001:
            FilePackage fp = (FilePackage) source.getProperty("PACKAGE");
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            for (DownloadLink l : fp.getDownloadLinkList()) {
                if (l.getLinkStatus().isFinished()) {
                    links.add(l);
                }
            }
            if (links.size() <= 0) return;
            ArrayList<String> added = new ArrayList<String>();
            for (DownloadLink link0 : links) {
                link = findStartLink(link0);
                if (link == null) continue;
                final DownloadLink finalLink0 = link;
                if (added.contains(link.getFileOutput0())) {
                    continue;
                } else {
                    added.add(link.getFileOutput0());
                }
                System.out.print("queued to extract: " + link);
                new Thread() {
                    @Override
                    public void run() {
                        addToQueue(finalLink0);
                    }
                }.start();
            }
            break;
        case 1002:
            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) return;
            String path = link.getStringProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH + "2");
            if (!new File(path).exists()) {
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.jdunrar.messages", "The path %s does not exist.", path));
            } else {
                JDUtilities.openExplorer(new File(path));
            }
            break;
        case 1003:
            link = (DownloadLink) source.getProperty("LINK");
            ArrayList<DownloadLink> list = this.getArchiveList(link);
            JDFileChooser fc = new JDFileChooser("_JDUNRAR_");
            fc.setMultiSelectionEnabled(false);
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);
            FileFilter ff = new FileFilter() {

                @Override
                public boolean accept(File pathname) {
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                @Override
                public String getDescription() {
                    return JDL.L("plugins.optional.jdunrar.filefilter.extractto", "Extract Directory");
                }

            };
            fc.setFileFilter(ff);
            File extractto = this.getExtractToPath(link);
            while (extractto != null && !extractto.isDirectory())
                extractto = extractto.getParentFile();
            fc.setCurrentDirectory(extractto);
            if (fc.showOpenDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) {
                File dl = fc.getSelectedFile();
                if (dl == null) { return; }
                for (DownloadLink l : list) {
                    l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTTOPATH, dl);
                }
            }
            break;
        case 1005:
            link = (DownloadLink) source.getProperty("LINK");
            if (link == null) return;
            link.getFilePackage().setExtractAfterDownload(!link.getFilePackage().isExtractAfterDownload());
            break;
        case 1006:
            fp = (FilePackage) source.getProperty("PACKAGE");
            if (fp == null) return;
            fp.setExtractAfterDownload(!fp.isExtractAfterDownload());
            break;
        }

    }

    @Override
    public boolean initAddon() {
        if (menuAction == null) menuAction = new MenuAction("optional.jdunrar.menu.extract.singlefils", "gui.images.addons.unrar") {
            private static final long serialVersionUID = -7569522709162921624L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
            }

            @Override
            public void onAction(ActionEvent e) {
                JDFileChooser fc = new JDFileChooser("_JDUNRAR_");
                fc.setMultiSelectionEnabled(true);
                FileFilter ff = new FileFilter() {

                    @Override
                    public boolean accept(File pathname) {
                        if (pathname.getName().matches(".*pa?r?t?\\.?[0]*[1].rar$")) return true;
                        if (!pathname.getName().matches(".*pa?r?t?\\.?[0-9]+.rar$") && pathname.getName().matches(".*rar$")) return true;
                        if (pathname.isDirectory()) return true;
                        return false;
                    }

                    @Override
                    public String getDescription() {
                        return JDL.L("plugins.optional.jdunrar.filefilter", "Rar-Startvolumes");
                    }

                };
                fc.setFileFilter(ff);
                if (fc.showOpenDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) {
                    File[] list = fc.getSelectedFiles();
                    if (list == null) return;
                    DownloadLink link;
                    for (File archiveStartFile : list) {

                        link = findStartLink(archiveStartFile);
                        if (link == null) {
                            continue;
                        }
                        final DownloadLink finalLink = link;
                        System.out.print("queued to extract: " + archiveStartFile);
                        new Thread() {
                            @Override
                            public void run() {
                                addToQueue(finalLink);
                            }
                        }.start();
                    }
                }
            }
        };
        return true;
    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;
        ConfigEntry conditionEntry;
        String hash = this.getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null);
        if (hash == null) {
            config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, JDL.L("gui.config.unrar.cmd", "UnRAR command")));
            ce.setDefaultValue("please install unrar");
            JDController.getInstance().addControlListener(new ConfigPropertyListener(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND) {
                @Override
                public void onPropertyChanged(Property source, final String key) {
                    if (JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND.equalsIgnoreCase(key)) {
                        String path = getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
                        if (path != null && isUnrarCommandValid(path)) {
                            getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                            getPluginConfig().save();
                        } else {
                            getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
                            getPluginConfig().save();
                        }
                    }
                }
            });
        }

        config.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_USE_EXTRACT_PATH, JDL.L("gui.config.unrar.use_extractto", "Use customized extract path")));
        conditionEntry.setDefaultValue(false);

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, JDUnrarConstants.CONFIG_KEY_UNRARPATH, JDL.L("gui.config.unrar.path", "Extract to")));
        ce.setDefaultValue(JDUtilities.getDefaultDownloadDirectory());
        ce.setEnabledCondidtion(conditionEntry, true);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_REMVE_AFTER_EXTRACT, JDL.L("gui.config.unrar.remove_after_extract", "Delete archives after suc. extraction?")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_OVERWRITE, JDL.L("gui.config.unrar.overwrite", "Overwrite existing files?")));
        ce.setDefaultValue(false);

        ConfigContainer ext = new ConfigContainer(JDL.L("plugins.optional.jdunrar.config.advanced", "Premium settings"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER, ext));

        ext.addEntry(conditionEntry = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, JDL.L("gui.config.unrar.use_subpath", "Use subpath")));
        conditionEntry.setDefaultValue(false);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, JDUnrarConstants.CONFIG_KEY_SUBPATH, JDL.L("gui.config.unrar.subpath", "Subpath")));
        ce.setDefaultValue("%PACKAGENAME%");
        ce.setEnabledCondidtion(conditionEntry, true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, JDUnrarConstants.CONFIG_KEY_SUBPATH_MINNUM, JDL.L("gui.config.unrar.subpath_minnum", "Only use subpath if archive contains more than x files"), 0, 600).setDefaultValue(0));
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, JDL.L("gui.config.unrar.ask_path", "Ask for unknown passwords?")));
        ce.setDefaultValue(true);
        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_DEEP_EXTRACT, JDL.L("gui.config.unrar.deep_extract", "Deep-Extraction")));
        ce.setDefaultValue(true);

        ext.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, JDL.L("gui.config.unrar.remove_infofile", "Delete Infofile after extraction")));
        ce.setDefaultValue(false);
    }

    private void chmodUnrar(String path) {
        Executer exec = new Executer("chmod");
        exec.setLogger(JDLogger.getLogger());
        exec.addParameter("+x");
        exec.addParameter(path);
        exec.setWaitTimeout(-1);
        exec.start();
        exec.waitTimeout();
    }

    /**
     * Überprüft den eingestellten Unrarbefehl und setzt ihn notfalls neu.
     */
    private void checkUnrarCommand() {
        if (checkUnrarCommandIntern()) {
            logger.info("Found valid unrar binary at " + this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null));
        } else {
            logger.severe("No valid unrar binary found!");
        }
    }

    private boolean checkUnrarCommandIntern() {
        String path = this.getPluginConfig().getStringProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
        String hash = this.getPluginConfig().getStringProperty(JDUnrarConstants.UNRAR_HASH, null);
        if (hash != null && hash.length() == 32 && path != null && path.length() != 0) {
            String curhash = JDHash.getMD5(new File(path));
            if (curhash != null && curhash.equalsIgnoreCase(hash)) return true;
        } else {
            path = null;
            hash = null;
        }
        if (path == null || path.length() == 0) {
            if (OSDetector.isWindows()) {
                path = JDUtilities.getResourceFile("tools\\windows\\unrarw32\\unrar.exe").getAbsolutePath();
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                this.getPluginConfig().save();
                return true;
            }
            if (OSDetector.isLinux()) {
                path = JDUtilities.getResourceFile("tools/linux/unrar/unrar").getAbsolutePath();
                chmodUnrar(path);
                if (isUnrarCommandValid(path)) {
                    this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                    this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                    this.getPluginConfig().save();
                    return true;
                }
            }
            if (OSDetector.isMac()) {
                path = JDUtilities.getResourceFile("tools/mac/unrar2/unrar").getAbsolutePath();
                chmodUnrar(path);
                if (isUnrarCommandValid(path)) {
                    this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                    this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                    this.getPluginConfig().save();
                    return true;
                }
            }
            try {
                String[] charset = System.getenv("PATH").split(":");
                for (String element : charset) {
                    File fi = new File(element, "unrar");
                    File fi2 = new File(element, "rar");
                    if (fi.isFile() && isUnrarCommandValid(fi.getAbsolutePath())) {
                        path = fi.getAbsolutePath();
                        this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                        this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                        this.getPluginConfig().save();
                        return true;
                    } else if (fi2.isFile() && isUnrarCommandValid(fi2.getAbsolutePath())) {
                        path = fi2.getAbsolutePath();
                        this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, path);
                        this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, JDHash.getMD5(new File(path)));
                        this.getPluginConfig().save();
                        return true;
                    }
                }
            } catch (Throwable e) {
            }
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
            this.getPluginConfig().save();
            return false;
        }
        this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
        this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
        this.getPluginConfig().save();
        return false;
    }

    /**
     * Prüft ob ein bestimmter Unrarbefehl gültig ist
     * 
     * @param path
     * @return
     */
    private boolean isUnrarCommandValid(String path) {
        return UnrarWrapper.isUnrarCommandValid(path);
    }

    @Override
    public void onExit() {
    }

    public void onUnrarEvent(int id, UnrarWrapper wrapper) {
        LinkStatus ls = wrapper.getDownloadLink().getLinkStatus();
        // Falls der link entfernt wird während dem entpacken
        if (wrapper.getDownloadLink().getFilePackage() == FilePackage.getDefaultFilePackage() && wrapper.getProgressController() == null) {
            logger.warning("LINK GOT REMOVED_: " + wrapper.getDownloadLink());
            ProgressController progress = new ProgressController(JDL.LF("plugins.optional.jdunrar.progress.extractfile", "Extract %s", wrapper.getDownloadLink().getFileOutput()), 100, "gui.images.addons.unrar");
            wrapper.setProgressController(progress);
        }

        if (wrapper.getProgressController() != null) {
            onUnrarDummyEvent(id, wrapper);
            return;
        }
        // int min;
        switch (id) {
        case JDUnrarConstants.INVALID_BINARY:
            logger.severe("Invalid unrar binary!");
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
            this.getPluginConfig().save();
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED:

            ArrayList<DownloadLink> list = this.getArchiveList(wrapper.getDownloadLink());

            for (DownloadLink link : list) {

                if (link == null) continue;
                LinkStatus lls = link.getLinkStatus();

                if (wrapper.getException() != null) {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed: " + wrapper.getException().getMessage());
                    link.requestGuiUpdate();
                } else {
                    lls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    lls.setErrorMessage("Extract failed");
                    link.requestGuiUpdate();
                }
            }
            this.onFinished(wrapper);

            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:

            wrapper.getDownloadLink().requestGuiUpdate();

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = UserIO.getInstance().requestInputDialog(0, JDL.LF("plugins.optional.jdunrar.askForPassword", "Password for %s?", wrapper.getDownloadLink().getName()), null);
                if (pass == null || pass.length() == 0) {
                    ls.addStatus(LinkStatus.ERROR_POST_PROCESS);
                    ls.setStatusText(JDL.L("plugins.optional.jdunrar.status.extractfailedpass", "Extract failed (password)"));
                    this.onFinished(wrapper);
                    break;
                }
                wrapper.setPassword(pass);
            }
            break;
        case JDUnrarConstants.WRAPPER_CRACK_PASSWORD:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));
            wrapper.getDownloadLink().requestGuiUpdate();
            break;
        case JDUnrarConstants.WRAPPER_NEW_STATUS:
            break;
        case JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.openingarchive", "Opening archive"));
            wrapper.getDownloadLink().requestGuiUpdate();
            break;
        case JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(wrapper);
            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_FOUND:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.passfound", "Password found"));
            wrapper.getDownloadLink().requestGuiUpdate();
            wrapper.getDownloadLink().setPluginProgress(null);
            break;
        case JDUnrarConstants.WRAPPER_PASSWORT_CRACKING:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));
            if (wrapper.getDownloadLink().getPluginProgress() == null) {
                wrapper.getDownloadLink().setPluginProgress(new PluginProgress(wrapper.getCrackProgress(), 100, Color.GREEN.darker()));
            } else {
                wrapper.getDownloadLink().getPluginProgress().setCurrent(wrapper.getCrackProgress());
            }
            wrapper.getDownloadLink().requestGuiUpdate();
            break;
        case JDUnrarConstants.WRAPPER_ON_PROGRESS:
            wrapper.getDownloadLink().getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.extracting", "Extracting"));
            if (wrapper.getDownloadLink().getPluginProgress() == null) {
                wrapper.getDownloadLink().setPluginProgress(new PluginProgress(wrapper.getExtractedSize(), wrapper.getTotalSize(), Color.YELLOW.darker()));
            } else {
                wrapper.getDownloadLink().getPluginProgress().setCurrent(wrapper.getExtractedSize());
            }
            wrapper.getDownloadLink().requestGuiUpdate();
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            list = this.getArchiveList(wrapper.getDownloadLink());
            try {
                if (wrapper.getCurrentFile() != null) {
                    logger.info("Delete " + wrapper.getCurrentFile().getFile() + " because it was not extracted successfully!");
                    wrapper.getCurrentFile().getFile().delete();
                }
            } catch (Exception e) {
            }
            DownloadLink crc = null;
            if (wrapper.getCurrentVolume() > 0) {
                crc = list.size() >= wrapper.getCurrentVolume() ? list.get(wrapper.getCurrentVolume() - 1) : null;
            }
            if (crc != null) {
                for (DownloadLink link : list) {
                    if (link == null) {
                        continue;
                    }
                    if (link == crc) {
                        link.getLinkStatus().removeStatus(LinkStatus.FINISHED);
                        link.getLinkStatus().removeStatus(LinkStatus.ERROR_ALREADYEXISTS);
                        link.getLinkStatus().addStatus(LinkStatus.ERROR_DOWNLOAD_FAILED);
                        link.getLinkStatus().setValue(LinkStatus.VALUE_FAILED_HASH);
                        link.getLinkStatus().setErrorMessage(JDL.LF("plugins.optional.jdunrar.crcerrorin", "Extract: failed (CRC in %s)", crc.getName()));
                    } else {
                        link.getLinkStatus().addStatus(LinkStatus.ERROR_POST_PROCESS);
                        link.getLinkStatus().setErrorMessage("Extract failed");
                    }
                    link.requestGuiUpdate();
                }
            } else {
                for (DownloadLink link : list) {
                    if (link == null) continue;
                    link.getLinkStatus().setErrorMessage(JDL.L("plugins.optional.jdunrar.error.extrfailedcrc", "Extract: failed (CRC in unknown file)"));
                    link.requestGuiUpdate();
                }
            }
            this.onFinished(wrapper);

            break;
        case JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL:
            list = this.getArchiveList(wrapper.getDownloadLink());
            File[] files = new File[wrapper.getFiles().size()];
            int i = 0;
            for (ArchivFile af : wrapper.getFiles()) {
                files[i++] = af.getFile();
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            for (DownloadLink link : list) {
                if (link == null) continue;
                link.getLinkStatus().addStatus(LinkStatus.FINISHED);
                link.getLinkStatus().removeStatus(LinkStatus.ERROR_POST_PROCESS);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.extractok", "Extract OK"));
                link.requestGuiUpdate();
            }
            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, false)) {
                File fileOutput = new File(wrapper.getDownloadLink().getFileOutput());
                String packname = wrapper.getDownloadLink().getFilePackage().getName();
                File infoFiles = new File(fileOutput.getParentFile(), packname.replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            this.onFinished(wrapper);
            break;

        case JDUnrarConstants.NOT_ENOUGH_SPACE:
            ArrayList<DownloadLink> links = this.getArchiveList(wrapper.getDownloadLink());

            for (DownloadLink link : links) {
                if (link == null) continue;

                link.getLinkStatus().setStatus(LinkStatus.FINISHED);
                link.getLinkStatus().setStatusText(JDL.L("plugins.optional.jdunrar.status.notenoughspace", "Not enough space to extract"));
                link.requestGuiUpdate();
            }

            this.onFinished(wrapper);
            break;
        }
    }

    private void assignRealDownloadDir(UnrarWrapper wrapper) {
        PasswordListController.getInstance().addPassword(wrapper.getPassword(), true);

        int min = this.getPluginConfig().getIntegerProperty(JDUnrarConstants.CONFIG_KEY_SUBPATH_MINNUM, 0);
        if (min > 0) {

            ArrayList<ArchivFile> files = wrapper.getFiles();
            int i = 0;
            // get filenum without directories
            for (ArchivFile af : files) {
                if (af.getSize() > 0) i++;
            }
            Boolean usesub = this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false);
            if (min >= i) {
                // reset extractdirectory to default
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, false);
            } else {
                this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, true);
            }
            File dl = this.getExtractToPath(wrapper.getDownloadLink());
            wrapper.setExtractTo(dl);
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_USE_SUBPATH, usesub);

            ArrayList<DownloadLink> linkList = this.getArchiveList(wrapper.getDownloadLink());
            for (DownloadLink l : linkList) {
                if (l == null) continue;
                l.setProperty(JDUnrarConstants.DOWNLOADLINK_KEY_EXTRACTEDPATH, dl.getAbsolutePath());
            }

        }

    }

    /**
     * Als Dummy wird ein downloadlink bezeicnet, der nicht ind er downloadliste
     * war, sondern nur angelegt wurde um als container für ein externes archiv
     * zu dienen. Zur Fortschrittsanzeige wird ein progresscontroller verwendet
     * 
     * @param id
     * @param wrapper
     */
    private void onUnrarDummyEvent(int id, UnrarWrapper wrapper) {
        ProgressController pc = wrapper.getProgressController();
        // int min;
        switch (id) {
        case JDUnrarConstants.INVALID_BINARY:
            logger.severe("Invalid unrar binary!");
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_UNRARCOMMAND, null);
            this.getPluginConfig().setProperty(JDUnrarConstants.UNRAR_HASH, null);
            this.getPluginConfig().save();
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED:

            if (wrapper.getException() != null) {

                pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extractfailed", "Extract failed") + ": " + wrapper.getException().getMessage());

            } else {

                pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extractfailed", "Extract failed"));

            }

            this.onFinished(wrapper);

            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_NEEDED_TO_CONTINUE:

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extractfailedpass", "Extract failed (password)"));

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_ASK_UNKNOWN_PASS, true)) {
                String pass = UserIO.getInstance().requestInputDialog(0, JDL.LF("plugins.optional.jdunrar.askForPassword", "Password for %s?", wrapper.getDownloadLink().getName()), null);
                if (pass == null || pass.length() == 0) {
                    this.onFinished(wrapper);
                    break;
                }
                wrapper.setPassword(pass);
            }

            break;
        case JDUnrarConstants.WRAPPER_PASSWORT_CRACKING:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.crackingpass", "Cracking password"));
            pc.setRange(100);
            pc.setStatus(wrapper.getCrackProgress());
            break;
        case JDUnrarConstants.WRAPPER_START_OPEN_ARCHIVE:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.openingarchive", "Opening archive"));
            break;
        case JDUnrarConstants.WRAPPER_OPEN_ARCHIVE_SUCCESS:
            assignRealDownloadDir(wrapper);
            break;
        case JDUnrarConstants.WRAPPER_PASSWORD_FOUND:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.passfound", "Password found"));
            break;
        case JDUnrarConstants.WRAPPER_ON_PROGRESS:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extracting", "Extracting"));
            pc.setRange(wrapper.getTotalSize());
            pc.setStatus(wrapper.getExtractedSize());
            break;
        case JDUnrarConstants.WRAPPER_EXTRACTION_FAILED_CRC:
            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extractfailedcrc", "Extract failed (CRC error)"));
            this.onFinished(wrapper);
            break;
        case JDUnrarConstants.WRAPPER_FINISHED_SUCCESSFULL:
            File[] files = new File[wrapper.getFiles().size()];
            int i = 0;
            for (ArchivFile af : wrapper.getFiles()) {
                files[i++] = af.getFile();
            }
            JDUtilities.getController().fireControlEvent(new ControlEvent(wrapper, ControlEvent.CONTROL_ON_FILEOUTPUT, files));

            pc.setStatusText(wrapper.getFile().getName() + ": " + JDL.L("plugins.optional.jdunrar.status.extractok", "Extract OK"));

            if (this.getPluginConfig().getBooleanProperty(JDUnrarConstants.CONFIG_KEY_REMOVE_INFO_FILE, false)) {
                File fileOutput = new File(wrapper.getDownloadLink().getFileOutput());
                File infoFiles = new File(fileOutput.getParentFile(), fileOutput.getName().replaceFirst("(?i)(\\.pa?r?t?\\.?[0-9]+\\.rar|\\.rar)$", "") + ".info");
                if (infoFiles.exists() && infoFiles.delete()) {
                    logger.info(infoFiles.getName() + " removed");
                }
            }
            this.onFinished(wrapper);
            break;
        }
    }

    /**
     * Gibt alle downloadlinks zum übergebenen link zurück. d.h. alle links die
     * zu dem archiv gehören
     * 
     * @param downloadLink
     * @return
     */
    private ArrayList<DownloadLink> getArchiveList(DownloadLink downloadLink) {
        ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();

        File file;
        int type = JDUnrar.getArchivePartType(downloadLink);
        String name = null;
        int nums = 0;
        int i = 0;
        switch (type) {
        case JDUnrarConstants.NO_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART_V2:
            name = this.getArchiveName(downloadLink);
            String test = null;
            String partid = new Regex(downloadLink.getName(), "(\\.pa?r?t?\\.?)(\\d+)\\.").getMatch(0);
            if ((test = new Regex(downloadLink.getName(), "\\.pa?r?t?\\.?(\\d+)\\.").getMatch(0)) != null) {
                nums = test.length();
                i = 1;
                while ((file = new File(new File(downloadLink.getFileOutput()).getParentFile(), name + partid + Formatter.fillString(i + "", "0", "", nums) + ".rar")).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                    DownloadLink dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
                    if (dl == null) dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.ERROR_ALREADYEXISTS);
                    if (dl == null) dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, null);
                    if (dl != null) ret.add(dl);
                    i++;
                }
                break;
            } else if ((test = new Regex(downloadLink.getName(), "(.*)\\.rar$").getMatch(0)) != null) {
                ret.add(downloadLink);
                i = 0;
                nums = -1;
                for (int a = 5; a > 0; a--) {
                    String len = ".r";
                    for (int b = a; b > 0; b--) {
                        len = len + "0";
                    }
                    if (new File(test + len).exists()) {
                        nums = a;
                        break;
                    }
                }
                if (nums != -1) {
                    while ((file = new File(new File(downloadLink.getFileOutput()).getParentFile(), name + ".r" + Formatter.fillString(i + "", "0", "", nums))).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                        DownloadLink dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED);
                        if (dl == null) dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.ERROR_ALREADYEXISTS);
                        if (dl == null) dl = JDUtilities.getController().getDownloadLinkByFileOutput(file, null);
                        if (dl != null) ret.add(dl);
                        i++;
                    }
                }
            }
            break;
        case JDUnrarConstants.SINGLE_PART_ARCHIVE:
            ret.add(downloadLink);
            break;

        }
        return ret;

    }

    private ArrayList<String> getFileList(String filepath) {
        ArrayList<String> ret = new ArrayList<String>();
        File file;
        int type = getArchivePartType(new File(filepath));
        String name = null;
        int nums = 0;
        int i = 0;
        switch (type) {
        case JDUnrarConstants.NO_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART:
        case JDUnrarConstants.MULTIPART_START_PART_V2:
            name = this.getArchiveName(filepath);
            String test = null;
            String partid = new Regex(filepath, "(\\.pa?r?t?\\.?)(\\d+)\\.").getMatch(0);
            if ((test = new Regex(filepath, "\\.pa?r?t?\\.?(\\d+)\\.").getMatch(0)) != null) {
                nums = test.length();
                i = 1;
                while ((file = new File(new File(filepath).getParentFile(), name + partid + Formatter.fillString(i + "", "0", "", nums) + ".rar")).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                    ret.add(file.toString());
                    i++;
                }
                break;
            } else if ((test = new Regex(filepath, "(.*)\\.rar$").getMatch(0)) != null) {
                ret.add(filepath);
                i = 0;
                nums = -1;
                for (int a = 5; a > 0; a--) {
                    String len = ".r";
                    for (int b = a; b > 0; b--) {
                        len = len + "0";
                    }
                    if (new File(test + len).exists()) {
                        nums = a;
                        break;
                    }
                }
                if (nums != -1) {
                    while ((file = new File(new File(filepath).getParentFile(), name + ".r" + Formatter.fillString(i + "", "0", "", nums))).exists() || JDUtilities.getController().getDownloadLinkByFileOutput(file, LinkStatus.FINISHED) != null) {
                        ret.add(file.toString());
                        i++;
                    }
                }
            }
            break;
        case JDUnrarConstants.SINGLE_PART_ARCHIVE:
            ret.add(filepath);
            break;
        }
        return ret;

    }

    private void onFinished(UnrarWrapper wrapper) {
        wrapper.getDownloadLink().setPluginProgress(null);
        if (wrapper.getProgressController() != null) {
            wrapper.getProgressController().doFinalize(8000);
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