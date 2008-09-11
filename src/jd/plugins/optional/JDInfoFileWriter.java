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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.Replacer;

public class JDInfoFileWriter extends PluginOptional implements ControlListener {

    public static final String CODER = "JD-Team";

    private static final String FILENAME_DEFAULT = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";

    private static final String INFO_STRING_DEFAULT = "Passwort: %LAST_FINISHED_PACKAGE.PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFertig gestellt am %SYSTEM.DATE% um %SYSTEM.TIME% Uhr";

    private static final String PARAM_FILENAME = "FILENAME";

    private static final String PARAM_INFO_STRING = "INFO_STRING";

    private static final long serialVersionUID = 7680205811276541375L;

    public static final String VERSION = "$Revision$";

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    private SubConfiguration subConfig = JDUtilities.getSubConfig("JDInfoFileWriter");

    public JDInfoFileWriter(PluginWrapper wrapper) {
        super(wrapper);
        initConfig();
    }

    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        DownloadLink lastDownloadFinished;
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) { return; }
            lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                write(lastDownloadFinished);
            }
        }

    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.infoFileWriter.name", "Info File Writer");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        return true;

    }

    public void initConfig() {
        String[] keys = new String[Replacer.KEYS.length];
        for (int i = 0; i < Replacer.KEYS.length; i++) {
            keys[i] = "%" + Replacer.KEYS[i][0] + "%" + "   (" + Replacer.KEYS[i][1] + ")";
        }
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // subConfig, "INFOFILEWRITER_ENABLED",
        // JDLocale.L("plugins.optional.infoFileWriter.disable", "Infofilewriter
        // aktivieren")).setDefaultValue(false));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, "VARS", keys, JDLocale.L("plugins.optional.infoFileWriter.variables", "Available variables")));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PARAM_FILENAME, JDLocale.L("plugins.optional.infoFileWriter.filename", "Filename:")).setDefaultValue(FILENAME_DEFAULT));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PARAM_INFO_STRING, JDLocale.L("plugins.optional.infoFileWriter.content", "Content:")).setDefaultValue(INFO_STRING_DEFAULT));

    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    protected boolean write(Object arg) {

        // if
        // (!JDUtilities.getConfiguration().getBooleanProperty(
        // "INFOFILEWRITER_ENABLED",
        // false)) { return false; }

        String content = subConfig.getStringProperty(PARAM_INFO_STRING, INFO_STRING_DEFAULT);
        String filename = subConfig.getStringProperty(PARAM_FILENAME, FILENAME_DEFAULT);

        content = Replacer.insertVariables(content);

        filename = Replacer.insertVariables(filename);

        File dest = new File(filename);

        try {
            if (dest.createNewFile() && dest.canWrite()) {
                JDUtilities.writeLocalFile(dest, content);
                return true;
            } else {
                logger.severe("Can not write to: " + dest.getAbsolutePath());
                return false;
            }
        } catch (Exception e) {

            e.printStackTrace();
            logger.severe("Can not write2 to: " + dest.getAbsolutePath());
            return false;
        }

    }

}