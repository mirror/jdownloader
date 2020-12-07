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
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wcostream.com" }, urls = { "https?://(?:www[0-9]*\\.)?wcostream\\.com/(?:anime/)?.+$" })
public class WCOStream extends antiDDoSForDecrypt {
    public WCOStream(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String page = br.toString();
        String fpName = br.getRegex("<title>\\s*([^<]+)\\s+\\|\\s+Watch").getMatch(0);
        ArrayList<String> links = new ArrayList<String>();
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
            for (String embedPageLink : embedPageLinks) {
                if (StringUtils.isNotEmpty(embedPageLink) && isAbort()) {
                    Browser br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    br2.getPage(embedPageLink);
                    String embedServiceLink = br2.getRegex("get\\s*\\(\\s*\"\\s*([^\"]+)\\s*\"\\s*\\)").getMatch(0);
                    if (StringUtils.isNotEmpty(embedServiceLink)) {
                        br2.getPage(embedServiceLink);
                        String embedServiceResponse = br2.toString();
                        final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(embedServiceResponse);
                        if (entries != null) {
                            String server = (String) entries.get("server");
                            String enc = (String) entries.get("enc");
                            String hd = (String) entries.get("hd");
                            if (StringUtils.isNotEmpty(server)) {
                                if (StringUtils.isNotEmpty(enc)) {
                                    links.add(server + "/getvid?evid=" + enc);
                                    links.add("directhttp://" + server + "/getvid?evid=" + enc);
                                }
                                if (StringUtils.isNotEmpty(hd)) {
                                    links.add(server + "/getvid?evid=" + hd);
                                    links.add("directhttp://" + server + "/getvid?evid=" + hd);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String link : links) {
            if (StringUtils.isNotEmpty(link)) {
                link = Encoding.htmlDecode(link).replaceAll("^//", "https://");
                DownloadLink dl = createDownloadlink(link);
                if (StringUtils.isNotEmpty(fpName) && StringUtils.startsWithCaseInsensitive(link, "directhttp://")) {
                    dl.setFinalFileName(fpName.trim() + ".mp4");
                }
                decryptedLinks.add(dl);
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
            fp.setProperty(LinkCrawler.PACKAGE_ALLOW_INHERITANCE, true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}