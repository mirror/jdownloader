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
import java.util.Map;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WCOStream extends antiDDoSForDecrypt {
    public WCOStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wcostream.net", "wcostream.com" });
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
            ret.add("https?://(?:www[0-9]*\\.)?" + buildHostsPatternPart(domains) + "/(?:anime/)?.+");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>\\s*([^<]+)\\s+\\|\\s+Watch").getMatch(0);
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
        }
        final ArrayList<String> links = new ArrayList<String>();
        // Handle list page
        Collections.addAll(links, br.getRegex("<li><a[^>]+href\\s*=\\s*\"\\s*([^\"]+)\\s*\"[^>]+class\\s*=\\s*\"[^\"]*sonra[^\"]*\"").getColumn(0));
        // Handle video page
        ArrayList<String> embedPageLinks = new ArrayList<String>();
        String[][] encodedSections = br.getRegex("var\\s+\\w+\\s*=\\s*\"\\s*\"\\;\\s*var\\s+\\w+\\s*=\\s*\\[([^\\]]+)\\][^>]+replace[^>]+-\\s*(\\d+)").getMatches();
        if (encodedSections != null && encodedSections.length > 0) {
            for (String[] encodedSection : encodedSections) {
                String[] encodedStrings = new Regex(encodedSection[0], "\"\\s*([^\"\\s]+)\\s*\"").getColumn(0);
                int offset = Integer.parseInt(encodedSection[1]);
                StringBuilder builder = new StringBuilder();
                String decodedString = "";
                if (encodedStrings != null && encodedStrings.length > 0) {
                    for (String encodedString : encodedStrings) {
                        decodedString = Encoding.Base64Decode(encodedString);
                        decodedString = decodedString.replaceAll("[^\\d]+", ""); // Remove everything non-numeric
                        int decodedNumber = Integer.parseInt(decodedString) - offset; // Get the ASCII character ID
                        builder.append((char) decodedNumber);
                    }
                    decodedString = builder.toString();
                    decodedString = new Regex(decodedString, "src\\s*=\\s*\"([^\"]+)").getMatch(0);
                    embedPageLinks.add(decodedString);
                }
            }
        }
        Collections.addAll(embedPageLinks, br.getRegex("<meta[^>]+itemprop\\s*=\\s*\"embedURL\"[^>]+content=\"\\s*([^\"]+)\\s*\"").getColumn(0));
        if (!embedPageLinks.isEmpty()) {
            int counter = 0;
            for (String embedPageLink : embedPageLinks) {
                counter++;
                logger.info("Crawling mirror " + counter + "/" + embedPageLinks.size());
                if (StringUtils.isEmpty(embedPageLink)) {
                    continue;
                }
                final Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                br2.getPage(embedPageLink);
                String embedServiceLink = br2.getRegex("get\\s*\\(\\s*\"\\s*([^\"]+)\\s*\"\\s*\\)").getMatch(0);
                if (StringUtils.isNotEmpty(embedServiceLink)) {
                    br2.getPage(embedServiceLink);
                    String embedServiceResponse = br2.toString();
                    final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(embedServiceResponse);
                    if (entries == null) {
                        continue;
                    }
                    final String server = (String) entries.get("cdn");
                    final String enc = (String) entries.get("enc");
                    final String hd = (String) entries.get("hd");
                    if (StringUtils.isEmpty(server)) {
                        continue;
                    }
                    if (StringUtils.isNotEmpty(enc)) {
                        final String finallink = server + "/getvid?evid=" + enc;
                        links.add(finallink);
                        links.add("directhttp://" + finallink);
                    }
                    if (StringUtils.isNotEmpty(hd)) {
                        final String finallink = server + "/getvid?evid=" + enc;
                        links.add(finallink);
                        links.add("directhttp://" + finallink);
                    }
                }
                if (this.isAbort()) {
                    break;
                }
            }
        }
        for (String url : links) {
            if (StringUtils.isNotEmpty(url)) {
                url = Encoding.htmlDecode(url).replaceAll("^//", "https://");
                final DownloadLink dl = createDownloadlink(url);
                if (StringUtils.startsWithCaseInsensitive(url, "directhttp://")) {
                    dl.setFinalFileName(title + ".mp4");
                }
                dl.setAvailable(true);
                ret.add(dl);
            }
        }
        if (StringUtils.isNotEmpty(title)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.setAllowMerge(true);
            fp.setAllowInheritance(true);
            fp.addLinks(ret);
        }
        return ret;
    }
}