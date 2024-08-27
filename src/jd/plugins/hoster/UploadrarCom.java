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
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UploadrarCom extends XFileSharingProBasic {
    public UploadrarCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2019-02-11: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uploadrar.com", "uploadrar.net" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("uploadrar.net"); // 2024-07-03
        return deadDomains;
    }

    @Override
    public String[] scanInfo(String[] fileInfo) {
        super.scanInfo(fileInfo);
        // if (StringUtils.isEmpty(fileInfo[0])) {
        // fileInfo[0] = br.getRegex("div\\s*class\\s*=\\s*\"desc\"\\s*>\\s*<span>\\s*(.*?)\\s*</span>").getMatch(0);
        // }
        if (StringUtils.isEmpty(fileInfo[1])) {
            fileInfo[1] = br.getRegex("<p>\\s*size\\s*:\\s*([0-9\\.]+(?:\\s+|\\&nbsp;)?(KB|MB|GB))").getMatch(0);
        }
        return fileInfo;
    }

    @Override
    public void doFree(DownloadLink link, Account account) throws Exception, PluginException {
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog();
        }
        super.doFree(link, account);
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
    public boolean preDownloadWaittimeSkippable() {
        /* 2019-05-09: Special */
        return true;
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
    protected boolean supports_availablecheck_filesize_html() {
        return false;
    }

    public static String[] getAnnotationUrls() {
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    @Override
    protected boolean supports_availablecheck_alt() {
        // 2024-07-04
        return false;
    }

    @Override
    protected boolean supports_availablecheck_filename_abuse() {
        // 2024-07-10
        return true;
    }

    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = super.getDllink(link, account, br, src);
        if (dllink != null && this.findFormDownload2Free(br) == null) {
            /* 2024-07-10: Special as they put a fake direct-downloadlink in their html code */
            return dllink;
        } else {
            return null;
        }
    }

    @Override
    protected String getFnameViaAbuseLink(final Browser br, final DownloadLink link) throws Exception {
        /*
         * 2024-07-10: Special because an ad-redirect might have happened before (redirect to e.g. "get.rahim-soft.com/xen-vs-nvidia.html")
         * so we need to force-do this request with the current plugin base domain to ensure it works.
         */
        getPage(br, "https://" + getHost() + "/?op=report_file&id=" + this.getFUIDFromURL(link), false);
        /*
         * 2019-07-10: ONLY "No such file" as response might always be wrong and should be treated as a failure! Example: xvideosharing.com
         */
        if (br.containsHTML(">\\s*No such file\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = regexFilenameAbuse(br);
        if (filename != null) {
            logger.info("Successfully found filename via report_file");
            return filename;
        } else {
            logger.info("Failed to find filename via report_file");
            final boolean fnameViaAbuseUnsupported = br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 500 || !br.getURL().contains("report_file") || br.getRequest().getHtmlCode().trim().equalsIgnoreCase("No such file");
            if (fnameViaAbuseUnsupported) {
                logger.info("Seems like report_file availablecheck seems not to be supported by this host");
                final SubConfiguration config = this.getPluginConfig();
                // config.setProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_TIMESTAMP, System.currentTimeMillis());
                // config.setProperty(PROPERTY_PLUGIN_REPORT_FILE_AVAILABLECHECK_LAST_FAILURE_VERSION, getPluginVersionHash());
            }
            return null;
        }
    }

    @Override
    public String regexFilenameAbuse(final Browser br) {
        final String filename_src = br.getRegex("name=\"file_name\"[^>]*value=\"([^\"]+)\"").getMatch(0);
        if (filename_src != null) {
            return filename_src;
        } else {
            return super.regexFilenameAbuse(br);
        }
    }
}