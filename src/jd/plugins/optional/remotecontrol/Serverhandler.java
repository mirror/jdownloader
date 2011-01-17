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

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.swing.jdgui.GUIUtils;
import jd.gui.swing.jdgui.JDGuiConstants;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginOptional;
import jd.plugins.optional.interfaces.Handler;
import jd.plugins.optional.interfaces.Request;
import jd.plugins.optional.interfaces.Response;
import jd.plugins.optional.remotecontrol.helppage.HelpPage;
import jd.plugins.optional.remotecontrol.utils.RemoteSupport;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Serverhandler implements Handler {

    private final OptionalPluginWrapper rc                          = JDUtilities.getOptionalPlugin("remotecontrol");
    private static Logger               logger                      = JDLogger.getLogger();

    private static final String         LINK_TYPE_OFFLINE           = "offline";
    private static final String         LINK_TYPE_AVAIL             = "available";

    private static final String         ERROR_MALFORMED_REQUEST     = "JDRemoteControl - Malformed request. Use /help";
    private static final String         ERROR_LINK_GRABBER_RUNNING  = "ERROR: Link grabber is currently running. Please try again in a few seconds.";
    private static final String         ERROR_UNKNOWN_RESPONSE_TYPE = "Error: Unknown response type. Please inform us.";

    private final DecimalFormat         f                           = new DecimalFormat("#0.00");

    private Element addDownloadLink(final Document xml, final DownloadLink dl) {
        final Element element = xml.createElement("file");
        element.setAttribute("file_name", dl.getName());
        element.setAttribute("file_package", dl.getFilePackage().getName());
        element.setAttribute("file_percent", this.f.format(dl.getDownloadCurrent() * 100.0 / Math.max(1, dl.getDownloadSize())));
        element.setAttribute("file_hoster", dl.getHost());
        element.setAttribute("file_status", dl.getLinkStatus().getStatusString().toString());
        element.setAttribute("file_speed", dl.getDownloadSpeed() + "");
        element.setAttribute("file_size", Formatter.formatReadable(dl.getDownloadSize()));
        element.setAttribute("file_downloaded", Formatter.formatReadable(dl.getDownloadCurrent()));
        return element;
    }

    private Element addFilePackage(final Document xml, final FilePackage fp) {
        final Element element = xml.createElement("packages");
        xml.getFirstChild().appendChild(element);
        element.setAttribute("package_name", fp.getName());
        element.setAttribute("package_percent", this.f.format(fp.getPercent()));
        element.setAttribute("package_linksinprogress", fp.getLinksInProgress() + "");
        element.setAttribute("package_linkstotal", fp.size() + "");
        element.setAttribute("package_ETA", Formatter.formatSeconds(fp.getETA()));
        element.setAttribute("package_speed", Formatter.formatReadable(fp.getTotalDownloadSpeed()));
        element.setAttribute("package_loaded", Formatter.formatReadable(fp.getTotalKBLoaded()));
        element.setAttribute("package_size", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
        element.setAttribute("package_todo", Formatter.formatReadable(fp.getTotalEstimatedPackageSize() - fp.getTotalKBLoaded()));
        return element;
    }

    private Element addGrabberLink(final Document xml, final DownloadLink dl) {
        final Element element = xml.createElement("file");

        // fetch available status in advance - also updates other stuff like
        // file size
        final AvailableStatus status = dl.getAvailableStatus();

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

    private Element addGrabberPackage(final Document xml, final LinkGrabberFilePackage fp) {
        final Element element = xml.createElement("packages");
        xml.getFirstChild().appendChild(element);
        element.setAttribute("package_name", fp.getName());
        element.setAttribute("package_linkstotal", fp.size() + "");
        return element;
    }

    public void finish(final Request req, final Response res) {
    }

    private String[] getHTMLDecoded(final String[] arr) {
        final int arrLength = arr.length;
        final String[] retval = new String[arrLength];

        for (int i = 0; i < arrLength; ++i) {
            retval[i] = Encoding.htmlDecode(arr[i]);
        }

        return retval;
    }

    public void handle(final Request request, final Response response) {
        final Document xml = JDUtilities.parseXmlString("<jdownloader></jdownloader>", false);

        response.setReturnType("text/html");
        response.setReturnStatus(Response.OK);

        String requestUrl = request.getRequestUrl();

        response.setJSONFormat(Boolean.parseBoolean(request.getParameter("getjson")));
        response.setCallbackJSONP(request.getParameter("jsonp_callback"));

        if (requestUrl.equals("/help")) {
            // Get help page

            response.addContent(HelpPage.getHTML());
        } else if (requestUrl.equals("/get/rcversion")) {
            // Get JDRemoteControl version

            response.addContent(this.rc.getVersion());
        } else if (requestUrl.equals("/get/version")) {
            // Get version

            response.addContent(JDUtilities.getJDTitle());
        } else if (requestUrl.equals("/get/config")) {
            // Get config

            Property config = JDUtilities.getConfiguration();

            Element element = xml.createElement("config");
            xml.getFirstChild().appendChild(element);

            if (request.getParameters().containsKey("sub")) {
                config = SubConfiguration.getConfig(request.getParameters().get("sub").toUpperCase());
            }

            for (final Entry<String, Object> next : config.getProperties().entrySet()) {
                element.setAttribute(next.getKey(), next.getValue() + "");
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/ip")) {
            // Get IP

            if (SubConfiguration.getConfig("DOWNLOAD").getBooleanProperty(Configuration.PARAM_GLOBAL_IP_DISABLE, false)) {
                response.addContent("IPCheck disabled");
            } else {
                response.addContent(IPController.getInstance().getIP());
            }
        } else if (requestUrl.equals("/get/randomip")) {
            // Get random-IP

            final Random r = new Random();
            response.addContent(r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255));
        } else if (requestUrl.equals("/get/speed")) {
            // Get current speed

            response.addContent(DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() / 1000);
        } else if (requestUrl.equals("/get/speedlimit")) {
            // Get speed limit

            final int value = SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0);
            response.addContent(value);
        } else if (requestUrl.equals("/get/downloadstatus")) {
            // Get download status

            response.addContent(DownloadWatchDog.getInstance().getDownloadStatus().toString());
        } else if (requestUrl.equals("/get/isreconnect")) {
            // Get isReconnect

            response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
        } else if (requestUrl.matches("/get/grabber/list")) {
            // Get grabber content as xml

            for (final LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                final Element fp_xml = this.addGrabberPackage(xml, fp);

                for (final DownloadLink dl : fp.getDownloadLinks()) {
                    fp_xml.appendChild(this.addGrabberLink(xml, dl));
                }
            }

            response.addContent(xml);
        } else if (requestUrl.matches("/get/grabber/count")) {
            // Get number of links in grabber

            int counter = 0;

            for (final LinkGrabberFilePackage fp : LinkGrabberController.getInstance().getPackages()) {
                counter += fp.getDownloadLinks().size();
            }

            response.addContent(counter);
        } else if (requestUrl.equals("/get/grabber/isbusy")) {
            // Get whether grabber is busy or not

            boolean isbusy = false;

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                isbusy = true;
            } else {
                isbusy = false;
            }

            response.addContent(isbusy);
        } else if (requestUrl.equals("/get/grabber/isset/startafteradding")) {
            // Get whether start after adding option is set or not

            final boolean value = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true);
            response.addContent(value);
        } else if (requestUrl.equals("/get/grabber/isset/autoadding")) {
            // Get whether auto-adding option is set or not

            final boolean value = GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, true);
            response.addContent(value);
        } else if (requestUrl.equals("/get/downloads/all/count")) {
            // Get number of all DLs in DL-list

            int counter = 0;

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                counter += fp.getDownloadLinkList().size();
            }

            response.addContent(counter);
        } else if (requestUrl.equals("/get/downloads/current/count")) {
            // Get number of current DLs

            int counter = 0;

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                for (final DownloadLink dl : fp.getDownloadLinkList()) {
                    if (dl.getLinkStatus().isPluginActive()) {
                        counter++;
                    }
                }
            }

            response.addContent(counter);
        } else if (requestUrl.equals("/get/downloads/finished/count")) {
            // Get number of finished DLs

            int counter = 0;

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                for (final DownloadLink dl : fp.getDownloadLinkList()) {
                    if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        counter++;
                    }
                }
            }

            response.addContent(counter);
        } else if (requestUrl.equals("/get/downloads/all/list")) {
            // Get DLList

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                final Element fp_xml = this.addFilePackage(xml, fp);

                for (final DownloadLink dl : fp.getDownloadLinkList()) {
                    fp_xml.appendChild(this.addDownloadLink(xml, dl));
                }
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/downloads/current/list")) {
            // Get current DLs

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                final Element fp_xml = this.addFilePackage(xml, fp);

                for (final DownloadLink dl : fp.getDownloadLinkList()) {
                    if (dl.getLinkStatus().isPluginActive()) {
                        fp_xml.appendChild(this.addDownloadLink(xml, dl));
                    }
                }
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/downloads/finished/list")) {
            // Get finished DLs

            for (final FilePackage fp : JDUtilities.getController().getPackages()) {
                final Element fp_xml = this.addFilePackage(xml, fp);

                for (final DownloadLink dl : fp.getDownloadLinkList()) {
                    if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                        fp_xml.appendChild(this.addDownloadLink(xml, dl));
                    }
                }
            }

            response.addContent(xml);
        } else if (requestUrl.matches("(?is).*/set/reconnect/(true|false)")) {
            // Set Reconnect enabled

            final boolean newrc = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/reconnect/(true|false)").getMatch(0));

            Serverhandler.logger.fine("RemoteControl - Set ReConnect: " + newrc);

            if (newrc != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, newrc);
                JDUtilities.getConfiguration().save();
            }

            response.addContent("PARAM_ALLOW_RECONNECT=" + newrc);
        } else if (requestUrl.matches("(?is).*/set/premium/(true|false)")) {
            // Set Use premium

            final boolean newuseprem = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/premium/(true|false)").getMatch(0));
            Serverhandler.logger.fine("RemoteControl - Set Premium: " + newuseprem);

            if (newuseprem != JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, true)) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_USE_GLOBAL_PREMIUM, newuseprem);
                JDUtilities.getConfiguration().save();
            }

            response.addContent("PARAM_USE_GLOBAL_PREMIUM=" + newuseprem);
        } else if (requestUrl.matches("(?is).*/set/downloaddir/general/.+")) {
            final String dir = new Regex(requestUrl, ".*/set/downloaddir/general/(.+)").getMatch(0);

            // TODO: Doesn't seem to work but I really don't know why :-/
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY, dir);
            SubConfiguration.getConfig("DOWNLOAD").save();

            response.addContent("PARAM_DOWNLOAD_DIRECTORY=" + dir);
        } else if (requestUrl.matches("(?is).*/set/download/limit/[0-9]+")) {
            // Set download limit

            final Integer newdllimit = Integer.parseInt(new Regex(requestUrl, ".*/set/download/limit/([0-9]+)").getMatch(0));
            Serverhandler.logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, newdllimit.toString());
            SubConfiguration.getConfig("DOWNLOAD").save();

            response.addContent("PARAM_DOWNLOAD_MAX_SPEED=" + newdllimit);
        } else if (requestUrl.matches("(?is).*/set/download/max/[0-9]+")) {
            // Set max. sim. downloads

            final Integer newsimdl = Integer.parseInt(new Regex(requestUrl, ".*/set/download/max/([0-9]+)").getMatch(0));
            Serverhandler.logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
            SubConfiguration.getConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, newsimdl.toString());
            SubConfiguration.getConfig("DOWNLOAD").save();

            response.addContent("PARAM_DOWNLOAD_MAX_SIMULTAN=" + newsimdl);
        } else if (requestUrl.matches("(?is).*/set/grabber/startafteradding/(true|false)")) {
            final boolean value = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/grabber/startafteradding/(true|false)").getMatch(0));

            Serverhandler.logger.fine("RemoteControl - Set PARAM_START_AFTER_ADDING_LINKS: " + value);

            if (value != GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, true)) {
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS, value);
                JDUtilities.getConfiguration().save();
            }

            response.addContent("PARAM_START_AFTER_ADDING_LINKS=" + value);
        } else if (requestUrl.matches("(?is).*/set/grabber/autoadding/(true|false)")) {
            final boolean value = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/grabber/autoadding/(true|false)").getMatch(0));

            Serverhandler.logger.fine("RemoteControl - Set PARAM_START_AFTER_ADDING_LINKS_AUTO: " + value);

            if (value != GUIUtils.getConfig().getBooleanProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, true)) {
                GUIUtils.getConfig().setProperty(JDGuiConstants.PARAM_START_AFTER_ADDING_LINKS_AUTO, value);
                JDUtilities.getConfiguration().save();
            }

            response.addContent("PARAM_START_AFTER_ADDING_LINKS_AUTO=" + value);
        } else if (requestUrl.equals("/action/start")) {
            // Do Start downloads

            DownloadWatchDog.getInstance().startDownloads();
            response.addContent("Downloads started");
        } else if (requestUrl.equals("/action/pause")) {
            // Do Pause downloads

            DownloadWatchDog.getInstance().pauseDownloads(!DownloadWatchDog.getInstance().isPaused());
            response.addContent("Downloads paused");
        } else if (requestUrl.equals("/action/stop")) {
            // Do Stop downloads

            DownloadWatchDog.getInstance().stopDownloads();
            response.addContent("Downloads stopped");
        } else if (requestUrl.equals("/action/toggle")) {
            // Do Toggle downloads

            DownloadWatchDog.getInstance().toggleStartStop();
            response.addContent("Downloads toggled");
        } else if (requestUrl.equals("/action/reconnect")) {
            // Do Reconnect

            response.addContent("Do Reconnect...");
            Reconnecter.getInstance().forceReconnect();
        } else if (requestUrl.matches(".*?/action/(force)?update")) {
            // Do Perform webupdate

            if (requestUrl.matches(".+/action/forceupdate")) {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true);
                JSonWrapper.get("WEBUPDATE").setProperty(Configuration.PARAM_WEBUPDATE_DISABLE, false);
            }

            WebUpdate.doUpdateCheck(true);
            response.addContent("Do Webupdate...");
        } else if (requestUrl.equals("/action/restart")) {
            // Do Restart JD

            response.addContent("Restarting...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (final InterruptedException e) {
                        JDLogger.exception(e);
                    }
                    JDUtilities.restartJD(false);
                }
            }).start();
        } else if (requestUrl.equals("/action/shutdown")) {
            // Do Shutdown JD

            response.addContent("Shutting down...");

            new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (final InterruptedException e) {
                        JDLogger.exception(e);
                    }
                    JDUtilities.getController().exit();
                }
            }).start();
        } else if (requestUrl.matches("(?is).*/action/add/links/.+")) {
            // Add link(s)

            final ArrayList<String> links = new ArrayList<String>();
            String link = new Regex(requestUrl, ".*/action/add/links/(.+)").getMatch(0);

            for (final String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(link, false), null)) {
                links.add(tlink);
            }

            if (request.getParameters().size() > 0) {
                final Iterator<String> it = request.getParameters().keySet().iterator();

                while (it.hasNext()) {
                    final String help = it.next();

                    final String reqParameter = request.getParameter(help);
                    if (!reqParameter.equals("")) {
                        links.add(reqParameter);
                    }
                }
            }

            final StringBuilder ret = new StringBuilder();
            final char tmp[] = new char[] { '"', '\r', '\n' };

            for (final String element : links) {
                ret.append('\"');
                ret.append(element.trim());
                ret.append(tmp);
            }

            link = ret.toString();
            new DistributeData(link, false).start();

            response.addContent("Link(s) added. (" + link + ")");
        } else if (requestUrl.matches("(?is).*/action/add/container/.+")) {
            // Open a local or remote DLC-container

            String dlcfilestr = new Regex(requestUrl, ".*/action/add/container/(.+)").getMatch(0);
            dlcfilestr = Encoding.htmlDecode(dlcfilestr);

            // import dlc
            if (dlcfilestr.matches("http://.*?\\.(dlc|ccf|rsdf)")) {
                // remote container file
                final String containerFormat = new Regex(dlcfilestr, ".+\\.((dlc|ccf|rsdf))").getMatch(0);
                final File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);

                try {
                    Browser.download(container, dlcfilestr);
                    JDController.loadContainerFile(container, false, false);

                    try {
                        Thread.sleep(3000);
                    } catch (final Exception e) {
                        JDLogger.exception(e);
                    }

                    container.delete();
                } catch (final Exception e) {
                    JDLogger.exception(e);
                }
            } else {
                // local container file
                JDController.loadContainerFile(new File(dlcfilestr), false, false);
            }

            response.addContent("Container opened. (" + dlcfilestr + ")");
        } else if (requestUrl.matches("(?is).*/action/save/container(/fromgrabber)?/.+")) {
            // Save linklist as DLC-container

            ArrayList<DownloadLink> dllinks = new ArrayList<DownloadLink>();

            String dlcfilestr = new Regex(requestUrl, ".*/action/save/container(/fromgrabber)/?(.+)").getMatch(1);
            dlcfilestr = Encoding.htmlDecode(dlcfilestr);

            final boolean savefromGrabber = new Regex(requestUrl, ".+/fromgrabber/.+").matches();

            if (savefromGrabber) {
                if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                    response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
                } else {
                    final ArrayList<LinkGrabberFilePackage> lgPackages = new ArrayList<LinkGrabberFilePackage>();
                    final ArrayList<FilePackage> packages = new ArrayList<FilePackage>();

                    // disable packages, confirm them, save dlc, then delete
                    synchronized (LinkGrabberController.ControllerLock) {
                        lgPackages.addAll(LinkGrabberController.getInstance().getPackages());

                        for (int i = 0; i < lgPackages.size(); i++) {
                            DownloadLink dl = null;

                            for (final DownloadLink link : lgPackages.get(i).getDownloadLinks()) {
                                dllinks.add(link); // collecting download
                                // links
                                link.setEnabled(false);
                                if (dl == null) {
                                    dl = link;
                                }
                            }

                            LinkGrabberPanel.getLinkGrabber().confirmPackage(lgPackages.get(i), null, i);
                            packages.add(dl.getFilePackage());
                        }

                        JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);

                        for (final FilePackage fp : packages) {
                            JDUtilities.getDownloadController().removePackage(fp);
                        }
                    }
                }
            } else {
                dllinks = JDUtilities.getDownloadController().getAllDownloadLinks();
                JDUtilities.getController().saveDLC(new File(dlcfilestr), dllinks);
            }

            response.addContent("Container saved. (" + dlcfilestr + ")");
        } else if (requestUrl.matches("(?is).*/action/grabber/set/archivepassword/.+/.+")) {
            // Add an archive password to package

            final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/set/archivepassword/(.+)/.+").getMatch(0).split("/"));
            final String password = new Regex(requestUrl, ".*/action/grabber/set/archivepassword/.+/(.+)").getMatch(0);

            final ArrayList<LinkGrabberFilePackage> packages = LinkGrabberController.getInstance().getPackages();
            final ArrayList<LinkGrabberFilePackage> packagesWithPW = new ArrayList<LinkGrabberFilePackage>();
            boolean isErrorMsg = false;

            // TODO: Could make trouble if info panel is opened by hand.
            synchronized (LinkGrabberController.ControllerLock) {
                outer: for (final String packagename : packagenames) {
                    for (final LinkGrabberFilePackage pack : packages) {
                        if (packagename.equals(pack.getName())) {
                            pack.setPassword(password);
                            packagesWithPW.add(pack);
                            LinkGrabberController.getInstance().throwRefresh();
                            continue outer;
                        }
                    }

                    if (!isErrorMsg) {
                        response.addContent("Package '" + packagename + "' doesn't exist! ");
                        isErrorMsg = true;
                    }
                }
            }

            if (packagesWithPW.size() > 0) {
                response.addContent("Added Password '" + password + "' to packages: ");

                for (int i = 0; i < packagesWithPW.size(); i++) {
                    if (i != 0) {
                        response.addContent(", ");
                    }
                    response.addContent("'" + packagesWithPW.get(i).getName() + "'");
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/set/downloaddir/.+/.+")) {
            // set download directory for a single package

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final String[] params = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/set/downloaddir/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    final LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(params[0]);

                    if (destPackage == null) {
                        response.addContent("ERROR: Package '" + params[0] + "' not found!");
                    } else {
                        destPackage.setDownloadDirectory(params[1]);
                        LinkGrabberController.getInstance().throwRefresh();
                        response.addContent("Set '" + params[1] + "' as download directory for package: '" + params[0] + "'.");
                    }
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/set/comment/.+/.+")) {
            // set download directory for a single package

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final String[] params = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/set/comment/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    final LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(params[0]);

                    if (destPackage == null) {
                        response.addContent("ERROR: Package '" + params[0] + "' not found!");
                    } else {
                        destPackage.setComment(params[1]);
                        LinkGrabberController.getInstance().throwRefresh();
                        response.addContent("Added comment '" + params[1] + "' to package: '" + params[0] + "'.");
                    }
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/join/.+")) {
            // Join link grabber packages

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<LinkGrabberFilePackage> srcPackages = new ArrayList<LinkGrabberFilePackage>();
                final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, ".*/action/grabber/join/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    final LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                    if (destPackage == null) {
                        response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                    } else {
                        // iterate packages; add all to tmp package list
                        // that match the given join names
                        for (final LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
                            for (final String src : packagenames) {
                                if (pack.getName().equals(src) && pack != destPackage) {
                                    srcPackages.add(pack);
                                }
                            }
                        }

                        // process src
                        for (final LinkGrabberFilePackage pack : srcPackages) {
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
                                if (i != 0) {
                                    response.addContent(", ");
                                }
                                response.addContent("'" + srcPackages.get(i).getName() + "'");
                            }
                        } else {
                            response.addContent("ERROR: No packages joined into '" + packagenames[0] + "'!");
                        }
                    }
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/rename/.+")) {
            // rename link grabber package

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/rename/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    final LinkGrabberFilePackage destPackage = LinkGrabberController.getInstance().getFPwithName(packagenames[0]);

                    if (destPackage == null) {
                        response.addContent("ERROR: Package '" + packagenames[0] + "' not found!");
                    } else {
                        destPackage.setName(packagenames[1]);
                        LinkGrabberController.getInstance().throwRefresh();
                        response.addContent("Package '" + packagenames[0] + "' renamed to '" + packagenames[1] + "'.");
                    }
                }
            }

        } else if (requestUrl.matches("(?is).*/action/grabber/confirmall")) {
            // add all links in linkgrabber list to download list

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                synchronized (LinkGrabberController.ControllerLock) {
                    packages.addAll(LinkGrabberController.getInstance().getPackages());

                    for (int i = 0; i < packages.size(); i++) {
                        LinkGrabberPanel.getLinkGrabber().confirmPackage(packages.get(i), null, i);
                    }

                    response.addContent("All links are now scheduled for download.");
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/confirm/.+")) {
            // add denoted links in linkgrabber list to download list

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<LinkGrabberFilePackage> addedlist = new ArrayList<LinkGrabberFilePackage>();
                final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/confirm/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    packages.addAll(LinkGrabberController.getInstance().getPackages());

                    for (int i = 0; i < packages.size(); i++) {
                        final LinkGrabberFilePackage fp = packages.get(i);

                        for (final String name : packagenames) {
                            if (name.equalsIgnoreCase(fp.getName())) {
                                LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, i);
                                addedlist.add(fp);
                            }
                        }
                    }

                    response.addContent("The following packages are now scheduled for download: ");

                    for (int i = 0; i < addedlist.size(); i++) {
                        if (i != 0) {
                            response.addContent(", ");
                        }
                        response.addContent("'" + addedlist.get(i).getName() + "'");
                    }
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/removetype/.+")) {
            // remove denoted links from grabber that matches the type
            // 'offline', 'available' (in grabber and download list)

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<String> delLinks = new ArrayList<String>();
                final ArrayList<String> delPackages = new ArrayList<String>();

                final String[] types = this.getHTMLDecoded(new Regex(requestUrl, "(?is).*/action/grabber/removetype/(.+)").getMatch(0).split("/"));

                synchronized (LinkGrabberController.ControllerLock) {
                    final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                    packages.addAll(LinkGrabberController.getInstance().getPackages());

                    response.addContent("Removing links from grabber of type: ");

                    for (int i = 0; i < types.length; ++i) {
                        if (i != 0) {
                            response.addContent(", ");
                        }
                        if (types[i].equals(Serverhandler.LINK_TYPE_OFFLINE) || types[i].equals(Serverhandler.LINK_TYPE_AVAIL)) {
                            response.addContent(types[i]);
                        } else {
                            response.addContent("(unknown type: " + types[i] + ")");
                        }
                    }

                    for (final LinkGrabberFilePackage fp : packages) {
                        final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>(fp.getDownloadLinks());

                        for (final DownloadLink link : links) {
                            for (final String type : types) {
                                if (type.equals(Serverhandler.LINK_TYPE_OFFLINE) && link.getAvailableStatus().equals(AvailableStatus.FALSE) || type.equals(Serverhandler.LINK_TYPE_AVAIL) && link.getLinkStatus().hasStatus(LinkStatus.ERROR_ALREADYEXISTS)) {
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
        } else if (requestUrl.matches("(?is).*/action/grabber/removeall")) {
            // remove all links from grabber

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();

                synchronized (LinkGrabberController.ControllerLock) {
                    packages.addAll(LinkGrabberController.getInstance().getPackages());

                    for (final LinkGrabberFilePackage fp : packages) {
                        LinkGrabberController.getInstance().removePackage(fp);
                    }

                    response.addContent("All links removed from grabber.");
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/remove/.+")) {
            // remove denoted packages from grabber

            final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, ".*/action/grabber/remove/(.+)").getMatch(0).split("/"));

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                final ArrayList<LinkGrabberFilePackage> removelist = new ArrayList<LinkGrabberFilePackage>();

                synchronized (LinkGrabberController.ControllerLock) {
                    packages.addAll(LinkGrabberController.getInstance().getPackages());

                    for (int i = 0; i < packages.size(); i++) {
                        final LinkGrabberFilePackage fp = packages.get(i);

                        for (final String name : packagenames) {
                            if (name.equalsIgnoreCase(fp.getName())) {
                                LinkGrabberController.getInstance().removePackage(fp);
                                removelist.add(fp);
                            }
                        }
                    }

                    response.addContent("The following packages were removed from grabber: ");

                    for (int i = 0; i < removelist.size(); i++) {
                        if (i != 0) {
                            response.addContent(", ");
                        }
                        response.addContent("'" + removelist.get(i).getName() + "'");
                    }
                }
            }
        } else if (requestUrl.matches("(?is).*/action/grabber/move/.+")) {
            // move links (index 1 to length-1) into package (index 0)

            if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
                response.addContent(Serverhandler.ERROR_LINK_GRABBER_RUNNING);
            } else {
                final Regex reg = new Regex(requestUrl, ".*/action/grabber/move/([^/]+)/(.+)");

                if (reg.getMatch(0) == null || reg.getMatch(1) == null) {
                    response.addContent(Serverhandler.ERROR_MALFORMED_REQUEST);
                    return;
                }

                final String dest_package_name = Encoding.htmlDecode(reg.getMatch(0));
                final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                for (final String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(reg.getMatch(1), false), null)) {
                    links.addAll(new DistributeData(tlink).findLinks());
                }

                boolean packageWasAvailable = false;
                int numLinksMoved = 0;
                final int numPackagesDeleted = 0;

                synchronized (LinkGrabberController.ControllerLock) {

                    // search package
                    LinkGrabberFilePackage dest_package = null;
                    for (final LinkGrabberFilePackage pack : LinkGrabberController.getInstance().getPackages()) {
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
                    final List<LinkGrabberFilePackage> availPacks = LinkGrabberController.getInstance().getPackages();
                    for (int k = 0; k < availPacks.size(); ++k) {

                        final LinkGrabberFilePackage pack = availPacks.get(k);
                        for (int i = 0; i < pack.size(); ++i) {

                            if (pack == dest_package) {
                                continue;
                            }

                            final DownloadLink packLink = pack.get(i);
                            for (final DownloadLink userLink : links) {

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
        } else if (requestUrl.matches("(?is).*/action/downloads/removeall")) {
            // remove all packages in download list

            final ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
            packages.addAll(DownloadController.getInstance().getPackages());

            for (final FilePackage fp : packages) {
                DownloadController.getInstance().removePackage(fp);
            }

            response.addContent("All scheduled packages removed.");
        } else if (requestUrl.matches("(?is).*/action/downloads/remove/.+")) {
            // remove denoted packages from download list

            final ArrayList<FilePackage> packages = new ArrayList<FilePackage>();
            final ArrayList<FilePackage> removelist = new ArrayList<FilePackage>();
            final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, ".*/action/downloads/remove/(.+)").getMatch(0).split("/"));

            packages.addAll(DownloadController.getInstance().getPackages());

            final int packagesSize = packages.size();
            for (int i = 0; i < packagesSize; i++) {
                final FilePackage fp = packages.get(i);

                for (final String name : packagenames) {
                    if (name.equalsIgnoreCase(fp.getName())) {
                        DownloadController.getInstance().removePackage(fp);
                        removelist.add(fp);
                    }
                }
            }

            response.addContent("The following packages were removed from download list: ");

            final int removelistSize = removelist.size();
            for (int i = 0; i < removelistSize; i++) {
                if (i != 0) {
                    response.addContent(", ");
                }
                response.addContent("'" + removelist.get(i).getName() + "'");
            }
        } else if (requestUrl.matches("(?is).*/special/check/.+")) {

            final ArrayList<String> links = new ArrayList<String>();

            final String link = new Regex(requestUrl, ".*/special/check/(.+)").getMatch(0);

            for (final String tlink : HTMLParser.getHttpLinks(Encoding.urlDecode(link, false), null)) {
                links.add(tlink);
            }

            if (request.getParameters().size() > 0) {
                final Iterator<String> it = request.getParameters().keySet().iterator();

                while (it.hasNext()) {
                    final String help = it.next();

                    if (!request.getParameter(help).equals("")) {
                        links.add(request.getParameter(help));
                    }
                }
            }

            // check all links sequentially
            ArrayList<DownloadLink> dls = new ArrayList<DownloadLink>();
            for (final String chklink : links) {

                dls = new DistributeData(chklink, false).findLinks();

                final Element element = xml.createElement("link");
                xml.getFirstChild().appendChild(element);
                element.setAttribute("url", chklink);

                for (final DownloadLink dl : dls) {
                    dl.getAvailableStatus(); // force update
                    final LinkGrabberFilePackage pack = LinkGrabberController.getInstance().getGeneratedPackage(dl);
                    dl.getFilePackage().setName(pack.getName());
                    element.appendChild(this.addGrabberLink(xml, dl));
                }
            }

            response.addContent(xml);
        } else if (requestUrl.matches("(?is).*/addon/.+")) {
            // search in addons
            final ArrayList<OptionalPluginWrapper> addons = OptionalPluginWrapper.getOptionalWrapper();
            Object cmdResponse = null;

            for (final OptionalPluginWrapper addon : addons) {

                if (addon != null && addon.isLoaded() && addon.isEnabled()) {
                    final PluginOptional addonIntance = addon.getPlugin();

                    if (addonIntance instanceof RemoteSupport) {
                        cmdResponse = ((RemoteSupport) addonIntance).handleRemoteCmd(requestUrl);
                    }
                }
            }

            if (cmdResponse != null) {
                if (cmdResponse instanceof String) {
                    response.addContent((String) cmdResponse);
                } else if (cmdResponse instanceof Document) {
                    response.addContent((Document) cmdResponse);
                } else {
                    response.addContent(Serverhandler.ERROR_UNKNOWN_RESPONSE_TYPE);
                }
            } else {
                response.addContent(Serverhandler.ERROR_MALFORMED_REQUEST);
            }
        } else {
            response.addContent(Serverhandler.ERROR_MALFORMED_REQUEST);
        }
    }
}