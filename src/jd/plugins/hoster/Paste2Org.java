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
import jd.plugins.decrypter.Paste2OrgCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { Paste2OrgCrawler.class })
public class Paste2Org extends AbstractPastebinHoster {
    public Paste2Org(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://paste2.org/";
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(Paste2OrgCrawler.getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(Paste2OrgCrawler.getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return Paste2OrgCrawler.getAnnotationUrls();
    }
}