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

import org.jdownloader.plugins.components.XFileSharingProBasic;
import org.jdownloader.plugins.components.config.XFSConfigVideo;
import org.jdownloader.plugins.components.config.XFSConfigVideoVidloxMe;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class VidloxTv extends XFileSharingProBasic {
    public VidloxTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: Premium untested, set FREE limits<br />
     * captchatype-info: null<br />
     * other: Sister sites: vidlox.tv, vidlox.me, videobin.co <br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "vidlox.me", "vidlox.tv" });
        return ret;
    }

    @Override
    public String rewriteHost(String host) {
        /* 2020-03-27: vidlox.tv --> vidlox.me */
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
        /* 2019-08-13: Special */
        return false;
    }

    @Override
    protected boolean isVideohoster_enforce_video_filename() {
        /* 2019-08-13: Special */
        return true;
    }

    @Override
    protected boolean isVideohosterEmbed() {
        /* 2020-04-08: Special */
        return true;
    }

    @Override
    protected boolean supports_availablecheck_filesize_via_embedded_video() {
        /* 2019-08-17: Special and experimental. Disable this if it slows down the linkcheck too much! */
        return true;
    }

    @Override
    protected void getPage(final Browser ibr, final String page) throws Exception {
        /* 2020-04-08: Special */
        if (ibr == null || page == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        sendRequest(ibr, ibr.createGetRequest(page));
        /* 2020-04-08: Cheap protection on top of their Cloudflare: Server: WorldShield PROXY software by VPZ */
        final String antiddosCookie = br.getRegex("document\\.cookie\\s*=\\s*\"VPZ=([a-f0-9]+)").getMatch(0);
        if (antiddosCookie != null) {
            logger.info("Spotted antiddos cookie");
            br.setCookie(br.getHost(), "VPZ", antiddosCookie);
            super.getPage(ibr, page);
        }
    }

    public String[] scanInfo(final String[] fileInfo) {
        fileInfo[0] = new Regex(correctedBR, "<title>\\s*Watch([^<>\"]+)</title>").getMatch(0);
        return super.scanInfo(fileInfo);
    }

    @Override
    public Class<? extends XFSConfigVideo> getConfigInterface() {
        return XFSConfigVideoVidloxMe.class;
    }
}