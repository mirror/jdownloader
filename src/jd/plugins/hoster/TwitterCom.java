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
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.ffmpeg.json.StreamInfo;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "twitter.com" }, urls = { "https?://[a-z0-9]+\\.twimg\\.com/media/[^/]+|https?://amp\\.twimg\\.com/prod/[^<>\"]*?/vmap/[^<>\"]*?\\.vmap|https?://amp\\.twimg\\.com/v/.+|https?://(?:www\\.)?twitter\\.com/i/videos/tweet/\\d+" })
public class TwitterCom extends PluginForHost {
    public TwitterCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://twitter.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://twitter.com/tos";
    }

    private static final String  TYPE_DIRECT               = "https?://[a-z0-9]+\\.twimg\\.com/.+";
    private static final String  TYPE_VIDEO                = "https?://amp\\.twimg\\.com/v/.+";
    private static final String  TYPE_VIDEO_VMAP           = "https?://amp\\.twimg\\.com/prod/[^<>\"]*?/vmap/[^<>\"]*?\\.vmap";
    private static final String  TYPE_VIDEO_EMBED          = "https?://(?:www\\.)?twitter\\.com/i/videos/tweet/\\d+";
    /* Connection stuff - don't allow chunks as we only download small pictures */
    private static final boolean FREE_RESUME               = true;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = 20;
    private static final boolean ACCOUNT_FREE_RESUME       = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = 20;
    private String               dllink                    = null;
    private boolean              account_required          = false;
    private boolean              server_issues             = false;
    private String               tweetid                   = null;

    private void setconstants(final DownloadLink dl) {
        dllink = null;
        server_issues = false;
        account_required = false;
        if (dl.getDownloadURL().matches(TYPE_VIDEO_EMBED)) {
            tweetid = new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
        } else {
            tweetid = dl.getStringProperty("tweetid", null);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setconstants(link);
        URLConnectionAdapter con = null;
        /* Most times twitter-image/videolinks will come from the decrypter. */
        String filename = link.getStringProperty("decryptedfilename", null);
        if (link.getDownloadURL().matches(TYPE_VIDEO) || link.getDownloadURL().matches(TYPE_VIDEO_VMAP)) {
            this.br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                account_required = true;
                return AvailableStatus.TRUE;
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String vmap_url = null;
            if (link.getDownloadURL().matches(TYPE_VIDEO_VMAP)) {
                /* Direct vmap url was added by user- or decrypter. */
                vmap_url = link.getDownloadURL();
            } else {
                /* Videolink was added by user or decrypter. */
                vmap_url = this.br.getRegex("name=\"twitter:amplify:vmap\" content=\"(https?://[^<>\"]*?\\.vmap)\"").getMatch(0);
                if (vmap_url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (this.dllink == null) {
                this.br.getPage(vmap_url);
                dllink = this.br.getRegex("<MediaFile>[\t\n\r ]+<\\!\\[CDATA\\[(http[^<>\"]*?)\\]\\]>[\t\n\r ]+</MediaFile>").getMatch(0);
                if (dllink == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else if (link.getDownloadURL().matches(TYPE_VIDEO_EMBED)) {
            this.br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 403) {
                account_required = true;
                return AvailableStatus.TRUE;
            } else if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.getRequest().setHtmlCode(Encoding.htmlDecode(this.br.toString()));
            dllink = PluginJSonUtils.getJson(this.br, "video_url");
            filename = tweetid + "_" + tweetid + ".mp4";
        } else { // TYPE_DIRECT - jpg/png/mp4
            dllink = link.getDownloadURL();
            if (dllink.contains("jpg") || dllink.contains("png")) {
                try {
                    final String dllink_temp;
                    if (dllink.contains(":large")) {
                        dllink_temp = dllink.replace(":large", "") + ":orig";
                    } else if (dllink.lastIndexOf(":") < 8 && dllink.matches(".+\\.(jpg|jpeg|png)$")) {
                        /* Append this to get the highest quality possible */
                        dllink_temp = dllink + ":orig";
                    } else {
                        dllink_temp = dllink;
                    }
                    con = br.openHeadConnection(dllink_temp);
                    if (!con.getContentType().contains("html")) {
                        dllink = dllink_temp;
                        link.setUrlDownload(dllink);
                    }
                } finally {
                    con.disconnect();
                }
            }
        }
        if (!StringUtils.isEmpty(dllink)) {
            try {
                if (dllink.contains(".m3u8")) {
                    link.setFinalFileName(filename);
                    checkFFProbe(link, "Download a HLS Stream");
                    br.getPage(this.dllink);
                    if (this.br.getHttpConnection().getResponseCode() == 403) {
                        /* 2017-06-01: Unsure because browser shows the thumbnail and video 'wants to play' but doesn't. */
                        throw new PluginException(LinkStatus.ERROR_FATAL, "GEO-blocked or offline content");
                    }
                    final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
                    this.dllink = hlsbest.getDownloadurl();
                    final HLSDownloader downloader = new HLSDownloader(link, br, dllink);
                    final StreamInfo streamInfo = downloader.getProbe();
                    if (streamInfo == null) {
                        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        server_issues = true;
                    } else {
                        final long estimatedSize = downloader.getEstimatedSize();
                        if (estimatedSize > 0) {
                            link.setDownloadSize(estimatedSize);
                        }
                    }
                } else {
                    con = br.openHeadConnection(dllink);
                    final long filesize = con.getLongContentLength();
                    if (filesize == 0) {
                        /* E.g. abused video */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    if (!con.getContentType().contains("html") && con.isOK() && con.getLongContentLength() > 0) {
                        if (filename == null) {
                            filename = Encoding.htmlDecode(getFileNameFromHeader(con)).replace(":orig", "");
                        }
                        if (tweetid != null && !filename.contains(tweetid)) {
                            filename = tweetid + "_" + filename;
                        }
                        link.setFinalFileName(filename);
                        link.setDownloadSize(filesize);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static String regexTwitterVideo(final String source) {
        String finallink = PluginJSonUtils.getJson(source, "video_url");
        // String finallink = new Regex(source, "video_url\\&quot;:\\&quot;(https:[^<>\"]*?\\.mp4)\\&").getMatch(0);
        // if (finallink != null) {
        // finallink = finallink.replace("\\", "");
        // }
        return finallink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (account_required) {
            /*
             * 2017-05-10: This can also happen when a user is logged in because there are e.g. timelines which only 'friends' can view
             * which means having an account does not necessarily mean that a user has the rights to view all of the other users' content ;)
             */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (this.dllink.contains(".m3u8")) {
            dl = new HLSDownloader(downloadLink, br, this.dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private static final String MAINPAGE = "http://twitter.com";
    private static Object       LOCK     = new Object();

    public static void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(account.getHoster(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage("https://twitter.com/login");
                final String authenticytoken = br.getRegex("type=\"hidden\" value=\"([^<>\"]*?)\" name=\"authenticity_token\"").getMatch(0);
                if (authenticytoken == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if ("pl".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBłąd wtyczki, skontaktuj się z Supportem JDownloadera!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                final String postData = "session%5Busername_or_email%5D=" + Encoding.urlEncode(account.getUser()) + "&session%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&authenticity_token=" + Encoding.urlEncode(authenticytoken) + "&scribe_log=&redirect_after_login=&authenticity_token=" + Encoding.urlEncode(authenticytoken) + "&remember_me=1";
                br.postPage("https://twitter.com/sessions", postData);
                if (br.getCookie(MAINPAGE, "auth_token") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(MAINPAGE), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setMaxSimultanDownloads(1);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Free Account");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(this.br, account, false);
        requestFileInformation(link);
        doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}