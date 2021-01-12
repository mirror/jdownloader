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
public class KernelVideoSharingComV2HostsDefault extends KernelVideoSharingComV2 {
    public KernelVideoSharingComV2HostsDefault(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /** Add all KVS hosts to this list that fit the main template without the need of ANY changes to this class. */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "kvs-demo.com" });
        ret.add(new String[] { "sleazyneasy.com" });
        ret.add(new String[] { "pornwhite.com" });
        ret.add(new String[] { "private-shows.net" });
        ret.add(new String[] { "anon-v.com" });
        ret.add(new String[] { "voyeurhit.com" });
        ret.add(new String[] { "hotmovs.com" });
        ret.add(new String[] { "theclassicporn.com" });
        ret.add(new String[] { "porndr.com" });
        ret.add(new String[] { "momvids.com" });
        ret.add(new String[] { "wankoz.com" });
        ret.add(new String[] { "clipcake.com" });
        /* zbporn.com belongs to the same "porn network" as sexvid.xxx. */
        ret.add(new String[] { "zbporn.com" });
        ret.add(new String[] { "xozilla.com" });
        ret.add(new String[] { "femdomtb.com" });
        ret.add(new String[] { "cutscenes.net" });
        ret.add(new String[] { "fetishshrine.com" });
        ret.add(new String[] { "sheshaft.com" });
        ret.add(new String[] { "yeswegays.com" });
        ret.add(new String[] { "analdin.com" });
        ret.add(new String[] { "tryboobs.com" });
        ret.add(new String[] { "vikiporn.com" });
        ret.add(new String[] { "katestube.com" });
        ret.add(new String[] { "babestube.com" });
        ret.add(new String[] { "japan-whores.com" });
        ret.add(new String[] { "boundhub.com" });
        ret.add(new String[] { "bravoteens.com" });
        ret.add(new String[] { "onlygayvideo.com" });
        ret.add(new String[] { "mylust.com" });
        ret.add(new String[] { "yourporngod.com" });
        ret.add(new String[] { "everydayporn.co" });
        ret.add(new String[] { "dato.porn" });
        ret.add(new String[] { "camhub.world" });
        ret.add(new String[] { "upornia.com" });
        ret.add(new String[] { "finevids.xxx" });
        ret.add(new String[] { "txxx.com", "tubecup.com" });
        ret.add(new String[] { "freepornvs.com" });
        ret.add(new String[] { "vr.pornhat.com" });
        ret.add(new String[] { "xxxymovies.com" });
        ret.add(new String[] { "needgayporn.com" });
        /* Formerly sexwebvideo.com --> sexwebvideo.net */
        ret.add(new String[] { "videowebcam.tv" });
        ret.add(new String[] { "yoxhub.com" });
        ret.add(new String[] { "hdzog.com" });
        ret.add(new String[] { "submityourflicks.com" });
        ret.add(new String[] { "hclips.com" });
        ret.add(new String[] { "pornicom.com" });
        ret.add(new String[] { "tubepornclassic.com" });
        ret.add(new String[] { "camvideos.org" });
        ret.add(new String[] { "nudogram.com" });
        ret.add(new String[] { "deviants.com" });
        ret.add(new String[] { "faapy.com" });
        ret.add(new String[] { "nudez.com" });
        ret.add(new String[] { "pornomovies.com" });
        ret.add(new String[] { "3movs.com" });
        /* 2021-01-12 */
        ret.add(new String[] { "camhoes.tv" });
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
    protected String generateContentURL(final String fuid, final String urlTitle) {
        if (StringUtils.isEmpty(fuid) || StringUtils.isEmpty(urlTitle)) {
            return null;
        }
        return "https://www." + this.getHost() + "/videos/" + fuid + "/" + urlTitle;
    }
}