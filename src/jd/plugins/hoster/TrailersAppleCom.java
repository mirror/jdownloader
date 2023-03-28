//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "trailers.apple.com" }, urls = { "https?://\\w+\\.appledecrypted\\.com/.+" })
public class TrailersAppleCom extends PluginForHost {
    // DEV NOTES
    // yay for fun times
    public TrailersAppleCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    public static final String  preferBest         = "preferBest";
    public static final boolean preferBest_default = true;
    public static final String  p1080              = "p1080";
    public static final boolean p1080_default      = true;
    public static final String  p720               = "p720";
    public static final boolean p720_default       = true;
    public static final String  p640               = "p640";
    public static final boolean p640_default       = true;
    public static final String  p480               = "p480";
    public static final boolean p480_default       = true;
    public static final String  p360               = "p360";
    public static final boolean p360_default       = true;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), preferBest, "Prefer Best, highest availble 'p' within the selection enabled below.").setDefaultValue(preferBest_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), p1080, "Enable '" + p1080 + "'.").setDefaultValue(p1080_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), p720, "Enable '" + p720 + "'.").setDefaultValue(p720_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), p640, "Enable '" + p640 + "'.").setDefaultValue(p640_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), p480, "Enable '" + p480 + "'.").setDefaultValue(p480_default));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), p360, "Enable '" + p360 + "'.").setDefaultValue(p360_default));
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("appledecrypted", "apple"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.apple.com/legal/terms/site.html";
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        br.setFollowRedirects(true);
        String referer = link.getStringProperty("Referer"); // backward compatibility
        if (referer == null) {
            referer = link.getReferrerUrl();
        }
        br.getHeaders().put("User-Agent", "QuickTime/7.2 (qtver=7.2;os=Windows NT 5.1Service Pack 3)");
        br.getHeaders().put("Referer", referer);
        br.getHeaders().put("Accept", null);
        br.getHeaders().put("Accept-Language", null);
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Connection", null);
        final String downloadURL = link.getDownloadURL();
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, downloadURL, true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            String newDownloadURL = br.getRegex("(https?://[^\r\n\\s]+\\.mov)").getMatch(0);
            // required for poster 'SD' movies aka non p results
            if (newDownloadURL == null) {
                newDownloadURL = br.getRegex("!(.*\\.mov)").getMatch(0);
                if (newDownloadURL != null) {
                    newDownloadURL = downloadURL.replaceFirst("/[^/]+$", "/" + newDownloadURL);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, newDownloadURL, true, 0);
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}