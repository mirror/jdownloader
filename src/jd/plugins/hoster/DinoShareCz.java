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

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dinoshare.cz" }, urls = { "http://(www\\.)?dinoshare\\.cz/[a-z0-9\\-]+/[A-Za-z0-9]+/" })
public class DinoShareCz extends PluginForHost {
    public DinoShareCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.dinoshare.cz/uzivatel/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.dinoshare.cz/terms-dinoshare.pdf";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            prepBR_API(br);
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once (50 tested, more might be possible) */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("?check_link=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%2C");
                }
                br.getPage("http://www.dinoshare.cz/api/" + sb.toString());
                final String alljson = br.getRegex("\\{\"status\":1,\"files_count\":\\d+,\"0\":(.+)\\}$").getMatch(0);
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final String linkdata = new Regex(alljson, "(\\{[^\\}]*?\"file_name\":\"" + fid + "\"[^\\}]*\\})").getMatch(0);
                    if (linkdata == null || linkdata.contains("File does not exist")) {
                        dllink.setName(fid);
                        dllink.setAvailable(false);
                    } else {
                        final String ftitle = PluginJSonUtils.getJsonValue(linkdata, "file_title");
                        final String fext = PluginJSonUtils.getJsonValue(linkdata, "file_ext");
                        final String fsize = PluginJSonUtils.getJsonValue(linkdata, "file_size");
                        if (ftitle == null || fext == null || fsize == null) {
                            dllink.setName(fid);
                            logger.warning("Linkchecker broken for " + this.getHost());
                            return false;
                        }
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(ftitle + "." + fext);
                        dllink.setDownloadSize(Long.parseLong(fsize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by registered- and premium users");
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)/$").getMatch(0);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void loginAPI(final Account account) throws Exception {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        br.getPage("http://www.dinoshare.cz/api/?check_balance=&user_email=" + Encoding.urlEncode(account.getUser()) + "&user_pass=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("Wrong login credentials or query argument")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    private void loginWeb(final Account account) throws Exception {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        // br.getPage("http://www.dinoshare.cz/uzivatel/");
        /* Todo: Use form */
        br.postPageRaw("http://www.dinoshare.cz/uzivatel/", "user_email=" + Encoding.urlEncode(account.getUser()) + "&user_pass=" + Encoding.urlEncode(account.getPass()) + "&action=P%C5%99ihl%C3%A1sit+se");
        if (br.containsHTML("Wrong login credentials or query argument")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        prepBR_API(this.br);
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            // loginAPI(account); // Get no response 2017-12-27
            loginWeb(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        // final long creditsleft = Long.parseLong(PluginJSonUtils.getJsonValue(br, "credits")); // For loginAPI
        final long creditsleft = Long.valueOf(br.getRegex("kreditů</td>\\s*<td><a href=\"/premium/\">([^<>]+)<").getMatch(0));
        /*
         * Also treat premium accounts with low traffic (1,5 GB or less) as free so that they can't get disabled or similar because of low
         * traffic.
         */
        if (creditsleft <= 1500000000l) {
            /* Traffic gone --> Free account --> Needed to download (not possible as unregistered user) --> Simply set unlimited traffic */
            ai.setUnlimitedTraffic();
            maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
            try {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(maxPrem.get());
                /* Free accounts do not have captchas either (via API). */
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
                account.setProperty("free", true);
            }
            ai.setStatus("Registered (free) account");
        } else {
            ai.setTrafficLeft(creditsleft);
            maxPrem.set(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            try {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        prepBR_API(this.br);
        String dllink = this.checkDirectLink(link, "premium_directlink");
        if (dllink == null) {
            br.getPage("http://www.dinoshare.cz/api/?user_email=" + Encoding.urlEncode(account.getUser()) + "&user_pass=" + Encoding.urlEncode(account.getPass()) + "&download_link=" + Encoding.urlEncode(link.getDownloadURL()));
            dllink = PluginJSonUtils.getJsonValue(br, "download_link");
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        boolean resume = ACCOUNT_PREMIUM_RESUME;
        int maxchunks = ACCOUNT_PREMIUM_MAXCHUNKS;
        if (account.getBooleanProperty("free", false)) {
            resume = ACCOUNT_FREE_RESUME;
            maxchunks = ACCOUNT_FREE_MAXCHUNKS;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.getURL().contains("limit-slotu")) {
                logger.info("Account probably out of traffic --> Changing it to free and limiting max simultan downloads to free account limit");
                account.setProperty("free", true);
                account.getAccountInfo().setUnlimitedTraffic();
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    private void prepBR_API(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}