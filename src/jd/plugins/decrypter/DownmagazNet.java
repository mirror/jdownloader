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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DownmagazNet extends PluginForDecrypt {
    public DownmagazNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "downmagaz.net" });
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
            ret.add("https?://(?:[a-z0-9]+\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9\\-_]+/\\d+-[a-z0-9\\-_]+\\.html|out\\.php\\?f=[a-z0-9]+\\&down=\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_MULTIPLE = "https?://[^/]+/([a-z0-9\\-_]+)/(\\d+)-([a-z0-9\\-_]+)\\.html";
    private final String TYPE_REDIRECT = "https?://[^/]+/out\\.php\\?f=[a-z0-9]+\\&down=(\\d+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (param.getCryptedUrl().matches(TYPE_MULTIPLE)) {
            // final Regex urlInfo = new Regex(TYPE_MULTIPLE, param.getCryptedUrl());
            br.setFollowRedirects(true);
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String multiplicatorStr = br.getRegex("\\&down='\\+\\(down\\*(\\d+)\\)").getMatch(0);
            if (multiplicatorStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String[] mirrorIDs = br.getRegex("href=\"[^\"]*\" data-field=\"([a-z0-9]+)\" data-down=\"\\d+\"").getColumn(0);
            if (mirrorIDs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String contentIDStr = br.getRegex("data-down=\"(\\d+)\"").getMatch(0);
            if (contentIDStr == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final long downValue = Long.parseLong(contentIDStr) * Long.parseLong(multiplicatorStr);
            for (final String mirrorID : mirrorIDs) {
                /* These URLs will go back into this crawler and will be handled one by one. */
                final String url = "/out.php?f=" + mirrorID + "&down=" + downValue;
                decryptedLinks.add(this.createDownloadlink(br.getURL(url).toString()));
            }
        } else {
            br.setFollowRedirects(false);
            br.getPage(param.getCryptedUrl());
            String redirect = br.getRedirectLocation();
            if (redirect == null) {
                redirect = br.getRegex("window\\.location\\.replace\\('(http[^<>\"\\']+)'\\)").getMatch(0);
            }
            if (redirect == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            decryptedLinks.add(this.createDownloadlink(redirect));
        }
        return decryptedLinks;
    }
}
