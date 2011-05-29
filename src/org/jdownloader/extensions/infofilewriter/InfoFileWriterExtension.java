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

package org.jdownloader.extensions.infofilewriter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Property;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.JSonWrapper;
import jd.controlling.SingleDownloadController;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.components.JDTextArea;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nutils.io.JDIO;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForHost;
import jd.utils.Replacer;
import jd.utils.StringUtil;

import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.infofilewriter.translate.T;
import org.jdownloader.images.NewTheme;

public class InfoFileWriterExtension extends AbstractExtension<InfoFileWriterConfig> implements ActionListener, ControlListener {

    private static final String                           FILENAME_DEFAULT    = "%LAST_FINISHED_PACKAGE.DOWNLOAD_DIRECTORY%/%LAST_FINISHED_PACKAGE.PACKAGENAME%.info";

    /**
     * Usually overridden by localization
     */
    private static final String                           INFO_STRING_DEFAULT = T._.plugins_optional_infofilewriter_contentdefault();

    private static final String                           PARAM_CREATION      = "CREATION";

    private static final String                           PARAM_FILENAME      = "FILENAME";

    private static final String                           PARAM_INFO_STRING   = "INFO_STRING";

    private static final String                           PARAM_CREATE_FILE   = "CREATE_FILE";

    private static final String                           PARAM_ONLYPASSWORD  = "ONLYPASSWORD";

    private ConfigEntry                                   cmbVars;

    private ConfigEntry                                   txtInfo;

    private JSonWrapper                                   subConfig           = null;

    private ExtensionConfigPanel<InfoFileWriterExtension> configPanel;

    public ExtensionConfigPanel<InfoFileWriterExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public InfoFileWriterExtension() throws StartException {
        super(T._.jd_plugins_optional_jdinfofilewriter());

        subConfig = JSonWrapper.get("JDInfoFileWriter");

    }

    @SuppressWarnings("unchecked")
    public void controlEvent(ControlEvent event) {
        switch (event.getEventID()) {
        case ControlEvent.CONTROL_PLUGIN_INACTIVE:
            // Nur Hostpluginevents auswerten
            if (!(event.getCaller() instanceof PluginForHost)) return;

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
            if (event.getCaller() instanceof DownloadLink && subConfig.getIntegerProperty(PARAM_CREATION, 0) == 0) return;
            if (event.getCaller() instanceof FilePackage && subConfig.getIntegerProperty(PARAM_CREATION, 0) == 1) return;

            final Property obj = (Property) event.getCaller();
            final MenuAction m = new MenuAction("Create InfoFile", T._.jd_plugins_optional_JDInfoFileWriter_createInfoFile(), 1337) {

                private static final long serialVersionUID = 3571965392155396021L;

                @Override
                protected String createMnemonic() {
                    return null;
                }

                @Override
                protected String createAccelerator() {
                    return null;
                }

                @Override
                protected String createTooltip() {
                    return null;
                }
            };
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

    public void actionPerformed(ActionEvent e) {
        int index = Integer.parseInt(cmbVars.getGuiListener().getText().toString());
        if (index < 0) return;
        JDTextArea txt = ((JDTextArea) ((GUIConfigEntry) txtInfo.getGuiListener()).getInput());
        txt.insert("%" + Replacer.getKey(index) + "%", txt.getCaretPosition());
    }

    private void writeInfoFile(DownloadLink lastDownloadFinished) {
        String filename = Replacer.insertVariables(subConfig.getStringProperty(PARAM_FILENAME, FILENAME_DEFAULT), lastDownloadFinished);
        File dest = new File(filename);

        try {
            if (dest.createNewFile() && dest.canWrite()) {
                String rawContent = subConfig.getStringProperty(PARAM_INFO_STRING, INFO_STRING_DEFAULT);
                String content = Replacer.insertVariables(rawContent.replaceAll("(\r\n|\n)", StringUtil.LINE_SEPARATOR), lastDownloadFinished);

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
        return "list";
    }

    @Override
    protected void stop() throws StopException {
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        JDController.getInstance().addControlListener(this);
    }

    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, PARAM_CREATION, new String[] { T._.jd_plugins_optional_JDInfoFileWriter_packages(), T._.jd_plugins_optional_JDInfoFileWriter_downloadlinks() }, "Create info file for complete ...").setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, PARAM_ONLYPASSWORD, T._.plugins_optional_infofilewriter_onlywithpassword()).setDefaultValue(false));

        config.addEntry(cmbVars = new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, "VARS", Replacer.getKeyList(), T._.plugins_optional_infofilewriter_variables()));

        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, T._.plugins_optional_infofilewriter_insertKey_short(), T._.plugins_optional_infofilewriter_insertKey(), NewTheme.I().getIcon("paste", 16)));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, subConfig, PARAM_FILENAME, T._.plugins_optional_infofilewriter_filename()).setDefaultValue(FILENAME_DEFAULT));
        config.addEntry(txtInfo = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, subConfig, PARAM_INFO_STRING, T._.plugins_optional_infofilewriter_content()).setDefaultValue(INFO_STRING_DEFAULT));

    }

    @Override
    public String getConfigID() {
        return "infofilewriter";
    }

    @Override
    public String getAuthor() {
        return "JDTeam";
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_jdinfofilewriter_description();
    }

    @Override
    public AddonPanel<InfoFileWriterExtension> getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }

}