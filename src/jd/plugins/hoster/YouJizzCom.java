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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "youjizz.com" }, urls = { "https?://(www\\.)?youjizz\\.com/videos/(embed/\\d+|.*?\\-\\d+\\.html)" })
public class YouJizzCom extends PluginForHost {

    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    public YouJizzCom(PluginWrapper wrapper) {
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
        return "http://www.youjizz.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().contains("/embed/")) {
            link.setUrlDownload("https://www.youjizz.com/videos/x-" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + ".html");
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        // if (!br.containsHTML("flvPlayer\\.swf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (filename == null || filename.trim().length() == 0) {
            filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
        }
        if (filename == null || filename.trim().length() == 0) {
            filename = br.getRegex("title1\">(.*?)</").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = filename.trim();
        final String embed = br.getRegex("src=('|\"|&#x22;)(https?://(?:www\\.)?youjizz\\.com/videos/embed/[0-9]+)\\1").getMatch(1);
        if (embed == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(Encoding.htmlOnlyDecode(embed));
        // 20170717
        final String filter = br.getRegex("var\\s+encodings\\s*=\\s*(\\[.*?\\]);").getMatch(0);
        if (filter != null) {
            // mobile has mp4 and non mobile is hls
            final String[] results = PluginJSonUtils.getJsonResultsFromArray(filter);
            int quality = 0;
            for (String result : results) {
                final Integer q = Integer.parseInt(PluginJSonUtils.getJson(result, "quality"));
                final String d = PluginJSonUtils.getJson(result, "filename");
                if (q > quality && d.contains(".mp4")) {
                    quality = q;
                    dllink = d;
                }
            }

        }
        if (dllink == null) {
            dllink = br.getRegex("addVariable\\(\"file\"\\s*,.*?\"(https?://.*?\\.flv(\\?.*?)?)\"").getMatch(0);
            if (dllink == null) {
                // 02.dec.2016
                dllink = br.getRegex("<source src=\"([^\"]+)").getMatch(0);
            }
            if (dllink == null) {
                // 02.dec.2016
                dllink = br.getRegex("newLink\\.setAttribute\\('href'\\s*,\\s*'([^']+)").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("\"(https?://(mediax|cdn[a-z]\\.videos)\\.youjizz\\.com/[A-Z0-9]+\\.flv(\\?.*?)?)\"").getMatch(0);
                if (dllink == null) {
                    // class="buttona" >Download This Video</
                    dllink = br.getRegex("\"(http://im\\.[^<>\"]+)\"").getMatch(0);
                }
                if (dllink == null) {
                    String playlist = br.getRegex("so\\.addVariable\\(\"playlist\"\\s*,\\s*\"(https?://(www\\.)?youjizz\\.com/playlist\\.php\\?id=\\d+)").getMatch(0);
                    if (playlist == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    Browser br2 = br.cloneBrowser();
                    br2.getPage(playlist);
                    // multiple qualities (low|med|high) grab highest for now, decrypter will be needed for others.
                    dllink = br2.getRegex("<level bitrate=\"\\d+\" file=\"(https?://(\\w+\\.){1,}youjizz\\.com/[^\"]+)\" ?></level>[\r\n\t ]+</levels>").getMatch(0);
                    if (dllink != null) {
                        dllink = dllink.replace("%252", "%2");
                    }
                }
            }
        }

        if (filename == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlOnlyDecode(dllink);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(dllink);
            if (!con.getContentType().contains("html")) {
                String ext = getFileNameFromHeader(con).substring(getFileNameFromHeader(con).lastIndexOf("."));
                downloadLink.setFinalFileName(Encoding.htmlDecode(filename) + ext);
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
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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