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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bitporno.sx", "raptu.com", }, urls = { "https?://(?:www\\.)?bitporno\\.(?:sx|com)/\\?v=[A-Za-z0-9]+", "https?://(?:www\\.)?(?:playernaut\\.com|rapidvideo\\.com|raptu\\.com)/(?:\\?v=|embed/)[A-Za-z0-9]+" })
public class BitvideoIo extends PluginForHost {

    public BitvideoIo(PluginWrapper wrapper) {
        super(wrapper);
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
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        /* Better filenames for offline case */
        link.setName(fid + ".mp4");
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        String filename = null;
        String json_source = null;
        if (link.getDownloadURL().contains("rapidvideo.com")) {
            /* 2017-03-24: Special handling for this domain - video json is not encrypted! */
            br.getPage(link.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }

            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (filename == null) {
                /* Fallback */
                filename = fid;
            }

            json_source = this.br.getRegex("\"sources\"\\s*?:\\s*?(\\[.*?\\])").getMatch(0);
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
            if (this.br.containsHTML(html_video_encoding)) {
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
            String dllink_temp = null;
            final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json_source);
            LinkedHashMap<String, Object> entries = null;
            int maxvalue = 0;
            int tempvalue = 0;
            String tempquality = null;
            for (final Object videoo : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) videoo;
                tempquality = (String) entries.get("label");
                dllink_temp = (String) entries.get("file");
                // if ("Source( File)?".equalsIgnoreCase(tempquality)) {
                if (tempquality.contains("Source")) {
                    /* That IS the highest quality */
                    dllink = dllink_temp;
                    break;
                } else {
                    /* Look for the highest quality! */
                    tempvalue = Integer.parseInt(new Regex(tempquality, "(\\d+)p?").getMatch(0));
                    if (tempvalue > maxvalue) {
                        maxvalue = tempvalue;
                        dllink = dllink_temp;
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
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
        if (this.br.containsHTML(html_video_encoding)) {
            /*
             * 2016-06-16, psp: I guess if this message appears longer than some hours, such videos can never be downloaded/streamed or only
             * the original file via premium account.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Not downloadable (yet) because 'This video is still in encoding progress - Please patient'", 60 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
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
