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
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http://[\\w\\.]*?facebook\\.com/video/video\\.php\\?v=\\d+" }, flags = { 2 })
public class FaceBookComVideos extends PluginForHost {

    public FaceBookComVideos(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.facebook.com/r.php");
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "http://www.facebook.com/terms.php";
    }

    private static String       FACEBOOKMAINPAGE = "http://www.facebook.com";
    private static final String DLLINKREGEXP     = "\\(\"video_src\", \"(http.*?)\"\\)";

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(FACEBOOKMAINPAGE + "//login.php");
        Form loginForm = br.getForm(0);
        if (loginForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        loginForm.put("email", account.getUser());
        loginForm.put("pass", account.getPass());
        br.submitForm(loginForm);
        if (br.getCookie(FACEBOOKMAINPAGE, "c_user") == null || br.getCookie(FACEBOOKMAINPAGE, "xs") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        String addedlink = downloadLink.getDownloadURL();
        if (dllink == null) {
            br.getPage(addedlink);
            dllink = br.getRegex(DLLINKREGEXP).getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.urlDecode(dllink, true);
        logger.info("Final downloadlink = " + dllink + " starting download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) return false;
        try {
            br.setCookie("http://www.facebook.com", "locale", "en_GB");
            Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) throw new PluginException(LinkStatus.ERROR_FATAL, "Kann Links ohne gültigen Account nicht überprüfen");
            br.setFollowRedirects(true);
            login(aa);
            for (DownloadLink dl : urls) {
                String addedlink = dl.getDownloadURL();
                br.getPage(addedlink);
                if (br.containsHTML("No htmlCode read")) {
                    dl.setAvailable(false);
                    continue;
                }
                dllink = br.getRegex(DLLINKREGEXP).getMatch(0);
                // extrahiere Videoname aus HTML-Quellcode
                String filename = br.getRegex("class=\"video_title datawrap\">(.*)</h3>").getMatch(0);

                // wird nichts gefunden, versuche Videoname aus einem anderen
                // Teil des Quellcodes rauszusuchen
                if (filename == null) {
                    filename = br.getRegex("<title>Facebook \\| Videos posted by .*: (.*)</title>").getMatch(0);
                }

                // falls Videoname immer noch nicht gefunden wurde, dann
                // versuche Username & Video-ID als Filename zu nehmen
                if (filename == null) {
                    filename = br.getRegex("<title>Facebook \\| Videos posted by (.*): .*</title>").getMatch(0).trim();

                    // falls Username gefunden wurde, so setze dies und Video-ID
                    // zusammen
                    if (filename != null) {
                        String videoid = new Regex(dl.getDownloadURL(), "facebook\\.com/video/video\\.php\\?v=(\\d+)").getMatch(0);
                        filename = filename + " - Video_" + videoid;
                    }
                }

                // wurde Filename extrahiert, setze entgültiger Dateiname &
                // Dateiendung
                if (filename != null) {
                    filename = filename.trim();
                    dl.setFinalFileName(filename + ".mp4");
                    dl.setAvailable(true);
                } else {
                    // falls nicht, so setze den Download als nicht verfügbar
                    dl.setAvailable(false);
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}