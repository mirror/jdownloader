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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PrivateRecordsCom extends PornEmbedParser {
    public PrivateRecordsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        ret.add(new String[] { "private-records.com", "webcamvau.com", "privat-zapisi.biz" });
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
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/videos/\\d+-([a-z0-9\\-]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    protected boolean isSelfhosted(final Browser br) {
        if (br.containsHTML("video_url\\s*:\\s*'[^\\']+/get_file/")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.getURL().endsWith("/404.php")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
    }
}