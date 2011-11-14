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

package org.jdownloader.extensions.remotecontrol;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import jd.config.Configuration;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.captcha.CaptchaDialogQueue;
import jd.controlling.captcha.CaptchaDialogQueueEntry;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.reconnect.ReconnectConfig;
import jd.controlling.reconnect.Reconnecter;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;
import jd.utils.WebUpdate;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.interfaces.Handler;
import org.jdownloader.extensions.interfaces.RemoteSupport;
import org.jdownloader.extensions.interfaces.Request;
import org.jdownloader.extensions.interfaces.Response;
import org.jdownloader.extensions.remotecontrol.helppage.HelpPage;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.update.RestartController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Serverhandler implements Handler {

    private static Logger          logger                      = JDLogger.getLogger();

    private static final String    LINK_TYPE_OFFLINE           = "offline";
    private static final String    LINK_TYPE_AVAIL             = "available";

    private static final String    ERROR_MALFORMED_REQUEST     = "JDRemoteControl - Malformed request. Use /help";
    private static final String    ERROR_LINK_GRABBER_RUNNING  = "ERROR: Link grabber is currently running. Please try again in a few seconds.";
    private static final String    ERROR_UNKNOWN_RESPONSE_TYPE = "Error: Unknown response type. Please inform us.";

    private final DecimalFormat    f                           = new DecimalFormat("#0.00");

    private RemoteControlExtension owner;

    public Serverhandler(RemoteControlExtension remoteControlModule) {
        owner = remoteControlModule;
        HelpPage.OWNER = remoteControlModule;
    }

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
        /* Also show the file's priority, raw status, and download URL */
        element.setAttribute("file_priority", dl.getPriority() + "");
        StringBuilder rawstatus = new StringBuilder();
        for (int num = 0; num < 32; num++) {
            if (dl.getLinkStatus().hasStatus(1 << num)) {
                if (rawstatus.length() != 0) rawstatus.append("|");
                rawstatus.append(LinkStatus.toString(1 << num));
            }
        }
        element.setAttribute("file_rawstatus", rawstatus.toString());
        element.setAttribute("file_download_url", dl.getDownloadURL().toString());
        return element;
    }

    private Element addFilePackage(final Document xml, final FilePackage fp) {
        final Element element = xml.createElement("packages");
        xml.getFirstChild().appendChild(element);
        element.setAttribute("package_name", fp.getName());
        element.setAttribute("package_percent", this.f.format(fp.getPercent()));
        element.setAttribute("package_linkstotal", fp.size() + "");
        element.setAttribute("package_ETA", Formatter.formatSeconds(fp.getETA()));
        element.setAttribute("package_speed", Formatter.formatReadable(DownloadWatchDog.getInstance().getDownloadSpeedbyFilePackage(fp)));
        element.setAttribute("package_loaded", Formatter.formatReadable(fp.getTotalKBLoaded()));
        element.setAttribute("package_size", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()));
        element.setAttribute("package_todo", Formatter.formatReadable(fp.getTotalEstimatedPackageSize() - fp.getTotalKBLoaded()));
        /*
         * Also show download directory, and whether or not it's expanded in the
         * GUI
         */
        element.setAttribute("package_downloaddir", fp.getDownloadDirectory());
        element.setAttribute("package_isexpanded", fp.getBooleanProperty("expanded", false) ? "true" : "false");
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

            response.addContent(this.owner.getVersion());
        } else if (requestUrl.equals("/get/version")) {
            // Get version

            response.addContent(JDUtilities.getJDTitle(0));
        } else if (requestUrl.equals("/get/config")) {
            // Get config

            Property config = JDUtilities.getConfiguration();

            Element element = xml.createElement("config");
            xml.getFirstChild().appendChild(element);

            if (request.getParameters().containsKey("sub")) {
                config = SubConfiguration.getConfig(request.getParameters().get("sub").toUpperCase());
            }

            HashMap<String, Object> props = null;
            if ((props = config.getProperties()) != null) {
                for (final Entry<String, Object> next : props.entrySet()) {
                    element.setAttribute(next.getKey(), next.getValue() + "");
                }
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/ip")) {
            // Get IP

            if (JsonConfig.create(ReconnectConfig.class).isIPCheckGloballyDisabled()) {
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

            final int value = JsonConfig.create(GeneralSettings.class).isDownloadSpeedLimitEnabled() ? JsonConfig.create(GeneralSettings.class).getDownloadSpeedLimit() : 0;
            response.addContent(value);
        } else if (requestUrl.equals("/get/downloadstatus")) {
            // Get download status
            /* TODO: wait for my other commits */
            // response.addContent(DownloadWatchDog.getInstance().getStateMachine().getState().getLabel());
        } else if (requestUrl.equals("/get/isreconnect")) {
            // Get isReconnect

            response.addContent(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true));
        } else if (requestUrl.equals("/get/grabber/isset/startafteradding")) {
            // Get whether start after adding option is set or not

            final boolean value = JsonConfig.create(GeneralSettings.class).isAutoDownloadStartAfterAddingEnabled();
            response.addContent(value);
        } else if (requestUrl.equals("/get/grabber/isset/autoadding")) {
            // Get whether auto-adding option is set or not

            final boolean value = JsonConfig.create(GeneralSettings.class).isAutoaddLinksAfterLinkcheck();
            response.addContent(value);
        } else if (requestUrl.equals("/get/downloads/all/count")) {
            // Get number of all DLs in DL-list

            int counter = 0;
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (final FilePackage fp : DownloadController.getInstance().getPackages()) {
                    counter += fp.size();
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }
            response.addContent(counter);
        } else if (requestUrl.equals("/get/downloads/current/count")) {
            // Get number of current DLs

            response.addContent(DownloadWatchDog.getInstance().getActiveDownloads());
        } else if (requestUrl.equals("/get/downloads/finished/count")) {
            // Get number of finished DLs

            int counter = 0;
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (final FilePackage fp : DownloadController.getInstance().getPackages()) {
                    synchronized (fp) {
                        for (final DownloadLink dl : fp.getChildren()) {
                            if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                                counter++;
                            }
                        }
                    }
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }

            response.addContent(counter);
        } else if (requestUrl.equals("/get/downloads/all/list")) {
            // Get DLList
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (final FilePackage fp : DownloadController.getInstance().getPackages()) {
                    final Element fp_xml = this.addFilePackage(xml, fp);
                    synchronized (fp) {
                        for (final DownloadLink dl : fp.getChildren()) {
                            fp_xml.appendChild(this.addDownloadLink(xml, dl));
                        }
                    }
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/downloads/current/list")) {
            // Get current DLs
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (final FilePackage fp : DownloadController.getInstance().getPackages()) {
                    final Element fp_xml = this.addFilePackage(xml, fp);
                    synchronized (fp) {
                        for (final DownloadLink dl : fp.getChildren()) {
                            if (dl.getLinkStatus().isPluginActive()) {
                                fp_xml.appendChild(this.addDownloadLink(xml, dl));
                            }
                        }
                    }
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }

            response.addContent(xml);
        } else if (requestUrl.equals("/get/downloads/finished/list")) {
            // Get finished DLs
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                for (final FilePackage fp : DownloadController.getInstance().getPackages()) {
                    final Element fp_xml = this.addFilePackage(xml, fp);
                    synchronized (fp) {
                        for (final DownloadLink dl : fp.getChildren()) {
                            if (dl.getLinkStatus().hasStatus(LinkStatus.FINISHED)) {
                                fp_xml.appendChild(this.addDownloadLink(xml, dl));
                            }
                        }
                    }
                }
            } finally {
                DownloadController.getInstance().readUnlock(readL);
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

            JsonConfig.create(GeneralSettings.class).setDefaultDownloadFolder(dir);
            response.addContent("PARAM_DOWNLOAD_DIRECTORY=" + dir);
        } else if (requestUrl.matches("(?is).*/set/download/limit/[0-9]+")) {
            // Set download limit

            final Integer newdllimit = Integer.parseInt(new Regex(requestUrl, ".*/set/download/limit/([0-9]+)").getMatch(0));
            Serverhandler.logger.fine("RemoteControl - Set max. Downloadspeed: " + newdllimit.toString());
            // JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED,
            // newdllimit.toString());
            JsonConfig.create(GeneralSettings.class).setDownloadSpeedLimit(newdllimit);
            // JSonWrapper.get("DOWNLOAD").save();

            response.addContent("PARAM_DOWNLOAD_MAX_SPEED=" + newdllimit);
        } else if (requestUrl.matches("(?is).*/set/download/max/[0-9]+")) {
            // Set max. sim. downloads

            final Integer newsimdl = Integer.parseInt(new Regex(requestUrl, ".*/set/download/max/([0-9]+)").getMatch(0));
            Serverhandler.logger.fine("RemoteControl - Set max. sim. Downloads: " + newsimdl.toString());
            // JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN,
            // newsimdl.toString());
            //
            // JSonWrapper.get("DOWNLOAD").save();
            JsonConfig.create(GeneralSettings.class).setMaxSimultaneDownloads(newsimdl);

            response.addContent("PARAM_DOWNLOAD_MAX_SIMULTAN=" + newsimdl);
        } else if (requestUrl.matches("(?is).*/set/grabber/startafteradding/(true|false)")) {
            final boolean value = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/grabber/startafteradding/(true|false)").getMatch(0));

            Serverhandler.logger.fine("RemoteControl - Set PARAM_START_AFTER_ADDING_LINKS: " + value);
            //

            JsonConfig.create(GeneralSettings.class).setAutoDownloadStartAfterAddingEnabled(value);

            response.addContent("PARAM_START_AFTER_ADDING_LINKS=" + value);
        } else if (requestUrl.matches("(?is).*/set/grabber/autoadding/(true|false)")) {
            final boolean value = Boolean.parseBoolean(new Regex(requestUrl, ".*/set/grabber/autoadding/(true|false)").getMatch(0));

            Serverhandler.logger.fine("RemoteControl - Set PARAM_START_AFTER_ADDING_LINKS_AUTO: " + value);

            JsonConfig.create(GeneralSettings.class).setAutoaddLinksAfterLinkcheck(value);

            response.addContent("PARAM_START_AFTER_ADDING_LINKS_AUTO=" + value);
        } else if (requestUrl.equals("/action/start")) {
            // Do Start downloads

            DownloadWatchDog.getInstance().startDownloads();
            response.addContent("Downloads started");
        } else if (requestUrl.equals("/action/pause")) {
            // Do Pause downloads

            DownloadWatchDog.getInstance().pauseDownloadWatchDog(!DownloadWatchDog.getInstance().isPaused());
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
        } else if (requestUrl.equals("/action/captcha/getcurrent")) {
            // Get current captcha image

            CaptchaDialogQueueEntry entry = CaptchaDialogQueue.getInstance().getCurrentQueueEntry();
            if (entry == null) {
                response.addContent("none available");
            } else {
                File captchafile = entry.getFile();
                response.setReturnType("image/jpeg");
                try {
                    BufferedImage image = ImageIO.read(captchafile);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    response.setByteData(baos.toByteArray());
                } catch (final Throwable e) {
                    e.printStackTrace();
                    response.addContent("error");
                }
            }
        } else if (requestUrl.matches("(?is).*/action/captcha/solve/.+")) {
            // Solve captcha

            String code = Encoding.urlDecode(new Regex(requestUrl, ".*/action/captcha/solve/(.+)").getMatch(0), false);

            CaptchaDialogQueueEntry entry = CaptchaDialogQueue.getInstance().getCurrentQueueEntry();
            if (entry != null) {
                entry.setResponse(code);

                response.addContent("CAPTCHA-code has been sent.");
            } else {
                response.addContent("no captcha currently available");
            }
        } else if (requestUrl.matches(".*?/action/update")) {
            // Do Perform webupdate

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
                    RestartController.getInstance().directRestart();
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
                    }
                    RestartController.getInstance().exit();
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
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(link));

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
                    container.deleteOnExit();
                    LinkCrawler lc = LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob("file://" + container));
                    lc.waitForCrawling();
                    container.delete();
                } catch (final Throwable e) {
                    JDLogger.exception(e);
                }
            } else {
                // local container file
                LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob("file://" + dlcfilestr));
            }

            response.addContent("Container opened. (" + dlcfilestr + ")");
        } else if (requestUrl.matches("(?is).*/action/downloads/removeall")) {
            // remove all packages in download list
            ArrayList<FilePackage> packages = null;
            boolean readL = DownloadController.getInstance().readLock();
            try {
                packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }
            for (final FilePackage fp : packages) {
                DownloadController.getInstance().removePackage(fp);
            }

            response.addContent("All scheduled packages removed.");
        } else if (requestUrl.matches("(?is).*/action/downloads/remove/.+")) {
            // remove denoted packages from download list

            ArrayList<FilePackage> packages = null;
            final ArrayList<FilePackage> removelist = new ArrayList<FilePackage>();
            final String[] packagenames = this.getHTMLDecoded(new Regex(requestUrl, ".*/action/downloads/remove/(.+)").getMatch(0).split("/"));
            final boolean readL = DownloadController.getInstance().readLock();
            try {
                packages = new ArrayList<FilePackage>(DownloadController.getInstance().getPackages());
            } finally {
                DownloadController.getInstance().readUnlock(readL);
            }

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
        } else if (requestUrl.matches("(?is).*/addon/.+")) {
            // search in addons
            final ArrayList<AbstractExtension<?>> addons = ExtensionController.getInstance().getEnabledExtensions();
            Object cmdResponse = null;

            for (final AbstractExtension addon : addons) {

                if (addon instanceof RemoteSupport) {
                    cmdResponse = ((RemoteSupport) addon).handleRemoteCmd(requestUrl);
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