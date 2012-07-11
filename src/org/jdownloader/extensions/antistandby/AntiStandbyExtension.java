//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package org.jdownloader.extensions.antistandby;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.plugins.AddonPanel;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.antistandby.translate.AntistandbyTranslation;
import org.jdownloader.logging.LogController;

public class AntiStandbyExtension extends AbstractExtension<AntiStandbyConfig, AntistandbyTranslation> {

    private static final String                        CONFIG_MODE = "CONFIG_MODE2";
    private String[]                                   modes;

    private boolean                                    status;
    private JDAntiStandbyThread                        asthread    = null;
    private ExtensionConfigPanel<AntiStandbyExtension> configPanel;

    public boolean isStatus() {
        return status;
    }

    public int getMode() {
        return getPropertyConfig().getIntegerProperty(CONFIG_MODE, 0);
    }

    public ExtensionConfigPanel<AntiStandbyExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public AntiStandbyExtension() throws StartException {
        super();
        setTitle(_.jd_plugins_optional_antistandby_jdantistandby());
        modes = new String[] { _.gui_config_antistandby_whiledl(), _.gui_config_antistandby_whilejd() };

    }

    @Override
    protected void stop() throws StopException {
        if (asthread != null) {
            asthread.setRunning(false);
            asthread = null;
        }
    }

    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    protected void start() throws StartException {
        switch (CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
        case CrossSystem.OS_WINDOWS_2000:
        case CrossSystem.OS_WINDOWS_NT:
            asthread = new JDAntiStandbyThread(this);
            asthread.start();
        default:
            LogController.CL().fine("JDAntiStandby: System is not supported (" + CrossSystem.getOSString() + ")");
        }

    }

    protected void initSettings(ConfigContainer config) {
        config.setGroup(new ConfigGroup(getName(), "settings"));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPropertyConfig(), CONFIG_MODE, modes, _.gui_config_antistandby_mode()).setDefaultValue(0));

    }

    @Override
    public String getDescription() {
        return _.jd_plugins_optional_antistandby_jdantistandby_description();
    }

    @Override
    public AddonPanel<AntiStandbyExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }

}