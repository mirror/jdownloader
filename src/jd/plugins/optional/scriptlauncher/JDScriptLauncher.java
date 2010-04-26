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
import java.util.HashMap;

import jd.PluginWrapper;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

@OptionalPlugin(rev = "$Revision$", id = "scriptlauncher", interfaceversion = 5)
public class JDScriptLauncher extends PluginOptional implements ControlListener {

    private static final String scriptdir = "./scripts/";

    private ArrayList<File> scripts;
    private static HashMap<Integer, Process> processlist = new HashMap<Integer, Process>();

    private ArrayList<MenuAction> menuitems = new ArrayList<MenuAction>();
    private ArrayList<String> scriptconfig = new ArrayList<String>();

    private static final String LINUX_INFINITE_LOOP = "LINUX_INFINITE_LOOP";
    private static final String ADD_CHECKBOX = "ADD_CHECKBOX";

    public JDScriptLauncher(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        this.scripts = JDScriptLauncher.getScripts();

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

            if (JDScriptLauncher.processlist.get(index) == null) {
                launch(index);
            } else if (scriptconfig.contains(LINUX_INFINITE_LOOP)) {
                kill(index);
            }
        }
    }

    private static void launch(int index) {
        ArrayList<File> scripts = JDScriptLauncher.getScripts();

        try {
            Process p = Runtime.getRuntime().exec(scripts.get(index).getPath());
            JDScriptLauncher.processlist.put(index, p);
        } catch (IOException eio) {
            logger.warning(eio.toString());
        }
    }

    public static boolean launch(String name) {
        Integer index = JDScriptLauncher.getScriptIndexByName(name);

        if (index != null) {
            JDScriptLauncher.launch(index);
            return true;
        }

        return false;
    }

    /**
     * UNIX-only method (killing processes)
     */
    private static void kill(int index) {
        String[] cmd = { "/bin/sh", "-c", "killall " + JDScriptLauncher.getScripts().get(index).getName() };

        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            logger.warning(e.toString());
        }
    }

    public static boolean kill(String name) {
        Integer index = JDScriptLauncher.getScriptIndexByName(name);

        if (index != null) {
            JDScriptLauncher.kill(index);
            return true;
        }

        return false;
    }

    private static Integer getScriptIndexByName(String name) {
        Integer index = null;

        int i = 0;
        for (File script : JDScriptLauncher.getScripts()) {
            String scriptname = script.getName().split("\\.")[0];

            if (scriptname.equals(name)) {
                index = i;
                break;
            }

            i++;
        }

        return index;
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

    private ArrayList<String> readScriptConfiguration(int index) throws IOException {
        FileReader fr = new FileReader(this.scripts.get(index));
        BufferedReader in = new BufferedReader(fr);
        String line = "";

        while ((line = in.readLine()) != null) {
            if (line.matches(".*?__LAUNCHER__:[A-Z_]+")) {
                scriptconfig.add(new Regex(line, ".*?__LAUNCHER__:([A-Z_]+)").getMatch(0));
            }
        }

        in.close();
        fr.close();

        return scriptconfig;
    }

    private void initMenuActions() throws IOException {
        MenuAction ma = null;

        for (int i = 0; i < this.scripts.size(); i++) {
            ArrayList<String> launcherprops = readScriptConfiguration(i);
            String scriptname = this.scripts.get(i).getName().split("\\.")[0];
            ma = new MenuAction(scriptname, i + 1000);

            if (launcherprops.contains(ADD_CHECKBOX)) {
                ma.setSelected(false);
            }

            this.menuitems.add(ma);
            ma.setActionListener(this);
        }
    }

    public static ArrayList<File> getScripts() {
        ArrayList<File> scripts = new ArrayList<File>();
        File dir = new File(scriptdir);

        if (dir.isDirectory()) {
            File[] filelist = dir.listFiles();
            for (int i = 0; i < filelist.length; i++) {
                File file = filelist[i];
                if (file.isFile() && file.canExecute()) {
                    logger.info("JDScriptLauncher: Loaded script \"" + file.getName() + "\"");
                    scripts.add(file);
                }
            }
        } else {
            dir.mkdir();
        }

        return scripts;
    }
}