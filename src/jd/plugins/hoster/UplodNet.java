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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UplodNet extends XFileSharingProBasic {
    public UplodNet(final PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-05: Limits untested because download never worked <br />
     * captchatype-info: custom captcha<br />
     * other: 2020-11-05: Heavily modified XFS <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uplod.net" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        try {
            final String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "([a-z0-9]+)").getMatch(0);
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return true;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account, final boolean downloadsStarted) throws Exception {
        this.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("class=\"dfilename\">([^<>\"]+)<").getMatch(0);
        final String filesize = br.getRegex("class=\"statd\">size</span>\\s*<span>([^<>\"]+)</span>").getMatch(0);
        if (filename != null) {
            link.setName(filename);
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception, PluginException {
        /* First bring up saved final links */
        final String directlinkproperty = getDownloadModeDirectlinkProperty(account);
        String dllink = checkDirectLink(link, directlinkproperty);
        if (StringUtils.isEmpty(dllink)) {
            this.checkErrors(link, account, false);
            final Form dl1 = br.getFormByInputFieldKeyValue("op", "step1");
            if (dl1 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.submitForm(dl1);
            final Form dl2 = br.getFormByInputFieldKeyValue("op", "step2");
            if (dl2 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String image_b64 = br.getRegex("src=\"data:image/jpeg;base64,(.*?)\"").getMatch(0);
            if (image_b64 == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long timeBefore = System.currentTimeMillis();
            // final String code = this.getCaptchaCode(file, link);
            final String code = this.getCaptchaCodeBase64ImageString(image_b64, link);
            dl2.put("captcha", Encoding.urlEncode(code));
            this.waitTime(link, timeBefore);
            link.setProperty(PROPERTY_captcha_required, true);
            this.submitForm(dl2);
            this.checkErrors(link, account, true);
            dllink = this.getDllink(link, account);
        }
        handleDownload(link, account, dllink, null);
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(link, account, checkAll);
        if (checkAll) {
            if (correctedBR.contains("Invalid captcha")) {
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
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = super.getDllink(link, account, br, src);
        if (StringUtils.isEmpty(dllink)) {
            dllink = new Regex(src, "href=\"(https?://[^<>\"]+)\">\\s*Click here to download").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                dllink = new Regex(src, "\"(https?://fs\\d+.uplod.net/[a-z0-9]+/[^\"]+)\"").getMatch(0);
            }
        }
        return dllink;
    }
}