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
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DropgalaxyIn extends XFileSharingProBasic {
    public DropgalaxyIn(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-04-06: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "dropgalaxy.in", "dropgalaxy.com" });
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

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br) {
            @Override
            protected String getSiteUrl() {
                // temp workaround by jiaz
                return br._getURL().getProtocol() + "://" + br._getURL().getHost();
            }
        };
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return -2;
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
        return 1;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            try {
                /* 2020-04-06: Workaround to find filenames */
                final Form download1free = this.findFormDownload1Free();
                if (download1free != null) {
                    this.submitForm(download1free);
                    // if (StringUtils.isEmpty(fileInfo[0])) {
                    // fileInfo[0] = new Regex(br.getURL(), "https?://[^/]+/(.+)\\.html").getMatch(0);
                    // }
                    // if (StringUtils.isEmpty(fileInfo[0])) {
                    // fileInfo[0] = new Regex(correctedBR, "name=\"F1\" action=\"https?://[^/]+/([^\"]+)\\.html").getMatch(0);
                    // }
                    if (StringUtils.isEmpty(fileInfo[0])) {
                        fileInfo[0] = new Regex(correctedBR, "<h1 style=\"word-break: break-all;\">([^<>\"]+)</h1>").getMatch(0);
                    }
                    if (StringUtils.isEmpty(fileInfo[1])) {
                        fileInfo[1] = new Regex(correctedBR, "<span>\\s*size\\s*<b>([^<>\"]+)</b>").getMatch(0);
                    }
                }
            } catch (final Throwable e) {
                logger.log(e);
            }
        }
        return fileInfo;
    }

    public String regexFilenameAbuse(final Browser br) {
        String filename = super.regexFilenameAbuse(br);
        if (filename == null) {
            filename = br.getRegex("<label>Filename\\s*:\\s*<b>([^<>\"]+)</b>").getMatch(0);
        }
        return filename;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        if (this.findFormDownload2Free() != null) {
            /*
             * 2020-04-08: Special: Their html will often contain direct-URLs to e.g. view pdf files in a gdrive viewer but these URLs will
             * not work and lead to server error 500 thus if the captcha-Form (download2 Form) exists we know we cannot yet look for any
             * downloadlinks!
             */
            return null;
        }
        return super.getDllink(link, account, br, src);
    }
}