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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tnaflix.com" }, urls = { "https?://(?:[a-z0-9]+\\.)?tnaflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)|https?://(?:www\\.)?tnaflix\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+" }, flags = { 2 })
public class TnaFlixCom extends PluginForHost {

    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;

    private static final String  TYPE_NORMAL                     = "https?://(?:www\\.)?tnaflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)";
    private static final String  TYPE_embedding_player           = "https?://(?:www\\.)?tnaflix\\.com/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+";

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String urlpart = new Regex(link.getDownloadURL(), "(tnaflix\\.com/.+)").getMatch(0);
        link.setUrlDownload("http://www." + urlpart);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://tnaflix.com/", "content_filter2", "type%3Dstraight%26filter%3Dcams");
        br.setCookie("http://tnaflix.com/", "content_filter3", "type%3Dstraight%2Ctranny%2Cgay%26filter%3Dcams");
        if (downloadLink.getDownloadURL().matches(TYPE_embedding_player)) {
            /* Convert embed urls --> Original urls */
            br.getPage(downloadLink.getDownloadURL().replace("http://", "https://"));
            String videoID = br.getRegex("start_thumb>https?://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg<").getMatch(0);
            if (videoID == null) {
                videoID = br.getRegex("<start_thumb><\\!\\[CDATA\\[https?://static\\.tnaflix\\.com/thumbs/[a-z0-9\\-_]+/[a-z0-9]+_(\\d+)l\\.jpg\\]\\]></start_thumb>").getMatch(0);
            }
            if (videoID == null) {
                /* Either plugin broken or link offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String newlink = "http://www.tnaflix.com/cum-videos/" + System.currentTimeMillis() + "/video" + videoID;
            downloadLink.setUrlDownload(newlink);
        }
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("class=\"errorPage page404\"|> This video is set to private") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().length() < 30) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String redirect = br.getRedirectLocation();
        if (redirect != null) {
            if (redirect.contains("errormsg=true")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (redirect.contains("video")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
            }
            br.getPage(redirect);
        }
        String filename = br.getRegex("<title>([^<>]*?) \\- TNAFlix Porn Videos</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim();
        filename = encodeUnicode(filename);
        downloadLink.setFinalFileName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String download = br.getRegex("<div class=\"playlist_listing\" data-loaded=\"true\">(.*?)</div>").getMatch(0);
        String configLink = br.getRegex("addVariable\\(\\'config\\', \\'(http.*?)\\'").getMatch(0);
        if (configLink == null) {
            configLink = br.getRegex("flashvars.config.*?escape\\(.*?(cdn.*?)\"").getMatch(0);
        }
        if (configLink == null && download == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dllink = null;
        if (configLink != null) {
            br.getPage(Encoding.htmlDecode("http://" + configLink));
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Config file is not correct'", 30 * 60 * 1000l);
            }
            final String[] qualities = { "720p", "480p", "360p", "240p", "144p" };
            for (final String quality : qualities) {
                dllink = br.getRegex("" + quality + "</res>\\s*<videolink>(http://.*?)</videoLink>").getMatch(0);
                if (dllink != null) {
                    break;
                }
            }
        } else {
            // download support
            final String[] qualities = { "720", "480", "360", "240", "144" };
            for (final String quality : qualities) {
                dllink = new Regex(download, "href=(\"|')((?:https?:)?//.*?)\\1>Download in " + quality).getMatch(1);
                if (dllink != null) {
                    break;
                }
            }
        }

        if (dllink == null) {
            dllink = br.getRegex("<file>(http://.*?)</file>").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<videolink>(http://.*?)</videoLink>").getMatch(0);
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Range", "bytes=0-");
        final URLConnectionAdapter con = brc.openHeadConnection(dllink);
        final long fileSize = con.getCompleteContentLength();
        con.disconnect();
        downloadLink.setVerifiedFileSize(fileSize);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getResponseCode() == 416) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
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

    @Override
    public void resetPluginGlobals() {
    }
}