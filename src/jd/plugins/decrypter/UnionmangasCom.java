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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UnionmangasCom extends antiDDoSForDecrypt {
    public UnionmangasCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "unionmangas.com", "unionmangas.net", "unionmangas.top", "unionleitor.top" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:leitor/[^/]+/[a-z0-9\\.]+[^/\\s]*|manga/[a-z0-9\\-\\.]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        final String url_fpname;
        final String url_name;
        if (param.getCryptedUrl().matches(".+/leitor/.+")) {
            /* Decrypt single chapter */
            if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/leitor/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex urlinfo = new Regex(param.getCryptedUrl(), "https?://[^/]+/leitor/([^/]+)/([a-z0-9\\-\\.]+)");
            final String chapter_str = urlinfo.getMatch(1);
            url_name = urlinfo.getMatch(0);
            url_fpname = Encoding.urlDecode(url_name + "_chapter_" + chapter_str, false);
            String[] links = br.getRegex("data\\-lazy=\"(http[^<>\"]+)\"").getColumn(0);
            if (links.length == 0) {
                links = br.getRegex("<img\\s*src=\"(.*?\\.(jpe?g|png|gif))\"").getColumn(0);
            }
            if (links == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String url : links) {
                url = br.getURL(url).toString();
                final DownloadLink dl = this.createDownloadlink("directhttp://" + Encoding.urlEncode_light(url), false);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        } else {
            url_name = new Regex(param.getCryptedUrl(), "/([^/]+)$").getMatch(0);
            url_fpname = url_name;
            if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/manga/")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] chapterUrls = br.getRegex("\"(https?://(?:unionmangas|unionleitor)\\.[a-z0-9]+/leitor/[^\"\\']+)\"").getColumn(0);
            for (final String chapterUrl : chapterUrls) {
                decryptedLinks.add(this.createDownloadlink(chapterUrl));
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(url_fpname);
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
