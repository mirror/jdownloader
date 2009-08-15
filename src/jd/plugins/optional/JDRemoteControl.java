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

//
//    Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität zu erhöhen.
//

package jd.plugins.optional;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.MenuAction;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.JDLogger;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.http.IPCheck;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 4)
public class JDRemoteControl extends PluginOptional implements ControlListener {

    private static final String PARAM_PORT = "PORT";
    private static final String PARAM_ENABLED = "ENABLED";
    private final SubConfiguration subConfig;

    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);

        subConfig = getPluginConfig();
        initConfig();
    }

    public String getIconKey() {
        return "gui.images.network";
    }

    private class Serverhandler implements Handler {

        public void handle(Request request, Response response) {
            Document xml = JDUtilities.parseXmlString("<jdownloader></jdownloader>", false);

            response.setReturnType("text/html");
            response.setReturnStatus(Response.OK);

            // ---------------------------------------
            // Help
            // ---------------------------------------

            if (request.getRequestUrl().equals("/help")) {

                Vector<String> commandvec = new Vector<String>();
                Vector<String> infovector = new Vector<String>();

                commandvec.add(" ");
                infovector.add("<br /><b>Get Values:</b><br />&nbsp;");

                commandvec.add("/get/speed");
                infovector.add("Get current Speed");

                commandvec.add("/get/ip");
                infovector.add("Get IP");

                commandvec.add("/get/config");
                infovector.add("Get Config");

                commandvec.add("/get/version");
                infovector.add("Get Version");

                commandvec.add("/get/rcversion");
                infovector.add("Get RemoteControl Version");

                commandvec.add("/get/speedlimit");
                infovector.add("Get current Speedlimit");

                commandvec.add("/get/isreconnect");
                infovector.add("Get If Reconnect");

                commandvec.add("/get/downloads/currentcount");
                infovector.add("Get amount of current downloads");
                commandvec.add("/get/downloads/currentlist");
                infovector.add("Get Current Downloads List (XML)");

                commandvec.add("/get/downloads/allcount");
                infovector.add("Get amount of downloads in list");
                commandvec.add("/get/downloads/alllist");
                infovector.add("Get list of downloads in list (XML)");

                commandvec.add("/get/downloads/finishedcount");
                infovector.add("Get amount of finished Downloads");
                commandvec.add("/get/downloads/finishedlist");
                infovector.add("Get finished Downloads List (XML)");

                commandvec.add(" ");
                infovector.add("<br /><b>Actions:</b><br />&nbsp;");

                commandvec.add("/action/start");
                infovector.add("Start DLs");

                commandvec.add("/action/pause");
                infovector.add("Pause DLs");

                commandvec.add("/action/stop");
                infovector.add("Stop DLs");

                commandvec.add("/action/toggle");
                infovector.add("Toggle DLs");

                commandvec.add("/action/update/force(0|1)/");
                infovector.add("Do Webupdate <br />" + "force1 activates auto-restart if update is possible");

                commandvec.add("/action/reconnect");
                infovector.add("Do Reconnect");

                commandvec.add("/action/restart");
                infovector.add("Restart JD");

                commandvec.add("/action/shutdown");
                infovector.add("Shutdown JD");

                commandvec.add("/action/set/download/limit/%X%");
                infovector.add("Set Downloadspeedlimit %X%");

                commandvec.add("/action/set/download/max/%X%");
                infovector.add("Set max sim. Downloads %X%");

                commandvec.add("/action/add/links/grabber(0|1)/%X%");
                infovector.add("Add Links %X% to Grabber<br />" + "Options:<br />" + "grabber(0|1): Show/Hide LinkGrabber<br />" + "Sample:<br />" + "/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq http://tinyurl.com/4khvhn<br />" + "Don't forget Space between Links!");

                commandvec.add("/action/add/container/%X%");
                infovector.add("Add Container %X%<br />" + "Sample:<br />" + "/action/add/container/C:\\container.dlc");

                commandvec.add("/action/save/container/%X%");
                infovector.add("Save DLC-Container with all Links to %X%<br /> " + "Sample see /action/add/container/%X%");

                commandvec.add("/action/set/reconnectenabled/(true|false)");
                infovector.add("Set Reconnect enabled or not");

                commandvec.add("/action/set/premiumenabled/(true|false)");
                infovector.add("Set Use Premium enabled or not");

                response.addContent("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\"http://www.w3.org/TR/html4/strict.dtd\"><html><head><title>JDRemoteControl Help</title><style type=\"text/css\">a {    font-size: 14px;    text-decoration: none;    background: none;    color: #599ad6;}a:hover {    text-decoration: underline;    color:#333333;}body {    color: #333333;    background:#f0f0f0;    font-family: Verdana, Arial, Helvetica, sans-serif;    font-size: 14px;    vertical-align: top;  }</style></head><body><p><br /><b>JDRemoteControl " + getVersion() + "<br /><br />Usage:</b><br />&nbsp;<br />1)Replace %X% with your value<br />Sample: /action/save/container/C:\\backup.dlc <br />2)Replace (true|false) with true or false<br /><table border=\"0\" cellspacing=\"5\">");
                for (int commandcount = 0; commandcount < commandvec.size(); commandcount++) {
                    response.addContent("<tr><td valign=\"top\"><a href=\"http://127.0.0.1:" + subConfig.getIntegerProperty(PARAM_PORT, 10025) + commandvec.get(commandcount) + "\">" + commandvec.get(commandcount) + "</a></td><td valign=\"top\">" + infovector.get(commandcount) + "</td></tr>");
                }
                response.addContent("</table><br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;</p></body></html>");
            } else if (request.getRequestUrl().equals("/get/ip")) {
                // Get IP
                response.addContent(IPCheck.getIPAddress(null));
            } else if (request.getRequestUrl().equals("/get/config")) {
                // Get Config
                Property config = JDUtilities.getConfiguration();
                response.addContent("<pre>");
                if (request.getParameters().containsKey("sub")) {
                    config = SubConfiguration.getConfig(request.getParameters().get("sub").toUpperCase());
                }
                for (Entry<String, Object> next : config.getProperties().entrySet()) {
                    response.addContent(next.getKey() + " = " + next.getValue() + "\r\n");
                }
                response.addContent("</pre>");
            } else if (request.getRequestUrl().equals("/get/version")) {
                // Get Version
                response.addContent(JDUtilities.getJDTitle());
            } else if (request.getRequestUrl().equals("/get/rcversion")) {
                // Get RemoteControlVersion
                response.addContent(getVersion());
            } else if (request.getRequestUrl().equals("/get/speedlimit")) {
                // Get SpeedLimit
                response.addContent(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            } else if (request.getRequestUrl().equals("/get/downloads/currentcount")) {
                // Get Current DLs COUNT
                int counter = 0;
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            counter++;
                        }
                    }
                }
                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/currentlist")) {
                // Get Current DLs
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            fp_xml.appendChild(addDownloadLink(xml, dl));
                        }
                    }
                }
                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/allcount")) {
                // Get DLList COUNT
                int counter = 0;
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    counter += fp.getDownloadLinkList().size();
                }
                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/alllist")) {
                // Get DLList
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        fp_xml.appendChild(addDownloadLink(xml, dl));
                    }
                }
                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/finishedcount")) {
                // Get finished DLs COUNT
                int counter = 0;
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            counter++;
                        }
                    }
                }
                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/finishedlist")) {
                // Get finished DLs
                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                            fp_xml.appendChild(addDownloadLink(xml, dl));
                        }
                    }
                }
                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/speed")) {
                // Get current Speed
                response.addContent(JDUtilities.getController().getSpeedMeter() / 1000);
            } else if (request.getRequestUrl().equals("/get/isreconnect")) {
                // Get IsReconnect
                response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
            } else if (request.getRequestUrl().equals("/action/start")) {
                // Do Start Download
                JDUtilities.getController().startDownloads();

                response.addContent("Downloads started");
            } else if (request.getRequestUrl().equals("/action/pause")) {
                // Do Pause Download
                JDUtilities.getController().pauseDownloads(true);
                response.addContent("Downloads paused");
            } else if (request.getRequestUrl().equals("/action/stop")) {
                // Do Stop Download
                JDUtilities.getController().stopDownloads();
                response.addContent("Downloads stopped");
            } else if (request.getRequestUrl().equals("/action/toggle")) {
                // Do Toggle Download
                JDUtilities.getController().toggleStartStop();
                response.addContent("Downloads toggled");
            } else if (request.getRequestUrl().matches("[\\s\\S]*?/action/update/force[01]{1}/[\\s\\S]*")) {
                // Do Make Webupdate
                Integer force = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/update/force([01]{1})/[\\s\\S]*").getMatch(0));
                if (force == 1) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                    SubConfiguration.getConfig("WEBUPDATE").setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
                }

                new WebUpdate().doUpdateCheck(true, true);

                response.addContent("Do Webupdate...");
            } else if (request.getRequestUrl().equals("/action/reconnect")) {
                // Do Reconnect
                response.addContent("Do Reconnect...");

                Reconnecter.doManualReconnect();

            } else if (request.getRequestUrl().equals("/action/restart")) {
                // Do Restart JD
                response.addContent("Restarting...");

                new Thread(new Runnable() {

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {

                            JDLogger.exception(e);
                        }
                        JDUtilities.restartJD();
                    }

                }).start();
            } else if (request.getRequestUrl().equals("/action/shutdown")) {
                // Do Shutdown JD
                response.addContent("Shutting down...");

                new Thread(new Runnable() {

                    public void run() {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {

                            JDLogger.exception(e);
                        }
                        JDUtilities.getController().exit();
                    }

                }).start();
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/limit/[0-9]+.*")) {
                // Set Downloadlimit
                Integer newdllimit = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/download/limit/([0-9]+).*").getMatch(0));
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newlimit=" + newdllimit);
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/max/[0-9]+.*")) {
                // Set max. sim. Downloads
                Integer newsimdl = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/download/max/([0-9]+).*").getMatch(0));
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newmax=" + newsimdl);
            } else if (request.getRequestUrl().matches("(?is).*/action/add/links/grabber[01]{1}/start[01]{1}/[\\s\\S]+")) {
                // Add Link(s)
                String link = new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/grabber[01]{1}/start[01]{1}/(.*)").getMatch(0);
                if (request.getParameters().size() > 0) {
                    link += "?";
                    Iterator<String> it = request.getParameters().keySet().iterator();
                    while (it.hasNext()) {
                        String help = it.next();
                        link += help;
                        if (!request.getParameter(help).equals("")) {
                            link += "=" + request.getParameter(help);
                        }
                        if (it.hasNext()) link += "&";
                    }
                }
                // response.addContent(link);
                Integer showgrab = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/grabber([01]{1})/start[01]{1}/[\\s\\S]+").getMatch(0));
                Boolean hidegrabber = false;
                if (showgrab == 0) {
                    hidegrabber = true;
                }

                // response.addContent(startdl.toString());
                // link = Encoding.htmlDecode(link);
                // wegen leerzeichen etc, die ja in urls verändert werden...

                new DistributeData(link, hidegrabber).start();
                response.addContent("Link(s) added. (" + link + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/add/container/[\\s\\S]+")) {
                // Open DLC Container
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/add/container/([\\s\\S]+)").getMatch(0);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);
                // wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().loadContainerFile(new File(dlcfilestr));
                response.addContent("Container opened. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/save/container/[\\s\\S]+")) {
                // Save Linklist as DLC Container
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/save/container/([\\s\\S]+)").getMatch(0);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);
                // wegen leerzeichen etc, die ja in urls verändert werden...
                JDUtilities.getController().saveDLC(new File(dlcfilestr), JDUtilities.getDownloadController().getAllDownloadLinks());
                response.addContent("Container saved. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/set/reconnectenabled/.*")) {
                // Set ReconnectEnabled
                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/reconnectenabled/(.*)").getMatch(0));
                boolean disprc = newrc;
                newrc = !newrc;
                logger.fine("RemoteControl - Set ReConnect: " + disprc);
                if (newrc != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newrc);
                    JDUtilities.getConfiguration().save();

                    response.addContent("reconnect=" + disprc + " (CHANGED=true)");
                } else {
                    response.addContent("reconnect=" + disprc + " (CHANGED=false)");
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/set/premiumenabled/.*")) {
                // Set use premium
                boolean newuseprem = Boolean.parseBoolean(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/premiumenabled/(.*)").getMatch(0));
                logger.fine("RemoteControl - Set Premium: " + newuseprem);
                if (newuseprem != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                    JDUtilities.getConfiguration().save();

                    response.addContent("newprem=" + newuseprem + " (CHANGED=true)");
                } else {
                    response.addContent("newprem=" + newuseprem + " (CHANGED=false)");
                }

            } else {
                response.addContent("JDRemoteControl - Malformed Request. use /help");
            }
        }

        private Element addFilePackage(Document xml, FilePackage fp) {
            Element element = xml.createElement("package");
            xml.getFirstChild().appendChild(element);
            element.setAttribute("package_name", fp.getName());
            element.setAttribute("package_percent", f.format(fp.getPercent()));
            element.setAttribute("package_linksinprogress", fp.getLinksInProgress() + "");
            element.setAttribute("package_linkstotal", fp.size() + "");
            element.setAttribute("package_ETA", Formatter.formatSeconds(fp.getETA()));
            element.setAttribute("package_speed", Formatter.formatReadable(fp.getTotalDownloadSpeed()));
            element.setAttribute("package_loaded", Formatter.formatReadable(fp.getTotalKBLoaded()));
            element.setAttribute("package_size", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
            element.setAttribute("package_todo", Formatter.formatReadable(fp.getTotalEstimatedPackageSize() - fp.getTotalKBLoaded()));
            return element;
        }

        private Element addDownloadLink(Document xml, DownloadLink dl) {
            Element element = xml.createElement("file");
            element.setAttribute("file_name", dl.getName());
            element.setAttribute("file_package", dl.getFilePackage().getName());
            element.setAttribute("file_percent", f.format(dl.getDownloadCurrent() * 100.0 / Math.max(1, dl.getDownloadSize())));
            element.setAttribute("file_hoster", dl.getHost());
            element.setAttribute("file_status", dl.getLinkStatus().getStatusString().toString());
            element.setAttribute("file_speed", dl.getDownloadSpeed() + "");
            return element;
        }

    }

    private DecimalFormat f = new DecimalFormat("#0.00");

    private HttpServer server;

    public void actionPerformed(ActionEvent e) {
        try {
            boolean enablePlugin = !subConfig.getBooleanProperty(PARAM_ENABLED, true);
            subConfig.setProperty(PARAM_ENABLED, enablePlugin);
            subConfig.save();

            if (enablePlugin) {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.startedonport2", "%s started on port %s\nhttp://127.0.0.1:%s\n/help for Developer Information.", getHost(), subConfig.getIntegerProperty(PARAM_PORT, 10025), subConfig.getIntegerProperty(PARAM_PORT, 10025)));
            } else {
                if (server != null) server.sstop();
                UserIO.getInstance().requestMessageDialog(JDL.LF("plugins.optional.remotecontrol.stopped2", "%s stopped.", getHost()));
            }
        } catch (Exception ex) {
            JDLogger.exception(ex);
        }
    }

    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        MenuAction m;
        menu.add(m = (MenuAction) new MenuAction(MenuAction.TOGGLE, JDL.L("plugins.optional.remotecontrol.activate", "Aktivieren"), 0).setActionListener(this));
        m.setSelected(subConfig.getBooleanProperty(PARAM_ENABLED, true));

        return menu;
    }

    public boolean initAddon() {
        logger.info("RemoteControl OK");
        initRemoteControl();
        JDUtilities.getController().addControlListener(this);
        return true;
    }

    private void initConfig() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, subConfig, PARAM_PORT, JDL.L("plugins.optional.RemoteControl.port", "Port:"), 1000, 65500));
        cfg.setDefaultValue(10025);
    }

    private void initRemoteControl() {
        if (subConfig.getBooleanProperty(PARAM_ENABLED, true)) {
            try {
                server = new HttpServer(subConfig.getIntegerProperty(PARAM_PORT, 10025), new Serverhandler());
                server.start();
            } catch (Exception e) {
                JDLogger.exception(e);
            }
        }
    }

    public void onExit() {

    }
}