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

package jd.plugins.optional.hjsplit;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.gui.swing.SwingGui;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.actions.ToolBarAction.Types;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Formatter;
import jd.nutils.io.FileSignatures;
import jd.nutils.io.Signature;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.plugins.optional.hjsplit.jaxe.JAxeJoiner;
import jd.plugins.optional.hjsplit.jaxe.JoinerFactory;
import jd.plugins.optional.hjsplit.jaxe.ProgressEvent;
import jd.plugins.optional.hjsplit.jaxe.ProgressEventListener;
import jd.utils.JDHexUtils;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "hjsplit", interfaceversion = 5)
public class JDHJSplit extends PluginOptional {

    private static enum ARCHIV_TYPE {
        NONE, NORMAL, UNIX, CUTKILLER, XTREMSPLIT
    }

    private static final String CONFIG_KEY_REMOVE_MERGED = "REMOVE_MERGED";
    private static final String DUMMY_HOSTER = "dum.my";
    private static final String CONFIG_KEY_OVERWRITE = "OVERWRITE";

    /** Wird als reihe für anstehende extracthjobs verwendet */
    private Jobber queue;

    public JDHJSplit(PluginWrapper wrapper) {
        super(wrapper);
        this.queue = new Jobber(1);
        initConfig();
    }

    @Override
    public String getIconKey() {
        return "gui.images.addons.merge";
    }

