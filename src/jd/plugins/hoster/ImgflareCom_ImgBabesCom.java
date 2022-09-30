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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ImgflareCom_ImgBabesCom extends XFileSharingProBasic {
    public ImgflareCom_ImgBabesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-06-17: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imgflare.com" }); // down 2022-09-30 waiting another 30+ days until deletion
        /* down 2022-09-30 */
        // ret.add(new String[] { "imgbabes.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    private static final String TYPE_SPECIAL = "https?//[^/]+/f/([a-z0-9]{12,})(?:/([^/]+)(?:\\.html)?)?";

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:d/[A-Za-z0-9]+|(?:embed-)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?|f/[a-z0-9]{12,}(/[^/]+(?:\\.html)?)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
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
    protected boolean isImagehoster() {
        /* 2020-06-17: Special */
        return true;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        final String dllink = new Regex(correctedBR, "\"(https?://[^/]+/i\\.php[^\"]+)\"").getMatch(0);
        if (!StringUtils.isEmpty(dllink)) {
            return dllink;
        } else {
            return super.getDllink(link, account, br, src);
        }
    }

    @Override
    public Form findImageForm(final Browser br) {
        Form imghost_next_form = super.findImageForm(br);
        if (imghost_next_form == null && br.containsHTML("Continue to your image")) {
            imghost_next_form = br.getFormbyKey("token");
            if (imghost_next_form == null) {
                imghost_next_form = br.getForm(0);
            }
            if (imghost_next_form != null) {
                try {
                    this.sleep(3500, this.getDownloadLink());
                } catch (final Throwable e) {
                    return imghost_next_form;
                }
            }
        }
        return imghost_next_form;
    }

    @Override
    public String getFUIDFromURL(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_SPECIAL)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_SPECIAL).getMatch(0);
        } else {
            return super.getFUIDFromURL(link);
        }
    }

    @Override
    public String getFilenameFromURL(final DownloadLink link) {
        if (link != null && link.getPluginPatternMatcher() != null && link.getPluginPatternMatcher().matches(TYPE_SPECIAL)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_SPECIAL).getMatch(1);
        } else {
            return super.getFilenameFromURL(link);
        }
    }

    protected String buildURLPath(final DownloadLink link, final String fuid, final URL_TYPE type) {
        if (type == URL_TYPE.NORMAL) {
            return "/f/" + fuid;
        } else {
            return super.buildURLPath(link, fuid, type);
        }
    }

    @Override
    public String[] scanInfo(final String html, final String[] fileInfo) {
        super.scanInfo(html, fileInfo);
        final String filenameURL = this.getFilenameFromURL(getDownloadLink());
        final String filenameHTML = br.getRegex("<title>Viewing ([^<>\"]+) - (?:IMGFlare|IMGBabes)</title>").getMatch(0);
        if (filenameURL != null) {
            fileInfo[0] = filenameURL;
        } else if (filenameHTML != null) {
            fileInfo[0] = Encoding.htmlDecode(filenameHTML);
        }
        return fileInfo;
    }

    @Override
    protected boolean supportsHEADRequestForDirecturlCheck() {
        /* 2021-11-23: Special */
        return false;
    }
}