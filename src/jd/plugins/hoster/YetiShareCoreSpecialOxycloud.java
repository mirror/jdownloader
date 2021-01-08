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

import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

/** TODO: Move to JDownloader/src/jdownloader/plugins/components */
public class YetiShareCoreSpecialOxycloud extends YetiShareCore {
    public YetiShareCoreSpecialOxycloud(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * other: Special <br />
     */
    @Override
    protected boolean usesNewYetiShareVersion() {
        return true;
    }

    protected String getAccountNameSpaceLogin() {
        return "/account/login";
    }

    @Override
    protected String getAccountNameSpaceHome() {
        return "/account";
    }

    @Override
    protected String getAccountNameSpaceUpgrade() {
        return "/upgrade";
    }

    /**
     * @return true: Cookies were validated</br>
     *         false: Cookies were not validated
     */
    public boolean loginWebsiteSpecial(final Account account, boolean force) throws Exception {
        super.loginWebsite(account, force);
        return true;
    }

    @Override
    protected boolean isLoggedin() {
        return this.isLoggedinSpecial();
    }

    public boolean isLoggedinSpecial() {
        boolean loggedIN = super.isLoggedin();
        if (!loggedIN) {
            /*
             * Traits depend on where user currently is: Case 1: For whenever logout button is visible (e.g. account overview) | Case 2:
             * When logout button is not visible e.g. on "/upgrade" page.
             */
            loggedIN = br.containsHTML("/account/logout\"") || br.containsHTML("/account\"");
        }
        return loggedIN;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (this.supports_api()) {
            this.handleDownloadAPI(link, account);
        } else {
            requestFileInformation(link, account, true);
            loginWebsite(account, false);
            this.handleDownloadWebsite(link, account);
        }
    }

    @Override
    public void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* 2020-11-13: Anonymous- & Free Account- and premium account download works the same way. */
        br.setFollowRedirects(true);
        String dllink = checkDirectLink(link, account);
        if (dllink != null) {
            logger.info("Continuing with stored directURL");
        } else {
            logger.info("Generating new directURL");
            final URLConnectionAdapter con = br.openGetConnection(link.getPluginPatternMatcher());
            if (this.looksLikeDownloadableContent(con)) {
                dllink = con.getURL().toString();
            } else {
                br.followConnection();
                br.setFollowRedirects(false);
                Form pwProtected = getPasswordProtectedForm();
                if (pwProtected != null) {
                    /* File is password protected --> Totally different download-way */
                    String passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
                    }
                    pwProtected.put("filePassword", Encoding.urlEncode(passCode));
                    this.submitForm(pwProtected);
                    if (!this.isDownloadlink(br.getRedirectLocation()) || this.getPasswordProtectedForm() != null) {
                        /* Assume that entered password is wrong! */
                        link.setDownloadPassword(null);
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                    } else {
                        /* Entered password is correct - we can start the download. */
                        dllink = br.getRedirectLocation();
                        link.setDownloadPassword(passCode);
                    }
                } else {
                    String internalFileID = this.getInternalFileID(link, this.br);
                    if (internalFileID == null) {
                        /* Check for redirects before this step. E.g. letsupload.io */
                        final String continueURL = this.getContinueLink();
                        if (continueURL == null) {
                            this.checkErrors(link, account);
                            this.checkErrorsLastResort(link, account);
                        }
                        this.getPage(continueURL);
                        internalFileID = this.getInternalFileID(link, this.br);
                        if (internalFileID == null) {
                            /* Dead end */
                            checkErrorsLastResort(link, account);
                        } else {
                            /* Save for the next time. This ID should never change! */
                            link.setProperty(PROPERTY_INTERNAL_FILE_ID, internalFileID);
                        }
                    }
                    if (internalFileID == null) {
                        this.checkErrors(link, account);
                        checkErrorsLastResort(link, account);
                    }
                    br.getPage("/account/direct_download/" + internalFileID);
                    dllink = br.getRedirectLocation();
                }
            }
        }
        if (dllink == null) {
            this.checkErrors(link, account);
            checkErrorsLastResort(link, account);
        }
        final boolean resume = this.isResumeable(link, account);
        final int maxchunks = this.getMaxChunks(account);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
        link.setProperty(getDownloadModeDirectlinkProperty(account), dl.getConnection().getURL().toString());
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            checkErrors(link, account);
            /*
             * Do not check for logged-out state because we could easily get other errorpages here and we do not want to temp. disable
             * accounts by mistake!
             */
            // checkErrorsLastResort(link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown download error", 5 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private Form getPasswordProtectedForm() {
        return br.getFormbyKey("filePassword");
    }

    @Override
    protected boolean isOfflineWebsiteAfterLinkcheck() {
        return this.br.containsHTML(">Status:</span>\\s*<span>\\s*(Deleted|Usunięto)\\s*</span>");
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        /* 2020-10-12 */
        final String waittimeBetweenDownloadsStr = br.getRegex(">\\s*You must wait (\\d+) minutes? between downloads").getMatch(0);
        if (waittimeBetweenDownloadsStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait between downloads", Integer.parseInt(waittimeBetweenDownloadsStr) * 60 * 1001l);
        }
    }

    protected String getInternalFileID(final DownloadLink link, final Browser br) throws PluginException {
        String internalFileID = link.getStringProperty(PROPERTY_INTERNAL_FILE_ID);
        if (internalFileID == null) {
            internalFileID = br.getRegex("showFileInformation\\((\\d+)\\);").getMatch(0);
        }
        return internalFileID;
    }
}