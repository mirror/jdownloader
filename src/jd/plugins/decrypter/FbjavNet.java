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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fbjav.net" }, urls = { "https?://(www\\.)?fbjav\\.(?:net|com)/\\w+-\\d+[^/]*" })
public class FbjavNet extends antiDDoSForDecrypt {
    public FbjavNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        getPage(parameter);
        String fpName = null;
        fpName = br.getRegex("<title>\\s*([^<]+)\\s*").getMatch(0);
        String[][] videoSources = br.getRegex("eid\\s*=\\s*[\"']*(\\d+)[\"']*\\s+dtl\\s*=\\s*[\"']*(\\w+)[\"']*").getMatches();
        if (videoSources != null && videoSources.length > 0) {
            final ScriptEngine engine = JavaScriptEngineFactory.getScriptEngineManager(null).getEngineByName("javascript");
            final String decodeJS = getDecryptJS(br);
            engine.eval(decodeJS);
            for (String[] videoSource : videoSources) {
                String eid = videoSource[0];
                String dtl = videoSource[1];
                try {
                    engine.eval("var res = link_decode(\"" + dtl + "\");");
                    String result = engine.get("res").toString();
                    decryptedLinks.add(createDownloadlink(result));
                } catch (Exception e) {
                    getLogger().log(e);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, null, e);
                }
            }
        }
        if (StringUtils.isNotEmpty(fpName)) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String getDecryptJS(Browser br) throws Exception {
        final StringBuilder str = new StringBuilder();
        final Browser brc = br.cloneBrowser();
        brc.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        final String scriptURL = "https://static.fbjav.com/wp-content/themes/fbjav/assets/js/custom28919.js";
        getPage(brc, scriptURL);
        str.append(brc.getRegex("(function\\s*reverse\\s*\\(\\s*s\\s*\\)\\s*\\{[^§]+return link;\\s*\\})").getMatch(0));
        str.append("\r\n");
        str.append(brc.getRegex("(var\\s*Base64\\s*=\\s*\\{[^$]+\\}\\s*;)").getMatch(0));
        final String result = str.toString().trim().replace("window.location.host", "\"" + brc.getHost() + "\"");
        if (StringUtils.isEmpty(result)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return result;
    }
}