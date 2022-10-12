//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.plugins.PluginDependencies;
import jd.plugins.decrypter.AnotepadComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { AnotepadComCrawler.class })
public class AnotepadCom extends AbstractPastebinHoster {
    public AnotepadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://anotepad.com/privacy";
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(AnotepadComCrawler.getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(AnotepadComCrawler.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return AnotepadComCrawler.getAnnotationUrls();
    }
}