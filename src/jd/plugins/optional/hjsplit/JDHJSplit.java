//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.gui.skins.simple.config.ConfigEntriesPanel;
import jd.gui.skins.simple.config.ConfigurationPopup;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginOptional;
import jd.plugins.optional.jdunrar.JDUnrarConstants;
import jd.unrar.hjsplitt.JAxeJoiner;
import jd.unrar.hjsplitt.JoinerFactory;
import jd.unrar.hjsplitt.ProgressEvent;
import jd.unrar.hjsplitt.ProgressEventListener;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDHJSplit extends PluginOptional implements ControlListener {

    private static final String CONFIG_KEY_REMOVE_MERGED = "REMOVE_MERGED";
    private static final String DUMMY_HOSTER = "dum.my";
    private static final int ARCHIVE_TYPE_NONE = -1;
    private static final int ARCHIVE_TYPE_NORMAL = 0;
    private static final int ARCHIVE_TYPE_UNIX = 1;
    private static final int ARCHIVE_TYPE_7Z = 2;
    private static final String CONFIG_KEY_OVERWRITE = "OVERWRITE";
    /**
     * Wird als reihe für anstehende extracthjobs verwendet
     */
    private ArrayList<DownloadLink> queue;

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    @SuppressWarnings("unchecked")
    public JDHJSplit(PluginWrapper wrapper) {
        super(wrapper);
        this.queue = (ArrayList<DownloadLink>) this.getPluginConfig().getProperty(JDUnrarConstants.CONFIG_KEY_LIST, new ArrayList<DownloadLink>());

        initConfig();

    }

    /**
     * das controllevent fängt heruntergeladene file ab und wertet sie aus
     */
    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);

        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:

        case ControlEvent.CONTROL_ON_FILEOUTPUT:

            addFileList((File[]) event.getParameter());
            break;

        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            break;

        }

    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;

        menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("plugins.optional.hjsplit.menu.toggle", "Activate"), 1).setActionListener(this));
        m.setSelected(this.getPluginConfig().getBooleanProperty("ACTIVATED", true));

        menu.add(m = new MenuItem(MenuItem.SEPARATOR));

        m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.hjsplit.menu.extract.singlefils", "Merge archive(s)"), 21);
        m.setActionListener(this);
        menu.add(m);

        menu.add(m = new MenuItem(MenuItem.SEPARATOR));

        menu.add(m = new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.hjsplit.menu.config", "Settings"), 4).setActionListener(this));

        return menu;
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof MenuItem) {
            menuitemActionPerformed(e, (MenuItem) e.getSource());
        }

    }

    private void menuitemActionPerformed(ActionEvent e, MenuItem source) {
        SubConfiguration cfg = this.getPluginConfig();
        switch (source.getActionID()) {
        case 1:
            boolean newValue;
            cfg.setProperty("ACTIVATED", newValue = !cfg.getBooleanProperty("ACTIVATED", true));
            if (newValue) {
                JDUtilities.getController().addControlListener(this);
            } else {
                JDUtilities.getController().removeControlListener(this);
            }

            break;
        case 21:
            JDFileChooser fc = new JDFileChooser("_JDHJSPLIT_");
            fc.setMultiSelectionEnabled(true);
            FileFilter ff = new FileFilter() {

                public boolean accept(File pathname) {
                    if (isStartVolume(pathname)) return true;
                    if (pathname.isDirectory()) return true;
                    return false;
                }

                @Override
                public String getDescription() {
                    // TODO Auto-generated method stub
                    return "HJSPLIT-Startvolumes";
                }

            };
            fc.setFileFilter((javax.swing.filechooser.FileFilter) ff);
            fc.showSaveDialog(SimpleGUI.CURRENTGUI.getFrame());
            File[] list = fc.getSelectedFiles();
            if (list == null) return;

            addFileList(list);
            break;
        case 4:
            ConfigEntriesPanel cpanel = new ConfigEntriesPanel(config);

            JPanel panel = new JPanel(new BorderLayout());
            panel.add(new JPanel(), BorderLayout.NORTH);
            panel.add(cpanel, BorderLayout.CENTER);
            ConfigurationPopup pop = new ConfigurationPopup(SimpleGUI.CURRENTGUI.getFrame(), cpanel, panel);
            pop.setLocation(JDUtilities.getCenterOfComponent(SimpleGUI.CURRENTGUI.getFrame(), pop));
            pop.setVisible(true);
            break;
        }

    }

    private void addFileList(File[] list) {
        DownloadLink link;
        for (File archiveStartFile : list) {
            if (!isStartVolume(archiveStartFile)) continue;
            boolean b = validateArchive(archiveStartFile);
            if (!b) {
                logger.info("Archive " + archiveStartFile + " is incomplete or no archive. Validation failed");
                return;
            }

            // try {
            // Signature signature =
            // FileSignatures.getFileSignature(archiveStartFile);
            // if (signature.getDesc().equals("Rar Archiv") ||
            // signature.getDesc().equals("7-Zip Archiv")) {
            // logger.info("Archive " + archiveStartFile + " is " +
            // signature.getDesc() + ". cannot merge this type");
            // return;
            // }
            // } catch (IOException e1) {
            // // TODO Auto-generated catch block
            // e1.printStackTrace();
            // }
            link = JDUtilities.getController().getDownloadLinkByFileOutput(archiveStartFile);
            if (link == null) link = createDummyLink(archiveStartFile);

            final DownloadLink finalLink = link;
            new Thread() {
                public void run() {

                    addToQueue(finalLink);
                }
            }.start();

        }

    }

    /**
     * Fügt einen Link der Extractqueue hinzu
     * 
     * @param link
     */
    private void addToQueue(DownloadLink link) {
        synchronized (queue) {

            this.queue.add(link);
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_LIST, queue);
            this.getPluginConfig().save();
        }
        this.startExtraction();

    }

    /**
     * Startet das abwarbeiten der extractqueue
     */
    private synchronized void startExtraction() {

        if (queue.size() == 0) {
            System.out.print("return 0 queue");
            return;
        }

        final DownloadLink link;

        synchronized (queue) {
            link = queue.remove(0);
            this.getPluginConfig().setProperty(JDUnrarConstants.CONFIG_KEY_LIST, queue);
            this.getPluginConfig().save();
        }
        final ProgressController progress = new ProgressController("Default HJMerge", 100);
        try {
            JAxeJoiner join = JoinerFactory.getJoiner(new File(link.getFileOutput()));
            final File output = getOutputFile(new File(link.getFileOutput()));

            join.setProgressEventListener(new ProgressEventListener() {

                long last = System.currentTimeMillis() + 1000;

                public void handleEvent(ProgressEvent pe) {
                    try {
                        if (System.currentTimeMillis() - last > 100) {
                            progress.setStatus((int) (pe.getCurrent() * 100 / pe.getMax()));
                            last = System.currentTimeMillis();
                            progress.setStatusText(output.getName()+": "+(pe.getCurrent() / 1048576) + " MB merged");
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }

                }
            });
          
            if (this.getPluginConfig().getBooleanProperty(CONFIG_KEY_OVERWRITE, false)) {

                if (output.exists()) output.delete();

            }
            join.run();
            if (this.getPluginConfig().getBooleanProperty(CONFIG_KEY_REMOVE_MERGED, false)) {
                ArrayList<File> list = getFileList(new File(link.getFileOutput()));
                for (File f : list) {
                    f.delete();
                    f.deleteOnExit();
                }
            }

        
            JDUtilities.getController().fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_ON_FILEOUTPUT, new File[] { output }));
        } catch (Exception e) {
            e.printStackTrace();
            try {
                progress.finalize();
            } catch (Exception e2) {
                // TODO: handle exception
            }
            startExtraction();
        }
    }

    private File getOutputFile(File file) {

        int type = getArchiveType(file);
        switch (type) {
        case ARCHIVE_TYPE_UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ""));

        case ARCHIVE_TYPE_NORMAL:

            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\..*)", ""));

        default:
            return null;
        }
    }

    private boolean validateArchive(File file) {
        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return false;
        int type = getArchiveType(file);

        switch (type) {
        case ARCHIVE_TYPE_UNIX:
            return validateUnixType(startFile) != null;

        case ARCHIVE_TYPE_NORMAL:

            return validateNormalType(startFile) != null;

        default:
            return false;
        }
    }

    private ArrayList<File> getFileList(File file) {

        File startFile = getStartFile(file);
        if (startFile == null || !startFile.exists() || !startFile.isFile()) return null;
        int type = getArchiveType(file);

        switch (type) {
        case ARCHIVE_TYPE_UNIX:
            return validateUnixType(startFile);

        case ARCHIVE_TYPE_NORMAL:

            return validateNormalType(startFile);

        default:
            return null;
        }

    }

    private ArrayList<File> validateNormalType(File file) {
        final String matcher = file.getName().replaceFirst("\\.[\\d]+($|\\..*)", "\\\\.[\\\\d]+$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getMatchingLinks(matcher);
        for (DownloadLink miss : missing) {
            if (new File(miss.getFileOutput()).exists() && new File(miss.getFileOutput()).getParentFile().equals(file.getParentFile())) continue;
            return null;
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
            String volume = JDUtilities.fillString(c + "", "0", "", 3);
            File newFile;
            if ((newFile = new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\..*)", "\\." + volume + "$1"))).exists()) {

                c++;
                ret.add(newFile);
            } else {
                return null;
            }
        }
        return ret;
    }

    /**
     * Validiert das archiv auf 2 arten 1. wird ind er downloadliste nach
     * passenden unfertigen archiven gesucht 2. wird das archiv durchnummeriert
     * und geprüft ob es lücken/fehlende files gibts
     * 
     * @param file
     * @return
     */
    private ArrayList<File> validateUnixType(File file) {

        final String matcher = file.getName().replaceFirst("\\.a.($|\\..*)", "\\\\.a.$1");
        ArrayList<DownloadLink> missing = JDUtilities.getController().getMatchingLinks(matcher);
        for (DownloadLink miss : missing) {
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
        return ret;
    }

    /**
     * SUcht den dateinamen und den pfad der des startvolumes heraus
     * 
     * @param file
     * @return
     */
    private File getStartFile(File file) {
        int type = getArchiveType(file);
        switch (type) {
        case ARCHIVE_TYPE_UNIX:
            return new File(file.getParentFile(), file.getName().replaceFirst("\\.a.$", ".aa"));

        case ARCHIVE_TYPE_NORMAL:

            return new File(file.getParentFile(), file.getName().replaceFirst("\\.[\\d]+($|\\.)", ".001$1"));

        default:
            return null;
        }
    }

    /**
     * gibt zurück ob es sich beid er datei um ein hjsplit startvolume handelt.
     * 
     * @param file
     * @return
     */
    private boolean isStartVolume(File file) {
        if (file.getName().matches("(?is).*\\.7z\\.[\\d]+$")) return false;

        if (file.getName().matches(".*\\.aa$")) return true;

        if (file.getName().matches(".*\\.001($|\\..*)")) return true;

        return false;

    }

    /**
     * Gibt den Archivtyp zurück. möglich sind: ARCHIVE_TYPE_7Z (bad)
     * ARCHIVE_TYPE_NONE (bad) ARCHIVE_TYPE_UNIX ARCHIVE_TYPE_NORMAL
     * 
     * @param file
     * @return
     */
    private int getArchiveType(File file) {
        String name = file.getName();

        if (name.matches("(?is).*\\.7z\\.[\\d]+$")) return ARCHIVE_TYPE_7Z;

        if (name.matches(".*\\.\\a.$")) { return ARCHIVE_TYPE_UNIX; }
        if (name.matches(".*\\.[\\d]+($|\\..*)")) { return ARCHIVE_TYPE_NORMAL; }

        return ARCHIVE_TYPE_NONE;
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
        FilePackage fp = new FilePackage();
        fp.setDownloadDirectory(archiveStartFile.getParent());
        link.setFilePackage(fp);
        return link;

    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdhjsplit.name", "JD-HJMerge");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public boolean initAddon() {
        if (this.getPluginConfig().getBooleanProperty("ACTIVATED", true)) {
            JDUtilities.getController().addControlListener(this);
        }
        return true;

    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;

        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_KEY_REMOVE_MERGED, JDLocale.L("gui.config.hjsplit.remove_merged", "Delete archive after merging")));
        ce.setDefaultValue(true);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_KEY_OVERWRITE, JDLocale.L("gui.config.hjsplit.overwrite", "Overwrite existing files")));
        ce.setDefaultValue(true);
    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

}