    /**
     * Das controllevent fängt heruntergeladene file ab und wertet sie aus.
     * CONTROL_PLUGIN_INACTIVE: Wertet die frisch fertig gewordenen Downloads
     * aus. CONTROL_ON_FILEOUTPUT: wertet frisch fertig verarbeitete files aus,
     * z.B. frisch entpackte CONTROL_LINKLIST_CONTEXT_MENU: wird verwendet um
     * ins kontextmenü der gui die menüpunkte zu schreiben
     */
    @SuppressWarnings("unchecked")
    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        DownloadLink link;
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            if (!(event.getSource() instanceof PluginForHost)) return;
            if (this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
                link = ((SingleDownloadController) event.getParameter()).getDownloadLink();
                File file = new File(link.getFileOutput());

                if (link.getLinkStatus().isFinished()) {
                    file = this.getStartFile(file);
                    if (file == null) return;
                    if (this.validateArchive(file)) {
                        addFileList(new File[] { file });
                    }
                }
            }
            break;
        case ControlEvent.CONTROL_ON_FILEOUTPUT:
            addFileList((File[]) event.getParameter());
            break;
        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            MenuAction m;
            if (event.getSource() instanceof DownloadLink) {
                link = (DownloadLink) event.getSource();

                items.add(m = new MenuAction("optional.jdhjsplit.linkmenu.merge", 1000));
                m.setActionListener(this);
                m.setEnabled(false);
                if (link.getLinkStatus().hasStatus(LinkStatus.FINISHED) && this.isStartVolume(new File(link.getFileOutput()))) m.setEnabled(true);
                if (new File(link.getFileOutput()).exists() && link.getName().matches(".*rar$")) m.setEnabled(true);

                m.setProperty("LINK", link);

            } else {
                FilePackage fp = (FilePackage) event.getSource();
                items.add(m = new MenuAction("optional.jdhjsplit.linkmenu.package.merge", 1001));
                m.setActionListener(this);
                m.setProperty("PACKAGE", fp);
            }
            break;
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        MenuAction m;
        menu.add(m = new MenuAction("optional.hjsplit.menu.toggle", 1));
        m.setActionListener(this);
        m.setSelected(this.getPluginConfig().getBooleanProperty("ACTIVATED", true));
        menu.add(new MenuAction(Types.SEPARATOR));
        menu.add(m = new MenuAction("optional.hjsplit.menu.extract.singlefils", 21));
        m.setActionListener(this);
        return menu;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof MenuAction) {
            menuitemActionPerformed((MenuAction) e.getSource());
        }
    }

    private void menuitemActionPerformed(MenuAction source) {
        SubConfiguration cfg = this.getPluginConfig();
        switch (source.getActionID()) {
        case 1:
            cfg.setProperty("ACTIVATED", !cfg.getBooleanProperty("ACTIVATED", true));
            cfg.save();
            break;
        case 21:
            JDFileChooser fc = new JDFileChooser("_JDHJSPLIT_");
            fc.setMultiSelectionEnabled(true);
            FileFilter ff = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (isStartVolume(pathname)) return true;
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                @Override
                public String getDescription() {
                    return JDL.L("plugins.optional.hjsplit.filefilter", "HJSPLIT-Startvolumes");
                }

            };
            fc.setFileFilter(ff);
            if (fc.showOpenDialog(SwingGui.getInstance().getMainFrame()) == JDFileChooser.APPROVE_OPTION) {
                File[] list = fc.getSelectedFiles();
                if (list == null) return;
                addFileList(list);
            }
            break;

        case 1000:
            File file = new File(((DownloadLink) source.getProperty("LINK")).getFileOutput());
            file = this.getStartFile(file);
            if (this.validateArchive(file)) {
                addFileList(new File[] { file });
            }
            break;

        case 1001:
            FilePackage fp = (FilePackage) source.getProperty("PACKAGE");
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            for (DownloadLink l : fp.getDownloadLinkList()) {
                if (l.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                    file = new File(l.getFileOutput());
                    if (this.validateArchive(file)) {
                        links.add(l);
                    }
                }
            }
            if (links.size() <= 0) return;
            addFileList(links.toArray(new File[] {}));
            break;
        }
    }

    /**
     * Fügt lokale files der mergqueue hinzu. Es muss sich bereits zum
     * startarchive handeln.
     * 
     * @param list
     */
    private void addFileList(File[] list) {
        DownloadLink link;
        for (File archiveStartFile : list) {
            if (!isStartVolume(archiveStartFile)) continue;
            boolean b = validateArchive(archiveStartFile);
            if (!b) {
                logger.info("Archive " + archiveStartFile + " is incomplete or no archive. Validation failed");
                return;
            }
            link = JDUtilities.getController().getDownloadLinkByFileOutput(archiveStartFile, LinkStatus.FINISHED);
            if (link == null) link = createDummyLink(archiveStartFile);

            final DownloadLink finalLink = link;

            addToQueue(finalLink);
        }
    }

    /**
     * Checks if the merged file(s) has enough space.
     * 
     * @param files
     * @param extractTo
     * @return
     */
    private boolean checkSize(ArrayList<File> files, DownloadLink extractTo) {
        if (JDUtilities.getJavaVersion() < 1.6) return true;

        long totalSize = 0;

        for (File f : files) {
            totalSize += f.length();
        }

        // Add 500MB extra Buffer
        totalSize += 1024 * 1024 * 1024 * 500;

        for (DownloadLink dlink : DownloadWatchDog.getInstance().getRunningDownloads()) {
            totalSize += dlink.getDownloadSize() - dlink.getDownloadCurrent();
        }

        if (new File(extractTo.getFileOutput()).getUsableSpace() < totalSize) return false;

        return true;
    }

    /** Startet das Abwarbeiten der extractqueue */
    private void addToQueue(final DownloadLink link) {

        queue.add(new JDRunnable() {

            public void go() {
                ArrayList<File> list = getFileList(new File(link.getFileOutput()));

                if (!checkSize(list, link)) {
                    JDLogger.getLogger().warning("Not enough available space for merging");
                    return;
                }

                final File output = getOutputFile(new File(link.getFileOutput()));
                if (output == null) return;
                final ProgressController progress = new ProgressController("Default HJMerge", 100, "gui.images.addons.merge");
                JAxeJoiner join = JoinerFactory.getJoiner(new File(link.getFileOutput()));

                if (list != null) {
                    String cutKillerExt = getCutkillerExtension(new File(link.getFileOutput()), list.size());
                    join.setCutKiller(cutKillerExt);
                } else {
                    JDLogger.getLogger().severe("getFileList returned null?");
                    return;
                }
                join.setProgressEventListener(new ProgressEventListener() {

                    long last = System.currentTimeMillis() + 1000;

                    public void handleEvent(ProgressEvent pe) {
                        try {
                            if (System.currentTimeMillis() - last > 100) {
                                progress.setStatus((int) (pe.getCurrent() * 100 / pe.getMax()));
                                last = System.currentTimeMillis();
                                progress.setStatusText(JDL.LF("plugins.optional.hjsplit.merged", "%s: %s MB merged", output.getName(), pe.getCurrent() / 1048576));
                            }
                        } catch (Exception e) {
                        }
                    }
                });
                join.overwriteExistingFile(getPluginConfig().getBooleanProperty(CONFIG_KEY_OVERWRITE, false));
                try {
                    join.run();
                    if (join.wasSuccessfull() && getPluginConfig().getBooleanProperty(CONFIG_KEY_REMOVE_MERGED, false)) {
                        if (list != null) {
                            for (File f : list) {
                                f.delete();
                                f.deleteOnExit();
                            }
                        }
                    }
                } catch (Exception e) {
                }
                progress.doFinalize();
                JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_FILEOUTPUT, new File[] { output }));
            }
        });
        queue.start();
    }

    /**
     * Gibt die zu entpackende Datei zurück.
     * 
     * @param file
     * @return
     */
    private File getOutputFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            /* maybe parse header here */
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ""));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ""));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ""));
        default:
            return null;
        }
    }

    /**
     * Validiert ein Archiv. Archive haben zwei formen: unix: *.aa..*.ab..*.ac .
     * das zweite a wird hochgezählt normal: *.001...*.002
     * 
     * Die Funktion versucht zu prüfen ob das Archiv komplett heruntergeladen
     * wurde und ob es ein gültoges Archiv ist.
     * 
     * @param file
     * @return
     */
    private boolean validateArchive(File file) {
        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return false;
        ARCHIV_TYPE type = getArchiveType(file);

        switch (type) {
        case UNIX:
            return typeCrossCheck(validateUnixType(startFile)) != null;
        case NORMAL:
            return typeCrossCheck(validateNormalType(startFile)) != null;
        default:
            return false;
        }
    }

    /**
     * Gibt alle files die zum Archiv von file gehören zurück
     * 
     * @param file
     * @return
     */
    private ArrayList<File> getFileList(File file) {

        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return null;
        ARCHIV_TYPE type = getArchiveType(file);

        switch (type) {
        case UNIX:
            return validateUnixType(startFile);
        case NORMAL:
            return validateNormalType(startFile);
        default:
            return null;
        }
    }

    /**
     * Validiert typ normal (siehe validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateNormalType(File file) {
        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\\\.[\\\\d]+$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            File par1 = new File(miss.getFileOutput()).getParentFile();
            File par2 = file.getParentFile();
            if (par1.equals(par2)) {

                if (!new File(miss.getFileOutput()).exists()) { return null; }
            }
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        int c = 1;
        ArrayList<File> ret = new ArrayList<File>();
        for (int i = 0; i < files.length; i++) {
            String volume = Formatter.fillString(c + "", "0", "", 3);
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1"))).exists()) {
                c++;
                ret.add(newFile);
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist, will
         * check for next possible filename
         */
        String volume = Formatter.fillString(c + "", "0", "", 3);
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", "\\." + volume + "$1")), null) != null) return null;
        return ret;
    }

    private ArrayList<File> typeCrossCheck(ArrayList<File> files) {
        if (files == null) return null;
        int ArchiveCheckFailed = 0;
        for (File file : files) {
            try {
                Signature fs = FileSignatures.getFileSignature(file);
                if (fs != null && (fs.getId().equals("RAR") || fs.getId().equals("﻿7Z"))) {
                    ArchiveCheckFailed++;
                }
            } catch (IOException e) {
            }
        }
        if (ArchiveCheckFailed > 1) {
            /*
             * mehr als 1 mal sollte kein Rar oder 7zip header signatur gefunden
             * werden
             */
            logger.warning("Found more than 1 non-HJArchive Header, skip HJMerge!");
            return null;
        }
        return files;
    }

    /**
     * Validiert das archiv auf 2 arten 1. wird ind er downloadliste nach
     * passenden unfertigen archiven gesucht 2. wird das archiv durchnummeriert
     * und geprüft ob es lücken/fehlende files gibts siehe (validateArchiv)
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateUnixType(File file) {

        final String matcher = file.getName().replaceAll("\\[|\\]|\\(|\\)|\\?", ".").replaceFirst("\\.a.($|\\..*)", "\\\\.a.$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getDownloadLinksByNamePattern(matcher);
        if (missing == null) return null;
        for (DownloadLink miss : missing) {
            /* do not continue if we have an unfinished file here */
            if (!miss.getLinkStatus().isFinished()) return null;
            if (new File(miss.getFileOutput()).exists() && new File(miss.getFileOutput()).getParentFile().equals(file.getParentFile())) continue;
            return null;
        }
        File[] files = file.getParentFile().listFiles(new java.io.FileFilter() {
            public boolean accept(File pathname) {
                if (pathname.isFile() && pathname.getName().matches(matcher)) { return true; }
                return false;
            }
        });
        ArrayList<File> ret = new ArrayList<File>();
        char c = 'a';
        for (int i = 0; i < files.length; i++) {
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1"))).exists()) {
                ret.add(newFile);
                c++;
            } else {
                return null;
            }
        }
        /*
         * securitycheck for missing file on disk but in downloadlist , will
         * check for next possible filename
         */
        if (JDUtilities.getController().getDownloadLinkByFileOutput(new File(file.getParentFile(), file.getName().replaceFirst("\\.a.($|\\..*)", "\\.a" + c + "$1")), null) != null) return null;
        return ret;
    }

    /**
     * Sucht den Dateinamen und den Pfad der des Startvolumes heraus
     * 
     * @param file
     * @return
     */
    private File getStartFile(File file) {
        ARCHIV_TYPE type = getArchiveType(file);
        switch (type) {
        case XTREMSPLIT:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.\\d+\\.xtm$", ".001.xtm"));
        case UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ".aa"));
        case NORMAL:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.[^\\d]*$)", ".001$1"));
        default:
            return null;
        }
    }

    /**
     * Gibt zurück ob es sich bei der Datei um ein hjsplit Startvolume handelt.
     * 
     * @param file
     * @return
     */
    private boolean isStartVolume(File file) {
        if (file.getName().matches(".*\\.aa$")) return true;
        if (file.getName().matches(".*\\.001\\.xtm$")) return true;
        if (file.getName().matches(".*\\.001($|\\.[^\\d]*$)")) return true;
        return false;
    }

    /**
     * Gibt den Archivtyp zurück. möglich sind: ARCHIVE_TYPE_7Z (bad)
     * ARCHIVE_TYPE_NONE (bad) ARCHIVE_TYPE_UNIX ARCHIVE_TYPE_NORMAL
     * 
     * @param file
     * @return
     */
    private ARCHIV_TYPE getArchiveType(File file) {
        String name = file.getName();
        if (name.matches(".*\\.a.$")) return ARCHIV_TYPE.UNIX;
        if (name.matches(".*\\.\\d+\\.xtm$")) return ARCHIV_TYPE.XTREMSPLIT;
        if (name.matches(".*\\.[\\d]+($|\\.[^\\d]*$)")) return ARCHIV_TYPE.NORMAL;
        return ARCHIV_TYPE.NONE;
    }

    /**
     * Erstellt einen Dummy Downloadlink. Dieser dient nur als container für die
     * Datei. Adapterfunktion
     * 
     * @param archiveStartFile
     * @return
     */
    private DownloadLink createDummyLink(File archiveStartFile) {

        DownloadLink link = new DownloadLink(null, archiveStartFile.getName(), DUMMY_HOSTER, "", true);
        link.setDownloadSize(archiveStartFile.length());
        FilePackage fp = FilePackage.getInstance();
        fp.setDownloadDirectory(archiveStartFile.getParent());
        link.setFilePackage(fp);
        return link;
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    /**
     * returns String with fileextension if we find a valid cutkiller fileheader
     * returns null if no cutkiller fileheader found
     * 
     * @param file
     * @return
     */
    private String getCutkillerExtension(File file, int filecount) {
        File startFile = getStartFile(file);
        if (startFile == null) return null;
        String sig = null;
        try {
            sig = JDHexUtils.toString(FileSignatures.readFileSignature(startFile));
        } catch (IOException e) {
            JDLogger.exception(e);
            return null;
        }
        if (new Regex(sig, "[\\w]{3}  \\d+").matches()) {
            String count = new Regex(sig, ".*?  (\\d+)").getMatch(0);
            if (count == null) return null;
            if (filecount != Integer.parseInt(count)) return null;
            String ext = new Regex(sig, "(.*?) ").getMatch(0);
            logger.info("CutKiller Header found! Ext: ." + ext + " Parts: " + filecount);
            return ext;
        }
        return null;
    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_KEY_REMOVE_MERGED, JDL.L("gui.config.hjsplit.remove_merged", "Delete archive after merging")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_KEY_OVERWRITE, JDL.L("gui.config.hjsplit.overwrite", "Overwrite existing files")));
        ce.setDefaultValue(true);
    }

    @Override
    public void onExit() {
    }
}