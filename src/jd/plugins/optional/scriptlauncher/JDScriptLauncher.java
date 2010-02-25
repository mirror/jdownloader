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

package jd.plugins.optional.scriptlauncher;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.appwork.utils.Regex;

import jd.PluginWrapper;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "scriptlauncher", interfaceversion = 5)
public class JDScriptLauncher extends PluginOptional implements ControlListener {

    private String scriptdir = "./scripts/";
    private ArrayList<File> scripts = new ArrayList<File>();
    private ArrayList<MenuAction> menuitems = new ArrayList<MenuAction>();
    private ArrayList<Process> processlist = new ArrayList<Process>();

    private static final String LINUX_POLLING = "LINUX_POLLING";

    public JDScriptLauncher(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        initScripts();
        try {
            initMenuActions();
        } catch (IOException e) {
            logger.warning(e.toString());
        }
        logger.info("Script Launcher OK");
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getID() >= 1000) {
            int index = event.getID() - 1000;

            if (this.menuitems.get(index).isSelected() == true) {
                try {
                    Process p = Runtime.getRuntime().exec(this.scripts.get(index).getPath());
                    this.processlist.add(index, p);
                } catch (IOException e) {
                    logger.warning(e.toString());
                }
            } else {
                /* unix only */
                String[] cmd = { "/bin/sh", "-c", "killall " + this.scripts.get(index).getName() };
                try {
                    Runtime.getRuntime().exec(cmd);
                } catch (IOException e) {
                    logger.warning(e.toString());
                }
            }
        }
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        for (int i = 0; i < this.menuitems.size(); i++) {
            menu.add(this.menuitems.get(i));
        }

        if (menu.size() == 0) {
            MenuAction ma = new MenuAction(JDL.L("plugins.optional.JDScriptLauncher.noscripts", "No scripts were found."), 1);
            ma.setEnabled(false);
            menu.add(ma);
        }

        return menu;
    }

    private ArrayList<String> readLauncherProps(int index) throws IOException {
        ArrayList<String> launcherprops = new ArrayList<String>();
        FileReader fr = new FileReader(this.scripts.get(index));
        BufferedReader in = new BufferedReader(fr);
        String line = "";

        while ((line = in.readLine()) != null) {
            if (line.matches("#___LAUNCHER_:[A-Z_]+")) {
                launcherprops.add(new Regex(line, "#___LAUNCHER_:([A-Z_]+)").getMatch(0));
            }
        }

        in.close();
        fr.close();

        return launcherprops;
    }

    private void initMenuActions() throws IOException {
        MenuAction ma = null;

        for (int i = 0; i < this.scripts.size(); i++) {
            ArrayList<String> launcherprops = readLauncherProps(i);
            String scriptname = this.scripts.get(i).getName().split("\\.")[0];
            ma = new MenuAction(scriptname, i + 1000);

            if (launcherprops.contains(LINUX_POLLING)) {
                ma.setSelected(false);
            }

            this.menuitems.add(ma);
            ma.setActionListener(this);
        }
    }

    private void initScripts() {
        File dir = new File(this.scriptdir);
        if (dir.isDirectory()) {
            File[] filelist = dir.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                File file = filelist[i];
                if (file.isFile() && file.canExecute()) {
                    logger.info("JDScriptLauncher: Loaded script \"" + file.getName() + "\"");
                    this.scripts.add(file);
                }
            }
        } else {
            dir.mkdir();
        }
    }
}