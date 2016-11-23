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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "ah.pe" }, urls = { "https?://(?:www\\.)?ah\\.pe/[A-Za-z0-9]+" })
public class AdhyPe extends PluginForDecrypt {

    public AdhyPe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String key = unWise();
        this.br.getPage("/g/" + key);
        final String redirecturl = this.br.toString();
        if (redirecturl == null || redirecturl.length() > 500 || !(redirecturl.startsWith("/") && !redirecturl.startsWith("http"))) {
            this.br.getPage(redirecturl);
        }
        final String finallink = this.br.getRedirectLocation();
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private String unWise() {
        String result = null;
        // String fn = br.getRegex("eval\\((function\\(.+)\\)\\);").getMatch(0);
        String fn = br.getRegex("eval\\(function\\(w,i,s,e\\)\\{(.+\\));\\s*?\\}").getMatch(0);
        fn = fn.replace("function(w,i,s,e)", "function('','','','')");
        final String vars = "var w = 'test'; var i = 'test'; var s = 'test'; var e = 'test';";
        fn = vars + fn;
        if (fn == null) {
            return null;
        }
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        try {
            // engine.eval("var res = " + fn + ")");
            engine.eval(fn + ";");
            result = (String) engine.get("res");
        } catch (final Exception e) {
            logger.log(e);
            return null;
        }
        return result;
    }
}
