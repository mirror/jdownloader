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
import java.util.Map;

import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GettSu extends XFileSharingProBasic {
    public GettSu(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2023-10-17: null <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "gett.su" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?::\\d+)?(" + XFileSharingProBasic.getDefaultAnnotationPatternPart() + "|/file/[a-z0-9]{12}/.+)");
        }
        return ret.toArray(new String[0]);
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
            return 0;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
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
    public String getFUIDFromURL(final DownloadLink link) {
        final Regex patternSpecial = new Regex(link.getPluginPatternMatcher(), "https://[^/]+/file/([a-z0-9]{12})/.+");
        if (patternSpecial.patternFind()) {
            return patternSpecial.getMatch(0);
        } else {
            final URL_TYPE type = getURLType(link);
            return getFUID(link, type);
        }
    }

    @Override
    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        if (type == URL_TYPE.FILE) {
            return "/file/" + fuid + "/view.html";
        } else {
            return super.buildURLPath(link, fuid, type);
        }
    }
    // @Override
    // public Form findFormDownload1Free(final Browser br) throws Exception {
    // /* 2022-04-07: Special */
    // final Form download1 = super.findFormDownload1Free(br);
    // if (download1 != null && br.containsHTML("type: 'POST',\\s*url: 'https?://[^/]+/download'")) {
    // /* 2023-08-14 */
    // download1.put("ajax", "1");
    // download1.put("method_free", "1");
    // download1.put("dataType", "json");
    // }
    // return download1;
    // }

    @Override
    public void handleCaptcha(final DownloadLink link, Browser br, final Form captchaForm) throws Exception {
        if (br.containsHTML("downloadbtn g-recaptcha") && br.containsHTML("data-sitekey")) {
            /* 2023-10-20: Special */
            handleRecaptchaV2(link, br, captchaForm);
        } else {
            super.handleCaptcha(link, br, captchaForm);
        }
    }

    @Override
    protected String getDllink(DownloadLink link, Account account, Browser br, String src) {
        if (br.getRequest().getHtmlCode().startsWith("{")) {
            /* 2023-10-20 */
            try {
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> resultmap = (Map<String, Object>) entries.get("result");
                return resultmap.get("url").toString();
            } catch (final Throwable e) {
                logger.log(e);
                logger.warning("Ajax handling failed");
            }
        }
        return super.getDllink(link, account, br, src);
    }
}