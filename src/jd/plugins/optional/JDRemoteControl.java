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
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.http.Browser;
import jd.nrouter.IPCheck;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.nutils.httpserver.Handler;
import jd.nutils.httpserver.HttpServer;
import jd.nutils.httpserver.Request;
import jd.nutils.httpserver.Response;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 5)
public class JDRemoteControl extends PluginOptional implements ControlListener {

    private static final String PARAM_PORT = "PORT";
    private static final String PARAM_ENABLED = "ENABLED";
    private final SubConfiguration subConfig;

    private static final String LINK_TYPE_OFFLINE = "offline"; // offline links
    private static final String LINK_TYPE_AVAIL = "available"; // links present
                                                               // on download
                                                               // list and
                                                               // grabber

    private static final String ERROR_LINK_GRABBER_RUNNING = "ERROR: Link grabber is currently running. Please try again in a few seconds.";
    private static final String ERROR_TOO_FEW_PARAMETERS = "ERROR: Too few request parametes: check /help for instructions";

    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);

        subConfig = getPluginConfig();
        initConfig();
    }

    @Override
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

                commandvec.add("/get/randomip");
                infovector.add("Answers with Random IP as replacement for real IP-Check");

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

                commandvec.add("/get/downloadstatus");
                infovector.add("Get Downloadstatus<br/>Values: RUNNING, NOT_RUNNING, STOPPING");

                commandvec.add("/get/grabber");
                infovector.add("Get all links as XML that are currently held by the link grabber.");

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

                commandvec.add("/action/add/links/grabber(0|1)/start(0|1)/%X%");
                infovector.add("Add Links %X% to Grabber<br />" + "<p><span class=\"underline\">Optional:</span><br />" + "grabber(0|1): Hide/Show LinkGrabber<br />" + "grabber(0|1)/start(0|1): Hide/Show LinkGrabber and start/don't start downloads afterwards<br /></p>" + "<p><span class=\"underline\">Sample:</span><br />" + "/action/add/links/grabber0/start1/http://tinyurl.com/6o73eq<br />" + "Don't forget to URLEncode the links and use NEWLINE between Links!</p>");

                commandvec.add("/action/add/container/grabber(0|1)/start(0|1)/%X%");
                infovector.add("Add Container %X%<br />" + "<p><span class=\"underline\">Optional:</span><br />" + "grabber(0|1): Hide/Show LinkGrabber<br />" + "grabber(0|1)/start(0|1): Hide/Show LinkGrabber and start/don't start downloads afterwards<br /></p>" + "<p><span class=\"underline\">Sample:</span><br />" + "/action/add/container/grabber0/start1/C:\\container.dlc</p>");

                commandvec.add("/action/save/container/%X%");
                infovector.add("Save DLC-Container with all Links to %X%<br /> " + "Sample see /action/add/container/%X%");

                commandvec.add("/action/set/reconnectenabled/(true|false)");
                infovector.add("Set Reconnect enabled or not");

                commandvec.add("/action/set/premiumenabled/(true|false)");
                infovector.add("Set Use Premium enabled or not");

                commandvec.add("/action/grabber/join/(destination-package-name)/(package-name-to-join)/(another-package-name)/(and so on..)");
                infovector.add("Join all given link grabber packages after the first into the first one.");

                commandvec.add("/action/grabber/rename/(from-name)/(to-name)");
                infovector.add("Rename link grabber package");

                commandvec.add("/action/grabber/addall");
                infovector.add("Schedule all packages as download that are located in the link grabber.");

                commandvec.add("/action/grabber/add/(package-name-1)/(package-name-2)/(another-package-name)/(and so on..)");
                infovector.add("Schedule all given grabber packages as download.");

                commandvec.add("/action/grabber/remove/(type)/(type2)/(..)");
                infovector.add("Remove specified links from grabber. Possible values: 'offline' for offline links and 'available' for links that are already scheduled as download.");

                response.addContent("<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"><head><title>JDRemoteControl Help</title><style type=\"text/css\">a {    font-size: 14px;    text-decoration: none;    background: none;    color: #599ad6;}a:hover {    text-decoration: underline;    color:#333333;}body {    color: #333333;    background:#f0f0f0;    font-family: Verdana, Arial, Helvetica, sans-serif;    font-size: 14px;    vertical-align: top;  }.underline{    text-decoration:underline;  }</style></head><body><br /><b>JDRemoteControl " + getVersion()
                        + "<br /><br />Usage:</b><br />&nbsp;<br />1)Replace %X% with your value<br />Sample: /action/save/container/C:\\backup.dlc <br />2)Replace (true|false) with true or false<br /><table border=\"0\" cellspacing=\"5\">");
                for (int commandcount = 0; commandcount < commandvec.size(); commandcount++) {
                    response.addContent("\r\n<tr><td valign=\"top\"><a href=\"" + commandvec.get(commandcount) + "\">" + commandvec.get(commandcount) + "</a></td><td valign=\"top\">" + infovector.get(commandcount) + "</td></tr>");
                }
                response.addContent("\r\n</table><br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;<br />&nbsp;</body></html>");
            } else if (request.getRequestUrl().equals("/get/ip")) {
                // Get IP
                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                    response.addContent("IPCheck disabled");
                } else {
                    response.addContent(IPCheck.getIPAddress());
                }
            } else if (request.getRequestUrl().equals("/get/randomip")) {
                // Get Random-IP
                Random r = new Random();
                response.addContent(r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255));
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
                response.addContent(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() / 1000);
            } else if (request.getRequestUrl().equals("/get/isreconnect")) {
                // Get IsReconnect
                response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
            } else if (request.getRequestUrl().equals("/get/downloadstatus")) {
                // Get downloadstatus
                response.addContent(DownloadWatchDog.getInstance().getDownloadStatus().toString());
            } else if (request.getRequestUrl().matches("/get/grabber")) {
                // Get grabber content as xml
                for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                    Element fp_xml = addGrabberPackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinks()) {
                        fp_xml.appendChild(addDownloadLink(xml, dl));
                    }
                }
                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/action/start")) {
                // Do Start Download
                DownloadWatchDog.getInstance().startDownloads();
                response.addContent("Downloads started");
            } else if (request.getRequestUrl().equals("/action/pause")) {
                // Do Pause Download
                DownloadWatchDog.getInstance().pauseDownloads(!DownloadWatchDog.getInstance().isPaused());
                response.addContent("Downloads paused");
            } else if (request.getRequestUrl().equals("/action/stop")) {
                // Do Stop Download
                DownloadWatchDog.getInstance().stopDownloads();
                response.addContent("Downloads stopped");
            } else if (request.getRequestUrl().equals("/action/toggle")) {
                // Do Toggle Download
                DownloadWatchDog.getInstance().toggleStartStop();
                response.addContent("Downloads toggled");
            } else if (request.getRequestUrl().matches("[\\s\\S]*?/action/update/force[01]{1}/[\\s\\S]*")) {
                // Do Make Webupdate
                Integer force = Integer.parseInt(new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/update/force([01]{1})/[\\s\\S]*").getMatch(0));
                if (force == 1) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                    SubConfiguration.getConfig("WEBUPDATE").setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
                }

                WebUpdate.doUpdateCheck(true);

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
                        JDUtilities.restartJD(false);
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
            } else if (request.getRequestUrl().matches("(?is).*/action/add/links/((grabber[01]{1}/)?|(start[01]{1}/grabber[01]{1}/)?)?[\\s\\S]+")) {
                // Add Link(s)
                String link = new Regex(request.getRequestUrl(), "[\\s\\S]*?/action/add/links/([\\s\\S]+)").getMatch(0);
                ArrayList<String> links = new ArrayList<String>();
                for (String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(link, false), null)) {
                    links.add(tlink);
                }
                if (request.getParameters().size() > 0) {
                    Iterator<String> it = request.getParameters().keySet().iterator();
                    while (it.hasNext()) {
                        String help = it.next();
                        if (!request.getParameter(help).equals("")) {
                            links.add(request.getParameter(help));
                        }
                    }
                }

                Integer showgrab = null;
                Integer start = null;
                Boolean hidegrabber = false;

                // try to parseInt
                try {
                    showgrab = Integer.parseInt(new Regex(request.getRequestUrl(), ".+grabber([01]{1}).+").getMatch(0));
                    start = Integer.parseInt(new Regex(request.getRequestUrl(), ".+start([01]{1}).+").getMatch(0));
                } catch (Exception e) {
                }

                if ((showgrab != null) && (showgrab == 0)) {
                    hidegrabber = true;
                }
                StringBuilder ret = new StringBuilder();
                char tmp[] = new char[] { '"', '\r', '\n' };
                for (String element : links) {
                    ret.append('\"');
                    ret.append(element.trim());
                    ret.append(tmp);
                }
                link = ret.toString();
                new DistributeData(link, hidegrabber).start();

                // will start downloads in list if parameter is set
                if ((start != null) && start == 1) {
                    DownloadWatchDog.getInstance().startDownloads();
                }

                response.addContent("Link(s) added. (" + link + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/add/container/((grabber[01]{1}/)?|(start[01]{1}/grabber[01]{1}/)?)?[\\s\\S]+")) {
                // Open DLC Container (from web or local)
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/add/container/((grabber[01]{1}/)?|(start[01]{1}/grabber[01]{1}/)?)?([\\s\\S]+)").getMatch(3);
                // decoding of percent-encoding reserved characters in URLs like
                // %20 for space
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);

                Integer showgrab = null;
                Integer start = null;
                Boolean hidegrabber = false;
                Boolean startdl = false;

                // try to parseInt
                try {
                    showgrab = Integer.parseInt(new Regex(request.getRequestUrl(), ".+grabber([01]{1}).+").getMatch(0));
                    start = Integer.parseInt(new Regex(request.getRequestUrl(), ".+start([01]{1}).+").getMatch(0));
                } catch (Exception e) {
                    JDLogger.exception(e);
                }

                if ((showgrab != null) && (showgrab == 0)) {
                    hidegrabber = true;
                }

                if ((start != null) && start == 1) {
                    startdl = true;
                }

                if (dlcfilestr.matches("http://.*?\\.(dlc|ccf|rsdf)")) {
                    String containerFormat = new Regex(dlcfilestr, ".+\\.((dlc|ccf|rsdf))").getMatch(0);
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);

                    try {
                        Browser.download(container, dlcfilestr);
                        JDUtilities.getController().loadContainerFile(container, hidegrabber, startdl);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }
                        container.delete();
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }

                } else {
                    JDUtilities.getController().loadContainerFile(new File(dlcfilestr), hidegrabber, startdl);
                }

                response.addContent("Container opened. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/save/container/[\\s\\S]+")) {
                // Save Linklist as DLC Container
                String dlcfilestr = new Regex(request.getRequestUrl(), "[\\s\\S]*/action/save/container/([\\s\\S]+)").getMatch(0);
                // decoding of percent-encoding reserved characters in URLs like
                // %20 for space
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);
                JDUtilities.getController().saveDLC(new File(dlcfilestr), JDUtilities.getDownloadController().getAllDownloadLinks());
                response.addContent("Container saved. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/set/reconnectenabled/.*")) {
                // Set ReconnectEnabled
                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestUrl(), "[\\s\\S]*/action/set/reconnectenabled/(.*)").getMatch(0));
                logger.fine("RemoteControl - Set ReConnect: " + newrc);
                if (newrc != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newrc);
                    JDUtilities.getConfiguration().save();

                    response.addContent("reconnect=" + newrc + " (CHANGED=true)");
                } else {
                    response.addContent("reconnect=" + newrc + " (CHANGED=false)");
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
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/join/.*")) {
                // join link grabber packages: usage
                // .../join/<destpackage>/<package1>/<package2>/(...)

                String[] data = new Regex(request.getRequestUrl(), "(?is).*/action/grabber/join/(.*)").getMatch(0).split("/");
                // BAD style accessing any upper layer
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else if (data.length < 2) {

                    response.addContent(ERROR_TOO_FEW_PARAMETERS);
                } else {

                    LinkGrabberFilePackage destPackage = null;

                    synchronized (LinkGrabberController.ControllerLock) {
                        List<LinkGrabberFilePackage> srcPackages = new ArrayList<LinkGrabberFilePackage>();

                        // find destination package
                        for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                            if (pack.getName().equals(data[0])) {
                                destPackage = pack;
                                break;
                            }
                        }

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + data[0] + "' not found!");
                        } else {

                            // iterate packages; add all to tmp package list
                            // that
                            // match
                            // the
                            // given join names
                            for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                                for (String src : data) {
                                    if (pack.getName().equals(src) && pack != destPackage /*
                                                                                           * force
                                                                                           * unequal
                                                                                           */) {
                                        srcPackages.add(pack);
                                    }
                                }
                            }

                            // process src
                            for (LinkGrabberFilePackage pack : srcPackages) {
                                destPackage.addAll(pack.getDownloadLinks());
                                LinkGrabberController.getInstance().removePackage(pack);
                            }

                            // prepare response
                            if (srcPackages.size() > 0) {
                                if (srcPackages.size() < data.length - 1) {
                                    response.addContent("WARNING: Not all packages were found. ");
                                }

                                response.addContent("Joined " + srcPackages.size() + " packages into '" + data[0] + "': ");

                                for (int i = 0; i < srcPackages.size(); ++i) {
                                    if (i != 0) {
                                        response.addContent(", ");
                                    }

                                    response.addContent("'" + srcPackages.get(i).getName() + "'");
                                }
                            } else {
                                response.addContent("ERROR: No packages joined into '" + data[0] + "'!");
                            }
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/rename/.*")) {
                // rename link grabber package: usage .../rename/from/to
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    String[] data = new Regex(request.getRequestUrl(), "(?is).*/action/grabber/rename/(.*)").getMatch(0).split("/");

                    LinkGrabberFilePackage destPackage = null;

                    synchronized (LinkGrabberController.ControllerLock) {
                        // find destination package
                        for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                            if (pack.getName().equals(data[0])) {
                                destPackage = pack;
                                break;
                            }
                        }

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + data[0] + "' not found!");
                        } else {
                            destPackage.setName(data[1]);
                            LinkGrabberController.getInstance().throwRefresh();
                            response.addContent("Package '" + data[0] + "' renamed to '" + data[1] + "'.");
                        }
                    }
                }

            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/addall") || request.getRequestUrl().matches("(?is).*/action/grabber/add/.*")) {
                // add all links in grabber to download list or
                // add single packages separated by slashes:
                // add/pack1/pack2/pack3/...
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    response.addContent("The following packages are now scheduled for download: ");

                    synchronized (LinkGrabberController.ControllerLock) {
                        List<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                        if (request.getRequestUrl().matches("(?is).*/action/grabber/add/.*")) {
                            // add requested
                            String[] data = new Regex(request.getRequestUrl(), "(?is).*/action/grabber/add/(.*)").getMatch(0).split("/");
                            for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                                for (String name : data) {
                                    if (name.equalsIgnoreCase(pack.getName())) {
                                        packages.add(pack);
                                    }
                                }
                            }
                        } else {
                            // all
                            packages.addAll(LinkGrabberController.getInstance().getPackages());
                        }

                        boolean avail = (packages.size() > 0);
                        boolean hit = false;
                        for (LinkGrabberFilePackage pack : packages) {
                            ArrayList<DownloadLink> links = pack.getDownloadLinks();

                            ArrayList<FilePackage> fps = new ArrayList<FilePackage>();
                            FilePackage fp = FilePackage.getInstance();
                            fp.setName(pack.getName());
                            fp.setName(pack.getName());
                            fp.setComment(pack.getComment());
                            fp.setPassword(pack.getPassword());
                            fp.setExtractAfterDownload(pack.isExtractAfterDownload());
                            fp.setDownloadDirectory(pack.getDownloadDirectory());
                            if (pack.useSubDir()) {
                                File file = new File(new File(pack.getDownloadDirectory()), fp.getName());
                                fp.setDownloadDirectory(file.getAbsolutePath());
                                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CREATE_SUBFOLDER_BEFORE_DOWNLOAD, false)) {
                                    if (!file.exists()) {
                                        if (!file.mkdirs()) {
                                            logger.severe("could not create " + file.toString());
                                            fp.setDownloadDirectory(pack.getDownloadDirectory());
                                        }
                                    }
                                }
                            }
                            for (DownloadLink link : links) {
                                if (link.getFilePackage() == FilePackage.getDefaultFilePackage()) {
                                    fp.add(link);
                                    if (!fps.contains(fp)) fps.add(fp);
                                } else {
                                    if (!fps.contains(link.getFilePackage())) fps.add(link.getFilePackage());
                                }
                            }
                            // LinkCheck.getLinkChecker().checkLinksandWait(links);
                            if (GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_INSERT_NEW_LINKS_AT, false)) {
                                DownloadController.getInstance().addAllAt(fps, 0);
                            } else {
                                DownloadController.getInstance().addAll(fps);
                            }

                            if (hit) {
                                response.addContent(", ");
                            }
                            response.addContent("'" + pack.getName() + "'");

                            // remove package from link grabber
                            LinkGrabberController.getInstance().removePackage(pack);
                            hit = true;
                        }

                        if (!avail) {
                            response.addContent("(none)");
                        }
                    }
                }

            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/remove/.*")) {
                // remove specified links from grabber:
                // offline, available (in grabber and download list)
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    String[] data = new Regex(request.getRequestUrl(), "(?is).*/action/grabber/remove/(.*)").getMatch(0).split("/");

                    if (data.length == 0) {
                        response.addContent(ERROR_TOO_FEW_PARAMETERS);
                    } else {
                        synchronized (LinkGrabberController.ControllerLock) {
                            response.addContent("Removing links from grabber of type: ");
                            for (int i = 0; i < data.length; ++i) {
                                if (i > 0) {
                                    response.addContent(", ");
                                }
                                if (data[i].equals(LINK_TYPE_OFFLINE) || data[i].equals(LINK_TYPE_AVAIL)) {
                                    response.addContent(data[i]);
                                } else {
                                    response.addContent("(unknown type: " + data[i] + ")");
                                }
                            }

                            // determine links to delete
                            List<String> delLinks = new ArrayList<String>();
                            List<String> delPackages = new ArrayList<String>();
                            List<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>(LinkGrabberController.getInstance().getPackages());
                            for (LinkGrabberFilePackage pack : packages) {

                                List<DownloadLink> links = new ArrayList<DownloadLink>(pack.getDownloadLinks());
                                for (DownloadLink link : links) {
                                    for (String type : data) {
                                        if ((type.equals(LINK_TYPE_OFFLINE) && link.getAvailableStatus().equals(AvailableStatus.FALSE)) || (type.equals(LINK_TYPE_AVAIL) && link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS))) {
                                            pack.remove(link);
                                            delLinks.add(link.getDownloadURL());
                                        }
                                    }
                                }

                                // delete packages that are empty
                                if (pack.getDownloadLinks().size() == 0) {
                                    // LinkGrabberController.getInstance().removePackage(pack);
                                    delPackages.add(pack.getName());
                                }

                            }

                            response.addContent(" - " + delLinks.size() + " links removed (" + delLinks + ") which left " + delPackages.size() + " empty packages (" + delPackages + ").");
                        }
                    }
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

        private Element addGrabberPackage(Document xml, LinkGrabberFilePackage fp) {
            Element element = xml.createElement("package");
            xml.getFirstChild().appendChild(element);
            element.setAttribute("package_name", fp.getName());
            element.setAttribute("package_linkstotal", fp.size() + "");
            return element;
        }

    }

    private DecimalFormat f = new DecimalFormat("#0.00");
    private HttpServer server;
    private MenuAction activate;

    @Override
    public void actionPerformed(ActionEvent e) {
        try {

            subConfig.setProperty(PARAM_ENABLED, activate.isSelected());
            subConfig.save();

            if (activate.isSelected()) {
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

    @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        if (activate == null) {
            activate = new MenuAction("remotecontrol", 0);
            activate.setActionListener(this);
            activate.setSelected(subConfig.getBooleanProperty(PARAM_ENABLED, true));
            activate.setTitle(getHost());
        }
        menu.add(activate);

        return menu;
    }

    @Override
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

    @Override
    public void onExit() {

    }
}