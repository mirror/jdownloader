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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "icerbox.biz" }, urls = { "http://(?:www\\.)?(?:nitrobit\\.net|icerbox\\.biz)/(?:view|watch)/([A-Z0-9]+)" })
public class IcerboxBiz extends antiDDoSForHost {
    public IcerboxBiz(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("https://www.icerbox.biz/payment");
    }

    @Override
    public String getAGBLink() {
        return "http://www.icerbox.biz/tos";
    }

    /* Connection stuff */
    private static final int     FREE_MAXDOWNLOADS            = 20;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String               fuid                         = null;

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("/watch/", "/view/"));
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "nitrobit.net".equals(host)) {
            return "icerbox.biz";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /** Premium FULL browser response for case 'daily downloadlimit reached': "0הורדת קובץ זה תעבור על המכסה היומית" */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception, PluginException {
        fuid = getFID(link);
        link.setLinkID(fuid);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(br, link.getDownloadURL());
        if (br.containsHTML(">רוב הסיכויים שנמחק. אתה מועבר לדף הראשי<") || this.br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains(fuid)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("הורדת קובץ\\s*\\[(.*?)\\]\\s+<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<b>שם הקובץ: </b>\\s*<span title=\"([^<>\"]+)\"").getMatch(0);
        }
        final String filesize = br.getRegex("<b>גודל הקובץ: </b>\\s*<span[^>]+>([^<>\"]*?)</span>").getMatch(0);
        if (filename != null) {
            link.setName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        } else {
            link.setName(this.fuid);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new AccountRequiredException();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account acc) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink_account_premium");
        if (dllink == null) {
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            getPage(br, "http://www." + this.getHost() + "/ajax/unlock.php?password=" + Encoding.urlEncode(acc.getPass()) + "&file=" + fuid + "&keep=false&_=" + System.currentTimeMillis());
            /**
             * TODO: Find out if maybe this contains the expire date of the account and set it: <b>לקוח יקר: </b><br />
             * תוקף קוד הגישה שלך יפוג בעוד <b style="color:red">1 ימים, 18 שעות, 53 דקות.</b><br
             */
            if (this.br.containsHTML("הקובץ אינו קיים")) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (this.br.containsHTML("0קוד גישה שגוי")) {
                if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            } else if (this.br.containsHTML("0הורדת קובץ זה תעבור על המכסה היומית")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Trafficlimit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            dllink = this.br.getRegex("id=\"download\" href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = this.br.getRegex("\"(https?://[^/]+/d/[^<>\"]*?)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink_account_premium", dllink);
        /* start the dl */
        dl.startDownload();
    }

    /* We cannot check the premium password until downloadstart. */
    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        ai.setStatus("Unchecked account");
        return ai;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}