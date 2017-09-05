//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitporno.sx", "raptu.com" }, urls = { "https?://(?:www\\.)?bitporno\\.(?:sx|com)/(\\?v=|v/|embed/)[A-Za-z0-9]+", "https?://(?:www\\.)?(?:playernaut\\.com|rapidvideo\\.com|raptu\\.com)/(?:e(?:mbed)?/|(?:embed/)?\\?v=|v(?:iew)?/)[A-Za-z0-9]+" })
public class BitvideoIo extends PluginForHost {

    public BitvideoIo(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* DEV NOTES */
    // Tags:
    // protocol: no https
    // other: playernaut.com -> redirect into rapidvideo.com --> redirects to raptu.com
    // other: same owner/sites I assume..
    /* Connection stuff */
    private static final boolean free_resume         = true;
    private static final int     free_maxchunks      = 0;
    private static final int     free_maxdownloads   = -1;
    private static final String  html_video_encoding = ">This video is still in encoding progress";
    private String               dllink              = null;
    private boolean              server_issues       = false;

    @Override
    public String getAGBLink() {
        return "http://www.bitporno.sx/?c=tos";
    }

    @Override
    public String rewriteHost(String host) {
        if ("rapidvideo.com".equals(getHost())) {
            if (host == null || "rapidvideo.com".equals(host)) {
                return "raptu.com";
            }
        }
        return super.rewriteHost(host);
    }

    /** 2016-05-18: playernaut.com uses crypted js, bitporno.sx doesn't! */
    @SuppressWarnings({ "deprecation", "unchecked", "rawtypes" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        server_issues = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        if (!link.isNameSet()) {
            /* Better filenames for offline case */
            link.setName(fid + ".mp4");
        }
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        String filename = null;
        String json_source = null;
        if (getHost().equals("raptu.com")) {
            /* 2017-03-24: Special handling for this domain - video json is not encrypted! */
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Object not found<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                /* Fallback */
                filename = fid;
            }
            json_source = br.getRegex("\"sources\"\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
        } else {
            /* Only use one of their domains */
            br.getPage("https://www.bitporno.com/?v=" + fid);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<span itemprop=\"name\" title=\"(.*?)\"").getMatch(0);
            }
            if (filename == null) {
                /* Fallback */
                filename = fid;
            }
            if (filename.length() > 212) {
                int dash = filename.indexOf('-', 200);
                if (dash >= 0) {
                    filename = filename.substring(0, dash);
                } else {
                    filename = filename.substring(0, 212);
                }
            }
            if (br.containsHTML(html_video_encoding)) {
                return AvailableStatus.TRUE;
            }
            // from iframe
            br.getPage("/embed/" + fid);
            final Form f = br.getForm(0);
            if (f != null) {
                if (f.hasInputFieldByName("confirm") && "image".equals(f.getInputField("confirm").getType())) {
                    f.put("confirm.x", "62");
                    f.put("confirm.y", "70");
                }
                br.submitForm(f);
            }
            final String decode = new org.jdownloader.encoding.AADecoder(br.toString()).decode();
            json_source = new Regex(decode != null ? decode : br.toString(), "sources(?:\")?[\t\n\r ]*?:[\t\n\r ]*?(\\[.*?\\])").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (json_source != null) {
            final String userPreferredVideoquality = getConfiguredVideoQuality();
            logger.info("userPreferredVideoquality: " + userPreferredVideoquality);
            String dllink_user_prefered = null;
            String dllink_temp = null;
            String dllink_best = null;
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json_source);
            Map<String, Object> entries = null;
            int maxvalue = 0;
            int tempvalue = 0;
            String tempquality = null;
            for (final Object videoo : ressourcelist) {
                if (videoo instanceof Map) {
                    entries = (Map<String, Object>) videoo;
                    tempquality = (String) entries.get("label");
                    dllink_temp = (String) entries.get("file");
                    logger.info("label: " + tempquality + " file: " + dllink_temp);
                    if (StringUtils.isEmpty(tempquality) || StringUtils.isEmpty(dllink_temp)) {
                        /* Skip invalid objects */
                        continue;
                    }
                    if (tempquality.contains("Standard")) { // rapidvideo
                        tempquality = "360p";
                    }
                    if (tempquality.contains("High")) { // rapidvideo
                        tempquality = "480p";
                    }
                    if (tempquality.equalsIgnoreCase(userPreferredVideoquality)) {
                        logger.info("Found user selected videoquality: " + tempquality);
                        dllink_user_prefered = dllink_temp;
                        break;
                    }
                    // if ("Source( File)?".equalsIgnoreCase(tempquality)) {
                    if (tempquality.contains("Source")) {
                        /* That IS the highest quality */
                        tempvalue = 100000;
                        dllink_best = dllink_temp;
                    } else {
                        /* Look for the highest quality! */
                        tempvalue = Integer.parseInt(new Regex(tempquality, "(\\d+)p?").getMatch(0));
                    }
                    if (tempvalue > maxvalue) {
                        maxvalue = tempvalue;
                        dllink_best = dllink_temp;
                    }
                }
            }
            if (dllink_user_prefered != null) {
                logger.info("Downloading user-selected quality");
                dllink = dllink_user_prefered;
            } else {
                logger.info("Downloading highest quality possible");
                dllink = dllink_best;
                logger.info("file: " + dllink_best);
            }
        } else {
            // could be <source>, seems that it also shows highest quality to change you do another page grab to '&q=480p | &q=360p'
            final String[] source = br.getRegex("<source .*?/\\s*>").getColumn(-1);
            if (source != null) {
                int best = 0;
                for (String s : source) {
                    final String d = new Regex(s, "src=(\"|')(.*?)\\1").getMatch(1);
                    final String q = new Regex(s, "data-res=(\"|')(\\d+)\\1").getMatch(1);
                    if (q != null && d != null) {
                        if (best < Integer.parseInt(q)) {
                            dllink = d;
                            best = Integer.parseInt(q);
                        }
                    }
                }
            }
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        String extension = null;
        if (dllink != null) {
            dllink = Encoding.htmlDecode(dllink);
            extension = getFileNameExtensionFromString(dllink, ".mp4");
        } else {
            extension = ".mp4";
        }
        if (!filename.endsWith(extension)) {
            filename += extension;
        }
        if (dllink != null) {
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    con = br2.openHeadConnection(dllink);
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    server_issues = true;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.containsHTML(html_video_encoding)) {
            /*
             * 2016-06-16, psp: I guess if this message appears longer than some hours, such videos can never be downloaded/streamed or only
             * the original file via premium account.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable (yet) because 'This video is still in encoding progress - Please patient'", 60 * 60 * 1000l);
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown error occured");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getConfiguredVideoQuality() {
        final int selection = this.getPluginConfig().getIntegerProperty(SELECTED_VIDEO_FORMAT, 0);
        final String selectedQuality = FORMATS[selection];
        return selectedQuality;
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), SELECTED_VIDEO_FORMAT, FORMATS, "Select preferred videoquality:").setDefaultValue(0));
    }

    /* The list of qualities displayed to the user */
    private final String[] FORMATS               = new String[] { "Source", "360p", "480p", "720p", "1080p HD" };
    private final String   SELECTED_VIDEO_FORMAT = "SELECTED_VIDEO_FORMAT";

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
