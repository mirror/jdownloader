//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FilespaceCom extends XFileSharingProBasic {
    public FilespaceCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-04-29: solvemedia<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filespace.com", "spaceforfiles.com" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
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

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void checkErrors(final Browser br, final String correctedBR, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        /* 2019-05-21: Special */
        if (link != null && link.getMD5Hash() == null) {
            /* 2023-04-19: Special: Small hack: Look for md5 hash on every page. */
            final String md5hash = new Regex(correctedBR, "MD5 Checksum:\\s*([a-f0-9]{32})").getMatch(0);
            if (md5hash != null) {
                link.setMD5Hash(md5hash);
            }
        }
        super.checkErrors(br, correctedBR, link, account, checkAll);
        if (new Regex(correctedBR, "(?i)>\\s*You, or someone with the same IP address, are downloading the").matches()) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You're using all download slots for current IP", 10 * 60 * 1001l);
        } else if (br.containsHTML(">\\s*Bandwidth overload detected")) {
            /* 2023-04-19: Temp account ban because of accountsharing or user used VPN / server IP. */
            if (account != null) {
                throw new AccountUnavailableException("Bandwidth overload detected, are you sharing this account with someone else?", 5 * 60 * 1000);
            } else {
                /* This should never happen. */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Bandwidth overload detected");
            }
        }
        if (checkAll) {
            if (new Regex(correctedBR, "(?i)>[^<]*Wrong captcha[^<]*<").matches()) {
                logger.warning("Wrong captcha (or wrong password as well)!");
                /*
                 * TODO: Find a way to avoid using a property for this or add the property in very plugin which overrides handleCaptcha e.g.
                 * subyshare.com. If a dev forgets to set this, it will cause invalid errormessages on wrong captcha!
                 */
                final boolean websiteDidAskForCaptcha = link.getBooleanProperty(PROPERTY_captcha_required, false);
                if (websiteDidAskForCaptcha) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server says 'wrong captcha' but never prompted for one");
                }
            }
        }
    }

    @Override
    protected String[] supportsPreciseExpireDate() {
        return new String[] { "/?op=payments" };
    }

    @Override
    protected String findExpireDate(final Browser br) throws Exception {
        final String expireSecond = new Regex(getCorrectBR(br), Pattern.compile(">\\s*(\\d+ years?, )?(\\d+ days?, )?(\\d+ hours?, )?(\\d+ minutes?, )?\\d+ seconds\\s*<", Pattern.CASE_INSENSITIVE)).getMatch(-1);
        if (StringUtils.isEmpty(expireSecond)) {
            return super.findExpireDate(br);
        } else {
            return expireSecond;
        }
    }

    @Override
    public String regexWaittime() {
        /* 2019-04-29: Special */
        String wait = super.regexWaittime();
        if (wait == null) {
            wait = new Regex(correctedBR, "Please wait <span id=\"[a-z0-9]+\">(\\d+)</span>").getMatch(0);
        }
        return wait;
    }
}