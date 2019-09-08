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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.utils.Regex;
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

@DecrypterPlugin(revision = "$Revision: 41239 $", interfaceVersion = 3, names = { "dramavery.com" }, urls = { "https?://(?:(?:www|p)\\.)?dramavery\\.com/(?:drama|movie|player)/.+" })
public class DramaVeryCom extends PluginForDecrypt {
    public DramaVeryCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>([^<]+)\\s+(?:\\| DramaVery|- VIEW|- DramaVery)").getMatch(0);
        String[] links = br.getRegex("<iframe id=\"player-content\" src=\"([^\"]+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("<a class=\"list-item\" href=\"([^\"]+/(?:movie|drama)/[^\"]+)\"").getColumn(0);
        }
        if (new Regex(parameter, "p\\.dramavery\\.com/player/.+").matches()) {
            if (br.containsHTML("p,a,c,k,e,d")) {
                final Browser br2 = br.cloneBrowser();
                final String jsExternal1 = br2.getPage("/dist/js/app.min.js?v=4");
                String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                String result = null;
                try {
                    // engine.eval(jsExternal1);
                    engine.eval("var res = " + js + ";");
                    result = (String) engine.get("res");
                    String videoID = new Regex(result, "data:\\{url:\'([^\']+)\'").getMatch(0);
                    if (videoID != null && videoID.length() > 0) {
                        br2.getPage("https://p.dramavery.com/player?url=" + videoID);
                        String[] mirrorIds = br2.getRegex("<button value=\"(\\w*)\"").getColumn(0);
                        if (mirrorIds != null & mirrorIds.length > 0) {
                            links = new String[mirrorIds.length];
                            for (int i = 0; i < mirrorIds.length; i++) {
                                br2.getPage("https://p.dramavery.com/player?url=" + videoID + "&index=" + mirrorIds[i]);
                                String embedURL = br2.getRegex("<iframe\\s+class=\"iframe-video\"\\s+src=\"([^\"]+)\"").getMatch(0);
                                if (embedURL != null) {
                                    br2.getPage(embedURL);
                                    links[i] = br2.getRedirectLocation() == null ? br2.getURL() : br2.getRedirectLocation();
                                }
                            }
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (links != null && links.length > 0) {
            for (String link : links) {
                if (link.startsWith("//")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.setProperty("ALLOW_MERGE", true);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}