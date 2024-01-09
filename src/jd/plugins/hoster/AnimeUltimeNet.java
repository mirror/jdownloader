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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class AnimeUltimeNet extends PluginForHost {
    public AnimeUltimeNet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www.anime-ultime.net/premium-0-1");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "anime-ultime.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/info\\-0\\-1/((\\d+)(/([^/#]+))?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://www.anime-ultime.net/index-0-1#principal";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return "alimeultimenet://file/" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
    }

    private String getDirecturlProperty(final Account account) {
        if (account == null) {
            return "directlink";
        } else {
            return "directlink_account_ " + account.getType().getLabel();
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final Account account) {
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("https://www." + this.getHost() + "/info-0-1/" + new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0));
        // br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)>\\s*0 vostfr streaming\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fid = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        String filename = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = fid;
        }
        String filesize = br.getRegex("Taille\\s*:\\s*([^<>\"]*?)<br />").getMatch(0);
        String ext = br.getRegex("Conteneur\\s*:\\s*([^<>\"]*?)<br />").getMatch(0);
        if (ext != null) {
            ext = "." + ext.trim();
        } else {
            ext = "";
        }
        if (filesize != null) {
            if (filesize.equals("")) {
                /* Probably offline as filesize is not given and downloadlink is not available/dead(404) */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filesize = filesize.replace("mo", "mb");
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (filename != null) {
            filename = Encoding.htmlDecode(filename).trim() + ext;
            link.setName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        final String directlinkproperty = getDirecturlProperty(account);
        final String storedDirectlink = link.getStringProperty(directlinkproperty);
        String dllink = null;
        if (storedDirectlink != null) {
            logger.info("Re-using stored directlink: " + storedDirectlink);
            dllink = storedDirectlink;
        } else {
            requestFileInformation(link);
            if (account != null) {
                this.login(account, false);
            }
            final String fid = getFID(link);
            /*
             * 2023-01-09: It is possible to download as premium user in free mode but let's not be evil and hope that they will patch this
             * possibility soon!
             */
            final boolean enforcePremiumLinkAlsoInFreeMode = false;
            if ((account != null && account.getType() == AccountType.PREMIUM) || enforcePremiumLinkAlsoInFreeMode) {
                dllink = "https://www." + this.getHost() + "/ddl/" + fid + "/orig/";
            } else {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                brc.postPage("/ddl/authorized_download.php", "idfile=" + fid + "&type=orig");
                final Map<String, Object> entries = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                final boolean skipPreDownloadWaittime = false;
                if (!skipPreDownloadWaittime) {
                    final int waittime = ((Number) entries.get("wait")).intValue();
                    this.sleep(waittime * 1000l, link);
                }
                brc.postPage("/ddl/authorized_download.php", "idfile=" + fid + "&type=orig");
                final Map<String, Object> entries2 = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
                dllink = entries2.get("link").toString();
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, account), this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404 - file offline?", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        } catch (final Exception e) {
            if (storedDirectlink != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toExternalForm());
        dl.startDownload();
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                br.getPage("https://www." + this.getHost() + "/");
                if (this.isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost() + "/");
            final Form loginform = br.getFormbyProperty("name", "identification");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("Indexlogin", Encoding.urlEncode(account.getUser()));
            loginform.put("Indexpassword", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (!isLoggedin(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/disconnect");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        // if (br.containsHTML("")) {
        // account.setType(AccountType.FREE);
        // /* free accounts can still have captcha */
        // account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        // account.setConcurrentUsePossible(false);
        // } else {
        // final String expire = br.getRegex("").getMatch(0);
        // if (expire == null) {
        // throw new AccountInvalidException();
        // } else {
        // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
        // }
        // account.setType(AccountType.PREMIUM);
        // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
        // account.setConcurrentUsePossible(true);
        // }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
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
        // Untested -> Set to 1
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}