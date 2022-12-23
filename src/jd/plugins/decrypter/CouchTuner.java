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
import java.util.Collections;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLSearch;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class CouchTuner extends antiDDoSForDecrypt {
    public CouchTuner(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "couchtuner.show", "couchtuner.cloud", "couchtuner.click", "couchtuner.host", "couchtuner.website", "couchtuner.top", "couchtuner.fun", "couchtuner.me", "couchtuner.network", "couchtuner.vip", "couchtuner.win", "watch-online.xyz", "watchseries-online.me", "2mycouchtuner.me", "mycouchtuner.li", "1couchtuner.club", "1couchtuner.me", "1couchtuner.xyz", "ecouchtuner.club", "ecouchtuner.me", "ecouchtuner.xyz", "icouchtuner.club", "icouchtuner.me", "icouchtuner.xyz" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/\\d{4}/\\d{2}/[\\w\\-]+/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        String fpName = HTMLSearch.searchMetaTag(br, "og:title");
        if (StringUtils.isEmpty(fpName)) {
            fpName = br.getRegex("(?:og:)?(?:title|description)\\\"[^>]*content=[\\\"'](?:\\s*Watch\\s*Couchtuner\\s*)?([^\\\"\\']+)\\s+(?:online\\s+for\\s+free|\\|)").getMatch(0);
        }
        if (StringUtils.isEmpty(fpName)) {
            fpName = br.getRegex("<h2[^>]+class\\s*=\\s*\"[^\"]*title[^\"]*\"[^>]*>\\s*([^<]+)\\s*").getMatch(0);
        }
        ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("Watch[^\"]*[iI]t[^\"]*[hH]ere.{2,32}<a[^>]+href\\s*=\\s*\"\\s*([^\"]+)\\s*\"").getColumn(0));
        if (links.isEmpty()) {
            Collections.addAll(links, br.getRegex("Watch[^\"]*[iI]t[^\"]*[hH]ere[^\\|]*<a href=\"([^\"]+)\\\"[^\\|]*</a>").getColumn(0));
        }
        if (links.isEmpty()) {
            Collections.addAll(links, br.getRegex("<iframe[^>]+src=[\"\']([^\"\']+)[\"\']").getColumn(0));
        }
        if (links.isEmpty()) {
            Collections.addAll(links, br.getRegex("<a[^>]+href=\"([^\"]+)\"[^>]*rel=\"bookmark\"[^>]*>").getColumn(0));
        }
        if (links.isEmpty()) {
            Collections.addAll(links, br.getRegex("<iframe[^>]+src=\"([^\"]+)\"[^>]*>").getColumn(0));
        }
        for (String link : links) {
            link = br.getURL(Encoding.htmlDecode(link)).toString();
            ret.add(createDownloadlink(link));
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            fp.addLinks(ret);
        }
        if (ret.isEmpty()) {
            logger.info("Found no results at all");
        }
        return ret;
    }
}