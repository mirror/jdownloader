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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "embedy.cc" }, urls = { "https?://(?:www\\.)?embedy\\.cc/movies/[A-Za-z0-9=]+" })
public class EmbedyCc extends PluginForDecrypt {
    public EmbedyCc(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String linkid = new Regex(parameter, "/movies/(.+)").getMatch(0);
        this.br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !this.br.getURL().contains("/movies/")) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
        if (fpName == null) {
            fpName = linkid;
        }
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.postPage("https://" + this.getHost() + "/video.get/", "video=" + Encoding.urlEncode(linkid));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "response/1/files");
        final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            final Entry<String, Object> ipentry = it.next();
            final String quality = ipentry.getKey();
            final String url = (String) ipentry.getValue();
            if (url == null || !url.startsWith("http") || quality == null) {
                continue;
            }
            final DownloadLink dl;
            if (url.contains(this.getHost())) {
                dl = createDownloadlink("directhttp://" + url);
                /* Hoster by embedy.cc and/or vk.com[vk.me] */
                dl.setFinalFileName(fpName + "_" + quality + ".mp4");
                dl.setAvailable(true);
                dl.setProperty("refURL", parameter);
                dl.setProperty("requestType", "GET");
            } else {
                dl = createDownloadlink(url);
            }
            decryptedLinks.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}
