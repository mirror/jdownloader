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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RockFileCo extends XFileSharingProBasic {
    public RockFileCo(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rockfile.co", "rockfile.eu", "rockfileserver.eu", "rfservers.eu" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(List<String[]> pluginDomains) {
        /* 2020-03-02: Special */
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(?:/(?:embed-)?[a-z0-9]{12}(?:/[^/]+(?:\\.html)?)?|/f/[a-z0-9]+-[a-z0-9]+\\.html)");
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

    @Override
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        super.doFree(link, account);
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
        return 3;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* 2020-03-02: Do not modify URLs at all anymore! */
        final String fuid = this.fuid != null ? this.fuid : getFUIDFromURL(link);
        if (fuid != null && link.getPluginPatternMatcher().matches("https?://[^/]+/[a-z0-9]{12}")) {
            /* 2019-07-02: Special: Some URLs require '.html' at the end!! */
            final String url_with_html_ending = getMainPage() + "/" + fuid + ".html";
            link.setPluginPatternMatcher(url_with_html_ending);
            link.setContentUrl(url_with_html_ending);
            link.setLinkID(getHost() + "://" + fuid);
        }
    }

    @Override
    public String getFUIDFromURL(final DownloadLink dl) {
        /* 2020-03-02: Special */
        try {
            String result = new Regex(new URL(dl.getPluginPatternMatcher()).getPath(), "/[a-z0-9]+-([a-z0-9]+)\\.html").getMatch(0);
            if (result == null) {
                /* Fallback to template handling */
                result = super.getFUIDFromURL(dl);
            }
            return result;
        } catch (MalformedURLException e) {
            logger.log(e);
        }
        return null;
    }

    @Override
    public String[] scanInfo(final String[] fileInfo) {
        /* 2019-07-02: Special */
        super.scanInfo(fileInfo);
        if (StringUtils.isEmpty(fileInfo[0])) {
            /*
             * 2019-11-10: There is no way to find the real filename before starting the download - this is a workaround which also assumes
             * that the file has an extension which can be found at the end of that String. After all this is not the final name but quite
             * similar to it!
             */
            final String tmpName = new Regex(correctedBR, "<meta name=\"description\" content=\"Download File ([^<>\"]+)\"").getMatch(0);
            if (tmpName != null && tmpName.contains(" ")) {
                final int indext_last_space = tmpName.lastIndexOf(" ");
                final String extension = tmpName.substring(indext_last_space + 1);
                final String workarounded_filename = tmpName.substring(0, indext_last_space).replace(" ", "_") + "." + extension;
                fileInfo[0] = workarounded_filename;
            }
        }
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = new Regex(correctedBR, "var iniFileSize\\s*=\\s*(\\d+)\\s*").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-07-02: Special */
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        /*
         * 2019-11-10: No possible anymore. This will display a captcha and afterwards instead of displaying a filename, it will only
         * display "Please use Report DMCA from the footer.".
         */
        return false;
    }
}