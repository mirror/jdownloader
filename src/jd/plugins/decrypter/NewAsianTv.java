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
import java.util.HashMap;
import java.util.LinkedHashSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "newasiantv.tv" }, urls = { "https?://(?:\\w+\\.)?newasiantv\\.(tv|ch)/(?:(?:watch|files)/.+\\.html?|embed\\.php\\?.+)" })
public class NewAsianTv extends PluginForDecrypt {
    public NewAsianTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("<meta name=\"description\" content=[\"'](?:Watch )?([^\"]*) \\|").getMatch(0);
        String[] links = null;
        if (new Regex(parameter, "/(?:watch|files)/.+\\.html?").matches()) {
            links = br.getRegex("<a[^>]*href=\"([^\"]+)\" episode-type=\"watch\"[^>]*>").getColumn(0);
            String episodeJSON = br.getRegex("episodeJson\\s*=\\s*\'([^\']+)\'\\;").getMatch(0);
            if (episodeJSON != null && episodeJSON.length() > 0) {
                final Object[] jsonArray = JSonStorage.restoreFromString(episodeJSON, TypeRef.OBJECT_ARRAY);
                for (Object jsonObject : jsonArray) {
                    final Browser br2 = br.cloneBrowser();
                    final String apiUrl = "https://player.newasiantv.ch/v2/loader.php";
                    HashMap map = (HashMap) jsonObject;
                    String url = String.valueOf(map.getOrDefault("url", ""));
                    String subUrl = String.valueOf(map.getOrDefault("subUrl", ""));
                    String episodeID = String.valueOf(map.getOrDefault("episodeId", ""));
                    String filmID = br.getRegex("var filmId=\"(\\w+)\";").getMatch(0);
                    String currentEp = br.getRegex("var currentEp=\"(\\w+)\";").getMatch(0);
                    if (StringUtils.equalsIgnoreCase(episodeID, currentEp)) {
                        if (subUrl != null && subUrl.length() > 0) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(subUrl)));
                        }
                        br2.postPage(apiUrl, "url=" + url + "&subUrl=" + subUrl + "&eid=" + episodeID + "&filmID=" + filmID);
                        String cryptedData = br2.getRegex("(decodeLink\\(\\s*\"[^\"]+\"\\s*\\,\\s*[^\\)]+\\s*\\))").getMatch(0);
                        if (cryptedData != null && cryptedData.length() > 0) {
                            final String jsExternal1 = br2.getPage("https://newasiantv.tv/theme/js/main.js?v=02042018");
                            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                            final ScriptEngine engine = manager.getEngineByName("javascript");
                            try {
                                engine.eval(jsExternal1);
                                engine.eval("var res = " + cryptedData + ";");
                                String decryptedData = (String) engine.get("res");
                                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(decryptedData)));
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                        String iframeLink = br2.getRegex("<iframe[^>]+src=\"([^\"]+)\"").getMatch(0);
                        if (iframeLink != null && iframeLink.length() > 0) {
                            iframeLink = Encoding.htmlDecode(iframeLink);
                            if (iframeLink.startsWith("//")) {
                                iframeLink = iframeLink.replace("//", "");
                            }
                            decryptedLinks.add(createDownloadlink(iframeLink));
                        }
                    }
                }
            }
        } else if (new Regex(parameter, "/embed\\.php\\?.+").matches()) {
            String encodedURL = br.getRegex("atob\\(\"([^\"]+)\"").getMatch(0);
            if (encodedURL != null && encodedURL.length() > 0) {
                links = new String[] { Encoding.Base64Decode(encodedURL) };
            }
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}