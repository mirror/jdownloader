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
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.XFileSharingProBasic;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KenfilesCom extends XFileSharingProBasic {
    public KenfilesCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:2019-03-01: premium untested, set FREE account limits <br />
     * captchatype-info: 2019-03-01: reCaptchaV2<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "kenfiles.com", "kfs.space" });
        return ret;
    }

    @Override
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("kfs.space");
        return deadDomains;
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
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return true;
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
            return -2;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
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
        return -1;
    }

    /** Function to find the final downloadlink. */
    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, final String src) {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(src, "(\"|\\')(https?://(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|([\\w\\-]+\\.)?kfs\\.space)(:\\d{1,4})?/(files|d|p|f|cgi\\-bin/dl\\.cgi)/(\\d+/)?[a-z0-9]+/[^<>\"/]*?)(\"|\\')").getMatch(1);
            if (dllink == null) {
                final String cryptedScripts[] = new Regex(src, "p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
                if (cryptedScripts != null && cryptedScripts.length != 0) {
                    for (String crypted : cryptedScripts) {
                        dllink = decodeDownloadLink(link, account, br, crypted);
                        if (dllink != null) {
                            break;
                        }
                    }
                }
            }
        }
        return dllink;
    }

    @Override
    public String decodeDownloadLink(final DownloadLink link, final Account account, final Browser br, final String s) {
        String decoded = null;
        try {
            Regex params = new Regex(s, "'(.*?[^\\\\])',(\\d+),(\\d+),'(.*?)'");
            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");
            while (c != 0) {
                c--;
                if (k[c].length() != 0) {
                    p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
                }
            }
            decoded = p;
        } catch (Exception e) {
            logger.log(e);
        }
        String finallink = null;
        if (decoded != null) {
            /* Open regex is possible because in the unpacked JS there are usually only 1-2 URLs. */
            finallink = new Regex(decoded, "(?:\"|')(https?://[^<>\"']*?\\.(avi|flv|mkv|mp4))(?:\"|')").getMatch(0);
            if (finallink == null) {
                /* Maybe rtmp */
                finallink = new Regex(decoded, "(?:\"|')(rtmp://[^<>\"']*?mp4:[^<>\"']+)(?:\"|')").getMatch(0);
            }
        }
        return finallink;
    }

    @Override
    protected Form findFormDownload2Free(final Browser br) {
        /* 2020-09-02: Special */
        Form dlForm = super.findFormDownload2Free(br);
        // if (dlForm != null) {
        // dlForm.remove("method_premium");
        // }
        /* Correct Form */
        final String keyMethodPremium = "method_premium";
        if (dlForm.hasInputFieldByName(keyMethodPremium)) {
            for (int i = 0; i <= 3; i++) {
                dlForm.remove(keyMethodPremium);
            }
            dlForm.put(keyMethodPremium, "");
        }
        return dlForm;
    }

    @Override
    protected String regexWaittime(Browser br) {
        String waitStr = super.regexWaittime(br);
        if (waitStr == null) {
            /* 2020-09-02: Special */
            waitStr = new Regex(br.getRequest().getHtmlCode(), ">(\\d+)</span> seconds<").getMatch(0);
        }
        return waitStr;
    }

    @Override
    protected Long fetchAccountInfoWebsiteExpireDate(Browser br, Account account, AccountInfo ai) throws Exception {
        getPage("/?op=my_account");
        final String[] datesStr = br.getRegex("(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})").getColumn(0);
        if (datesStr != null && datesStr.length > 0) {
            for (final String expiredateStr : datesStr) {
                final long timestamp = TimeFormatter.getMilliSeconds(expiredateStr, "yyyy-MM-dd hh:mm:ss", Locale.ENGLISH);
                if (timestamp > System.currentTimeMillis()) {
                    return timestamp;
                }
            }
        }
        return super.fetchAccountInfoWebsiteExpireDate(br, account, ai);
    }

    @Override
    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection) {
        if (urlConnection == null) {
            return false;
        }
        final List<String> contentTypeValues = urlConnection.getRequest().getResponseHeaders("Content-Type");
        if (contentTypeValues != null) {
            /*
             * 2024-02-15: Special check to detect case when they send two content-type headers (e.g. "video/mp4" and at the same time
             * "text/html; charset=utf-8" lol)
             */
            for (final String contentTypeValue : contentTypeValues) {
                if (contentTypeValue.contains("html")) {
                    return false;
                }
            }
        }
        return super.looksLikeDownloadableContent(urlConnection);
    }
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // if (this.enableAccountApiOnlyMode()) {
    // /* API-only mode */
    // handleDownload(link, account, null, getDllinkAPI(link, account), null);
    // } else {
    // super.handlePremium(link, account);
    // }
    // }
    //
    // @Override
    // protected boolean supportsAPIMassLinkcheck() {
    // if (isAPIKey(this.getAPIKey())) {
    // return true;
    // } else {
    // return false;
    // }
    // }
    //
    // @Override
    // /** API docs: https://kenfiles.com/pages/api */
    // protected boolean enableAccountApiOnlyMode() {
    // return true;
    // }
}