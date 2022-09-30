//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.jdownloader.plugins.components.UnknownHostingScriptCore;
import org.jdownloader.plugins.components.config.AnonFilesComConfigBayfiles;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision $", interfaceVersion = 2, names = {}, urls = {})
public class BayFilesCom extends UnknownHostingScriptCore {
    public BayFilesCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
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

    /** 2021-04-19: Max. number of total connections: 16 */
    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return -4;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return -4;
        } else {
            /* Free(anonymous) and unknown account type */
            return -4;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 4;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 4;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bayfiles.com", "megaupload.is" });
        ret.add(new String[] { "letsupload.cc" });
        ret.add(new String[] { "openload.cc" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return UnknownHostingScriptCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public Class<? extends AnonFilesComConfigBayfiles> getConfigInterface() {
        return AnonFilesComConfigBayfiles.class;
    }

    @Override
    protected boolean allowLowerQualityStreamingFallback() {
        return PluginJsonConfig.get(this.getConfigInterface()).isAllowFallbackToLowerQuality();
    }

    @Override
    protected String getPreferredCDNNode() {
        return PluginJsonConfig.get(this.getConfigInterface()).getPreferredCdnNode();
    }
}