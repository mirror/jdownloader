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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.parser.Regex;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision: 9816 $", id = "proxyrotation", interfaceversion = 5)
public class ProxyRotation extends PluginOptional implements ControlListener {

    private static final String PARAM_PROXYLIST = "PARAM_PROXYLIST";
    private SubConfiguration cfg;

    public ProxyRotation(PluginWrapper wrapper) {
        super(wrapper);
        cfg = SubConfiguration.getConfig("PROXYROTATION");
        initConfig();
    }

    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);

        if (event.getID() == ControlEvent.CONTROL_RECONNECT_REQUEST) {
            try {
                String[] lines = Regex.getLines(cfg.getStringProperty(PARAM_PROXYLIST, ""));
                // get next line in list;
                String next = null;
                int i;
                for (i = 0; i < lines.length; i++) {
                    if (!lines[i].trim().startsWith("#")) {
                        next = lines[i];
                        break;
                    }
                }
                if (next == null) return;
                // resort proxylist;
                // current proxy to end of list.
                StringBuilder sb = new StringBuilder();
                for (int x = i + 1; x < lines.length; x++) {
                    sb.append(lines[x]);
                    sb.append("\r\n");
                }
                for (int x = 0; x <= i; x++) {
                    sb.append(lines[x]);
                    sb.append("\r\n");
                }
                cfg.setProperty(PARAM_PROXYLIST, sb.toString());
                cfg.save();
                // Pasre next prpxy line NOW
                i = next.indexOf("://");
                if (i < 4) { throw new Exception("Proxylist Format error in " + next); }
                String type = next.substring(0, i);
                String password = null;
                String username = null;
                String ip = null;
                String port = null;
                next = next.substring(i + 3);
                if (next.contains("@")) {
                    i = next.indexOf("@");
                    String logins = next.substring(0, i);
                    next = next.substring(i + 1);

                    if (logins.contains(":")) {
                        String[] splitted = logins.split(":");
                        username = splitted[0];
                        password = splitted[1];
                    } else {
                        username = logins;
                    }
                }
                String[] splitted = next.split(":");
                ip = splitted[0];
                if (splitted.length > 1) {
                    port = splitted[1];
                } else {
                    port = "8080";
                }

                JDProxy proxy = new JDProxy(type.equalsIgnoreCase("socks") ? JDProxy.Type.SOCKS : JDProxy.Type.HTTP, ip, Integer.parseInt(port));

                proxy.setUser(username);
                proxy.setPass(password);
                JDLogger.getLogger().info("Use Proxy: "+proxy);
                Browser.setGlobalProxy(proxy);
                //force the Reconnecter to verify IP again
                ((Property)event.getParameter()).setProperty(Reconnecter.VERFIFY_IP_AGAIN,true);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public boolean initAddon() {
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    public void initConfig() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, cfg, PARAM_PROXYLIST, JDL.L("jd.plugins.optional.ProxyRotation.initConfig", "ProxyList:")).setDefaultValue("#http://user:password@ip:port\r\n#socks://user:password@ip:port\r\n"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }

    @Override
    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

}