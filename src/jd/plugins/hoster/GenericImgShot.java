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

import org.jdownloader.plugins.components.ImgShotCore;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class GenericImgShot extends ImgShotCore {
    public GenericImgShot(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * mods: See overridden functions<br />
     * limit-info: no resume, no chunkload <br />
     * captchatype-info: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* imgshot.com = Official ImgShot DEMO website! */
        ret.add(new String[] { "imgshot.com" });
        ret.add(new String[] { "imgdrive.net" });
        ret.add(new String[] { "imagedecode.com", "dimtus.com" });
        ret.add(new String[] { "damimage.com" });
        ret.add(new String[] { "imageteam.org", "imgstudio.org" });
        ret.add(new String[] { "imgadult.com" });
        ret.add(new String[] { "imgtornado.com" });
        ret.add(new String[] { "imgwallet.com" });
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
        return ImgShotCore.buildAnnotationUrls(getPluginDomains());
    }
}