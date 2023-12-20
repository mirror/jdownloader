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
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class ClicknuploadOrg extends XFileSharingProBasic {
    public ClicknuploadOrg(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2019-07-25: premium untested, set FREE account limits <br />
     * captchatype-info: 2019-07-25: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "clicknupload.click", "clicknupload.link", "clicknupload.red", "clicknupload.to", "clicknupload.cc", "clicknupload.co", "clicknupload.org", "clicknupload.com", "clicknupload.me", "clicknupload.club", "clicknupload.online", "clicknupload.download", "clicknupload.vip", "clickndownload.org" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final List<String> deadDomains = new ArrayList<String>();
        deadDomains.add("clicknupload.red");
        deadDomains.add("clicknupload.link");
        deadDomains.add("clicknupload.com");
        deadDomains.add("clicknupload.club");
        return deadDomains;
    }

    @Override
    public String rewriteHost(final String host) {
        /* This host is frequently changing its main domain. */
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        final String[] supported_names_official = buildSupportedNames(getPluginDomains());
        final String[] supported_names_full = new String[supported_names_official.length + 1];
        int position = 0;
        for (final String supported_name : supported_names_official) {
            supported_names_full[position] = supported_name;
            position++;
        }
        /* 2019-08-27: For multihoster 'missing TLD handling' */
        supported_names_full[position] = "clicknupload";
        return supported_names_full;
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
            return -2;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -5;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    protected void processFileInfo(String[] fileInfo, Browser altbr, DownloadLink link) {
        try {
            // 2021-07: ?op=check_files is broken, use file size from 2nd free download page
            final Form download1 = findFormDownload1Free(br);
            if (download1 != null && (link.getKnownDownloadSize() == -1 && StringUtils.isEmpty(fileInfo[1]))) {
                final Browser brc = br.cloneBrowser();
                logger.info("Found download1 Form");
                final String correctedBR = this.correctedBR;
                try {
                    submitForm(brc, download1, true);
                    scanInfo(fileInfo);
                } finally {
                    this.correctedBR = correctedBR;
                }
            }
        } catch (Exception e) {
            logger.log(e);
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        /* 2022-04-20, concurrent downloads were possible */
        return 4;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        /* 2021-07-19 */
        return 10;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    protected boolean supports_availablecheck_filesize_html() {
        /* 2019-07-25: Special */
        return false;
    }

    @Override
    protected boolean enableAccountApiOnlyMode() {
        // return DebugMode.TRUE_IN_IDE_ELSE_FALSE;
        return false;
    }
}