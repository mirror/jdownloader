//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fastshare.cz" }, urls = { "http://(www\\.)?fastshare\\.cz/\\d+/[^<>\"#]+" })
public class FastShareCz extends antiDDoSForHost {
    public FastShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fastshare.cz/cenik_cs");
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    @Override
    public String getAGBLink() {
        return "https://www.fastshare.cz/podminky";
    }

    private static final String MAINPAGE = "https://www.fastshare.cz";
    private static Object       LOCK     = new Object();

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("http://", "https://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br = new Browser();
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "lang", "cs");
        br.setCustomCharset("utf-8");
        getPage(link.getDownloadURL());
        if (br.containsHTML("(<title>FastShare\\.cz</title>|>Tento soubor byl smazán na základě požadavku vlastníka autorských)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>([^<>\"]*?) \\| FastShare\\.cz</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<h2><b><span style=color:black;>([^<>\"]*?)</b></h2>").getMatch(0);
        }
        String filesize = br.getRegex("<tr><td>(Velikost|Size): </td><td style=font\\-weight:bold>([^<>\"]*?)</td></tr>").getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("(Velikost|Size): ([0-9]+ .*?),").getMatch(1);
            if (filesize == null) {
                filesize = br.getRegex("<strong>(Velikost|Size) :</strong>([^<>\"]*?)<").getMatch(1);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("(>100% FREE slotů je plných|>Využijte PROFI nebo zkuste později)")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
        }
        br.setFollowRedirects(false);
        final String captchaLink = br.getRegex("\"(/securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
        String action = br.getRegex("=\"(/free/[^<>\"]*?)\"").getMatch(0);
        if (action == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (captchaLink != null) {
            final String captcha = getCaptchaCode(MAINPAGE + captchaLink, downloadLink);
            postPage(action, "code=" + Encoding.urlEncode(captcha));
        } else {
            postPage(action, "");
        }
        if (br.containsHTML("Pres FREE muzete stahovat jen jeden soubor najednou")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 60 * 1000l);
        } else if (br.containsHTML("Špatně zadaný kód. Zkuste to znovu")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            /*
             * E.g.
             * "<script>alert('Přes FREE můžete stahovat jen jeden soubor současně.');top.location.href='http://www.fastshare.cz/123456789/blabla.bla';</script>"
             */
            if (br.containsHTML("Přes FREE můžete stahovat jen jeden soubor současně")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting more free downloads", 3 * 60 * 1000l);
            } else if (br.containsHTML("<script>alert\\(")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error #1", 30 * 60 * 1000l);
            } else if (br.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error #2", 30 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("No htmlCode read")) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Server error");
            }
            logger.info("fastshare.cz: Unknown error -> Retrying");
            int timesFailed = downloadLink.getIntegerProperty("timesfailedfastsharecz_unknown", 0);
            downloadLink.getLinkStatus().setRetryCount(0);
            if (timesFailed <= 2) {
                timesFailed++;
                downloadLink.setProperty("timesfailedfastsharecz_unknown", timesFailed);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Unknown error");
            } else {
                downloadLink.setProperty("timesfailedfastsharecz_unknown", Property.NULL);
                logger.info("fastshare.cz: Unknown error -> Plugin is broken");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setCookie(MAINPAGE, "lang", "cs");
                br.setCustomCharset("utf-8");
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                postPage("https://fastshare.cz/sql.php", "login=" + Encoding.urlEncode(account.getUser()) + "&heslo=" + Encoding.urlEncode(account.getPass()));
                if (br.getURL().contains("fastshare.cz/error=1") || !br.containsHTML(">Kredit[\t\n\r ]+:[\t\n\r ]+</td>")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", fetchCookies(MAINPAGE));
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        String availabletraffic = br.getRegex(">Kredit[\t\n\r ]+:[\t\n\r ]+</td>[\r\n\t ]+<td[^>]*?>([^<>\"&]+)").getMatch(0);
        if (availabletraffic != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(availabletraffic));
        } else {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        getPage(link.getDownloadURL());
        if (br.containsHTML("máte dostatečný kredit pro stažení tohoto souboru")) {
            logger.info("Trafficlimit reached!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        /* Maybe user has direct downloads active */
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            /* Direct downloads inactive --> We have to find the final downloadlink */
            dllink = br.getRegex("\"(https?://[a-z0-9]+\\.fastshare\\.cz/download\\.php[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("class=\"speed\"><a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
            }
        }
        if (dllink == null) {
            logger.warning("Failed to find final downloadlink");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Nemate dostatecny kredit pro stazeni tohoto souboru! Kredit si muzete dobit")) {
                logger.info("Trafficlimit reached!");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            logger.warning("The final dllink seems not to be a file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}