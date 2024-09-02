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
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class KernelVideoSharingPornadoXxx extends KernelVideoSharingComV2 {
    public KernelVideoSharingPornadoXxx(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* Not to be mistaken with pornado.co!! */
        ret.add(new String[] { "pornado.xxx", "private-shows.net", "anon-v.com", "anon-v.lol", "anon-v.net", "anon-v.org", "anon-v.ru", "anon-v.su", "anon-v.to", "anon-v.tube", "anon-v.vip", "anon-v.wtf", "anon-v.xyz" });
        return ret;
    }

    @Override
    protected ArrayList<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("private-shows.net"); // 2024-04-15
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
        return KernelVideoSharingComV2.buildAnnotationUrlsDefaultVideosPattern(getPluginDomains());
    }

    @Override
    protected String generateContentURL(final String host, final String fuid, final String urlTitle) {
        return generateContentURLDefaultVideosPattern(host, fuid, urlTitle);
    }
}