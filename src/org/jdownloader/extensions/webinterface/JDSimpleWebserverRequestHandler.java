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

package org.jdownloader.extensions.webinterface;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.DistributeData;
import jd.controlling.DownloadController;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.PasswordListController;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.swing.jdgui.views.linkgrabber.LinkGrabberPanel;
import jd.gui.swing.jdgui.views.settings.panels.JSonWrapper;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.jdownloader.update.RestartController;

public class JDSimpleWebserverRequestHandler {

    // private SubConfiguration guiConfig = null;
    private final HashMap<String, String>          headers;

    private final Logger                           logger = jd.controlling.JDLogger.getLogger();
    private final JDSimpleWebserverResponseCreator response;

    private final LinkGrabberController            lgi;

    public JDSimpleWebserverRequestHandler(final HashMap<String, String> headers, final JDSimpleWebserverResponseCreator response) {
        this.lgi = LinkGrabberController.getInstance();
        this.response = response;

        this.headers = headers;
    }

    public void handle() {

        final String request = this.headers.get(null);

        final String[] requ = request.split(" ");

        final String cPath = requ[1];
        String path, querry;
        path = cPath.substring(1);
        String[] params;
        final HashMap<String, String> requestParameter = new HashMap<String, String>();

        /* bekanntgebung der mehrfach belegbaren parameter */
        requestParameter.put("package_all_downloads_counter", "0");
        requestParameter.put("package_single_download_counter", "0");
        requestParameter.put("package_all_add_counter", "0");
        requestParameter.put("package_single_add_counter", "0");

        if (cPath.indexOf("?") >= 0) {
            querry = cPath.substring(cPath.indexOf("?") + 1);
            path = cPath.substring(1, cPath.indexOf("?"));
            params = querry.split("\\&");

            for (String entry : params) {
                entry = entry.trim();
                final int index = entry.indexOf("=");
                String key = entry;

                String value = null;
                if (index >= 0) {
                    key = entry.substring(0, index);
                    value = entry.substring(index + 1);
                }

                if (requestParameter.containsKey(key) || requestParameter.containsKey(key + "_counter")) {
                    /*
                     * keys mit _counter können mehrfach belegt werden, müssen
                     * vorher aber bekannt gegeben werden
                     */
                    if (requestParameter.containsKey(key + "_counter")) {
                        Integer keycounter = 0;
                        keycounter = Formatter.filterInt(requestParameter.get(key + "_counter"));
                        keycounter++;
                        requestParameter.put(key + "_counter", keycounter.toString());
                        requestParameter.put(key + "_" + keycounter.toString(), value);
                    }
                } else {
                    requestParameter.put(key, value);
                }
            }
        }
        String url = path.replaceAll("\\.\\.", "");

        /* parsen der paramter */
        if (requestParameter.containsKey("do")) {
            if (requestParameter.get("do").compareToIgnoreCase("submit") == 0) {
                if (requestParameter.containsKey("speed")) {
                    int setspeed = Formatter.filterInt(requestParameter.get("speed"));
                    if (setspeed < 0) {
                        setspeed = 0;
                    }
                    JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, setspeed);
                }

                if (requestParameter.containsKey("maxdls")) {
                    int maxdls = Formatter.filterInt(requestParameter.get("maxdls"));
                    if (maxdls < 1) {
                        maxdls = 1;
                    }
                    if (maxdls > 20) {
                        maxdls = 20;
                    }
                    JSonWrapper.get("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, maxdls);
                }

                if (!requestParameter.containsKey("selected_dowhat_link_adder")) {
                    if (requestParameter.containsKey("autoreconnect")) {
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
                    } else {
                        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, false);
                    }
                }
                if (requestParameter.containsKey("package_single_add_counter")) {
                    synchronized (LinkGrabberController.ControllerLock) {
                        synchronized (this.lgi.getPackages()) {
                            /* aktionen in der adder liste ausführen */
                            Integer download_id = 0;
                            Integer package_id = 0;
                            String[] ids;
                            final int counter_max = Formatter.filterInt(requestParameter.get("package_single_add_counter"));
                            int counter_index = 0;
                            DownloadLink link;
                            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                            final ArrayList<LinkGrabberFilePackage> packages = new ArrayList<LinkGrabberFilePackage>();
                            for (counter_index = 1; counter_index <= counter_max; counter_index++) {
                                if (requestParameter.containsKey("package_single_add_" + counter_index)) {
                                    ids = requestParameter.get("package_single_add_" + counter_index).toString().split("[+]", 2);
                                    package_id = Formatter.filterInt(ids[0].toString());
                                    download_id = Formatter.filterInt(ids[1].toString());
                                    links.add(this.lgi.getPackages().get(package_id).get(download_id));
                                    if (!packages.contains(this.lgi.getPackages().get(package_id))) {
                                        packages.add(this.lgi.getPackages().get(package_id));
                                    }
                                }
                            }
                            if (requestParameter.containsKey("selected_dowhat_link_adder")) {
                                final String dowhat = requestParameter.get("selected_dowhat_link_adder");
                                /* packages-namen des link-adders aktuell halten */

                                for (int i = 0; i < this.lgi.getPackages().size(); i++) {
                                    if (requestParameter.containsKey("adder_package_name_" + i)) {
                                        this.lgi.getPackages().get(i).setName(Encoding.htmlDecode(requestParameter.get("adder_package_name_" + i).toString()));
                                    }
                                }

                                if (dowhat.compareToIgnoreCase("remove") == 0) {
                                    /* entfernen */
                                    for (final LinkGrabberFilePackage fp : packages) {
                                        fp.remove(links);
                                    }
                                } else if (dowhat.compareToIgnoreCase("remove+offline") == 0) {
                                    /* entfernen(offline) */
                                    links = new ArrayList<DownloadLink>();
                                    for (int i = 0; i < this.lgi.getPackages().size(); i++) {
                                        for (int ii = 0; ii < this.lgi.getPackages().get(i).size(); ii++) {
                                            links.add(this.lgi.getPackages().get(i).get(ii));
                                        }
                                    }
                                    for (final Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                        link = it.next();
                                        if (link.isAvailabilityStatusChecked() == true && link.isAvailable() == false) {
                                            link.getFilePackage().remove(link);
                                        }
                                    }
                                } else if (dowhat.compareToIgnoreCase("add") == 0) {
                                    /* link adden */
                                    for (final LinkGrabberFilePackage fp : packages) {
                                        LinkGrabberPanel.getLinkGrabber().confirmPackage(fp, null, -1);
                                    }

                                }
                            }
                        }
                    }
                }

                if (requestParameter.containsKey("package_single_download_counter")) {

                    // Aktionen in der Download-liste ausführen
                    Integer download_id = 0;
                    Integer package_id = 0;
                    String[] ids;
                    final int counter_max = Formatter.filterInt(requestParameter.get("package_single_download_counter"));
                    int counter_index = 0;
                    DownloadLink link;
                    final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
                    for (counter_index = 1; counter_index <= counter_max; counter_index++) {
                        if (requestParameter.containsKey("package_single_download_" + counter_index)) {
                            ids = requestParameter.get("package_single_download_" + counter_index).toString().split("[+]", 2);
                            package_id = Formatter.filterInt(ids[0].toString());
                            download_id = Formatter.filterInt(ids[1].toString());

                            links.add(JDUtilities.getController().getPackages().get(package_id).getDownloadLinkList().get(download_id));
                        }
                    }

                    if (requestParameter.containsKey("selected_dowhat_index")) {
                        final String dowhat = requestParameter.get("selected_dowhat_index");
                        if (dowhat.compareToIgnoreCase("activate") == 0) {
                            /* aktivieren */
                            for (final Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(true);
                            }
                            DownloadController.getInstance().fireGlobalUpdate();
                        }
                        if (dowhat.compareToIgnoreCase("deactivate") == 0) {
                            /* deaktivieren */
                            for (final Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setEnabled(false);
                            }
                            DownloadController.getInstance().fireGlobalUpdate();
                        }
                        if (dowhat.compareToIgnoreCase("reset") == 0) {
                            /*
                             * reset
                             */
                            for (final Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.getLinkStatus().setStatus(LinkStatus.TODO);
                                link.getLinkStatus().setStatusText("");
                                link.reset();
                            }
                            DownloadController.getInstance().fireGlobalUpdate();
                        }
                        if (dowhat.compareToIgnoreCase("remove") == 0) {

                            // entfernen
                            for (final DownloadLink dl : links) {
                                dl.getFilePackage().remove(dl);
                            }
                        }
                        if (dowhat.compareToIgnoreCase("abort") == 0) {

                            // abbrechen
                            for (final Iterator<DownloadLink> it = links.iterator(); it.hasNext();) {
                                link = it.next();
                                link.setAborted(true);
                            }
                            DownloadController.getInstance().fireGlobalUpdate();
                        }
                    }
                }

            } else if (requestParameter.get("do").compareToIgnoreCase("reconnect") == 0) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (final InterruptedException e) {
                            JDLogger.exception(e);
                        }

                        if (Reconnecter.getInstance().forceReconnect()) {
                            JDSimpleWebserverRequestHandler.this.logger.info("Reconnect erfolgreich");
                        } else {
                            JDSimpleWebserverRequestHandler.this.logger.info("Reconnect fehlgeschlagen");
                        }
                    }
                }).start();

            } else if (requestParameter.get("do").compareToIgnoreCase("close") == 0) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (final InterruptedException e) {
                            JDLogger.exception(e);
                        }
                        JDUtilities.getController().exit();
                    }
                }).start();
            } else if (requestParameter.get("do").compareToIgnoreCase("start") == 0) {
                DownloadWatchDog.getInstance().startDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("stop") == 0) {
                DownloadWatchDog.getInstance().stopDownloads();
            } else if (requestParameter.get("do").compareToIgnoreCase("restart") == 0) {
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Thread.sleep(2000);
                        } catch (final InterruptedException e) {

                            JDLogger.exception(e);
                        }
                        RestartController.getInstance().directRestart();
                    }
                }).start();

            } else if (requestParameter.get("do").compareToIgnoreCase("add") == 0) {
                if (requestParameter.containsKey("addlinks")) {
                    final String AddLinks = Encoding.htmlDecode(requestParameter.get("addlinks"));
                    final ArrayList<DownloadLink> waitingLinkList = new DistributeData(AddLinks).findLinks();
                    LinkGrabberPanel.getLinkGrabber().addLinks(waitingLinkList);
                }
            } else if (requestParameter.get("do").compareToIgnoreCase("upload") == 0) {
                if (requestParameter.containsKey("file")) {
                    final File container = JDUtilities.getResourceFile("container/" + requestParameter.get("file"));
                    final ArrayList<DownloadLink> waitingLinkList = JDUtilities.getController().getContainerLinks(container);
                    LinkGrabberPanel.getLinkGrabber().addLinks(waitingLinkList);
                }
            }
        }
        /* passwortliste verändern */
        if (requestParameter.containsKey("passwd")) {
            if (requestParameter.get("passwd").compareToIgnoreCase("save") == 0) {
                if (requestParameter.containsKey("password_list")) {
                    final String passwordList = Encoding.htmlDecode(requestParameter.get("password_list"));
                    final ArrayList<String> pws = new ArrayList<String>();
                    for (final String pw : org.appwork.utils.Regex.getLines(passwordList)) {
                        pws.add(0, pw);
                    }
                    PasswordListController.getInstance().setPasswordList(pws);
                }
            }
        }

        File fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
        if (!fileToRead.isFile()) {
            /*
             * default soll zur index.tmpl gehen, fall keine angabe gemacht
             * wurde
             */
            final String tempurl = url + "index.tmpl";
            final File fileToRead2 = JDUtilities.getResourceFile("plugins/webinterface/" + tempurl);
            if (fileToRead2.isFile()) {
                url = tempurl;
                fileToRead = JDUtilities.getResourceFile("plugins/webinterface/" + url);
            }
        }

        if (!fileToRead.exists()) {
            this.response.setNotFound(url);
        } else {
            if (new Regex(url, ".+\\.tmpl").matches()) {
                JDSimpleWebserverTemplateFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverTemplateFileRequestHandler(this.response);
                filerequest.handleRequest(url, requestParameter);
            } else {
                JDSimpleWebserverStaticFileRequestHandler filerequest;
                filerequest = new JDSimpleWebserverStaticFileRequestHandler(this.headers, this.response);
                filerequest.handleRequest(url, requestParameter);
            }
        }
    }
}