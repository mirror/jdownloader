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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http://[\\w\\.]*?facebook\\.com/video/video\\.php\\?v=\\d+" }, flags = { 2 })
public class FaceBookComVideos extends PluginForHost {

    private String              dllink           = null;
    private static String       FACEBOOKMAINPAGE = "http://www.facebook.com";
    private static final String DLLINKREGEXP     = "\\(\"(highqual|video)_src\", \"(http.*?)\"\\)";

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.facebook.com/r.php");
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            br.setCookie("http://www.facebook.com", "locale", "en_GB");
            final Account aa = AccountController.getInstance().getValidAccount(this);
            if (aa == null || !aa.isValid()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Kann Links ohne gültigen Account nicht überprüfen"); }
            br.setFollowRedirects(true);
            login(aa);
            for (final DownloadLink dl : urls) {
                final String addedlink = dl.getDownloadURL();
                br.getPage(addedlink);
                if (br.containsHTML("No htmlCode read")) {
                    dl.setAvailable(false);
                    continue;
                }
                dllink = Encoding.urlDecode(decodeUnicode(br.getRegex(DLLINKREGEXP).getMatch(1)), true);
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
                        final String videoid = new Regex(dl.getDownloadURL(), "facebook\\.com/video/video\\.php\\?v=(\\d+)").getMatch(0);
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
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.facebook.com/terms.php";
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        final String addedlink = downloadLink.getDownloadURL();
        if (dllink == null) {
            br.getPage(addedlink);
            dllink = br.getRegex(DLLINKREGEXP).getMatch(1);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        final String Vollkornkeks = downloadLink.getDownloadURL().replace(FACEBOOKMAINPAGE, "");
        br.setCookie(FACEBOOKMAINPAGE, "x-referer", Encoding.urlEncode(FACEBOOKMAINPAGE + Vollkornkeks + "#" + Vollkornkeks));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public void login(final Account account) throws Exception {
        setBrowserExclusive();
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(FACEBOOKMAINPAGE);
        final Form loginForm = br.getForm(0);
        if (loginForm == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        loginForm.remove("persistent");
        loginForm.remove(null);
        loginForm.put("email", account.getUser());
        loginForm.put("pass", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginForm);
        if (br.getCookie(FACEBOOKMAINPAGE, "c_user") == null || br.getCookie(FACEBOOKMAINPAGE, "xs") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}