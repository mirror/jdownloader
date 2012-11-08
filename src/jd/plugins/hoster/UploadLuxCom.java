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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadlux.com" }, urls = { "http://(www\\.)?uploadlux\\.com/(l|play)\\-[a-z0-9]+" }, flags = { 2 })
public class UploadLuxCom extends PluginForHost {

    public UploadLuxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploadlux.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadlux.com/conditions";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("uploadlux.com/play-", "uploadlux.com/l-"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(MAINPAGE, "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Erreur fichier introuvable")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(">Fichier privée\\.<")) {
            link.getLinkStatus().setStatusText("Fichier privée!");
            link.setName(new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<b>Filename:</b> <b title=\"([^<>\"]*?)\"").getMatch(0);
        String filesize = br.getRegex("<b>File Size:</b> <b style=\"color:#2BB2E3\">([^<>\"]*?)</b><br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML(">Fichier privée\\.<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fichier privée!");
        br.setFollowRedirects(false);
        final String waittime = br.getRegex("style=\"font\\-size:30px; text\\-decoration:none;\">(\\d+)</span><br />").getMatch(0);
        int wait = 60;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL().replace("uploadlux.com/l", "uploadlux.com/lf"), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String  MAINPAGE = "http://uploadlux.com";
    private static Object        LOCK     = new Object();
    private static AtomicInteger maxPrem  = new AtomicInteger(1);

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("http://www.uploadlux.com/connexion", "souvenir=on&connexion=Connexion&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("Si cette page reste affichée plus de")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                br.getPage("http://www.uploadlux.com/profil");
                if (br.containsHTML("<b style=\"color: #FF3300\">Premium</b>")) {
                    account.setProperty("nopremium", false);
                } else if (br.containsHTML("<b style=\"color: #FF3300\">Membre</b>")) {
                    account.setProperty("nopremium", true);
                } else {
                    logger.warning("Unsupported accounttype!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex("<b>([^<>\"]*?)</b> on <b>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        ai.setUnlimitedTraffic();

        if (account.getBooleanProperty("nopremium")) {
            ai.setStatus("Registered (free) User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
        } else {
            final String expire = br.getRegex("<td> \\- (\\d{2}/\\d{2}/\\d{4})</td><td><a href=\"index\\.php\\?id=premium\\.php\"").getMatch(0);
            if (expire == null) {
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy", Locale.ENGLISH));
            }
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
            ai.setStatus("Premium User");
        }

        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML(">Fichier privée\\.<")) throw new PluginException(LinkStatus.ERROR_FATAL, "Fichier privée!");
        login(account, false);
        br.setFollowRedirects(false);
        if (account.getBooleanProperty("nopremium")) {
            br.getPage(link.getDownloadURL());
            doFree(link);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL().replace("uploadlux.com/l", "uploadlux.com/lf"), false, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
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