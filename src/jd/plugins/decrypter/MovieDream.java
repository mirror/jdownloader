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

@DecrypterPlugin(revision = "$Revision: 41234 $", interfaceVersion = 3, names = { "moviedream.ws" }, urls = { "https?://(?:www\\d*\\.)?moviedream\\.ws/(?:serie|film)/\\d+.+" })
public class MovieDream extends PluginForDecrypt {
    public MovieDream(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String fpName = br.getRegex("<title>([^<]+)\\s@ MovieDream.ws").getMatch(0);
        String[] linksPlain = br.getRegex("<a href=\"([^\"]+)\"[^>]+><img class=\"sdlinkbutton\"").getColumn(0);
        String[] linksEncrypted = br.getRegex("(writesout\\([^\\)]+\\))").getColumn(0);
        if (linksPlain == null || linksPlain.length == 0) {
            linksPlain = br.getRegex("<a href=\"([^\"]+)\" class=\"(?:episodebutton|seasonbutton)[^\"]*\"").getColumn(0);
        }
        if (linksPlain != null && linksPlain.length > 0) {
            for (String link : linksPlain) {
                if (link.contains("writesout(")) {
                    continue;
                }
                if (link.startsWith("/")) {
                    link = br.getURL(link).toString();
                }
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
            }
        }
        if (linksEncrypted != null && linksEncrypted.length > 0) {
            final Browser br2 = br.cloneBrowser();
            final String jsExternal1 = br2.getPage("https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.9-1/crypto-js.js");
            final String jsExternal2 = br2.getPage("https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.9-1/pbkdf2.js");
            final String jsExternal3 = br2.getPage("https://moviedream.ws/6845.ads.js");
            for (String link : linksEncrypted) {
                final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = manager.getEngineByName("javascript");
                try {
                    engine.eval(jsExternal1);
                    engine.eval(jsExternal2);
                    engine.eval(jsExternal3);
                    engine.eval("var res = " + link + ";");
                    link = (String) engine.get("res");
                    decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(link)));
                } catch (final Exception e) {
                    e.printStackTrace();
                }
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