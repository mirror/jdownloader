//    jDownloader - Downloadmanager
//    Copyright (C) 2010  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.scriptlauncher;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.config.ConfigContainer;
import jd.controlling.JDLogger;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.interfaces.RemoteSupport;
import org.jdownloader.extensions.remotecontrol.helppage.HelpPage;
import org.jdownloader.extensions.remotecontrol.helppage.Table;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ScriptLauncherExtension extends AbstractExtension implements RemoteSupport, ActionListener {

    private static final String              scriptdir    = "./scripts/";

    private ArrayList<File>                  scripts;
    private static HashMap<Integer, Process> processlist  = new HashMap<Integer, Process>();

    private ArrayList<MenuAction>            menuitems    = new ArrayList<MenuAction>();
    private ArrayList<String>                scriptconfig = new ArrayList<String>();

    private static final String              ADD_CHECKBOX = "ADD_CHECKBOX";

    public ExtensionConfigPanel<ScriptLauncherExtension> getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public ScriptLauncherExtension() throws StartException {
        super(JDL.L("jd.plugins.optional.scriptlauncher.jdscriptlauncher", null));
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getID() >= 1000) {
            int index = event.getID() - 1000;

            if (ScriptLauncherExtension.processlist.get(index) == null) {
                launch(index);
            }
        }
    }

    private static void launch(int index) {
        ArrayList<File> scripts = ScriptLauncherExtension.getScripts();

        try {
            String exepath = scripts.get(index).getPath();
            Process p = Runtime.getRuntime().exec(exepath);
            ScriptLauncherExtension.processlist.put(index, p);

            JDLogger.getLogger().info("\"" + exepath + "\" has been executed");
        } catch (IOException eio) {
            JDLogger.getLogger().warning(eio.toString());
        }
    }

    public static boolean launch(String name) {
        Integer index = ScriptLauncherExtension.getScriptIndexByName(name);

        if (index != null) {
            ScriptLauncherExtension.launch(index);
            return true;
        }

        return false;
    }

    private static Integer getScriptIndexByName(String name) {
        Integer index = null;

        int i = 0;
        for (File script : ScriptLauncherExtension.getScripts()) {
            String scriptname = script.getName().split("\\.")[0];

            if (scriptname.equals(name)) {
                index = i;
                break;
            }

            i++;
        }

        return index;
    }

    private ArrayList<String> readScriptConfiguration(int index) throws IOException {
        FileReader fr = new FileReader(scripts.get(index));
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

        for (int i = 0; i < scripts.size(); i++) {
            ArrayList<String> launcherprops = readScriptConfiguration(i);
            String scriptname = scripts.get(i).getName().split("\\.")[0];
            ma = new MenuAction(scriptname, i + 1000);

            if (launcherprops.contains(ADD_CHECKBOX)) {
                ma.setSelected(false);
            }

            menuitems.add(ma);
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
                    JDLogger.getLogger().info("\"" + file.getName() + "\" has been found");
                    scripts.add(file);
                }
            }
        } else {
            dir.mkdir();
        }

        return scripts;
    }

    public Object handleRemoteCmd(String cmd) {
        Document xmlDocument = JDUtilities.parseXmlString("<jdownloader></jdownloader>", false);

        if (cmd.matches("(?is).*/addon/scriptlauncher/getlist")) {
            for (File script : getScripts()) {
                Element element = xmlDocument.createElement("script");
                xmlDocument.getFirstChild().appendChild(element);
                element.setAttribute("name", script.getName().split("\\.")[0]);

                element = xmlDocument.createElement("absolutePath");
                xmlDocument.getFirstChild().appendChild(element);
                element.setTextContent(script.getAbsolutePath());

                return xmlDocument;
            }

        } else if (cmd.matches("(?is).*/addon/scriptlauncher/launch/.+")) {
            String scriptname = new Regex(cmd, "(?is).*/addon/scriptlauncher/launch/(.+)").getMatch(0);

            if (launch(scriptname)) {
                return "Script " + scriptname + " has been executed.";
            } else {
                return "Script " + scriptname + " doesn't exist.";
            }
        }
        return null;
    }

    public void initCmdTable() {
        Table t = HelpPage.createTable(new Table(this.getName()));

        t.setCommand("/addon/scriptlauncher/getlist");
        t.setInfo("Get list of all available scripts.");

        t.setCommand("/addon/scriptlauncher/launch/%X%");
        t.setInfo("Launches a script on the remote machine via JDScriptLauncher addon.");
    }

    @Override
    protected void stop() throws StopException {
    }

    @Override
    protected void start() throws StartException {
        scripts = ScriptLauncherExtension.getScripts();

        try {
            initMenuActions();
        } catch (IOException e) {
            logger.warning(e.toString());
        }

        logger.info("ScriptLauncher OK");

    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return "scriptlauncher";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return JDL.L("jd.plugins.optional.scriptlauncher.jdscriptlauncher.description", null);
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        MenuAction ma;
        menu.add(ma = new MenuAction(getName(), 0));

        for (int i = 0; i < menuitems.size(); i++) {
            ma.addMenuItem(menuitems.get(i));
        }

        if (menuitems.size() == 0) {
            ma.addMenuItem(ma = new MenuAction(JDL.L("plugins.optional.JDScriptLauncher.noscripts", "No scripts were found."), 0));
            ma.setEnabled(false);
        }

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }
}