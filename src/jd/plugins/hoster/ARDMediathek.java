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

package jd.plugins.hoster;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ardmediathek.de" }, urls = { "http://(www\\.)?(ardmediathek|mediathek\\.daserste)\\.de/[\\w\\-]+/([\\w\\-]+/)?[\\w\\-]+(\\?documentId=\\d+)?" }, flags = { 32 })
public class ARDMediathek extends PluginForHost {

    private static final String Q_LOW = "Q_LOW";
    private static final String Q_MEDIUM = "Q_MEDIUM";
    private static final String Q_HIGH = "Q_HIGH";
    private static final String Q_HD = "Q_HD";
    private static final String Q_BEST = "Q_BEST";

    public ARDMediathek(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> getDownloadLinks(String data, FilePackage fp) {
        ArrayList<DownloadLink> ret = super.getDownloadLinks(data, fp);

        try {
            if (ret != null && ret.size() > 0) {
                /*
                 * we make sure only one result is in ret, thats the case for
                 * svn/next major version
                 */
                final DownloadLink sourceLink = ret.get(0);
                String ID = new Regex(sourceLink.getDownloadURL(), "\\?documentId=(\\d+)").getMatch(0);
                if (ID != null) {
                    Browser br = new Browser();
                    setBrowserExclusive();
                    br.setFollowRedirects(true);
                    br.getPage(sourceLink.getDownloadURL());
                    if (br.containsHTML("<h1>Leider konnte die gew&uuml;nschte Seite<br />nicht gefunden werden.</h1>")) {
                        sourceLink.setAvailable(false);
                        ret.set(0, sourceLink);
                        return ret;
                    }
                    String title = getTitle();
                    /*
                     * little pause needed so the next call does not return
                     * trash
                     */
                    Thread.sleep(1000);

                    String url = null, fmt = null;
                    int t = 0;
                    ArrayList<DownloadLink> newRet = new ArrayList<DownloadLink>();
                    HashMap<String, DownloadLink> bestMap = new HashMap<String, DownloadLink>();
                    for (String quality[] : br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (\\d+), \"([^\"]+|)\", \"([^\"]+)\", \"([^\"]+)\"\\);").getMatches()) {
                        // get streamtype id
                        t = Integer.valueOf(quality[0]);
                        // http
                        url = quality[3] + "@";
                        // rtmp
                        if (t == 0) url = quality[2] + "@" + quality[3].split("\\?")[0];

                        fmt = quality[1];

                        switch (Integer.valueOf(quality[1])) {
                        case 0:
                            if (this.getPluginConfig().getBooleanProperty(Q_LOW, true) == false) {
                                continue;
                            } else {
                                fmt = "low";
                            }
                            break;
                        case 1:
                            if (this.getPluginConfig().getBooleanProperty(Q_MEDIUM, true) == false) {
                                continue;
                            } else {
                                fmt = "medium";
                            }
                            break;
                        case 2:
                            if (this.getPluginConfig().getBooleanProperty(Q_HIGH, true) == false) {
                                continue;
                            } else {
                                fmt = "high";
                            }
                            break;
                        case 3:
                            if (this.getPluginConfig().getBooleanProperty(Q_HD, true) == false) {
                                continue;
                            } else {
                                fmt = "hd";
                            }
                            break;
                        }

                        final String name = title + "@" + fmt.toUpperCase(Locale.ENGLISH) + ".mp4";
                        final DownloadLink link = new DownloadLink(this, name, getHost(), sourceLink.getDownloadURL(), true);
                        if (t == 1 ? false : true) link.setAvailable(true);
                        link.setFinalFileName(name);
                        link.setBrowserUrl(sourceLink.getBrowserUrl());
                        link.setProperty("directURL", url);
                        link.setProperty("directName", name);
                        link.setProperty("directQuality", quality[1]);
                        link.setProperty("streamingType", t);
                        link.setProperty("LINKDUPEID", "ard" + ID + name + fmt);

                        DownloadLink best = bestMap.get(fmt);
                        if (best == null || link.getDownloadSize() > best.getDownloadSize()) {
                            bestMap.put(fmt, link);
                        }
                        newRet.add(link);
                    }
                    if (newRet.size() > 0) {
                        if (this.getPluginConfig().getBooleanProperty(Q_BEST, false)) {
                            /* only keep best quality */
                            DownloadLink keep = bestMap.get("hd");
                            if (keep == null) keep = bestMap.get("high");
                            if (keep == null) keep = bestMap.get("medium");
                            if (keep == null) keep = bestMap.get("low");
                            if (keep != null) {
                                newRet.clear();
                                newRet.add(keep);
                            }
                        }
                        /*
                         * only replace original found links by new ones, when
                         * we have some
                         */
                        if (fp != null) {
                            fp.addLinks(newRet);
                            fp.remove(sourceLink);
                        } else if (newRet.size() > 1) {
                            fp = FilePackage.getInstance();
                            fp.setName(title);
                            fp.addLinks(newRet);
                        }
                        ret = newRet;
                    }
                } else {
                    /*
                     * no other qualities*&
                     */
                }
            }
        } catch (final Throwable e) {
            logger.severe(e.getMessage());
        }
        return ret;
    }

