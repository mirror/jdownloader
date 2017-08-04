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

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

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

//xvideos.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xvideos.com" }, urls = { "https?://((www\\.)?xvideos\\.com/video[0-9]+/|\\w+\\.xvideos\\.com/embedframe/\\d+|(www\\.)?xvideos\\.com/[a-z0-9\\-]+/(upload|pornstar)/[a-z0-9\\-]+/\\d+|(www\\.)?xvideos\\.com/prof\\-video\\-click/pornstar/[a-z0-9\\-]+/\\d+)" })
public class XvideosCom extends PluginForHost {

    public XvideosCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;

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
        return "http://info.xvideos.com/legal/tos/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String type_normal  = "https?://(www\\.)?xvideos\\.com/video[0-9]+/";
    private static final String type_embed   = "https?://\\w+\\.xvideos\\.com/embedframe/\\d+";
    private static final String type_special = "https?://(www\\.)?xvideos\\.com/([a-z0-9\\-]+/upload/[a-z0-9\\-]+/\\d+|prof\\-video\\-click/pornstar/[a-z0-9\\-]+/\\d+)";
    private static final String NOCHUNKS     = "NOCHUNKS";
    private String              dllink       = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().matches(type_embed) || link.getDownloadURL().matches(type_special)) {
            link.setUrlDownload(new Regex(link.getDownloadURL(), "https?://[^/]+").getMatch(-1) + "/video" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "/");
            link.setContentUrl(link.getDownloadURL());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        link.setName(new Regex(link.getDownloadURL(), "xvideos\\.com/(.+)").getMatch(0));
        br.setFollowRedirects(false);
        br.getHeaders().put("Accept-Encoding", "gzip");
        br.getHeaders().put("Accept-Language", "en-gb");
        br.getPage(link.getDownloadURL());
        while (br.getRedirectLocation() != null) {
            logger.info("Setting new downloadlink: " + br.getRedirectLocation());
            link.setUrlDownload(br.getRedirectLocation());
            br.getPage(link.getDownloadURL());
        }
        if (br.containsHTML("(This video has been deleted|Page not found|>Sorry, this video is not available\\.|>We received a request to have this video deleted|class=\"inlineError\")") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<h2>([^<>\"]*?)<span class").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?)\\- XVIDEOS\\.COM</title>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // hls first ? seems to be working most reliable
        dllink = getDllinkHls();
        if (dllink == null) {
            dllink = getDllinkHtml5();
            if (dllink == null) {
                dllink = getDllinkFlv();
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        if (!dllink.contains(".m3u8")) {
            filename = filename.trim() + getFileNameExtensionFromString(dllink, ".mp4");
            link.setFinalFileName(Encoding.htmlDecode(filename));
            sleep(2000, link);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (!con.getContentType().contains("html") && con.getResponseCode() == 200) {
                    link.setDownloadSize(con.getLongContentLength());
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
        } else {
            filename = filename.trim() + ".mp4";
            link.setFinalFileName(Encoding.htmlDecode(filename));
            return AvailableStatus.TRUE;
        }
    }

    private String getDllinkHls() {
        // hd
        String dllink = br.getRegex("html5player\\.setVideoHLS\\('(.*?)'\\)").getMatch(0);
        return dllink;
    }

    private String getDllinkFlv() {
        String dllink = br.getRegex("flv_url=(.*?)\\&").getMatch(0);
        if (dllink == null) {
            dllink = decode(br.getRegex("encoded=(.*?)\\&").getMatch(0));
        }
        return dllink;
    }

    private String getDllinkHtml5() {
        // hd
        String dllink = br.getRegex("html5player\\.setVideoUrlHigh\\('(.*?)'\\)").getMatch(0);
        if (dllink == null) {
            // sd
            dllink = br.getRegex("html5player\\.setVideoUrlLow\\('(.*?)'\\)").getMatch(0);
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (!dllink.contains(".m3u8")) {
            int chunks = 0;
            if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false)) {
                chunks = 1;
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, chunks);
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(XvideosCom.NOCHUNKS, false) == false) {
                    link.setProperty(XvideosCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } else {
            sleep(2000, link);
            final Browser br2 = br.cloneBrowser();
            br2.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(br2));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            sleep(2000, link);
            dl = new HLSDownloader(link, br, dllink);
            dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private static String decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        encoded = new String(jd.crypt.Base64.decode(encoded));
        String[] encodArr = encoded.split("-");
        String encodedUrl = "";
        for (String i : encodArr) {
            int charNum = Integer.parseInt(i);
            if (charNum != 0) {
                encodedUrl = encodedUrl + (char) charNum;
            }
        }
        return calculate(encodedUrl);
    }

    private static String calculate(String src) {
        if (src == null) {
            return null;
        }
        String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZABCDEFGHIJKLMabcdefghijklmnopqrstuvwxyzabcdefghijklm";
        String calculated = "";
        int i = 0;
        while (i < src.length()) {
            char character = src.charAt(i);
            int pos = CHARS.indexOf(character);
            if (pos > -1) {
                character = CHARS.charAt(13 + pos);
            }
            calculated = calculated + character;
            i++;
        }
        return calculated;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}