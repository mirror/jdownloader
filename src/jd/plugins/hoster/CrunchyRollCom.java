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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.CrhyRllCom;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision: 16156 $", interfaceVersion = 2, names = { "crunchyroll.com" }, urls = { "http://www.crunchyroll.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig&media_id=[0-9]+.*video_quality=[0-9]*.*" }, flags = { 0 })
public class CrunchyRollCom extends PluginForHost {

    static public final String                               SWF_DIR      = "http://static.ak.crunchyroll.com/flash/20120315193834.0fa282dfa08cb851004372906bfd7e14/";
    static public final Pattern                              CONFIG_URL   = Pattern.compile("(http://www.crunchyroll.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig.*video_quality=)([0-9]*)(.*)", Pattern.CASE_INSENSITIVE);

    private static final Object                              lock         = new Object();
    private static HashMap<Account, HashMap<String, String>> loginCookies = new HashMap<Account, HashMap<String, String>>();

    public CrunchyRollCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.crunchyroll.com/login");
    }

    @Override
    public String getAGBLink() {
        return "http://www.crunchyroll.com/tos";
    }

    @Override
    public String getDescription() {
        return "";
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
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            this.login(account, this.br, true, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus(JDL.L("plugins.hoster.crunchyrollcom.accountok", "Account is OK."));
        ai.setValidUntil(-1);
        account.setValid(true);
        return ai;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        downloadLink.setProperty("valid", false);
        this.setBrowserExclusive();
        br.clearCookies("crunchyroll.com");

        this.requestFileInformation(downloadLink);
        this.download(downloadLink);
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        downloadLink.setProperty("valid", false);
        this.login(account, this.br, false, true);

        this.requestFileInformation(downloadLink);
        this.download(downloadLink);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        downloadLink.setProperty("valid", false);
        final PluginForDecrypt plugin = JDUtilities.getPluginForDecrypt("crunchyroll.com");

        if (plugin == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Cannot decrypt video link"); }

        if (downloadLink.getStringProperty("swfdir") == null) {
            downloadLink.setProperty("swfdir", SWF_DIR);
        }

        boolean valid = ((CrhyRllCom) plugin).setRTMP(downloadLink, this.br);

        if (valid) {
            downloadLink.setProperty("valid", true);
            return AvailableStatus.TRUE;
        } else {
            return AvailableStatus.FALSE;
        }
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

    private void download(DownloadLink downloadLink) throws Exception {
        if ((Boolean) downloadLink.getProperty("valid", false) && downloadLink.getStringProperty("rtmphost").startsWith("rtmp")) {
            String filename = downloadLink.getFinalFileName().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            String url = downloadLink.getStringProperty("rtmphost") + "/" + downloadLink.getStringProperty("rtmpfile");

            downloadLink.setFinalFileName(filename);
            dl = new RTMPDownload(this, downloadLink, url);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setUrl(url);
            rtmp.setTcUrl(downloadLink.getStringProperty("rtmphost"));
            rtmp.setPlayPath(downloadLink.getStringProperty("rtmpfile"));
            rtmp.setSwfVfy(downloadLink.getStringProperty("swfdir") + downloadLink.getStringProperty("rtmpswf"));
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid download");
        }
    }

    public void login(final Account account, Browser br, boolean refresh, boolean showDialog) throws Exception {
        synchronized (CrunchyRollCom.lock) {
            if (br == null) {
                br = this.br;
            }
            try {
                this.setBrowserExclusive();
                br.clearCookies("crunchyroll.com");

                if (refresh == false && loginCookies.containsKey(account)) {
                    HashMap<String, String> cookies = loginCookies.get(account);
                    if (cookies != null) {
                        if (cookies.containsKey("c_userid")) {
                            for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                                final String key = cookieEntry.getKey();
                                final String value = cookieEntry.getValue();
                                br.setCookie("crunchyroll.com", key, value);
                            }
                            return;
                        }
                    }
                }

                LinkedHashMap<String, String> post = new LinkedHashMap<String, String>();
                post.put("formname", "RpcApiUser_Login");
                post.put("fail_url", "http://www.crunchyroll.com/login");
                post.put("name", Encoding.urlEncode(account.getUser()));
                post.put("password", Encoding.urlEncode(account.getPass()));
                post.put("submit", "submit");

                br.setFollowRedirects(true);
                br.postPage("https://www.crunchyroll.com/?a=formhandler", post);

                if (!br.containsHTML("<title>Redirecting\\.\\.\\.</title>")) {
                    if (showDialog) UserIO.getInstance().requestMessageDialog(0, "Crunchyroll Login Error", "Account check needed for crunchyroll.com!");
                    account.setValid(false);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }

                final HashMap<String, String> cookies = new HashMap<String, String>();

                final Cookies cYT = br.getCookies("crunchyroll.com");
                for (final Cookie c : cYT.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                loginCookies.put(account, cookies);
            } catch (PluginException e) {
                loginCookies.remove(account);
                throw e;
            }
        }
    }
}