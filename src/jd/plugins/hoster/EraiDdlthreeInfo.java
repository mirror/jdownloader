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

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class EraiDdlthreeInfo extends YetiShareCoreSpecialOxycloud {
    public EraiDdlthreeInfo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + this.getHost() + "/register");
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info 2020-09-14: No limits at all :<br />
     * captchatype-info: 2020-09-14: null<br />
     * other: 2020-11-12: Downloads are only possible via (free) account and only if URLs were added via crawler! <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "erai-ddl3.info" });
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
        return EraiDdlthreeInfo.buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            /* 2020-09-14: Special:Allow subdomains */
            ret.add("https?://(?:[a-z0-9]+\\.)?" + YetiShareCore.buildHostsPatternPart(domains) + YetiShareCore.getDefaultAnnotationPatternPart());
        }
        return ret.toArray(new String[0]);
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

    @Override
    public boolean requires_WWW() {
        /* 2020-11-12 */
        return false;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null, true);
        /* 2020-11-12: Downloads without account are not possible anymore */
        throw new AccountRequiredException();
    }

    @Override
    public void checkErrors(final DownloadLink link, final Account account) throws PluginException {
        super.checkErrors(link, account);
        if (br.containsHTML(">\\s*Please enter your information to register for an account")) {
            throw new AccountRequiredException();
        }
    }

    @Override
    protected String getInternalFileID(final DownloadLink link) throws PluginException {
        final String internalFileID = super.getInternalFileID(link);
        if (internalFileID == null) {
            /*
             * 2020-11-12: Cannot download without this ID! Needs to be set in crawler in beforehand! --> This should never happen because
             * of canHandle()!
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unable to download URLs without internal fileID");
        } else {
            return internalFileID;
        }
    }

    @Override
    protected AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = super.fetchAccountInfoWebsite(account);
        /*
         * 2020-11-12: Special: Needs premium status so that upper handling jumps into the right code-block - basically their free accounts
         * require premium handling too!
         */
        // account.setType(AccountType.PREMIUM);
        /* Correct status to not display default status -> Do not display as premium account (because ... it's not a premium account!) */
        ai.setStatus("Free account");
        return ai;
    }

    @Override
    protected boolean isPremiumAccount(final Browser br) {
        /** 2020-11-12: TODO: Implement proper premium account recognition */
        /** Workaround - see fetchAccountInfoWebsite */
        return true;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /*
         * 2020-11-12: Downloads without account are not possible anymore. Downloads are additionally only possible when this internal
         * fileID is given!
         */
        return account != null && link != null && link.getStringProperty(PROPERTY_INTERNAL_FILE_ID) != null;
        // return account != null;
    }
}