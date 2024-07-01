//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class ImgpornTo extends YetiShareCore {
    public ImgpornTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info: 2021-06-07: No limits at all <br />
     * captchatype-info: 2021-06-07 reCaptchaV2<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "imgporn.to", "thotdrive.com" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("thotdrive.com"); // 2024-07-01
        return deadDomains;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return YetiShareCore.buildAnnotationUrls(getPluginDomains());
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
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean supports_availablecheck_over_info_page(final DownloadLink link) {
        return false;
    }

    @Override
    protected void requestFileDetailsNewYetiShare(final Browser br, final String fileID) throws Exception {
        /* 2023-07-06: "p=true" has been added for wrzucaj.pl but that shouldn't affect other websites. */
        postPage(br, "/ajax/_account_file_details.ajax.php", "u=" + fileID);
        final Map<String, Object> root = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String html = root.get("html").toString();
        /* Small workaround to have this html code available in our current browser instance. */
        br.getRequest().setHtmlCode(html);
    }

    @Override
    protected String getContinueLink(final Browser br) throws Exception {
        final Regex filedl = br.getRegex("triggerFileDownload\\((\\d+),\\s*'([a-f0-9]{64})");
        if (filedl.patternFind()) {
            final String fileid = filedl.getMatch(0);
            final String fileHash = filedl.getMatch(1);
            /* The hash we have here is not the real file hash! */
            // this.getDownloadLink().setSha256Hash(fileHash);
            return "/page/direct_download.php?fileId=" + fileid + "&fileHash=" + fileHash;
        }
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\'[^>]*href=\\'([^<>\"\\']*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">\\s*<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(https?://[^/]+/[^<>\"\\':]*pt=[^<>\"\\']*)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink(br);
        }
        if (continue_link != null) {
            return Encoding.htmlOnlyDecode(continue_link);
        } else {
            return super.getContinueLink(br);
        }
    }
}