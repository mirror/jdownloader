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

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TxxxCom extends KernelVideoSharingComV2 {
    public TxxxCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "txxx.com", "tubecup.com", "videotxxx.com", "txxx.tube" });
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
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPattern(getPluginDomains());
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2022-02-21: Moved txxx.tube from KernelVideoSharingComV2HostsDefault into this one. */
        return this.rewriteHost(getPluginDomains(), host);
    }

    @Override
    protected String generateContentURL(final String fuid, final String urlTitle) {
        if (StringUtils.isEmpty(fuid) || StringUtils.isEmpty(urlTitle)) {
            return null;
        } else {
            return "https://www." + this.getHost() + "/videos/" + fuid + "/" + urlTitle + "/";
        }
    }

    @Override
    protected boolean useAPI() {
        return true;
    }

    @Override
    protected String getAPIParam1(final String videoID) {
        if (videoID.length() > 3) {
            return videoID.substring(0, 2) + "000000";
        } else {
            return null;
        }
    }

    @Override
    protected String getAPICroppedVideoID(final String videoID) {
        if (videoID.length() > 5) {
            return videoID.substring(0, 5) + "000";
        } else {
            return null;
        }
    }
}