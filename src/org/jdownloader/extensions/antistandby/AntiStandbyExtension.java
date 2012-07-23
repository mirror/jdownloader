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

import jd.plugins.AddonPanel;

import org.appwork.utils.os.CrossSystem;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.antistandby.translate.AntistandbyTranslation;

public class AntiStandbyExtension extends AbstractExtension<AntiStandbyConfig, AntistandbyTranslation> {

    private Thread                                     asthread = null;
    private ExtensionConfigPanel<AntiStandbyExtension> configPanel;

    public ExtensionConfigPanel<AntiStandbyExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public AntiStandbyExtension() throws StartException {
        super();
        setTitle(_.jd_plugins_optional_antistandby_jdantistandby());

    }

    public boolean isLinuxRunnable() {
        return false;
    }

    @Override
    public boolean isDefaultEnabled() {
        return true;
    }

    @Override
    protected void stop() throws StopException {
        if (asthread != null) {
            asthread.interrupt();
            asthread = null;
        }
    }

    public boolean isQuickToggleEnabled() {
        return true;
    }

    @Override
    protected void start() throws StartException {
        if (CrossSystem.isWindows()) {
            asthread = new WindowsAntiStandby(this);
            asthread.start();
        } else if (CrossSystem.isMac()) {
            asthread = new MacAntiStandBy(this);
            asthread.start();

        }

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

        configPanel = new AntistandbyConfigPanel(this);
    }

    public Mode getMode() {
        Mode ret = getSettings().getMode();
        if (ret == null) ret = Mode.DOWNLOADING;
        return ret;
    }

}