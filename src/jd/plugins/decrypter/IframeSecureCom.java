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

import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "iframe-secure.com", "protect-iframe.com" }, urls = { "https?://(?:www\\.)?iframe\\-secure\\.com/embed/[a-z0-9]+", "https?://(?:www\\.)?protect\\-iframe\\.com/embed\\-[a-z0-9]+" })
public class IframeSecureCom extends antiDDoSForDecrypt {

    public IframeSecureCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        getPage(parameter);
        if ("iframe-secure.com".equals(getHost())) {
            getPage("iframe.php?u=" + fid);
            // some form
            final Form f = br.getForm(0);
            if (f != null) {
                submitForm(f);
            }
        }
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        if ("protect-iframe.com".equals(getHost())) {
            getPage("/embed/embed.php?u=" + fid);
        }
        final String finallink = getLink(getPacked());
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException(DecrypterException.PLUGIN_DEFECT);
        }
        decryptedLinks.add(createDownloadlink(Request.getLocation(finallink, br.getRequest())));
        return decryptedLinks;
    }

    private String getPacked() {
        final String js = br.getRegex("eval\\((function\\(p,a,c,k,e,d\\)[^\r\n]+\\))\\)").getMatch(0);
        final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(null);
        final ScriptEngine engine = manager.getEngineByName("javascript");
        String result = null;
        try {
            engine.eval("var res = " + js + ";");
            result = (String) engine.get("res");
        } catch (final Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getLink(final String result) {
        if (result == null) {
            return null;
        }
        final String finallink = new Regex(result, "window\\.location\\.replace\\('(.*?)'\\);").getMatch(0);
        return finallink;
    }
}
