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

package jd.plugins.optional.webinterface;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import jd.OptionalPluginWrapper;
import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.PasswordListController;
import jd.gui.swing.jdgui.views.linkgrabberview.LinkGrabberPanel;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkGrabberFilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterface.Chunk;
import jd.plugins.optional.webinterface.template.Template;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

public class JDSimpleWebserverTemplateFileRequestHandler {

    private DecimalFormat f = new DecimalFormat("#0");
    private JDSimpleWebserverResponseCreator response;

    private Vector<Object> v_info = new Vector<Object>();
    private LinkGrabberController lgi;

    /**
     * Create a new handler that serves files from a base directory
     * 
     * @param base
     *            directory
     */
    public JDSimpleWebserverTemplateFileRequestHandler(JDSimpleWebserverResponseCreator response) {
        lgi = LinkGrabberController.getInstance();
        this.response = response;
    }

    private void add_all_info(Template t, HashMap<String, String> requestParameter) {
        FilePackage fp;
        String[] ids;
        String Single_Status;
        Integer package_id = 0;
        if (requestParameter.containsKey("all_info")) {
            ids = requestParameter.get("all_info").toString().split("[+]", 2);
            package_id = Formatter.filterInt(ids[0].toString());
            fp = JDUtilities.getController().getPackages().get(package_id);

            addEntry("name", fp.getName());
            addEntry("comment", fp.getComment());
            addEntry("dldirectory", fp.getDownloadDirectory());
            addEntry("packagesize", Formatter.formatReadable(fp.getTotalEstimatedPackageSize()) + " " + fp.getTotalEstimatedPackageSize() + " KB");
            addEntry("loaded", Formatter.formatReadable(fp.getTotalKBLoaded()) + " " + fp.getTotalKBLoaded() + " KB");
            addEntry("links", "");

            DownloadLink next = null;
            int i = 1;
            for (Iterator<DownloadLink> it = fp.getDownloadLinkList().iterator(); it.hasNext(); i++) {
                Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
                next = it.next();
                if (next.isEnabled()) {
                    switch (next.getLinkStatus().getLatestStatus()) {
                    case LinkStatus.FINISHED:
                        Single_Status = "finished";
                        break;
                    case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                        Single_Status = "running";
                        break;
                    default:
                        Single_Status = "activated";
                    }
                } else {
                    Single_Status = "deactivated";
                }
                double percent = next.getPercent() / 100.0;

                h_info.put("info_percent", f.format(percent));
                h_info.put("download_status", Single_Status);
                h_info.put("info_var", i + ". " + next.getName());
                h_info.put("info_value", Formatter.formatReadable(next.getDownloadSpeed()) + "/s " + f.format(next.getPercent() / 100.0) + " %| " + next.getDownloadCurrent() + "/" + next.getDownloadSize() + " bytes");
                h_info.put("download_id", i - 1);/*
                                                  * von 0 anfangen für js
                                                  * skripte
                                                  */
                v_info.addElement(h_info);
            }
            t.setParam("all_infos", v_info);
        }
    }

    private void add_linkadder_page(Template t, HashMap<String, String> requestParameter) {
        Vector<Object> v, v2;
        Hashtable<Object, Object> h, h2;
        v = new Vector<Object>();

        LinkGrabberFilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
        synchronized (lgi.getPackages()) {
            for (Package_ID = 0; Package_ID < lgi.getPackages().size(); Package_ID++) {
                filePackage = lgi.getPackages().get(Package_ID);

                h = new Hashtable<Object, Object>();
                /* Paket Infos */
                h.put("download_name", filePackage.getName());

                h.put("package_id", Package_ID.toString());

                v2 = new Vector<Object>();

                for (Download_ID = 0; Download_ID < filePackage.getDownloadLinks().size(); Download_ID++) {
                    dLink = filePackage.getDownloadLinks().get(Download_ID);

                    /* Download Infos */
                    h2 = new Hashtable<Object, Object>();

                    h2.put("package_id", Package_ID.toString());
                    h2.put("download_id", Download_ID.toString());
                    h2.put("download_name", dLink.getName());
                    if (dLink.isAvailabilityStatusChecked() && dLink.isAvailable()) {
                        h2.put("download_status", "online");
                    } else {
                        h2.put("download_status", "offline");
                    }

                    h2.put("download_hoster", dLink.getHost());

                    v2.addElement(h2);
                }
                h.put("downloads", v2);
                v.addElement(h);
            }
        }
        // t.setParam("message_status", "show");
        // t.setParam("message", "great work");
        t.setParam("pakete", v);
        if (LinkGrabberPanel.getLinkGrabber().isRunning()) {
            t.setParam("message_status", "show");
            t.setParam("message", "LinkGrabber still Running! Please Reload Page in few Secs!");
        }

    }

