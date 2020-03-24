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

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.YetiShareCore;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class VidloadNet extends YetiShareCore {
    public VidloadNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(getPurchasePremiumURL());
    }

    /**
     * DEV NOTES YetiShare<br />
     ****************************
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: 2020-03-24: null<br />
     * other: <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vidload.net" });
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
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    public int getMaxSimultaneousFreeAccountDownloads() {
        return 3;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 3;
    }

    @Override
    protected Form getContinueForm(final int loop_counter, final String continue_link) throws PluginException {
        /* 2020-03-24: Special and their website (at least official download) appears to be broken. */
        Form continueform = br.getForm(0);
        if (loop_counter == 0) {
            continueform = br.getForm(0);
        }
        if (loop_counter == 2) {
            continueform = new Form();
            continueform.setMethod(MethodType.POST);
            continueform.setAction(br.getURL());
            final String securenet = br.getRegex("name=\"securenet\" value=\"([a-f0-9]+)\"").getMatch(0);
            if (securenet == null) {
                logger.info("Failed to find securenet");
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            continueform.put("securenet", securenet);
        } else if (loop_counter == 3) {
            continueform = br.getForm(0);
            if (continueform == null) {
                /* 2020-03-25: Broken website */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        return continueform;
    }

    private boolean hasTriedEmbedHandling = false;

    @Override
    protected String getContinueLink() throws Exception {
        if (!hasTriedEmbedHandling) {
            final Browser brc = br.cloneBrowser();
            this.getPage(brc, this.getMainPage() + "/e/" + this.getFUIDFromURL(this.getDownloadLink()));
            final String token = brc.getRegex("var token=\"([^\"]+)\"").getMatch(0);
            final String crsf = brc.getRegex("var crsf=\"([^\"]+)\"").getMatch(0);
            if (token == null || crsf == null) {
                logger.warning("Failed to find token or crsf");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final UrlQuery query = new UrlQuery();
            query.append("gone", token, true);
            query.append("oujda", crsf, true);
            // query.append("", "", true);
            postPage(brc, "https://www.vidload.net/vid/", query.toString());
            final String dllink = brc.toString();
            if (!dllink.startsWith("http")) {
                logger.warning("Final downloadurl is not a URL ...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            hasTriedEmbedHandling = true;
            /* Workaround */
            if (this.getDownloadLink().getName().contains(".mp4")) {
                this.getDownloadLink().setFinalFileName(this.getDownloadLink().getName());
            }
            return dllink;
        } else {
            return super.getContinueLink();
        }
    }

    @Override
    public boolean isDownloadlink(final String url) {
        /* 2020-03-24: Special */
        if (hasTriedEmbedHandling && !StringUtils.isEmpty(url)) {
            return true;
        } else {
            return super.isDownloadlink(url);
        }
    }
}