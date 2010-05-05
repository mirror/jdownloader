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

package jd.plugins.optional.routersend;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.DefaultListCellRenderer;

import jd.PluginWrapper;
import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.ReconnectMethod;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", defaultEnabled = true, id = "routersend", interfaceversion = 5)
public class SendRouter extends PluginOptional {
    private static final String JDL_PREFIX = "jd.plugins.optional.SendRouter.";
    private static final boolean DEBUG = false;
    private String scripturl = "http://jdownloader.org:8081/advert/routerdb/index.php";
    /**
     * List separated by ','
     */
    private String manufactururl = "http://jdownloader.org:8081/advert/routerdb/hersteller";
    private String currentScript = "";
    private int reconnectCounter = 0;
    private Boolean send = false;

    public SendRouter(PluginWrapper wrapper) {
        super(wrapper);

    }

    @Override
    public boolean initAddon() {
        reconnectCounter = getPluginConfig().getIntegerProperty("ReconnectCounter", 0);
        currentScript = getPluginConfig().getStringProperty("CurrentScript", "");
        send = getPluginConfig().getBooleanProperty("send", false);
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    @Override
    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_AFTER_RECONNECT) {
            if (JDUtilities.getConfiguration().getIntegerProperty(ReconnectMethod.PARAM_RECONNECT_TYPE, ReconnectMethod.LIVEHEADER) == ReconnectMethod.LIVEHEADER) {
                reconnectCounter++;
                getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
                getPluginConfig().save();
                if ((reconnectCounter > 5 && check()) || DEBUG) {
                    new Thread() {
                        public void run() {
                            executeSend();
                        }
                    }.start();

                }
            }
        }
        super.controlEvent(event);
    }

    private void executeSend() {
        int ret = UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L(JDL_PREFIX + "info.topic", "Help to Improve JD"), JDL.L(JDL_PREFIX + "info.msg", "THIS IS A BETATEST! JD has detected that you had 5 successfull reconnects.\r\nYou now can send the script to our server so we can include it permanently in JD"), UserIO.getInstance().getIcon(UserIO.ICON_INFO), null, null);
        if (!UserIO.isOK(ret)) return;
        if (submitData()) {
            UserIO.getInstance().requestMessageDialog(JDL.L(JDL_PREFIX + "send.successfull", "Thank you for your help"));
            send = true;
            getPluginConfig().setProperty("send", send);
            getPluginConfig().save();
        } else {
            UserIO.getInstance().requestMessageDialog(JDL.L(JDL_PREFIX + "send.failed", "A error occured while sending your data.\r\n We will ask you again later."));
            reconnectCounter = 0;
            getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
            getPluginConfig().save();
        }
    }

    private boolean check() {
        if (!currentScript.equals(JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS))) {
            currentScript = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
            send = false;
            reconnectCounter = 0;
            // TODO auch aktuelle passwörter abfragen und abspeichern

            getPluginConfig().setProperty("CurrentScript", currentScript);
            getPluginConfig().setProperty("send", send);
            getPluginConfig().setProperty("ReconnectCounter", reconnectCounter);
            getPluginConfig().save();
            return false;
        }
        return true;
    }

    private boolean submitData() {
        Form data = new Form();
        data.setMethod(Form.MethodType.POST);
        data.setAction(scripturl);
        String[] manufacturerList;
        ArrayList<String> man = new ArrayList<String>();
        String other;
        try {
            manufacturerList = br.getPage(manufactururl).split(",");
            for (String m : manufacturerList)
                man.add(m);

            Collections.sort(man);

            man.add(0, other = JDL.L(JDL_PREFIX + "others", "Other(Enter name)"));
        } catch (IOException e) {
            JDLogger.exception(e);
            return false;
        }
        // Collect data
        int selection = UserIO.getInstance().requestComboDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "manufacturer.list", "Manufacturer"), JDL.L(JDL_PREFIX + "manufacturer.message", "Which manufacturer?"), man.toArray(new String[] {}), 0, null, JDL.L(JDL_PREFIX + "ok", "OK"), null, new DefaultListCellRenderer());
        String manufacturer = man.get(selection);
        if (manufacturer.equals(other)) {
            manufacturer = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "manufacturer.routermanufacturer", "Your router's manufacturer?"), "");
            // TODO: ask google
        }

        String routerName = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "manufacturer.name", "Router name?"), "");
        // TODO: ask google

        // TODO: prepare script
        // TODO: first search for username (See check() comments) in script an
        // replace with %%%username%%%
        // TODO: then search passwort and replace with %%%username%%%
        // TODO: then detect the users router's ip and replace it with
        // %%%routerip%%%

        // TODO: perhaps the user has not entered his routerpassword...ask him
        String routerlogins = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "password.name", "Router's Username?\r\nIt will NOT be uploaded. We need it to remove it from the script."), "");
        String routerpassword = UserIO.getInstance().requestInputDialog(UserIO.NO_COUNTDOWN | UserIO.NO_CANCEL_OPTION, JDL.L(JDL_PREFIX + "password.password", "Router's Password?\r\nIt will NOT be uploaded. We need it to remove it from the script."), "");

        if (routerlogins != null) currentScript = currentScript.replace(routerlogins, "%%%username%%%");
        if (routerpassword != null) currentScript = currentScript.replace(routerpassword, "%%%password%%%");
        // Send
        InputField hersteller = new InputField("hersteller", Encoding.urlEncode(manufacturer));
        InputField name = new InputField("name", Encoding.urlEncode(routerName));
        InputField script = new InputField("script", Encoding.urlEncode(currentScript));
        data.addInputField(hersteller);
        data.addInputField(name);
        data.addInputField(script);
        try {
            br.submitForm(data);
            if (br.toString().contains("2")) return true;
        } catch (Exception e) {
            JDLogger.exception(e);

            return false;
        }
        return true;
    }
}
