//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional.remotecontrol;

import java.awt.event.ActionEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
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
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.LinkGrabberControllerEvent;
import jd.controlling.LinkGrabberControllerListener;
import jd.controlling.reconnect.Reconnecter;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
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
import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;
import jd.utils.locale.JDL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Alle Ausgaben sollten lediglich eine Zeile lang sein, um die kompatibilität
 * zu erhöhen.
 */
@OptionalPlugin(rev = "$Revision$", id = "remotecontrol", interfaceversion = 5)
public class JDRemoteControl extends PluginOptional implements ControlListener, LinkGrabberControllerListener {

    private static final String PARAM_PORT = "PORT";
    private static final String PARAM_ENABLED = "ENABLED";
    private final SubConfiguration subConfig;

    private static final String LINK_TYPE_OFFLINE = "offline";
    private static final String LINK_TYPE_AVAIL = "available";

    private static final String ERROR_MALFORMED_REQUEST = "JDRemoteControl - Malformed request. Use /help";
    private static final String ERROR_LINK_GRABBER_RUNNING = "ERROR: Link grabber is currently running. Please try again in a few seconds.";

    @SuppressWarnings("unused")
    private boolean grabberIsBusy;

    public JDRemoteControl(PluginWrapper wrapper) {
        super(wrapper);
        subConfig = getPluginConfig();
        initConfig();
        LinkGrabberController.getInstance().addListener(this);
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

            if (request.getRequestUrl().equals("/help")) {
                // Get help page

                response.addContent(HelpPage.getHTML());
            } else if (request.getRequestUrl().equals("/get/ip")) {
                // Get IP

                if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                    response.addContent("IPCheck disabled");
                } else {
                    response.addContent(IPCheck.getIPAddress());
                }
            } else if (request.getRequestUrl().equals("/get/randomip")) {
                // Get random-IP

                Random r = new Random();
                response.addContent(r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255));
            } else if (request.getRequestUrl().equals("/get/config")) {
                // Get config

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
                // Get version

                response.addContent(JDUtilities.getJDTitle());
            } else if (request.getRequestUrl().equals("/get/rcversion")) {
                // Get JDRemoteControl version

                response.addContent(getVersion());
            } else if (request.getRequestUrl().equals("/get/speedlimit")) {
                // Get speed limit

                response.addContent(SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));
            } else if (request.getRequestUrl().equals("/get/downloads/current/count")) {
                // Get amount of current DLs COUNT

                int counter = 0;

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            counter++;
                        }
                    }
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/current/list")) {
                // Get current DLs

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        if (dl.getLinkStatus().isPluginActive()) {
                            fp_xml.appendChild(addDownloadLink(xml, dl));
                        }
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/all/count")) {
                // Get DLList COUNT

                int counter = 0;

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    counter += fp.getDownloadLinkList().size();
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/downloads/all/list")) {
                // Get DLList

                for (FilePackage fp : JDUtilities.getController().getPackages()) {
                    Element fp_xml = addFilePackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinkList()) {
                        fp_xml.appendChild(addDownloadLink(xml, dl));
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().equals("/get/downloads/finished/count")) {
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
            } else if (request.getRequestUrl().equals("/get/downloads/finished/list")) {
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
                // Get current speed

                response.addContent(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() / 1000);
            } else if (request.getRequestUrl().equals("/get/isreconnect")) {
                // Get isReconnect

                response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
            } else if (request.getRequestUrl().equals("/get/downloadstatus")) {
                // Get download status

                response.addContent(DownloadWatchDog.getInstance().getDownloadStatus().toString());
            } else if (request.getRequestUrl().matches("/get/grabber/list")) {
                // Get grabber content as xml

                for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                    Element fp_xml = addGrabberPackage(xml, fp);

                    for (DownloadLink dl : fp.getDownloadLinks()) {
                        fp_xml.appendChild(addGrabberLink(xml, dl));
                    }
                }

                response.addContent(JDUtilities.createXmlString(xml));
            } else if (request.getRequestUrl().matches("/get/grabber/count")) {
                // Get amount of links in grabber

                int counter = 0;

                for (LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                    counter += fp.getDownloadLinks().size();
                }

                response.addContent(counter);
            } else if (request.getRequestUrl().equals("/get/grabber/isbusy")) {
                // Get whether grabber is busy or not

                boolean isbusy = false;

                if (LinkGrabberPanel.getLinkGrabber().isRunning())
                    isbusy = true;
                else
                    isbusy = false;

                response.addContent(isbusy);
            } else if (request.getRequestUrl().equals("/get/grabber/isset/startafteradding")) {
                // Get whether start after adding option is set or not

                boolean value = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true);
                response.addContent(value);
            } else if (request.getRequestUrl().equals("/action/start")) {
                // Do Start downloads

                DownloadWatchDog.getInstance().startDownloads();
                response.addContent("Downloads started");
            } else if (request.getRequestUrl().equals("/action/pause")) {
                // Do Pause downloads

                DownloadWatchDog.getInstance().pauseDownloads(!DownloadWatchDog.getInstance().isPaused());
                response.addContent("Downloads paused");
            } else if (request.getRequestUrl().equals("/action/stop")) {
                // Do Stop downloads

                DownloadWatchDog.getInstance().stopDownloads();
                response.addContent("Downloads stopped");
            } else if (request.getRequestUrl().equals("/action/toggle")) {
                // Do Toggle downloads

                DownloadWatchDog.getInstance().toggleStartStop();
                response.addContent("Downloads toggled");
            } else if (request.getRequestUrl().matches(".*?/action/(force)?update")) {
                // Do Perform webupdate

                if (request.getRequestUrl().matches(".+/action/forceupdate")) {
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
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/limit/[0-9]+")) {
                // Set download limit

                Integer newdllimit = Integer.parseInt(new Regex(request.getRequestUrl(), ".*/action/set/download/limit/([0-9]+)").getMatch(0));
                logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newlimit=" + newdllimit);
            } else if (request.getRequestUrl().matches("(?is).*/action/set/download/max/[0-9]+")) {
                // Set max. sim. downloads

                Integer newsimdl = Integer.parseInt(new Regex(request.getRequestUrl(), ".*/action/set/download/max/([0-9]+)").getMatch(0));
                logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
                SubConfiguration.getConfig("DOWNLOAD").save();
                response.addContent("newmax=" + newsimdl);
            } else if (request.getRequestUrl().matches("(?is).*/action/set/grabber/startafteradding/(true|false)")) {
                boolean value = Boolean.parseBoolean(new Regex(request.getRequestUrl(), ".*/action/set/grabber/startafteradding/(true|false)").getMatch(0));
                logger.fine("RemoteControl - Set PARAM_START_AFTER_ADDING_LINKS: " + value);

                if (value != GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
                    GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, value);
                    JDUtilities.getConfiguration().save();
                    response.addContent("PARAM_START_AFTER_ADDING_LINKS =" + value + " (CHANGED=true)");
                } else {
                    response.addContent("PARAM_START_AFTER_ADDING_LINKS =" + value + " (CHANGED=false)");
                }

            } else if (request.getRequestUrl().matches("(?is).*/action/add(/auto)?/links/.+")) {
                // Add link(s)

                boolean issetAuto = false;
                ArrayList<String> links = new ArrayList<String>();

                if (request.getRequestUrl().matches(".+/auto/.+")) {
                    issetAuto = true;
                }

                // set PARAM_START_AFTER_ADDING_LINKS_AUTO
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, issetAuto);
                JDUtilities.getConfiguration().save();

                String link = new Regex(request.getRequestUrl(), ".*/action/add(/auto)?/links/(.+)").getMatch(1);

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

                StringBuilder ret = new StringBuilder();
                char tmp[] = new char[] { '"', '\r', '\n' };

                for (String element : links) {
                    ret.append('\"');
                    ret.append(element.trim());
                    ret.append(tmp);
                }

                link = ret.toString();
                new DistributeData(link, false).start();

                response.addContent("Link(s) added. (" + link + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/add(/auto)?/container/.+")) {
                // Open a local or remote DLC-container

                boolean issetAuto = false;

                if (request.getRequestUrl().matches(".+/auto/.+")) {
                    issetAuto = true;
                }

                // set PARAM_START_AFTER_ADDING_LINKS_AUTO
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, issetAuto);
                JDUtilities.getConfiguration().save();

                String dlcfilestr = new Regex(request.getRequestUrl(), ".*/action/add(/auto)?/container/(.+)").getMatch(1);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);

                // import dlc
                if (dlcfilestr.matches("http://.*?\\.(dlc|ccf|rsdf)")) {
                    // remote container file
                    String containerFormat = new Regex(dlcfilestr, ".+\\.((dlc|ccf|rsdf))").getMatch(0);
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);

                    try {
                        Browser.download(container, dlcfilestr);
                        JDController.loadContainerFile(container, false, false);

                        try {
                            Thread.sleep(3000);
                        } catch (Exception e) {
                            JDLogger.exception(e);
                        }

                        container.delete();
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
                } else {
                    // local container file
                    JDController.loadContainerFile(new File(dlcfilestr), false, false);
                }

                response.addContent("Container opened. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/save/container(/fromgrabber)?/.+")) {
                // Save linklist as DLC-container

                ArrayList<DownloadLink> dllinks = new ArrayList<DownloadLink>();

                String dlcfilestr = new Regex(request.getRequestUrl(), ".*/action/save/container(/fromgrabber)/?(.+)").getMatch(1);
                dlcfilestr = Encoding.htmlDecode(dlcfilestr);

                boolean savefromGrabber = new Regex(request.getRequestUrl(), ".+/fromgrabber/.+").matches();

                if (savefromGrabber) {
                    if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                        response.addContent(ERROR_LINK_GRABBER_RUNNING);
                    } else {
                        ArrayList<LinkGrabberFilePackage> lgPackages = new ArrayList<LinkGrabberFilePackage>();
                        ArrayList<FilePackage> packages = new ArrayList<FilePackage>();

                        // disable packages, confirm them, save dlc, then delete
                        synchronized (LinkGrabberController.ControllerLock) {
                            lgPackages.addAll(LinkGrabberController.getInstance().getPackages());

                            for (int i = 0; i < lgPackages.size(); i++) {
                                DownloadLink dl = null;

                                for (DownloadLink link : lgPackages.get(i).getDownloadLinks()) {
                                    dllinks.add(link); // collecting download
                                    // links
                                    link.setEnabled(false);
                                    if (dl == null) dl = link;
                                }

                                LinkGrabberPanel.getLinkGrabber().confirmPackage(lgPackages.get(i), null, i);
                                packages.add(dl.getFilePackage());
                            }

                            JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);

                            for (FilePackage fp : packages) {
                                JDUtilities.getDownloadController().removePackage(fp);
                            }
                        }
                    }
                } else {
                    dllinks = JDUtilities.getDownloadController().getAllDownloadLinks();
                    JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);
                }

                response.addContent("Container saved. (" + dlcfilestr + ")");
            } else if (request.getRequestUrl().matches("(?is).*/action/set/reconnect/(true|false)")) {
                // Set Reconnect enabled

                boolean newrc = Boolean.parseBoolean(new Regex(request.getRequestUrl(), ".*/action/set/reconnect/(true|false)").getMatch(0));
                logger.fine("RemoteControl - Set ReConnect: " + newrc);

                if (newrc != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newrc);
                    JDUtilities.getConfiguration().save();
                    response.addContent("reconnect=" + newrc + " (CHANGED=true)");
                } else {
                    response.addContent("reconnect=" + newrc + " (CHANGED=false)");
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/set/premium/(true|false)")) {
                // Set Use premium

                boolean newuseprem = Boolean.parseBoolean(new Regex(request.getRequestUrl(), ".*/action/set/premium/(true|false)").getMatch(0));
                logger.fine("RemoteControl - Set Premium: " + newuseprem);

                if (newuseprem != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
                    JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                    JDUtilities.getConfiguration().save();
                    response.addContent("newprem=" + newuseprem + " (CHANGED=true)");
                } else {
                    response.addContent("newprem=" + newuseprem + " (CHANGED=false)");
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/join/.+")) {
                // Join link grabber packages

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> srcPackages = new ArrayList<LinkGrabberFilePackage>();
                    String[] packagenames = getHTMLDecoded(new Regex(request.getRequestUrl(), ".*/action/grabber/join/(.+)").getMatch(0).split("/"));

                    synchronized (LinkGrabberController.ControllerLock) {
                        LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                        } else {
                            // iterate packages; add all to tmp package list
                            // that match the given join names
                            for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                                for (String src : packagenames) {
                                    if ((pack.getName().equals(src)) && (pack != destPackage)) {
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
                                if (srcPackages.size() < packagenames.length - 1) {
                                    response.addContent("WARNING: Not all packages were found. ");
                                }

                                response.addContent("Joined " + srcPackages.size() + " packages into '" + packagenames[0] + "': ");

                                for (int i = 0; i < srcPackages.size(); ++i) {
                                    if (i != 0) response.addContent(", ");
                                    response.addContent("'" + srcPackages.get(i).getName() + "'");
                                }
                            } else {
                                response.addContent("ERROR: No packages joined into '" + packagenames[0] + "'!");
                            }
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/rename/.+")) {
                // rename link grabber package

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    String[] packagenames = getHTMLDecoded(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/rename/(.+)").getMatch(0).split("/"));

                    synchronized (LinkGrabberController.ControllerLock) {
                        LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                        if (destPackage == null) {
                            response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                        } else {
                            destPackage.setName(packagenames[1]);
                            LinkGrabberController.getInstance().throwRefresh();
                            response.addContent("Package '" + packagenames[0] + "' renamed to '" + packagenames[1] + "'.");
                        }
                    }
                }

            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/confirmall")) {
                // add all links in linkgrabber list to download list

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberPanel.getLinkGrabber().confirmPackage(packages.get(i), null, i);
                        }

                        response.addContent("All links are now scheduled for download.");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/confirm/.+")) {
                // add denoted links in linkgrabber list to download list

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> addedlist = new ArrayList<LinkGrabberFilePackage>();
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    String[] packagenames = getHTMLDecoded(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/confirm/(.+)").getMatch(0).split("/"));

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberFilePackage fp = packages.get(i);

                            for (String name : packagenames) {
                                if (name.equalsIgnoreCase(fp.getName())) {
                                    LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, i);
                                    addedlist.add(fp);
                                }
                            }
                        }

                        response.addContent("The following packages are now scheduled for download: ");

                        for (int i = 0; i < addedlist.size(); i++) {
                            if (i != 0) response.addContent(", ");
                            response.addContent("'" + addedlist.get(i).getName() + "'");
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/removetype/.+")) {
                // remove denoted links from grabber that matches the type
                // 'offline', 'available' (in grabber and download list)

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<String> delLinks = new ArrayList<String>();
                    ArrayList<String> delPackages = new ArrayList<String>();

                    String[] types = getHTMLDecoded(new Regex(request.getRequestUrl(), "(?is).*/action/grabber/removetype/(.+)").getMatch(0).split("/"));

                    synchronized (LinkGrabberController.ControllerLock) {
                        ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        response.addContent("Removing links from grabber of type: ");

                        for (int i = 0; i < types.length; ++i) {
                            if (i != 0) response.addContent(", ");
                            if (types[i].equals(LINK_TYPE_OFFLINE) || types[i].equals(LINK_TYPE_AVAIL)) {
                                response.addContent(types[i]);
                            } else {
                                response.addContent("(unknown type: " + types[i] + ")");
                            }
                        }

                        for (LinkGrabberFilePackage fp : packages) {
                            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());

                            for (DownloadLink link : links) {
                                for (String type : types) {
                                    if ((type.equals(LINK_TYPE_OFFLINE) && link.getAvailableStatus().equals(AvailableStatus.FALSE)) || (type.equals(LINK_TYPE_AVAIL) && link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS))) {
                                        fp.remove(link);
                                        delLinks.add(link.getDownloadURL());
                                    }
                                }
                            }

                            if (fp.getDownloadLinks().size() == 0) {
                                delPackages.add(fp.getName());
                            }
                        }

                        response.addContent(" - " + delLinks.size() + " links removed (" + delLinks + ") thus removed " + delPackages.size() + " empty packages (" + delPackages + ").");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/removeall")) {
                // remove all links from grabber

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (LinkGrabberFilePackage fp : packages) {
                            LinkGrabberController.getInstance().removePackage(fp);
                        }

                        response.addContent("All links removed from grabber.");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/remove/.+")) {
                // remove denoted packages from grabber

                String[] packagenames = getHTMLDecoded(new Regex(request.getRequestUrl(), ".*/action/grabber/remove/(.+)").getMatch(0).split("/"));

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                    ArrayList<LinkGrabberFilePackage> removelist = new ArrayList<LinkGrabberFilePackage>();

                    synchronized (LinkGrabberController.ControllerLock) {
                        packages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < packages.size(); i++) {
                            LinkGrabberFilePackage fp = packages.get(i);

                            for (String name : packagenames) {
                                if (name.equalsIgnoreCase(fp.getName())) {
                                    LinkGrabberController.getInstance().removePackage(fp);
                                    removelist.add(fp);
                                }
                            }
                        }

                        response.addContent("The following packages were removed from grabber: ");

                        for (int i = 0; i < removelist.size(); i++) {
                            if (i != 0) response.addContent(", ");
                            response.addContent("'" + removelist.get(i).getName() + "'");
                        }
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/grabber/move/.+")) {
                // move links (index 1 to length-1) into package (index 0)

                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(ERROR_LINK_GRABBER_RUNNING);
                } else {
                    Regex reg = new Regex(request.getRequestUrl(), ".*/action/grabber/move/([^/]+)/(.+)");

                    if (reg.getMatch(0) == null || reg.getMatch(1) == null) {
                        response.addContent(ERROR_MALFORMED_REQUEST);
                        return;
                    }

                    String dest_package_name = Encoding.htmlDecode(reg.getMatch(0));
                    ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                    for (String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(reg.getMatch(1), false), null)) {
                        links.addAll(new DistributeData(tlink).findLinks());
                    }

                    boolean packageWasAvailable = false;
                    int numLinksMoved = 0;
                    int numPackagesDeleted = 0;

                    synchronized (LinkGrabberController.ControllerLock) {

                        // search package
                        LinkGrabberFilePackage dest_package = null;
                        for (LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                            if (pack.getName().equalsIgnoreCase(dest_package_name)) {
                                dest_package = pack;
                                packageWasAvailable = true;
                            }
                        }

                        // not available so create new
                        if (dest_package == null) {
                            dest_package = new LinkGrabberFilePackage(dest_package_name);
                            dest_package.setComment("Created by JDRemoteControl move command");
                        }

                        // move links
                        List<LinkGrabberFilePackage> availPacks = LinkGrabberController.getInstance().getPackages();
                        for (int k = 0; k < availPacks.size(); ++k) {

                            LinkGrabberFilePackage pack = availPacks.get(k);
                            for (int i = 0; i < pack.size(); ++i) {

                                if (pack == dest_package) {
                                    continue;
                                }

                                DownloadLink packLink = pack.get(i);
                                for (DownloadLink userLink : links) {

                                    if (packLink.compareTo(userLink) == 0) {
                                        pack.remove(i);
                                        --i;
                                        dest_package.add(packLink);
                                        ++numLinksMoved;
                                        if (pack.size() == 0) {
                                            --k; // adjust index as remove event
                                            // was fired
                                        }
                                        break;
                                    }
                                }
                            }
                        }

                        if (packageWasAvailable == false && dest_package.size() > 0) {
                            LinkGrabberController.getInstance().addPackage(dest_package);
                        }
                    }

                    if (numLinksMoved > 0) {
                        response.addContent(numLinksMoved + " links were moved into " + (packageWasAvailable ? "the available" : "the newly created") + " package '" + dest_package_name + "'. " + numPackagesDeleted + " packages were deleted as they were emtpy.");
                    } else {
                        response.addContent("No links moved - check input.");
                    }
                }
            } else if (request.getRequestUrl().matches("(?is).*/action/downloads/removeall")) {
                // remove all packages in download list

                ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
                packages.addAll(DownloadController.getInstance().getPackages());

                for (FilePackage fp : packages) {
                    DownloadController.getInstance().removePackage(fp);
                }

                response.addContent("All scheduled packages removed.");
            } else if (request.getRequestUrl().matches("(?is).*/action/downloads/remove/.+")) {
                // remove denoted packages from download list

                ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
                ArrayList<FilePackage> removelist = new ArrayList<FilePackage>();
                String[] packagenames = getHTMLDecoded(new Regex(request.getRequestUrl(), ".*/action/downloads/remove/(.+)").getMatch(0).split("/"));

                packages.addAll(DownloadController.getInstance().getPackages());

                for (int i = 0; i < packages.size(); i++) {
                    FilePackage fp = packages.get(i);

                    for (String name : packagenames) {
                        if (name.equalsIgnoreCase(fp.getName())) {
                            DownloadController.getInstance().removePackage(fp);
                            removelist.add(fp);
                        }
                    }
                }

                response.addContent("The following packages were removed from download list: ");

                for (int i = 0; i < removelist.size(); i++) {
                    if (i != 0) response.addContent(", ");
                    response.addContent("'" + removelist.get(i).getName() + "'");
                }

            } else if (request.getRequestUrl().matches("(?is).*/special/check/.+")) {

                ArrayList<String> links = new ArrayList<String>();

                String link = new Regex(request.getRequestUrl(), ".*/special/check/(.+)").getMatch(0);

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

                // check all links sequentially
                ArrayList<DownloadLink> dls = new ArrayList<DownloadLink>();
                for (String chklink : links) {

                    dls = new DistributeData(chklink, false).findLinks();

                    Element element = xml.createElement("link");
                    xml.getFirstChild().appendChild(element);
                    element.setAttribute("url", chklink);

                    for (DownloadLink dl : dls) {
                        dl.getAvailableStatus(); // force update
                        LinkGrabberFilePackage pack = LinkGrabberController.getInstance().getGeneratedPackage(dl);
                        dl.getFilePackage().setName(pack.getName());
                        element.appendChild(addGrabberLink(xml, dl));
                    }
                }
                response.addContent(JDUtilities.createXmlString(xml));
            } else {
                response.addContent(ERROR_MALFORMED_REQUEST);
            }
        }

        private String[] getHTMLDecoded(String[] arr) {
            String[] retval = new String[arr.length];

            for (int i = 0; i < arr.length; ++i) {
                retval[i] = Encoding.htmlDecode(arr[i]);
            }

            return retval;
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
            element.setAttribute("file_size", Formatter.formatReadable(dl.getDownloadSize()));
            element.setAttribute("file_downloaded", Formatter.formatReadable(dl.getDownloadCurrent()));
            return element;
        }

        private Element addGrabberLink(Document xml, DownloadLink dl) {
            Element element = xml.createElement("file");

            // fetch available status in advance - also updates other stuff like
            // file size
            AvailableStatus status = dl.getAvailableStatus();

            element.setAttribute("file_name", dl.getName());
            element.setAttribute("file_package", dl.getFilePackage().getName());
            element.setAttribute("file_hoster", dl.getHost());
            element.setAttribute("file_status", dl.getLinkStatus().getStatusString().toString());
            element.setAttribute("file_available", status.toString());
            element.setAttribute("file_download_url", dl.getDownloadURL().toString());
            element.setAttribute("file_browser_url", dl.getBrowserUrl().toString());
            element.setAttribute("file_size", Formatter.formatReadable(dl.getDownloadSize()));
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

    public void onLinkGrabberControllerEvent(LinkGrabberControllerEvent event) {
        if (event.getID() == LinkGrabberControllerEvent.FINISHED) {
            grabberIsBusy = false;
        }
    }
}