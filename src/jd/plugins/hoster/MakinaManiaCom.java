//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "makinamania.net" }, urls = { "https?://(?:www\\.)?makinamania\\.(?:com|net)/((download/|descargar\\-).+|index\\.php\\?action=dlattach;topic=\\d+(?:\\.0)?;attach=\\d+)" })
public class MakinaManiaCom extends PluginForHost {
    public MakinaManiaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://www.makinamania.net/";
    }

    private static final String ATTACHEDFILELINK = ".+/index\\.php\\?action=dlattach;topic=\\d+(?:\\.0)?;attach=\\d+";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(ATTACHEDFILELINK)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(link.getPluginPatternMatcher());
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    if (con.isContentDecoded()) {
                        link.setDownloadSize(con.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                }
                link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            br.getPage(link.getPluginPatternMatcher());
            if (br.getURL().contains("descargas-busca=")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
            String filesize = br.getRegex("(?i)\\&nbsp;\\&nbsp;Tamaño\\s*: ([^<>\"]*?)<br").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("Tama.o\\s*:\\s*(\\d+[^<>\"]+)<").getMatch(0);
            }
            if (filename != null) {
                link.setName(Encoding.htmlDecode(filename).trim());
            }
            if (filesize != null) {
                link.setDownloadSize(SizeFormatter.getSize(filesize));
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        this.handleDownload(link, null);
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    /* TODO: Add cookie-check if force == true */
                    logger.info("Trust cookies without login");
                    br.setCookies(cookies);
                    return;
                }
                br.postPage("http://www.makinamania.net/index.php?action=login2", "&user=" + Encoding.urlEncode(account.getUser()) + "&passwrd=" + Encoding.urlEncode(account.getPass()) + "&cookielength=999&hash_passwrd=");
                if (br.getRedirectLocation() == null || !br.getRedirectLocation().contains("sa=check")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        String dllink = null;
        if (link.getPluginPatternMatcher().matches(ATTACHEDFILELINK)) {
            dllink = link.getPluginPatternMatcher();
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getPluginPatternMatcher());
            br.setFollowRedirects(false);
            dllink = br.getRegex("\"javascript:download\\(\\'(https?://[^<>\"]*?)\\'\\)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(\"|\\')(https?://machines\\.[^/]+/descargas/descarga\\.php\\?id=[^<>\"]*?)(\"|\\')").getMatch(1);
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(dllink);
            if (br.containsHTML("No se ha encontrado el archivo")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        String filenameFromHeader = getFileNameFromHeader(dl.getConnection());
        if (filenameFromHeader != null) {
            filenameFromHeader = Encoding.htmlDecode(filenameFromHeader).trim();
            link.setFinalFileName(filenameFromHeader);
        }
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        if (account != null) {
            login(account, false);
        }
        requestFileInformation(link);
        String dllink = null;
        if (link.getPluginPatternMatcher().matches(ATTACHEDFILELINK)) {
            dllink = link.getPluginPatternMatcher();
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getPluginPatternMatcher());
            dllink = br.getRegex("\"javascript:download\\(\\'(https?://[^<>\"]*?)\\'\\)").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(\"|\\')(https?://machines\\.[^/]+/descargas/descarga\\.php\\?id=[^<>\"]*?)(\"|\\')").getMatch(1);
            }
            if (dllink == null) {
                if (account != null) {
                    logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    /* Assume that account is required to download this file. */
                    throw new AccountRequiredException();
                }
            }
            br.setFollowRedirects(false);
            br.getPage(dllink);
            if (br.containsHTML("No se ha encontrado el archivo")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                logger.warning("Failed to find expected redirect to final downloadurl");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        String filenameHeader = getFileNameFromHeader(dl.getConnection());
        if (filenameHeader != null) {
            filenameHeader = Encoding.htmlDecode(filenameHeader).trim();
            filenameHeader = filenameHeader.replace("_(MAKINAMANIA.COM)", "");
            link.setFinalFileName(filenameHeader);
        }
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (dl.getConnection().getLongContentLength() == 0) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: File is empty", 10 * 60 * 1000l);
        }
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}