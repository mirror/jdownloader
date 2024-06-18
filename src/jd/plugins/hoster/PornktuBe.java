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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.DebugMode;
import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PornktuBe extends KernelVideoSharingComV2 {
    public PornktuBe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /** 2022-07-26: Seems like they're GEO-blocking all IPs except for US IPs. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "pornktube.club", "pornktube.tv", "pornktube.vip", "pornktube.com", "pornktu.be" });
        return ret;
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("pornktube.tv");
        deadDomains.add("pornktube.vip");
        deadDomains.add("pornktube.com");
        deadDomains.add("pornktu.be");
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
        return buildAnnotationUrlsPatternPornktube(getPluginDomains());
    }

    private static final String pattern_special = "/(?:vid|view)/(\\d+)/(([\\w\\-]+)/)?";

    public static String[] buildAnnotationUrlsPatternPornktube(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:\\w+\\.)?" + buildHostsPatternPart(domains) + pattern_special);
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-07-26: Main domain has changed frequently. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    protected void getPage(final String page) throws Exception {
        /** 2020-11-05: Special: Website may answer with 404 along with a redirect on first request. */
        getPage(br, page);
        final String redirect = br.getRegex("http-equiv=\"Refresh\" content=\"0; URL=(http[^<>\"]+)").getMatch(0);
        if (redirect != null) {
            this.getPage(redirect);
        }
    }

    @Override
    protected boolean isOfflineWebsite(final Browser br) {
        if (br.containsHTML("(?i)>\\s*Video removed at request of the owner")) {
            /* 2021-11-23: Special */
            return true;
        } else if (super.isOfflineWebsite(br)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getDllink(final DownloadLink link, final Browser br) throws PluginException, IOException {
        final String id = this.getFUID(this.getDownloadLink());
        // final String s = br.getRegex("data-s=\"(\\d+)\"").getMatch(0);
        // final String t = br.getRegex("data-t=\"(\\d+)\"").getMatch(0);
        final String server = br.getRegex("data-n=\"(\\w+)\"").getMatch(0);
        if (server == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dllink = null;
        int best = 0;
        final boolean useNewHandling = true;
        if (useNewHandling) {
            /* 2024-06-18 */
            final int nt = Integer.parseInt(id) / 1000;
            final int n = nt * 1000;
            final String[] qualStrings = br.getRegex("data-c=\"([^\"]+)").getColumn(0);
            if (qualStrings == null || qualStrings.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String qualString : qualStrings) {
                qualString = Encoding.htmlOnlyDecode(qualString);
                final String[] qualData = qualString.split(";");
                final String qualityStr = qualData[1];
                final String number = qualData[4];
                final String timestampStr = qualData[5];
                final String keyyy = qualData[6];
                String url = "https://" + server + ".vstor.top/whpvid/" + timestampStr + "/" + keyyy + "/" + n + "/" + number + "/" + number + "_" + qualityStr + ".mp4";
                /* Special case */
                url = url.replace("_720p", "");
                final int quality = Integer.parseInt(qualityStr.replace("p", ""));
                if (quality > best) {
                    best = quality;
                    dllink = url;
                }
            }
        } else {
            final String qualitiesStr = br.getRegex("data-q=\"([^\"]+)\"").getMatch(0);
            final String[] qualities = qualitiesStr.split(",");
            final int nt = Integer.parseInt(id) / 1000;
            final int n = nt * 1000;
            for (final String qualityItems : qualities) {
                final Regex qualityInfo = new Regex(qualityItems, "(\\d+)p;\\d+;(\\d+);([^;]+)");
                final String qualityStr = qualityInfo.getMatch(0);
                final String number = qualityInfo.getMatch(1);
                final String key = qualityInfo.getMatch(2);
                if (qualityStr == null || number == null || key == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String url = "https://" + server + ".vstor.top/whpvid/" + number + "/" + key + "/" + n + "/" + id + "/" + id + ".mp4";
                /* Special case */
                url = url.replace("_720p", "");
                final int quality = Integer.parseInt(qualityStr);
                if (quality > best) {
                    best = quality;
                    dllink = url;
                }
            }
        }
        link.setProperty(PROPERTY_CHOSEN_QUALITY, best);
        if (dllink != null && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.setComment("ChosenQuality: " + best + "p");
        }
        return dllink;
    }

    @Override
    protected String getFUIDFromURL(final String url) {
        if (url == null) {
            return null;
        } else {
            final String fuid = new Regex(url, pattern_special).getMatch(0);
            if (fuid != null) {
                return fuid;
            } else {
                return super.getFUIDFromURL(url);
            }
        }
    }

    @Override
    protected String getURLTitle(final String url) {
        if (url == null) {
            return null;
        } else {
            final String urlTitle = new Regex(url, pattern_special).getMatch(2);
            if (urlTitle != null) {
                return urlTitle;
            } else {
                return super.getURLTitle(url);
            }
        }
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlSlug) {
        return "https://www." + host + "/view/" + fuid + "/";
    }
}