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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "swzz.xyz" }, urls = { "https?://(?:www\\.)?swzz\\.xyz/link/[A-Za-z0-9]+/" })
public class SwzzXyz extends antiDDoSForDecrypt {
    public SwzzXyz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if (br.containsHTML("<em>Questo Link Non è ancora attivo\\.\\.\\.riprova tra qualche istante!<em>")) {
            logger.warning("Retry in a few minutes: " + parameter);
            return decryptedLinks;
        }
        // within packed
        String finallink = br.getRegex("<a\\s*href\\s*=\\s*\"(https?[^\"]+)\"\\s*class\\s*=\\s*\"btn\\-wrapper link\"").getMatch(0);
        if (StringUtils.isEmpty(finallink)) {
            finallink = br.getRegex("var\\s*link\\s*=\\s*(\"|')([^'\"]*)").getMatch(1);
        }
        if (StringUtils.isEmpty(finallink)) {
            final String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
            final ScriptEngine engine = manager.getEngineByName("javascript");
            try {
                engine.eval("var res = " + js + ";");
                final String result = (String) engine.get("res");
                finallink = new Regex(result, "var link\\s*=\\s*(\"|')(.*?)\\1").getMatch(1);
            } catch (final Exception e) {
                logger.log(e);
            }
        }
        if (StringUtils.isEmpty(finallink)) {
            finallink = br.getRegex("href\\s*=\\s*\"(https?[^\"]+)\"\\s*role=\\s*\"button\"").getMatch(0);
        }
        if (StringUtils.isEmpty(finallink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            decryptedLinks.add(createDownloadlink(finallink));
            return decryptedLinks;
        }
    }
}
