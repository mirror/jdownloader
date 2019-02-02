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
import java.util.LinkedHashSet;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

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

@DecrypterPlugin(revision = "$Revision: 40332 $", interfaceVersion = 2, names = { "123movieswatch.fun" }, urls = { "https?://www\\.?123movieswatch\\.fun/(film|ref).+" })
public class MoviesWatch extends PluginForDecrypt {
    public MoviesWatch(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final LinkedHashSet<String> dupe = new LinkedHashSet<String>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("<meta (?:name|property)=\"og:title\" content=[\"'](?:Watch ?)([^<>\"]*?) (?:Full (?:Series|Movie) )[Oo]nline [Ff]ree").getMatch(0);
        //
        String mediaID = new Regex(parameter, "/film/[^/]+-([0-9]+).*").getMatch(0);
        String[][] links = br.getRegex("(?:id|class)=\"(?:movie_version_link|tv_episode_item|mv-info)\">[\t\r\n ]+<a href=\"([^\"]+)\"[^>]*>").getMatches();
        for (String[] link : links) {
            final Browser br2 = br.cloneBrowser();
            try {
                br2.getPage(link[0]);
                String videoLink = (br2.getRedirectLocation() == null) ? br2.getURL().toString() : br2.getRedirectLocation();
                if (StringUtils.containsIgnoreCase(videoLink, "ref.php")) {
                    if (!dupe.add(videoLink)) {
                        continue;
                    }
                    final Browser br3 = br2.cloneBrowser();
                    br3.getPage(videoLink);
                    if (br3.containsHTML("p,a,c,k,e,d")) {
                        String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
                        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                        final ScriptEngine engine = manager.getEngineByName("javascript");
                        String result = null;
                        try {
                            engine.eval("var res = " + js + ";");
                            result = (String) engine.get("res");
                            result = result.replaceAll("'\\+loc.*go\\('", "");
                            videoLink = new Regex(result, "url='([^']+)'").getMatch(0);
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!dupe.add(videoLink)) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(videoLink));
            } catch (Exception e) {
                // Some of the redirect URLs are malformed, but there's usually enough mirrors that we don't need to bother with those.
            }
        }
        //
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}