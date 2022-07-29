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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class CamwhoresTv extends KernelVideoSharingComV2 {
    public CamwhoresTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.camwhores.tv/");
    }

    /** Sync this between camwhores hoster + crawler plugins!! */
    public static List<String[]> getPluginDomains() {
        return jd.plugins.decrypter.CamwhoresTv.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPattern(getPluginDomains());
    }

    /**
     * Override this and add dead domains so upper handling can auto update added URLs and change domain if it contains a dead domain. This
     * way a lot of "old" URLs will continue to work in JD while they may fail in browser.
     */
    protected ArrayList<String> getDeadDomains() {
        return jd.plugins.decrypter.CamwhoresTv.getDeadDomainsStatic();
    }

    @Override
    protected boolean enableFastLinkcheck() {
        /* 2020-10-30 */
        return true;
    }

    @Override
    protected boolean isOfflineWebsite(final Browser br) {
        if (super.isOfflineWebsite(br)) {
            return true;
        } else if (isOfflineStatic(br)) {
            return true;
        } else {
            return false;
        }
    }

    public static final boolean isOfflineStatic(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)>\\s*404 / Page not found")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean useEmbedWorkaround() {
        /*
         * 2022-04-13: A lot of videos embedded by other websites either aren't allowed to be embedded at all or can only be embedded using
         * a specific refered --> This workaround should fix both of these issues!
         */
        return true;
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }
}