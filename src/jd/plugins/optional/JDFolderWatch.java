//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

@SuppressWarnings("unchecked")
public class JDFolderWatch extends PluginOptional {
    public class check extends Thread {
        @Override
        public void run() {
            threadend = true;
            while (threadend) {
                checkFolder();
                try {
                    Thread.sleep(getWaittime());
                } catch (InterruptedException e) {
                }
            }
            running = false;
        };
    }

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    private SubConfiguration subConfig = null;
    private ArrayList<String> added = null;
    private check i;

    private boolean running = true;

    private boolean threadend = true;

    public JDFolderWatch(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = JDUtilities.getSubConfig("FOLDERWATCH");
        added = (ArrayList<String>) subConfig.getProperty("ADDED");
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, "FOLDER", JDLocale.L("plugins.optional.folderwatch.folder", "Ordner:")));
        cfg.setDefaultValue(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, "WAITTIME", JDLocale.L("plugins.optional.folderwatch.checkintervall", "Checkintervall [min]"), 1, 60));
        cfg.setDefaultValue("5");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Check")) {
            checkFolder();
        }
        if (e.getActionCommand().equals("Reset")) {
            logger.info("Folderwatch: Reseting saved containers");
            added = new ArrayList<String>();
            subConfig.setProperty("ADDED", added);
            subConfig.save();
        }
        if (e.getActionCommand().equals("Start/Stop")) {
            if (running) {
                logger.info("Folderwatch: Stopping");
                threadend = false;
                running = false;
            } else {
                logger.info("Folderwatch: Starting");
                i = new check();
                i.start();
                running = true;
            }
        }
    }

    private synchronized void checkFolder() {
        logger.info("Folderwatch: Checking folder");
        boolean dabei = false;
        File folder = new File(subConfig.getStringProperty("FOLDER", JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY)));
        if (folder.isDirectory()) {
            String[] help = folder.list();

            for (String element : help) {
                if (element.toLowerCase().endsWith(".dlc") || element.toLowerCase().endsWith(".ccf") || element.toLowerCase().endsWith(".rsdf")) {
                    dabei = false;
                    File container = new File(folder, element);
                    try {
                        for (int j = 0; j < added.size(); j++) {
                            if (container.getAbsolutePath().equals(added.get(j))) {
                                dabei = true;
                                break;
                            }
                        }
                        if (!dabei) {
                            JDUtilities.getController().loadContainerFile(container);
                            added.add(container.getAbsolutePath());
                            subConfig.setProperty("ADDED", added);
                            subConfig.save();
                            Thread.sleep(5000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem(JDLocale.L("plugins.optional.folderwatch.menu.startstop", "Start/Stop"), 0).setActionListener(this));
        menu.add(new MenuItem(JDLocale.L("plugins.optional.folderwatch.menu.check", "Check"), 0).setActionListener(this));
        menu.add(new MenuItem(JDLocale.L("plugins.optional.folderwatch.menu.reset", "Reset"), 0).setActionListener(this));
        return menu;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.folderwatch.name", "JDFolderWatch");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    private int getWaittime() {
        return subConfig.getIntegerProperty("WAITTIME", 5) * 60000;
    }

    @Override
    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() >= 1.5) {
            logger.info("FolderWatch OK");
            if (added == null) {
                added = new ArrayList<String>();
                subConfig.setProperty("ADDED", added);
                subConfig.save();
            }

            i = new check();
            i.start();
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void onExit() {
    }
}