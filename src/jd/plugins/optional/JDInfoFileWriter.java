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
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.gui.swing.components.JDTextArea;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nutils.io.JDIO;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginOptional;
import jd.utils.JDTheme;
import jd.utils.Replacer;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "infofilewriter", interfaceversion = 5)
public class JDInfoFileWriter extends PluginOptional {

    private static final String JDL_PREFIX = "jd.plugins.optional.JDInfoFileWriter.";

    private static final String FILENAME_DEFAULT = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";

    /**
     * Usually overridden by localization
     */
    private static final String INFO_STRING_DEFAULT = JDL.L("plugins.optional.infofilewriter.contentdefault", "Comment: %LAST_FINISHED_PACKAGE.COMMENT%\r\nPassword: %LAST_FINISHED_PACKAGE.PASSWORD%\r\nAuto-Password: %LAST_FINISHED_PACKAGE.AUTO_PASSWORD%\r\n%LAST_FINISHED_PACKAGE.FILELIST%\r\nFinalized %SYSTEM.DATE% to %SYSTEM.TIME% Clock");

    private static final String PARAM_CREATION = "CREATION";

    private static final String PARAM_FILENAME = "FILENAME";

    private static final String PARAM_INFO_STRING = "INFO_STRING";

    private static final String PARAM_CREATE_FILE = "CREATE_FILE";

    private static final String PARAM_ONLYPASSWORD = "ONLYPASSWORD";

    private ConfigEntry cmbVars;

    private ConfigEntry txtInfo;

    private SubConfiguration subConfig = null;

    public JDInfoFileWriter(PluginWrapper wrapper) {
        super(wrapper);

        subConfig = SubConfiguration.getConfig("JDInfoFileWriter");

        initConfig();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onControlEvent(ControlEvent event) {
        switch (event.getID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getSource() instanceof PluginForHost)) return;

            DownloadLink dl = ((SingleDownloadController) event.getParameter()).getDownloadLink();

            if (subConfig.getBooleanProperty(PARAM_ONLYPASSWORD, false)) {
                // only set if password is availale
                if ((dl.getFilePackage().getPassword() == null || dl.getFilePackage().getPassword().trim().length() == 0) && (dl.getFilePackage().getPasswordAuto() == null || dl.getFilePackage().getPasswordAuto().size() == 0)) return;
            }
            if (subConfig.getIntegerProperty(PARAM_CREATION, 0) == 0) {
                FilePackage fp = dl.getFilePackage();
                if (fp.getRemainingLinks() == 0 && fp.getBooleanProperty(PARAM_CREATE_FILE, true)) {
                    writeInfoFile(dl);
                }
            } else {
                if (dl.getBooleanProperty(PARAM_CREATE_FILE, true)) {
                    writeInfoFile(dl);
                }
            }
            break;
        case ControlEvent.CONTROL_LINKLIST_CONTEXT_MENU:
            if (event.getSource() instanceof DownloadLink && subConfig.getIntegerProperty(PARAM_CREATION, 0) == 0) return;
            if (event.getSource() instanceof FilePackage && subConfig.getIntegerProperty(PARAM_CREATION, 0) == 1) return;

            final Property obj = (Property) event.getSource();
            final MenuAction m = new MenuAction(JDL.L(JDL_PREFIX + "createInfoFile", "Create Info File"), 1337);
            m.setIcon(this.getIconKey());
            m.setSelected(obj.getBooleanProperty(PARAM_CREATE_FILE, true));
            m.setActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    obj.setProperty(PARAM_CREATE_FILE, m.isSelected());
                }

            });

            ArrayList<MenuAction> items = (ArrayList<MenuAction>) event.getParameter();
            items.add(m);
            break;
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
        config.setGroup(new ConfigGroup(getHost(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, PARAM_CREATION, new String[] { JDL.L(JDL_PREFIX + "packages", "packages"), JDL.L(JDL_PREFIX + "downloadlinks", "downloadlinks") }, "Create info file for complete ...").setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_ONLYPASSWORD, JDL.L("plugins.optional.infofilewriter.onlywithpassword", "Use only if password is enabled")).setDefaultValue(false));

        config.addEntry(cmbVars = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, "VARS", Replacer.getKeyList(), JDL.L("plugins.optional.infofilewriter.variables", "Available variables")));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L("plugins.optional.infofilewriter.insertKey.short", "Insert"), JDL.L("plugins.optional.infofilewriter.insertKey", "Insert selected Key into the Content"), JDTheme.II("gui.icons.paste", 16, 16)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PARAM_FILENAME, JDL.L("plugins.optional.infofilewriter.filename", "Filename:")).setDefaultValue(FILENAME_DEFAULT));
        config.addEntry(txtInfo = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PARAM_INFO_STRING, JDL.L("plugins.optional.infofilewriter.content", "Content:")).setDefaultValue(INFO_STRING_DEFAULT));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        int index = Integer.parseInt(cmbVars.getGuiListener().getText().toString());
        if (index < 0) return;
        JDTextArea txt = ((JDTextArea) ((GUIConfigEntry) txtInfo.getGuiListener()).getInput());
        txt.insert("%" + Replacer.getKey(index) + "%", txt.getCaretPosition());
    }

    @Override
    public void onExit() {
    }

    private void writeInfoFile(DownloadLink lastDownloadFinished) {
        String filename = Replacer.insertVariables(subConfig.getStringProperty(PARAM_FILENAME, FILENAME_DEFAULT), lastDownloadFinished);
        File dest = new File(filename);

        try {
            if (dest.createNewFile() && dest.canWrite()) {
                String content = Replacer.insertVariables(subConfig.getStringProperty(PARAM_INFO_STRING, INFO_STRING_DEFAULT), lastDownloadFinished);

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

    @Override
    public String getIconKey() {
        return "gui.images.list";
    }

}