    private void add_password_list(Template t, HashMap<String, String> requestParameter) {
        StringBuilder pwlist = new StringBuilder();

        ArrayList<String> arrayList = PasswordListController.getInstance().getPasswordList();
        if (arrayList != null) {
            for (String pw : arrayList) {
                if (!pw.trim().equals("")) {
                    pwlist.append(System.getProperty("line.separator")).append(pw);
                }
            }
        }

        t.setParam("password_list", pwlist.toString());
    }

    private void add_single_info(Template t, HashMap<String, String> requestParameter) {

        /* überprüft ob single_info vorhanden und füllt ggf. dieses template */
        DownloadLink downloadLink;
        Integer download_id = 0;
        Integer package_id = 0;
        String[] ids;
        String Single_Status;
        if (requestParameter.containsKey("single_info")) {
            ids = requestParameter.get("single_info").toString().split("[+]", 2);
            package_id = Formatter.filterInt(ids[0].toString());
            download_id = Formatter.filterInt(ids[1].toString());
            downloadLink = JDUtilities.getController().getPackages().get(package_id).getDownloadLinkList().get(download_id);

            addEntry("file", new File(downloadLink.getFileOutput()).getName() + " @ " + downloadLink.getHost());
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getPassword() != null) {
                addEntry(JDL.L("gui.linkinfo.password", "Password"), downloadLink.getFilePackage().getPassword());
            }
            if (downloadLink.getFilePackage() != null && downloadLink.getFilePackage().getComment() != null) {
                addEntry(JDL.L("gui.linkinfo.comment", "Comment"), downloadLink.getFilePackage().getComment());
            }
            if (downloadLink.getFilePackage() != null) {
                addEntry(JDL.L("gui.linkinfo.package", "Package"), downloadLink.getFilePackage().getName());
            }
            if (downloadLink.getDownloadSize() > 0) {
                addEntry(JDL.L("gui.linkinfo.filesize", "Filesize"), Formatter.formatReadable(downloadLink.getDownloadSize()));
            }
            if (downloadLink.isAborted()) {
                addEntry(JDL.L("gui.linkinfo.download", "Download"), JDL.L("linkinformation.download.aborted", "Aborted"));
            }
            if (downloadLink.isAvailabilityStatusChecked()) {
                addEntry(JDL.L("gui.linkinfo.available", "Available"), downloadLink.isAvailable() ? JDL.L("gui.linkinfo.available.ok", "File is OK") : JDL.L("linkinformation.available.error", "Error!"));
            } else {
                addEntry(JDL.L("gui.linkinfo.available", "Available"), JDL.L("gui.linkinfo.available.notchecked", "Not checked"));
            }
            if (downloadLink.getDownloadSpeed() > 0) {
                addEntry(JDL.L("gui.linkinfo.speed", "Speed"), Formatter.formatReadable(downloadLink.getDownloadSpeed()) + " /s");
            }
            if (downloadLink.getFileOutput() != null) {
                addEntry(JDL.L("gui.linkinfo.saveto", "Save to"), downloadLink.getFileOutput());
            }
            if (DownloadWatchDog.getInstance().getRemainingTempUnavailWaittime(downloadLink.getHost()) > 0) {
                addEntry(JDL.L("gui.linkinfo.waittime", "Wait time"), JDL.LF("gui.linkinfo.secs", "{0} sec",  DownloadWatchDog.getInstance().getRemainingTempUnavailWaittime(downloadLink.getHost()) / 1000));
            } else if (DownloadWatchDog.getInstance().getRemainingIPBlockWaittime(downloadLink.getHost()) > 0) {
                addEntry(JDL.L("gui.linkinfo.waittime", "Wait time"), JDL.LF("gui.linkinfo.secs", "{0} sec",  DownloadWatchDog.getInstance().getRemainingIPBlockWaittime(downloadLink.getHost()) / 1000));
            }
            if (downloadLink.getLinkStatus().isPluginActive()) {
                addEntry(JDL.L("gui.linkinfo.download", "Download"), JDL.L("gui.linkinfo.download.underway", "is in process"));
            } else {
                addEntry(JDL.L("gui.linkinfo.download", "Download"), JDL.L("gui.linkinfo.download.notunderway", "is not in process"));
            }
            if (!downloadLink.isEnabled()) {
                addEntry(JDL.L("gui.linkinfo.download", "Download"), JDL.L("gui.linkinfo.download.deactivated", "is deactivated"));
            } else {
                addEntry(JDL.L("gui.linkinfo.download", "Download"), JDL.L("gui.linkinfo.download.activated", "is activated"));
            }
            addEntry(JDL.L("gui.linkinfo.status", "Status"), downloadLink.getLinkStatus().getStatusString());

            if (downloadLink.isEnabled()) {
                switch (downloadLink.getLinkStatus().getLatestStatus()) {
                case LinkStatus.FINISHED:
                    Single_Status = "finished";
                    break;
                case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                    Single_Status = "running";
                    break;
                default:
                    Single_Status = "activated";
                }
            } else {
                Single_Status = "deactivated";
            }
            DownloadInterface dl;
            if (downloadLink.getLinkStatus().isPluginActive() && (dl = downloadLink.getDownloadInstance()) != null) {
                addEntry(JDL.L("linkinformation.download.chunks.label", "Chunks"), "");
                int i = 1;
                for (Iterator<Chunk> it = dl.getChunks().iterator(); it.hasNext(); i++) {
                    Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
                    Chunk next = it.next();
                    double percent = next.getPercent() / 100.0;
                    h_info.put("download_status", Single_Status);
                    h_info.put("info_var", JDL.L("download.chunks.connection", "Connection") + " " + i);
                    h_info.put("info_value", Formatter.formatReadable(next.getSpeed()) + "/s " + f.format(next.getPercent() / 100.0) + " %");
                    h_info.put("info_percent", f.format(percent));
                    h_info.put("download_id", i - 1);/*
                                                      * von 0 anfangen für js
                                                      * skripte
                                                      */
                    v_info.addElement(h_info);
                }
            }
            t.setParam("single_infos", v_info);
        }
    }

    /*
     * private void addEntryandPercent(String var, String value, double percent)
     * { Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
     * h_info.put("info_var", var); h_info.put("info_value", value);
     * h_info.put("info_percent", f.format(percent)); v_info.addElement(h_info);
     * }
     */
    private void add_status_page(Template t, HashMap<String, String> requestParameter) {
        Vector<Object> v, v2;
        Hashtable<Object, Object> h, h2;
        v = new Vector<Object>();
        String value;
        FilePackage filePackage;
        DownloadLink dLink;
        Integer Package_ID;
        Integer Download_ID;
        Double percent = 0.0;
        for (Package_ID = 0; Package_ID < JDUtilities.getController().getPackages().size(); Package_ID++) {
            filePackage = JDUtilities.getController().getPackages().get(Package_ID);

            h = new Hashtable<Object, Object>();
            int status[] = { 0, 0, 0, 0 };
            /* Paket Infos */
            h.put("download_name", filePackage.getName());

            value = "";
            percent = filePackage.getPercent();
            h.put("download_status_percent", f.format(percent));

            if (filePackage.getLinksInProgress() > 0) {
                value = filePackage.getLinksInProgress() + "/" + filePackage.size() + " " + JDL.L("gui.treetable.packagestatus.links_active", "Active");
            }
            if (filePackage.getTotalDownloadSpeed() > 0) {
                value = "[" + filePackage.getLinksInProgress() + "/" + filePackage.size() + "] " + "ETA " + Formatter.formatSeconds(filePackage.getETA()) + " @ " + Formatter.formatReadable(filePackage.getTotalDownloadSpeed()) + "/s";
            }

            h.put("package_id", Package_ID.toString());
            h.put("download_hoster", value);
            h.put("download_status_text", f.format(percent) + " % (" + Formatter.formatReadable(filePackage.getTotalKBLoaded()) + " / " + Formatter.formatReadable(filePackage.getTotalEstimatedPackageSize()) + ")");

            v2 = new Vector<Object>();

            for (Download_ID = 0; Download_ID < filePackage.getDownloadLinkList().size(); Download_ID++) {
                dLink = filePackage.getDownloadLinkList().get(Download_ID);

                // Download Infos
                percent = (double) (dLink.getDownloadCurrent() * 100.0 / Math.max(1, dLink.getDownloadSize()));

                h2 = new Hashtable<Object, Object>();
                h2.put("download_status_percent", f.format(percent));
                h2.put("package_id", Package_ID.toString());
                h2.put("download_id", Download_ID.toString());
                h2.put("download_name", dLink.getName());

                h2.put("download_hoster", dLink.getHost());

                if (dLink.isAvailabilityStatusChecked() && !dLink.isAvailable()) {
                    status[0] = 1;
                    h2.put("download_status", "offline");
                } else if (dLink.isEnabled()) {

                    switch (dLink.getLinkStatus().getLatestStatus()) {
                    case LinkStatus.ERROR_FILE_NOT_FOUND:
                        status[0] = 1;
                        h2.put("download_status", "offline");
                        break;
                    case LinkStatus.FINISHED:
                        status[3] = 1;
                        h2.put("download_status", "finished");
                        break;

                    case LinkStatus.DOWNLOADINTERFACE_IN_PROGRESS:
                        status[2] = 1;
                        h2.put("download_status", "running");
                        break;

                    default:
                        status[1] = 1;
                        h2.put("download_status", "activated");
                    }
                } else {
                    status[0] = 1;
                    h2.put("download_status", "deactivated");
                }

                h2.put("download_status_text", f.format(percent) + "% " + dLink.getLinkStatus().getStatusString());
                v2.addElement(h2);
            }

            if (status[3] == 1 && status[2] == 0 && status[1] == 0 && status[0] == 0) {
                h.put("download_status", "finished");
            } else if (status[2] == 1) {
                h.put("download_status", "running");
            } else if (status[1] == 1) {
                h.put("download_status", "activated");
            } else if (status[0] == 1) {
                h.put("download_status", "deactivated");
            }

            h.put("downloads", v2);
            v.addElement(h);
        }
        t.setParam("config_current_speed", "" + (DownloadWatchDog.getInstance().getConnectionManager().getIncommingBandwidthUsage() / 1024));

        t.setParam("config_max_downloads", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SIMULTAN, 2));
        t.setParam("config_max_speed", SubConfiguration.getConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_SPEED, 0));

        if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
            t.setParam("config_autoreconnect", "checked");
        } else {
            t.setParam("config_autoreconnect", "");
        }

        if (DownloadWatchDog.getInstance().getDownloadStatus() == DownloadWatchDog.STATE.RUNNING) {
            t.setParam("config_startstopbutton", "stop");
        } else {
            t.setParam("config_startstopbutton", "start");
        }

        // t.setParam("message_status", "show");
        // t.setParam("message", "great work");

        t.setParam("pakete", v);
    }

    private void addEntry(String var, String value) {
        Hashtable<Object, Object> h_info = new Hashtable<Object, Object>();
        h_info.put("info_var", var);
        h_info.put("info_value", value);
        v_info.addElement(h_info);
    }

    @SuppressWarnings("deprecation")
    public void handleRequest(String url, HashMap<String, String> requestParameter) {
        try {
            Template t = new Template(JDUtilities.getResourceFile("plugins/webinterface/" + url).getAbsolutePath());

            t.setParam("webinterface_version", JDWebinterface.instance.getPluginID());
            t.setParam("page_refresh", JDWebinterface.getRefreshRate());

            boolean hasUnrar = false;
            OptionalPluginWrapper wrapper = JDUtilities.getOptionalPlugin("unrar");
            if (wrapper != null && wrapper.isEnabled()) hasUnrar = true;
            t.setParam("unrar_available", hasUnrar ? "unrarAvailable" : "unrarUnavailable");

            if (url.startsWith("single_info.tmpl") == true) {
                add_single_info(t, requestParameter);
            }
            if (url.startsWith("all_info.tmpl") == true) {
                add_all_info(t, requestParameter);
            }
            if (url.startsWith("index.tmpl") == true) {
                add_status_page(t, requestParameter);
            }
            if (url.startsWith("passwd.tmpl") == true) {
                add_password_list(t, requestParameter);
            }
            if (url.startsWith("link_adder.tmpl") == true) {
                add_linkadder_page(t, requestParameter);
            }

            response.addContent(t.output());
            response.setOk();
        } catch (FileNotFoundException e) {

            JDLogger.exception(e);
        } catch (IllegalStateException e) {

            JDLogger.exception(e);
        } catch (IOException e) {

            JDLogger.exception(e);
        }
    }
}
