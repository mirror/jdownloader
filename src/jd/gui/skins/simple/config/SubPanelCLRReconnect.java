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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.router.FindRouterIP;
import jd.router.RouterInfoCollector;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class SubPanelCLRReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnFindIP;

    private GUIConfigEntry ip;

    private GUIConfigEntry pass;

    private GUIConfigEntry routerScript;

    private GUIConfigEntry user;

    public SubPanelCLRReconnect(Configuration configuration) {
        super();
        this.configuration = configuration;
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnFindIP) {
            new FindRouterIP(ip);
        }
    }

    @Override
    public void initPanel() {

        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));
        btnFindIP.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnFindIP, 2, 0, GridBagConstraints.REMAINDER, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(pass);
        ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")));
        addGUIConfigEntry(ip);
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, RouterInfoCollector.PROPERTY_SHOW_ROUTERINFO_DIALOG, JDLocale.L("gui.config.reconnect.showupload", "Show ReconnectInfo Upload Window")).setDefaultValue(JDUtilities.getConfiguration().getBooleanProperty(RouterInfoCollector.PROPERTY_SHOW_ROUTERINFO_DIALOG, true))));

        routerScript = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS_CLR, JDLocale.L("gui.config.clr.script", "CLR Script")).setInstantHelp("http://wiki.jdownloader.org/index.php?title=CLR"));
        this.entries.add(routerScript);

        add(panel, BorderLayout.NORTH);
        add(routerScript, BorderLayout.CENTER);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();
    }

}
