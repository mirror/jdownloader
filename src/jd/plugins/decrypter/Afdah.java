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
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 40024 $", interfaceVersion = 2, names = { "afdah.to" }, urls = { "https?://(www\\.)?afdah\\.to/(watch-(movies|tv-episodes|tv-shows)|embed[0-9]+)/.*" })
public class Afdah extends PluginForDecrypt {
    public Afdah(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        String fpName = br.getRegex("<title>([^<]+) \\| Afdah</title>").getMatch(0);
        String[][] links = br.getRegex("<a rel=\"nofollow\" href=\"([^\"]+)\">").getMatches();
        if (links == null || links.length == 0) {
            links = br.getRegex("href=\"([^\"]+embed[0-9]+/[a-z0-9]+)\"").getMatches();
        }
        if (new Regex(parameter, ".*/embed[0-9]+/[a-zA-Z0-9]+").matches()) {
            Browser brPost = br.cloneBrowser();
            final PostRequest post = new PostRequest(brPost.getURL(parameter));
            post.addVariable("play", "continue");
            post.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            post.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
            brPost.getPage(post);
            if (fpName == null) {
                fpName = brPost.getRegex("<title>([^<]+)</title>").getMatch(0);
            }
            String encodedValue = brPost.getRegex("salt\\((\"[a-zA-Z0-9]+\")\\)").getMatch(0);
            String encodingURL = brPost.getURL("/show-ads.js").toString();
            String encodingJS = brPost.getPage(encodingURL).replaceAll("document.write\\([^\\)]+\\)\\;", "").trim();
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.eval(encodingJS);
            engine.eval("var res = unescape(getF(tor(getF(" + encodedValue + "))));");
            String decodedValue = (String) engine.get("res");
            links = new Regex(decodedValue, "src='([^']+)'").getMatches();
        }
        for (String[] link : links) {
            decryptedLinks.add(createDownloadlink(br.getURL(Encoding.htmlDecode(link[0])).toString()));
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}