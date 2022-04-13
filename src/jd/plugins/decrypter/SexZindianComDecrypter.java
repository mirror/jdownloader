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
import jd.plugins.DecrypterPlugin;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SexZindianComDecrypter extends PornEmbedParser {
    public SexZindianComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "sexzindian.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9\\-]+)/?");
        }
        return ret.toArray(new String[0]);
    }
    // public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
    // ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
    // br.setFollowRedirects(true);
    // br.getPage(param.getCryptedUrl());
    // if (br.getHttpConnection() == null || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)404 Not Found<|Page not
    // found")) {
    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // }
    // decryptedLinks.addAll(findEmbedUrls(null));
    // if (!decryptedLinks.isEmpty()) {
    // return decryptedLinks;
    // }
    // decryptedLinks = new ArrayList<DownloadLink>();
    // decryptedLinks.add(createDownloadlink(param.getCryptedUrl()));
    // return decryptedLinks;
    // }

    @Override
    protected boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)404 Not Found<|Page not found")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean assumeSelfhostedContentOnNoResults() {
        return true;
    }
}