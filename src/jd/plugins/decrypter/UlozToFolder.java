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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UlozToFolder extends PluginForDecrypt {
    public UlozToFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        /* ulozto.net = the english version of the site */
        ret.add(new String[] { "uloz.to", "ulozto.sk", "ulozto.cz", "ulozto.net", "zachowajto.pl" });
        ret.add(new String[] { "pornfile.cz", "pornfile.ulozto.net" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:download|file)-tracking/[a-f0-9]{96}");
        }
        return ret.toArray(new String[0]);
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    /** 2021-02-11: This host does not like many requests in a short time! */
    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }

    /** Special (temporary/IP bound) URLs available when searching for files directly on the website of this file-hoster. */
    private static final String TYPE_TRACKING = "https?://[^/]+/(?:download|file)-tracking/[a-f0-9]{96}";

    /** 2021-02-11: This host is GEO-blocking german IPs! */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        if (param.getCryptedUrl().matches(TYPE_TRACKING)) {
            return crawlSingleURL(param);
        } else {
            return crawlFolder(param);
        }
    }

    private ArrayList<DownloadLink> crawlSingleURL(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(param.getCryptedUrl());
        final String finallink = br.getRedirectLocation();
        if (finallink == null) {
            decryptedLinks.add(this.createOfflinelink(param.getCryptedUrl()));
            return decryptedLinks;
        } else {
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> crawlFolder(final CryptedLink param) throws IOException {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        return decryptedLinks;
    }
}
