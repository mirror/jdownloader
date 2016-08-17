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
import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "audioinbox.com" }, urls = { "http://(www\\.)?audioinbox\\.com/c/[A-Za-z0-9]+" }) 
public class AudioinboxCom extends PluginForDecrypt {

    public AudioinboxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String TYPE_NORMAL = "http://(www\\.)?audioinbox\\.com/c/[A-Za-z0-9]+";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().matches(TYPE_NORMAL)) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        final String json = br.getRegex("jp_container_1\" \\}, (\\[.*?\\]), \\{ play:").getMatch(0);
        String fpName = null;
        if (json == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final ArrayList<Object> ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json);
        for (final Object ressource : ressourcelist) {
            final LinkedHashMap<String, Object> singlemap = (LinkedHashMap<String, Object>) ressource;
            final String title = (String) singlemap.get("title");
            final String fid = (String) singlemap.get("song_id");
            final String url = (String) singlemap.get("link");
            final DownloadLink dl = createDownloadlink(url);
            dl.setLinkID(fid);
            dl.setName(title);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
