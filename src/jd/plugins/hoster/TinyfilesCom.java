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

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TinyfilesCom extends XFileSharingProBasic {
    public TinyfilesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* 2019-06-27: Special: Do not correct URLs at all! */
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null 4dignum solvemedia reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "tiny-files.com" });
        return ret;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
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
    public String getFUIDFromURL(final DownloadLink dl) {
        String fuid = super.getFUIDFromURL(dl);
        if (fuid == null) {
            try {
                fuid = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/([a-f0-9]+)").getMatch(0);
                return fuid;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String getFilenameFromURL(final DownloadLink dl) {
        String urlname = super.getFilenameFromURL(dl);
        if (urlname == null) {
            try {
                urlname = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/[a-f0-9]+/\\d+/(.+)/?$").getMatch(0);
                return urlname;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    protected void checkErrors(final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(link, account, checkAll);
        if (new Regex(correctedBR, ">\\s*?File not exists").matches()) {
            /*
             * 2019-08-15: Rare error which may sometimes happen after sending F1 Form. The file is not really offline in this case - this
             * is more likely a serverside bug!
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'File not exists'", 5 * 60 * 1000l);
        }
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
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/([a-f0-9]{24}/\\d+/[^/]+/?|(?:embed\\-)?[a-z0-9]{12}(?:/[^/]+\\.html)?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public void handleCaptcha(final DownloadLink link, final Form captchaForm) throws Exception {
        /* 2020-02-27: Special reCaptchaV2 invisible */
        final String reCaptchaKey = br.getRegex("grecaptcha\\.execute\\('([^<>\"\\']+)'").getMatch(0);
        if (captchaForm != null && captchaForm.containsHTML("googletoken") && reCaptchaKey != null) {
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br, reCaptchaKey).getToken();
            captchaForm.put("googletoken", Encoding.urlEncode(recaptchaV2Response));
            // br.getHeaders().put("accept",
            // "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            // br.getHeaders().put("content-type", "application/x-www-form-urlencoded");
            // br.getHeaders().put("origin", "https://tiny-files.com");
            // br.getHeaders().put("sec-fetch-dest", "document");
            // br.getHeaders().put("sec-fetch-mode", "navigate");
            // br.getHeaders().put("sec-fetch-site", "same-origin");
            // br.getHeaders().put("sec-fetch-user", "?1");
        } else {
            /* Fallback to template handling and hope that it will work */
            super.handleCaptcha(link, captchaForm);
        }
    }
}