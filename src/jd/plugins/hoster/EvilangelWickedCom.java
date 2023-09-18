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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.components.config.EvilangelCoreConfig;
import org.jdownloader.plugins.components.config.EvilangelCoreConfigWicked;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class EvilangelWickedCom extends EvilangelCore {
    public EvilangelWickedCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.wicked.com/en/join");
    }

    private static final String URL_MOVIE = "https?://members\\.[^/]+/[a-z]{2}/movie/([A-Za-z0-9\\-_]+)/(\\d+)";

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wicked.com" });
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
        return EvilangelCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    protected String getURLTitle(final DownloadLink link) {
        if (link.getPluginPatternMatcher().matches(URL_MOVIE)) {
            return new Regex(link.getPluginPatternMatcher(), URL_MOVIE).getMatch(0);
        } else {
            return super.getURLTitle(link);
        }
    }

    @Override
    protected boolean allowCookieLoginOnly() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public Class<? extends EvilangelCoreConfig> getConfigInterface() {
        return EvilangelCoreConfigWicked.class;
    }
}