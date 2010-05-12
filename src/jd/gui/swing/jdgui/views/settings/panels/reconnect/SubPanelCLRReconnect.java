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

package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.nrouter.RouterUtils;
import jd.utils.locale.JDL;

public class SubPanelCLRReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private ConfigEntry ip;

    public SubPanelCLRReconnect(Configuration configuration) {
        super();

        this.configuration = configuration;

        init();
        this.setBorder(null);
    }

    public void actionPerformed(ActionEvent e) {
        RouterUtils.findIP((GUIConfigEntry) ip.getGuiListener(), false);
    }

    @Override
    protected ConfigContainer setupContainer() {
        ConfigContainer container = new ConfigContainer();

        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDL.L("gui.config.liveHeader.btnfindip", "Fetch Router IP"), null, null));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDL.L("gui.config.liveheader.user", "Login User (->%%%user%%%)")));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDL.L("gui.config.liveheader.password", "Login Password (->%%%pass%%%)")));
        container.addEntry(ip = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDL.L("gui.config.liveheader.routerip", "RouterIP (->%%%routerip%%%)")));
        container.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS_CLR, null));

        return container;
    }

}
