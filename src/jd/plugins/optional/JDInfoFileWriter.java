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

import javax.swing.JComboBox;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.gui.swing.components.JDTextArea;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.Replacer;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "infofilewriter", interfaceversion = 5)
public class JDInfoFileWriter extends PluginOptional {

    private static final String FILENAME_DEFAULT = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";

    /**
     * Usually overridden by localization
     */
    private static final String INFO_STRING_DEFAULT = JDL.L("plugins.optional.infofilewriter.contentdefault", "Comment: %LAST_FINISHED_PACKAGE.COMMENT%\r\nPassword: %LAST_FINISHED_PACKAGE.PASSWORD%\r\nAuto-Password: %LAST_FINISHED_PACKAGE.AUTO_PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFinalized %SYSTEM.DATE% to %SYSTEM.TIME% Clock");

    private static final String PARAM_FILENAME = "FILENAME";

    private static final String PARAM_INFO_STRING = "INFO_STRING";

    private static final long serialVersionUID = 7680205811276541375L;

    private ConfigEntry cmbVars;

    private ConfigEntry txtInfo;

    private SubConfiguration subConfig = null;

    public JDInfoFileWriter(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = SubConfiguration.getConfig("JDInfoFileWriter");
        initConfig();
    }

    @Override
    public void onControlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_PLUGIN_INACTIVE && event.getSource() instanceof PluginForHost) {
            // Nur Hostpluginevents auswerten
            DownloadLink lastDownloadFinished = ((SingleDownloadController) event.getParameter()).getDownloadLink();
            if (lastDownloadFinished.getFilePackage().getRemainingLinks() == 0) {
                writeInfoFile(lastDownloadFinished);
            }
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    public void initConfig() {
        config.addEntry(cmbVars = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX, subConfig, "VARS", Replacer.getKeyList(), JDL.L("plugins.optional.infofilewriter.variables", "Available variables")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L("plugins.optional.infofilewriter.insertKey.short", "Insert"), JDL.L("plugins.optional.infofilewriter.insertKey", "Insert selected Key into the Content"), JDTheme.II("gui.icons.paste", 16, 16)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PARAM_FILENAME, JDL.L("plugins.optional.infofilewriter.filename", "Filename:")).setDefaultValue(FILENAME_DEFAULT));
        config.addEntry(txtInfo = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PARAM_INFO_STRING, JDL.L("plugins.optional.infofilewriter.content", "Content:")).setDefaultValue(INFO_STRING_DEFAULT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JComboBox cmb = ((JComboBox) ((GUIConfigEntry) cmbVars.getGuiListener()).getInput());
        if (cmb.getSelectedIndex() < 0) return;
        JDTextArea txt = ((JDTextArea) ((GUIConfigEntry) txtInfo.getGuiListener()).getInput());
        txt.insert("%" + Replacer.getKey(cmb.getSelectedIndex()) + "%", txt.getCaretPosition());
    }

    @Override
    public void onExit() {
    }

    private void writeInfoFile(DownloadLink lastDownloadFinished) {
        String content = Replacer.insertVariables(subConfig.getStringProperty(PARAM_INFO_STRING, INFO_STRING_DEFAULT), lastDownloadFinished);
        String filename = Replacer.insertVariables(subConfig.getStringProperty(PARAM_FILENAME, FILENAME_DEFAULT), lastDownloadFinished);

        File dest = new File(filename);

        try {
            if (dest.createNewFile() && dest.canWrite()) {
                JDIO.writeLocalFile(dest, content);
                logger.severe("JDInfoFileWriter: info file " + dest.getAbsolutePath() + " successfully created");
            } else {
                logger.severe("JDInfoFileWriter: can not write to: " + dest.getAbsolutePath());
            }
        } catch (Exception e) {
            JDLogger.exception(e);
            logger.severe("JDInfoFileWriter: can not write to: " + dest.getAbsolutePath());
        }
    }
}