    private String getTitle() {
        String title = br.getRegex("<div class=\"MainBoxHeadline\">([^<]+)</").getMatch(0);
        String titleUT = br.getRegex("<span class=\"BoxHeadlineUT\">([^<]+)</").getMatch(0);
        if (title == null) title = br.getRegex("<title>ard\\.online \\- Mediathek: ([^<]+)</title>").getMatch(0);
        if (title == null) title = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (title != null) title = Encoding.htmlDecode(title + (titleUT != null ? "__" + titleUT.replaceAll(":$", "") : "").trim());
        if (title == null) title = "UnknownTitle_" + System.currentTimeMillis();
        return title;
    }

    @Override
    public String getAGBLink() {
        return "http://www.ardmediathek.de/ard/servlet/content/3606532";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        download(downloadLink);
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setPlayPath(stream[1]);
        rtmp.setUrl(stream[0]);
        rtmp.setResume(true);
        rtmp.setTimeOut(10);
    }

    private void download(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getStringProperty("directURL", null) == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String stream[] = downloadLink.getStringProperty("directURL").split("@");
        if (stream[0].startsWith("rtmp")) {
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            br.setFollowRedirects(true);
            final String dllink = stream[0];
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink.startsWith("mms")) throw new PluginException(LinkStatus.ERROR_FATAL, "Protocol (mms://) not supported!");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (dl.getConnection().getResponseCode() == 403) throw new PluginException(LinkStatus.ERROR_FATAL, "This Content is not longer available!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException, GeneralSecurityException {
        if (downloadLink.getStringProperty("directURL", null) == null) {
            /* fetch fresh directURL */
            setBrowserExclusive();
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());

            if (br.containsHTML("<h1>Leider konnte die gew&uuml;nschte Seite<br />nicht gefunden werden.</h1>")) {
                logger.info("ARD-Mediathek: Nicht mehr verfügbar: " + downloadLink.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            String newUrl[] = br.getRegex("mediaCollection\\.addMediaStream\\((\\d+), (" + downloadLink.getStringProperty("directQuality", "1") + "), \"([^\"]+|)\", \"([^\"]+)\", \"([^\"]+)\"\\);").getRow(0);
            // http
            if (newUrl == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setProperty("directURL", newUrl[3] + "@");
            // rtmp
            if ("0".equals(downloadLink.getStringProperty("streamingType", "1"))) downloadLink.setProperty("directURL", newUrl[1] + "@" + newUrl[2].split("\\?")[0]);
        }
        if (downloadLink.getStringProperty("directName", null) == null) downloadLink.setFinalFileName(getTitle() + ".mp4");
        if (!downloadLink.getStringProperty("directURL").startsWith("http")) return AvailableStatus.TRUE;
        // get filesize
        Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(downloadLink.getStringProperty("directURL").split("@")[0]);
            if (!con.getContentType().contains("html")) {
                downloadLink.setDownloadSize(con.getLongContentLength());
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.ard.best", "Load Best Version ONLY")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_LOW, JDL.L("plugins.hoster.ard.loadlow", "Load Low Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MEDIUM, JDL.L("plugins.hoster.ard.loadmedium", "Load Medium Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HIGH, JDL.L("plugins.hoster.ard.loadhigh", "Load High Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.ard.loadhd", "Load HD Version")).setDefaultValue(false).setEnabled(false));
    }

}