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

package jd.plugins.optional;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlListener;
import jd.plugins.PluginOptional;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;

public class JDFolderWatch extends PluginOptional implements ControlListener {
    public static int getAddonInterfaceVersion(){
        return 0;
    }
    
    private boolean threadend = true;
    private boolean running = true;
    private check i;
    private ArrayList<String> added = new ArrayList<String>();
    private SubConfiguration subConfig = JDUtilities.getSubConfig("FOLDERWATCH");

    @Override
    public String getCoder() {
        return "jD-Team";
    }

    @Override
    public String getPluginID() {
        return "0.0.0.1";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.folderwatch.name", "FolderWatch");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public boolean initAddon() {
        if (JDUtilities.getJavaVersion() >= 1.5) {
            logger.info("FolderWatch OK");
            i = new check();
            i.start();
            return true;

        } else {
            return false;
        }
    }

    public JDFolderWatch() {        
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, subConfig, "FOLDER", JDLocale.L("plugins.optional.folderwatch.folder", "Folder:")));
        cfg.setDefaultValue(JDUtilities.getConfiguration().getDefaultDownloadDirectory());
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, "WAITTIME", JDLocale.L("plugins.optional.folderwatch.waittime", "Waittime"), 1, 60));
        cfg.setDefaultValue("5");
        
        //Hat nicht funktioniert
//        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, "DELETE", JDLocale.L("plugins.optional.folderwatch.delete", "Delete")));
//        cfg.setDefaultValue(true);
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Check")) {
            checkFolder();
        }
        if (e.getActionCommand().equals("Reset")) {
            added = new ArrayList<String>();
        }
        if (e.getActionCommand().equals("Start/Stop")) {
            if (running) {
                threadend = false;
            } else {
                i = new check();
                i.start();
                running = true;
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem("Start/Stop", 0).setActionListener(this));
        menu.add(new MenuItem("Check", 0).setActionListener(this));
        menu.add(new MenuItem("Reset", 0).setActionListener(this));
        return menu;
    }

    public class check extends Thread {
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

    private int getWaittime() {
        return subConfig.getIntegerProperty("WAITTIME", 5) * 60000;
    }

    private synchronized void checkFolder() {
        boolean dabei = false;
        File folder = new File(subConfig.getStringProperty("FOLDER", JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY)));
        if (folder.isDirectory()) {
            String[] help = folder.list();

            for (int i = 0; i < help.length; i++) {
                if (help[i].toLowerCase().endsWith(".dlc") || help[i].toLowerCase().endsWith(".ccf") || help[i].toLowerCase().endsWith(".rsdf")) {
                    File container = new File(folder, help[i]);
                    try{
                    for (int j = 0; j < added.size(); j++) {
                        if (container.getAbsolutePath().equals(added.get(j))) {
                            dabei = true;
                            break;
                        }
                    }
                    if (!dabei) {
                        JDUtilities.getController().loadContainerFile(container);
                        added.add(container.getAbsolutePath());
                        //Buggy
//                        if (subConfig.getBooleanProperty("DELETE", true)) {
//                            container.delete();
//                        }
                    }
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void onExit() {
        // TODO Auto-generated method stub

    }
}