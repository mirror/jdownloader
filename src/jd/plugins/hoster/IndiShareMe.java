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

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class IndiShareMe extends XFileSharingProBasic {
    public IndiShareMe(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-06-05: Unlimited<br />
     * captchatype-info: 2019-06-05: null<br />
     * other:<br />
     */
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

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "indishare.org", "indishare.cc", "indi-share.com", "indishare.co", "indishare.com", "indishare.me", "india-share.com", "news4town.com" });
        return ret;
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
    public boolean requiresWWW() {
        /* 2019-06-05: Special */
        return true;
    }

    private boolean workaround1Done = false;
    private boolean workaround2Done = false;

    // @Override
    // protected void getPage(final Browser ibr, final String page) throws Exception {
    // super.getPage(ibr, page);
    // }
    @Override
    protected void runPostRequestTask(final Browser br) throws Exception {
        super.runPostRequestTask(br);
        /* Usually to "https://dl.indishare.cc/..." */
        final String newURL = br.getRegex(">\\s*Site Moved to New Address\\s*<a href=\"(https?://[^/]+/[a-z0-9]{12})\"").getMatch(0);
        if (!workaround1Done && newURL != null) {
            /* Cat mouse games */
            final boolean oldFollowRedirects = br.isFollowingRedirects();
            br.setFollowRedirects(true);
            br.getPage(newURL);
            if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2023-01-26: Typically "news4town.com" */
                final String fakeBlogBaseURL = br.getRegex("var sora_base_url = \"(https?://[^\"]+)\";").getMatch(0);
                if (fakeBlogBaseURL == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String[] adURLs = br.getRegex("href=\"(https?://[^\"]+)\" rel=\"nofollow noopener noreferrer\"").getColumn(0);
                if (adURLs == null || adURLs.length == 0) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String adURL = adURLs[adURLs.length - 1];
                final String adURLb64 = Encoding.Base64Encode(adURL);
                br.getPage(fakeBlogBaseURL + "?r=" + Encoding.urlEncode(adURLb64));
                final Form landingform = br.getFormbyProperty("id", "landing");
                if (landingform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final InputField post_locationField = landingform.getInputFieldByName("post_location");
                if (post_locationField == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String post_location = post_locationField.getValue();
                if (post_location == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                landingform.setAction(Encoding.htmlDecode(post_location));
                br.submitForm(landingform);
            }
            workaround1Done = true;
            br.setFollowRedirects(oldFollowRedirects);
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        if (containsFakeError(br)) {
            /* Workaround */
            return;
        } else {
            super.checkErrors(br, html, link, account, checkAll);
        }
    }

    @Override
    protected boolean isOffline(final DownloadLink link, final Browser br, final String correctedBR) {
        if (containsFakeError(br)) {
            return false;
        } else if (super.isOffline(link, br, correctedBR)) {
            return true;
        } else {
            return false;
        }
    }

    @Deprecated
    private boolean containsFakeError(final Browser br) {
        final String host = br.getHost();
        if (br.getHttpConnection().getResponseCode() == 404 && (host.equals("indi-share.com") || host.equals("india-share.com"))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Form findFormDownload1Free(final Browser br) throws Exception {
        String specialAction = br.getRegex("<a href=\"(https?://techmyntra\\.net/[a-z0-9]{12})\"").getMatch(0);
        if (specialAction == null) {
            /* 2022-09-21 */
            specialAction = br.getRegex("<a href=\"https?://href\\.li/\\?(https?://techmyntra\\.net/[^\"]+)\"").getMatch(0);
        }
        if (!workaround2Done && specialAction != null) {
            /* 2021-11-19: Redirect to fake blog containing download2 Form. */
            final Form form = new Form();
            form.setAction(specialAction);
            form.setMethod(MethodType.GET);
            workaround2Done = true;
            return form;
        } else {
            return super.findFormDownload1Free(br);
        }
    }
}