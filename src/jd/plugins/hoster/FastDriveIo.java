//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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

import org.appwork.utils.Exceptions;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.YetiShareCore;
import org.jdownloader.plugins.components.YetiShareCoreNew;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FastDriveIo extends YetiShareCoreNew {
    public FastDriveIo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null solvemedia reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fastdrive.io" });
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
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return true;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 0;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 0;
        }
    }

    /** 2021-04-22: Heavily modified / workarounded! */
    @Override
    protected void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        try {
            requestFileInformationWebsite(link, account, true);
        } catch (final PluginException e) {
            ignorePluginException(e, br, link, account);
        }
        if (account != null) {
            loginWebsite(account, false);
            br.setFollowRedirects(false);
            getPage(link.getPluginPatternMatcher());
        }
        final boolean resume = this.isResumeable(link, account);
        final int maxchunks = this.getMaxChunks(account);
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        boolean captcha = false;
        boolean captchaSuccess = false;
        /* Try to re-used stored direct downloadurl */
        checkDirectLink(link, account);
        String externalRedirect = null;
        if (this.dl == null) {
            /*
             * Check for direct-download and ensure that we're logged in! </br> This is needed because we usually load- and set stored
             * cookies without verifying them to save time!
             */
            boolean hasGoneThroughVerifiedLoginOnce = false;
            do {
                String redirect = br.getRedirectLocation();
                if (redirect != null && !this.isDownloadlink(redirect)) {
                    if (Browser.getHost(redirect, true).equals(this.getHost())) {
                        /* just follow a single redirect */
                        this.br.followRedirect(false);
                        continue;
                    } else {
                        /* Looks like redirect to external website -> Stop here */
                        externalRedirect = redirect;
                        break;
                    }
                }
                if (this.isDownloadlink(redirect)) {
                    br.setFollowRedirects(true);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), resume, maxchunks);
                    if (this.looksLikeDownloadableContent(dl.getConnection())) {
                        logger.info("Direct download");
                        break;
                    } else {
                        try {
                            br.followConnection(true);
                        } catch (final IOException e) {
                            logger.log(e);
                        }
                        dl = null;
                    }
                }
                if (account == null) {
                    break;
                } else if (this.isLoggedin(this.br)) {
                    break;
                } else if (hasGoneThroughVerifiedLoginOnce) {
                    /**
                     * Only try once! </br>
                     * We HAVE to be logged in at this stage!
                     */
                    this.loggedInOrException(this.br, account);
                    break;
                } else {
                    /*
                     * Some websites only allow 1 session per user. If a user then logs in again via browser while JD is logged in, we might
                     * download as a free-user without noticing that. Example host: Przeslij.com </br> This may also help in other
                     * situations in which we get logged out all of the sudden.
                     */
                    logger.warning("Possible login failure -> Trying again");
                    loginWebsite(account, true);
                    br.setFollowRedirects(false);
                    getPage(link.getPluginPatternMatcher());
                    hasGoneThroughVerifiedLoginOnce = true;
                    continue;
                }
            } while (true);
        }
        if (this.dl == null) {
            br.setFollowRedirects(false);
            if (externalRedirect == null) {
                if (supports_availablecheck_over_info_page(link)) {
                    br.setFollowRedirects(true);
                    /* For premium mode, we might get our final downloadurl here already. */
                    final URLConnectionAdapter con = br.openGetConnection(link.getPluginPatternMatcher());
                    if (this.looksLikeDownloadableContent(con)) {
                        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, con.getRequest(), resume, maxchunks);
                    } else {
                        try {
                            br.followConnection(true);
                        } catch (final IOException e) {
                            logger.log(e);
                        }
                    }
                }
                if (br.getRedirectLocation() != null) {
                    br.setFollowRedirects(true);
                    /* For premium mode, we might get our final downloadurl here already. */
                    final URLConnectionAdapter con = br.openGetConnection(br.getRedirectLocation());
                    if (this.looksLikeDownloadableContent(con)) {
                        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, con.getRequest(), resume, maxchunks);
                    } else {
                        try {
                            br.followConnection(true);
                        } catch (final IOException e) {
                            logger.log(e);
                        }
                    }
                } else {
                    /* Redirect to roda.site or securitystickers.info -> Captcha -> Final downloadurl */
                    externalRedirect = br.getRegex("(https?://go\\." + Regex.escape(this.br.getHost()) + "/[A-Za-z0-9]+)").getMatch(0);
                }
            }
            if (externalRedirect == null) {
                this.checkErrorsLastResort(br, link, account);
            }
            br.setFollowRedirects(false);
            this.getPage(externalRedirect);
            final String redirectToUrlShortener = this.br.getRedirectLocation();
            if (redirectToUrlShortener == null) {
                this.checkErrorsLastResort(br, link, account);
            }
            final PluginForDecrypt crawler = this.getNewPluginForDecryptInstance("cut-urls.com");
            crawler.setBrowser(this.br.cloneBrowser());
            final CryptedLink param = new CryptedLink(redirectToUrlShortener, link);
            final ArrayList<DownloadLink> decryptedLinks = crawler.decryptIt(param, null);
            if (decryptedLinks.isEmpty()) {
                this.checkErrorsLastResort(br, link, account);
            }
            final String dllink = decryptedLinks.get(0).getPluginPatternMatcher();
            this.br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        }
        final URLConnectionAdapter con = dl.getConnection();
        /*
         * Save directurl before download-attempt as it should be valid even if it e.g. fails because of server issue 503 (= too many
         * connections) --> Should work fine after the next try.
         */
        link.setProperty(directlinkproperty, con.getURL().toString());
        try {
            checkResponseCodeErrors(con);
        } catch (final PluginException e) {
            try {
                br.followConnection(true);
            } catch (IOException ioe) {
                throw Exceptions.addSuppressed(e, ioe);
            }
            throw e;
        }
        if (!looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (captcha && !captchaSuccess) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            checkErrors(br, link, account);
            checkErrorsLastResort(br, link, account);
        }
        dl.setFilenameFix(isContentDispositionFixRequired(dl, con, link));
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }
}