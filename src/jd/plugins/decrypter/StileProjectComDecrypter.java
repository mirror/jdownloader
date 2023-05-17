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
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.hoster.StileProjectCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StileProjectComDecrypter extends PornEmbedParser {
    public StileProjectComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "pornrabbit.com" });
        ret.add(new String[] { "stileproject.com" });
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
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(video/[a-z0-9\\-_]+-\\d+\\.html|videos/\\d+/[a-z0-9\\-_]+/?)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        final Regex type1 = new Regex(param.getCryptedUrl(), StileProjectCom.TYPE_NORMAL);
        if (type1.matches()) {
            return type1.getMatch(0).replace("-", " ").trim();
        } else {
            return new Regex(param.getCryptedUrl(), StileProjectCom.TYPE_NORMAL2).getMatch(1).replace("-", " ").trim();
        }
    }

    @Override
    protected boolean isOffline(final Browser br) {
        return StileProjectCom.isOffline(br);
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        final String[] embedURLs = br.getRegex(StileProjectCom.TYPE_EMBED).getColumn(-1);
        // e.g. html will always contain: "embedUrl": "https://www.stileproject.com/embed/123456789",
        if (embedURLs != null && embedURLs.length > 1) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean allowResult(final String url) {
        /**
         * Ignore self-embed URLs which would go into host plugin for this host. </br>
         * To put it short: Without this check, even items with external URLs would always also return invalid selfhosted results which
         * would get displayed as offline -> We don't want that!
         */
        if (url.matches("https?://(www\\.)?" + Pattern.quote(this.getHost()) + "/embed/\\d+/?")) {
            return false;
        } else {
            return super.allowResult(url);
        }
    }
}
