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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixlr.com" }, urls = { "http://(www\\.)?mixlr\\.com/[a-z0-9\\-]+/[a-z0-9\\-]+" }, flags = { 0 })
public class MixlrCom extends PluginForDecrypt {

    public MixlrCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("<title>([^<>\"]*?) \\| Mixlr[\t\n\r ]+</title>").getMatch(0);
        final String jsarray = br.getRegex("var broadcasts = (\\[\\{.*?\\]);").getMatch(0);
        if (jsarray == null) {
            return null;
        }
        final ArrayList<Object> ressourcelist = (ArrayList) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(jsarray);
        for (final Object mobject : ressourcelist) {
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) mobject;
            final LinkedHashMap<String, Object> streams = (LinkedHashMap<String, Object>) entries.get("streams");
            final LinkedHashMap<String, Object> stream_http = (LinkedHashMap<String, Object>) streams.get("http");
            final String url = (String) stream_http.get("url");
            final String artist_username = (String) entries.get("username");
            final String title = (String) entries.get("title");
            if (url == null || artist_username == null || title == null) {
                return null;
            }
            final String filename = artist_username + " - " + title + ".mp3";
            final DownloadLink dl = createDownloadlink("directhttp://" + url);
            dl.setFinalFileName(filename);
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
