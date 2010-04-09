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

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.swing.jdgui.views.settings.ConfigPanel;
import jd.gui.swing.jdgui.views.settings.GUIConfigEntry;
import jd.router.FindRouterIP;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class SubPanelCLRReconnect extends ConfigPanel implements ActionListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private Configuration configuration;

    private JButton btnFindIP;

    private GUIConfigEntry ip;

    private GUIConfigEntry script;

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

        JPanel buttons = new JPanel();
        buttons.setLayout(new MigLayout("ins 0, wrap 3"));

        // JDUtilities.addToGridBag(panel, btnSelectRouter, 0, 0, 1, 1, 0, 1,
        // insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        btnFindIP = new JButton(JDL.L("gui.config.liveHeader.btnfindip", "Fetch Router IP"));
        btnFindIP.addActionListener(this);
        buttons.add(btnFindIP, "width 160!");

        panel.add(buttons, "spanx,gapleft 0,gaptop 10");
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_USER, JDL.L("gui.config.liveheader.user", "Login User (->%%%user%%%)"))));
        addGUIConfigEntry(new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, configuration, Configuration.PARAM_HTTPSEND_PASS, JDL.L("gui.config.liveheader.password", "Login Password (->%%%pass%%%)"))));
        addGUIConfigEntry(ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, configuration, Configuration.PARAM_HTTPSEND_IP, JDL.L("gui.config.liveheader.routerip", "RouterIP (->%%%routerip%%%)"))));

        panel.add(new JScrollPane((script = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, configuration, Configuration.PARAM_HTTPSEND_REQUESTS_CLR, JDL.L("gui.config.liveheader.script", "HTTP Script")))).getInput()[0]), "gaptop 10,spanx,gapright 20,pushy, growy");

        script.setData(configuration.getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR));
        // routerScript = new GUIConfigEntry();
        // this.entries.add(routerScript);

        add(panel);
        // add(routerScript);
    }

    @Override
    public void saveSpecial() {
        configuration.setProperty(Configuration.PARAM_HTTPSEND_REQUESTS_CLR, script.getText());
    }

}
