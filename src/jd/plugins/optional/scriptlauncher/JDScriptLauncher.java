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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
    private ArrayList<MenuAction> menuitems = new ArrayList<MenuAction>();;

    public JDScriptLauncher(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public boolean initAddon() {
        initScripts();
        initMenuActions();
        logger.info("Script Launcher OK");
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (event.getID() >= 1000) {
            try {
                Runtime.getRuntime().exec(scriptdir + event.getActionCommand());
            } catch (IOException e) {
                logger.warning(e.toString());
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

    private void initMenuActions() {
        MenuAction ma = null;

        for (int i = 0; i < this.scripts.size(); i++) {
            String scriptname = this.scripts.get(i).getName();
            ma = new MenuAction(scriptname, 1000 + i);
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