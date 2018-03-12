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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xnxx.com" }, urls = { "https?://[\\w\\.]*?xnxx\\.(?:com|hot1000\\.ru)/video[a-z0-9\\-]+" })
public class XnXxCom extends PluginForHost {
    public XnXxCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* DEV NOTES */
    /* Porn_plugin */
    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;
    private String               dllink                          = null;

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

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* Correct link for user 'open in browser' */
        final String addedlink = link.getDownloadURL();
        if (!addedlink.endsWith("/")) {
            final String user_url = addedlink + "/";
            link.setContentUrl(user_url);
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.xnxx.com/contact.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        // The regex only takes the short urls but these ones redirect to the real ones to if follow redirects is false the plugin doesn't
        // work at all!
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL() + "/");
        if (br.containsHTML("(Page not found|This page may be in preparation, please check back in a few minutes)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename_url = new Regex(downloadLink.getDownloadURL(), "/video([a-z0-9\\-]+)").getMatch(0);
        String filename = br.getRegex("<title>(.+) \\- [A-Za-z0-9\\.]+\\.[A-Za-z0-9]{3,8}</title>").getMatch(0);
        if (filename == null) {
            br.getRegex("<span class=\"style5\"><strong>(.*?)</strong>").getMatch(0);
            if (filename == null) {
                br.getRegex("name=description content=\"(.*?)free sex video").getMatch(0);
            }
        }
        if (filename == null) {
            /* Fallback */
            filename = filename_url;
        }
        if (!br.containsHTML(".mp4")) {
            downloadLink.setFinalFileName(filename.trim() + ".flv");
        } else {
            downloadLink.setFinalFileName(filename.trim() + ".mp4");
        }
        dllink = br.getRegex("setVideoUrlHigh\\('(http.*?)'").getMatch(0);
        if (dllink != null) {
            checkDllink(downloadLink, dllink);
        }
        if (dllink == null) {
            dllink = br.getRegex("setVideoUrlLow\\('(http.*?)'").getMatch(0);
            if (dllink != null) {
                checkDllink(downloadLink, dllink);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    private String checkDllink(final DownloadLink link, final String flink) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openHeadConnection(flink);
            if (!con.getContentType().contains("html")) {
                link.setDownloadSize(con.getLongContentLength());
                dllink = flink;
            } else {
                dllink = null;
            }
        } catch (final Exception e) {
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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