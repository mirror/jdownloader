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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "imageshack.com", "imageshack.us" }, urls = { "https?://(?:www\\.)?imageshack\\.(?:sus|com)/(i/[A-Za-z0-9]+|f/\\d+/[^<>\"/]+)", "z690hi09erhj6r0nrheswhrzogjrtehoDELETE_MEfhjtzjzjzthj" }, flags = { 0, 0 })
public class ImagesHackCom extends PluginForHost {

    public ImagesHackCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://imageshack.com/terms";
    }

    // More is possible but 1 is good to prevent errors
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("imageshack.us/", "imageshack.com/").replace("http://", "https://"));
    }

    private static final String  TYPE_DOWNLOAD    = "https?://(?:www\\.)?imageshack\\.(?:us|com)/f/\\d+/[^<>\"/]+";
    private static final String  TYPE_IMAGE       = "https?://(?:www\\.)?imageshack\\.(?:us|com)/i/[A-Za-z0-9]+";
    private String               DLLINK           = null;
    private static final boolean enable_api_image = true;

    private String               fid              = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        if (link.getDownloadURL().matches(TYPE_DOWNLOAD) || br.containsHTML("class=\"download-block\"")) {
            /* Download */
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("Looks like the image is no longer here")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRegex("\"(https?://imageshack\\.us/download/[^<>\"]*?)\"").getMatch(0);
        } else if (enable_api_image) {
            /* Image + usage of API. */
            this.fid = getFIDFRomURL_image(link);
            this.br.getPage("https://api.imageshack.com/v2/images/" + this.fid + "?next_prev_limit=0&related_images_limit=0");

            if (this.br.getHttpConnection().getResponseCode() != 200) {
                /* Typically response 500 for offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> json = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            json = (LinkedHashMap<String, Object>) json.get("result");
            final AvailableStatus status = apiImageGetAvailablestatus(link, json);
            DLLINK = (String) json.get("direct_link");
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = "https://" + DLLINK;
            return status;
        } else {
            /* Image - handling via website. */
            prepBR_API(this.br);
            this.fid = getFIDFRomURL_image(link);
            br.setFollowRedirects(false);
            br.getPage(link.getDownloadURL());
            if (br.getRedirectLocation() != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRegex("data\\-width=\"0\" data\\-height=\"0\" alt=\"\" src=\"(//imagizer\\.imageshack[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = "http" + DLLINK;
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(DLLINK);
            if (con.getContentType().contains("html")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            link.setName(getFileNameFromHeader(con));
            link.setDownloadSize(con.getLongContentLength());
        } catch (final Throwable e) {
            try {
                con.disconnect();
            } catch (final Throwable e2) {
            }
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus apiImageGetAvailablestatus(final DownloadLink dl, LinkedHashMap<String, Object> json) {
        final Object error = json.get("error");
        if (error != null) {
            /* Whatever it is - our picture is probably offline! */
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        final String id = api_json_get_id(json);
        final long filesize = DummyScriptEnginePlugin.toLong(json.get("filesize"), -1);
        final String username = (String) DummyScriptEnginePlugin.walkJson(json, "owner/username");
        final String album = (String) DummyScriptEnginePlugin.walkJson(json, "album/title");
        final boolean isDeleted = ((Boolean) json.get("hidden")).booleanValue();
        /*
         * Do NOT use 'original_filename' as it can happen that file got converted on the imageshack servers so we'd have a wrong file
         * extension!
         */
        String filename = (String) json.get("filename");
        if (filename == null || filesize == -1 || isDeleted) {
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        if (!inValidate(album)) {
            filename = username + " - " + album + "_" + filename;
        } else {
            filename = username + "_" + filename;
        }
        filename = encodeUnicode(filename);
        dl.setFinalFileName(filename);
        if (filesize > 0) {
            /* Happens e.g. when crawling all images of a user - API sometimes randomly returns 0 for filesize. */
            dl.setDownloadSize(filesize * 1024);
        }
        dl.setLinkID(id);
        return AvailableStatus.TRUE;
    }

    public static String api_json_get_id(final LinkedHashMap<String, Object> json) {
        return (String) json.get("id");
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    public static boolean inValidate(final String s) {
        if (s == null || s.matches("\\s+") || s.equals("")) {
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        if (downloadLink.getDownloadURL().matches(TYPE_IMAGE) && !br.containsHTML("class=\"download-block\"")) {
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            DLLINK = br.getRegex("/rss\\+xml\" href=\"(.*?)\\.comments\\.xml\"").getMatch(0);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }

        // More is possible but 1 chunk is good to prevent errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("deprecation")
    private String getFIDFRomURL_image(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    public static Browser prepBR_API(final Browser br) {
        br.setAllowedResponseCodes(500);
        return br;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    public static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}