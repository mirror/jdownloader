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
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "wikifeet.com" }, urls = { "https?://(?:\\w+\\.)?wikifeetx?\\.com/[a-zA-Z0-9\\-\\_]+" })
public class WikifeetCom extends PluginForDecrypt {
    public WikifeetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static final String type_pic      = "https?://(?:\\w+\\.)?pics\\.wikifeetx?\\.com";
    public static final String type_wikifeet = "https?://(?:\\w+\\.)?wikifeetx?\\.com/[a-zA-Z0-9\\-\\_]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.getPage(parameter);
        if (isOffline(this.br)) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String cfName = this.br.getRegex("messanger.cfname = \\'(.*?)\\';").getMatch(0);
        if (cfName == null) {
            return null;
        }
        String title = cfName;
        title = Encoding.htmlDecode(title).trim();
        if (parameter.matches(type_wikifeet)) {
            final String gData = this.br.getRegex("messanger\\[\\'gdata\\'\\] = ([\\s\\S]*?);").getMatch(0);
            List<Object> data = (List<Object>) JavaScriptEngineFactory.jsonToJavaObject(gData);
            if (data.size() < 1 || cfName == null) {
                return null;
            }
            for (final Object entry : data) {
                Map<String, Object> entryMap = (Map<String, Object>) entry;
                String pid = (String) entryMap.get("pid");
                final String dlurl = "directhttp://http://pics.wikifeet.com/" + cfName + "-Feet-" + pid + ".jpg";
                final DownloadLink dl = this.createDownloadlink(dlurl);
                dl.setName(cfName + "_" + pid);
                dl.setAvailable(true);
                dl.setProperty("fid", cfName + pid);
                decryptedLinks.add(dl);
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(title.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    public static boolean isOffline(final Browser br) {
        return br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("id=thepics");
    }
}
