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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hypem.com" }, urls = { "http://(www\\.)?hypem\\.com/((track|item)/[a-z0-9]+|go/[a-z0-9]+/[A-Za-z0-9]+)" }, flags = { 0 })
public class HypemCom extends PluginForDecrypt {

    public HypemCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_redirect = "http://(www\\.)?hypem\\.com/go/[a-z0-9]+/[A-Za-z0-9]+";

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            try {
                decryptedLinks.add(this.createOfflinelink(parameter));
            } catch (final Throwable e) {
                /* Not available in old 0.9.581 Stable */
            }
            return decryptedLinks;
        }
        if (parameter.matches(type_redirect)) {
            final String finallink = br.getRedirectLocation();
            if (finallink == null || finallink.contains("hypem.com/")) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        } else {
            String js = br.getRegex("id=\"displayList\\-data\">(.*?)<script").getMatch(0);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(js);
            final ArrayList<Object> ressourcelist = (ArrayList) entries.get("tracks");
            entries = (LinkedHashMap<String, Object>) ressourcelist.get(0);
            final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            final String title = (String) entries.get("song");
            final String artist = (String) entries.get("artist");
            final String key = (String) entries.get("key");
            if (title == null || artist == null || key == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.getPage("http://hypem.com/serve/source/" + fid + "/" + key + "?_=" + System.currentTimeMillis());
            entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            String finallink = (String) entries.get("url");
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (!finallink.contains("soundcloud.com/")) {
                finallink = "directhttp://" + finallink;
            }
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setFinalFileName(artist + " - " + title + ".mp3");
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
