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

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "redgifs.com" }, urls = { "https?://(?:www\\.)?redgifs\\.com/(?:watch|ifr)/([A-Za-z0-9]+)" })
public class RedGifsCom extends GfyCatCom {
    /**
     * 2022-12-27: different site/api, first work for later separation, add support for new api https://github.com/Redgifs/api/wiki
     * 
     * @param wrapper
     */
    public RedGifsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST, LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "redgifs.com" };
    }

    @Override
    public String getAGBLink() {
        return "https://www.redgifs.com/terms";
    }
}