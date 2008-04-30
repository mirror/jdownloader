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
import jd.event.ControlListener;
import jd.plugins.PluginOptional;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;

public class JDFolderWatch extends PluginOptional implements ControlListener {
    private boolean threadend = true;
    private boolean running = true;
    private check i;
    private ArrayList<String> added = new ArrayList<String>();
    
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
        return JDLocale.L("plugins.optional.folderwatch.name","FolderWatch");
    }

    @Override
    public String getVersion() {
        return "0.0.0.1";
    }

    @Override
    public void enable(boolean enable) throws Exception {
        if(JDUtilities.getJavaVersion() >= 1.5) {
            if (enable) {
                logger.info("FolderWatch OK");
                ConfigEntry cfg;
                config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFOLDER, getProperties(), "FOLDER", JDLocale.L("plugins.optional.folderwatch.folder", "Folder:")));
                cfg.setDefaultValue(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY));
                config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), "WAITTIME", JDLocale.L("plugins.optional.folderwatch.waittime","Waittime"), 1, 60).setDefaultValue(5));
                cfg.setDefaultValue("5");
                config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, getProperties(), "DELETE", JDLocale.L("plugins.optional.folderwatch.delete", "Delete")));
                cfg.setDefaultValue(true);
                i = new check();
                i.start();
            }
        }
        else {
            logger.severe("Error initializing FolderWatch");
        }
    }
    
    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("Check")) {
            checkFolder();
        }
        if(e.getActionCommand().equals("Reset")) {
            added = new ArrayList<String>();
        }
        if(e.getActionCommand().equals("Start/Stop")) {
            if(running) {
                threadend = false;
            }
            else {
                i = new check();
                i.start();
                running = true;
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        menu.add(new MenuItem("Start/Stop",0).setActionListener(this));
        menu.add(new MenuItem("Check",0).setActionListener(this));
        menu.add(new MenuItem("Reset",0).setActionListener(this));
        return menu;
    }
    
    public class check extends Thread {
        public void run() {
            threadend = true;
            while(threadend) {
                checkFolder();
                try {
                    Thread.sleep(getWaittime());
                }
                catch(InterruptedException e) {}
            }
            running = false;
        };
    }
    
    private int getWaittime() {
        return this.getProperties().getIntegerProperty("WAITTIME", 5) * 60000;
    }
    
    private void checkFolder() {
        boolean dabei = false;
        File folder = new File(this.getProperties().getStringProperty("FOLDER", JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY)));
        if(folder.isDirectory()) {
            String[] help = folder.list();
            
            for(int i=0; i<help.length; i++) {
                if(help[i].endsWith(".dlc") || help[i].endsWith(".ccf") || help[i].endsWith(".rsdf")) {
                    File container = new File(folder, help[i]);
                    for(int j=0; j<added.size(); j++) {
                        if(container.getAbsolutePath().equals(added.get(i))) {
                            dabei = true;
                            break;
                        }
                    }
                    if(!dabei) {
                        JDUtilities.getController().loadContainerFile(container);
                        added.add(container.getAbsolutePath());
                        if(this.getProperties().getBooleanProperty("DELETE", true)) {
                            container.delete();
                        }
                    }
                }
            }
        }
    }
}