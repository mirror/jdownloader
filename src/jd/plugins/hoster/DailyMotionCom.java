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
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.DownloadInterface;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dailymotion.com" }, urls = { "http://dailymotiondecrypted\\.com/video/\\w+" }, flags = { 2 })
public class DailyMotionCom extends PluginForHost {

    public String               dllink                 = null;
    private static final String MAINPAGE               = "http://www.dailymotion.com/";
    private static final String REGISTEREDONLYUSERTEXT = "Download only possible for registered users";
    private static final String COUNTRYBLOCKUSERTEXT   = "This video is not available for your country";
    /** Settings stuff */
    private static final String ALLOW_BEST             = "ALLOW_BEST";
    private static final String ALLOW_LQ               = "ALLOW_LQ";
    private static final String ALLOW_SD               = "ALLOW_SD";
    private static final String ALLOW_HQ               = "ALLOW_HQ";
    private static final String ALLOW_720              = "ALLOW_720";
    private static final String ALLOW_1080             = "ALLOW_1080";
    private static final String ALLOW_OTHERS           = "ALLOW_OTHERS";

    public DailyMotionCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dailymotion.com/register");
        setConfigElements();
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("dailymotiondecrypted.com/", "dailymotion.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        br.setFollowRedirects(true);
        br.setCookie("http://www.dailymotion.com", "family_filter", "off");
        br.setCookie("http://www.dailymotion.com", "lang", "en_US");
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (downloadLink.getBooleanProperty("countryblock", false)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.dailymotioncom.countryblocked", COUNTRYBLOCKUSERTEXT));
            return AvailableStatus.TRUE;
        } else if (downloadLink.getBooleanProperty("registeredonly", false)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.dailymotioncom.registeredonly", REGISTEREDONLYUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (downloadLink.getBooleanProperty("isrtmp", false)) {
            getRTMPlink();
        } else {
            dllink = downloadLink.getStringProperty("directlink");
            if (dllink == null) {
                logger.warning("dllink is null...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(dllink);
                if (con.getResponseCode() == 410 || con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                downloadLink.setDownloadSize(con.getLongContentLength());
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (dllink.startsWith("rtmp")) {
            String[] stream = dllink.split("@");
            dl = new RTMPDownload(this, downloadLink, stream[0]);
            setupRTMPConnection(stream, dl);
            ((RTMPDownload) dl).startDownload();
        } else {
            // They do allow resume and unlimited chunks but resuming or using
            // more
            // than 1 chunk causes problems, the file will then b corrupted!
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, this.br);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.dailymotion.com/de/legal/terms";

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (downloadLink.getBooleanProperty("countryblock", false)) { throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT); }
        if (downloadLink.getBooleanProperty("registeredonly", false)) { throw new PluginException(LinkStatus.ERROR_FATAL, REGISTEREDONLYUSERTEXT); }
        doFree(downloadLink);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, this.br);
        requestFileInformation(link);
        if (link.getBooleanProperty("countryblock", false)) { throw new PluginException(LinkStatus.ERROR_FATAL, COUNTRYBLOCKUSERTEXT); }
        doFree(link);
    }

    public void login(final Account account, final Browser br) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-Prototype-Version", "1.6.1");
        br.postPage("http://www.dailymotion.com/pageitem/login?urlback=%2Fde&request=%2Flogin", "form_name=dm_pageitem_login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&rememberme=1&login_submit=Login");
        if (br.getCookie(MAINPAGE, "sid") == null || br.getCookie(MAINPAGE, "dailymotion_auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private void getRTMPlink() throws IOException, PluginException {
        final String[] values = br.getRegex("new SWFObject\\(\"(http://player\\.grabnetworks\\.com/swf/GrabOSMFPlayer\\.swf)\\?id=\\d+\\&content=v([0-9a-f]+)\"").getRow(0);
        if (values == null || values.length != 2) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser rtmp = br.cloneBrowser();
        rtmp.getPage("http://content.grabnetworks.com/v/" + values[1] + "?from=" + dllink);
        dllink = rtmp.getRegex("\"url\":\"(rtmp[^\"]+)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink + "@" + values[0];
    }

    private void setupRTMPConnection(String[] stream, DownloadInterface dl) {
        jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();
        rtmp.setUrl(stream[0]);
        rtmp.setSwfVfy(stream[1]);
        rtmp.setResume(true);
    }

    @Override
    public String getDescription() {
        return "JDownloader's DailyMotion Plugin helps downloading Videoclips from dailymotion.com. DailyMotion provides different video formats and qualities.";
    }

    private void setConfigElements() {
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.youtube.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LQ, JDL.L("plugins.hoster.dailymotioncom.checkLQ", "Grab LQ?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_SD, JDL.L("plugins.hoster.dailymotioncom.checkSD", "Grab SD?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HQ, JDL.L("plugins.hoster.dailymotioncom.checkHQ", "Grab HQ?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720, JDL.L("plugins.hoster.dailymotioncom.check720", "Grab 720p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_1080, JDL.L("plugins.hoster.dailymotioncom.check1080", "Grab 1080p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_OTHERS, JDL.L("plugins.hoster.dailymotioncom.checkother", "Grab other available qualities?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
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