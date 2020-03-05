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
import java.util.SortedMap;
import java.util.TreeMap;

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
public class ExLoadCom extends XFileSharingProBasic {
    public ExLoadCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-05: Plaintext Captcha<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "ex-load.com" });
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
        return 10;
    }

    @Override
    protected String regexWaittime() {
        /* 2020-03-05: Special */
        String waitStr = super.regexWaittime();
        if (waitStr == null) {
            waitStr = new Regex(correctedBR, "<span id=\"[a-z0-9]+\">(\\d+)</span>\\s*</div>").getMatch(0);
        }
        return waitStr;
    }

    @Override
    public void handleCaptcha(final DownloadLink link, final Form captchaForm) throws Exception {
        /* 2020-03-05: Special */
        if (captchaForm.containsHTML(";background:#ccc;text-align")) {
            logger.info("Detected captcha method \"Plaintext Captcha\"");
            /** Captcha method by ManiacMansion */
            String[][] letters = captchaForm.getRegex("<span style=\"position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;\">(&#\\d+;)</span>").getMatches();
            if (letters == null || letters.length == 0) {
                letters = new Regex(br.toString(), "<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(&#\\d+;)</span>").getMatches();
                if (letters == null || letters.length == 0) {
                    logger.warning("plaintext captchahandling broken!");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
            for (String[] letter : letters) {
                capMap.put(Integer.parseInt(letter[0]), Encoding.htmlDecode(letter[1]));
            }
            final StringBuilder code = new StringBuilder();
            for (String value : capMap.values()) {
                code.append(value);
            }
            captchaForm.put("code", code.toString());
        } else {
            /* Use template handling */
            super.handleCaptcha(link, captchaForm);
        }
    }
}