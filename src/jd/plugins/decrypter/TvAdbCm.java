//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tv.adobe.com" }, urls = { "http://(www\\.)?tv\\.adobe\\.com/watch/[a-z0-9\\-]+/[a-z0-9\\-]+/?" }) 
public class TvAdbCm extends PluginForDecrypt {

    // dev notes
    // final links seem to not have any session info bound, nor restricted to IP and are hotlinkable, hoster plugin not required.

    /**
     * @author raztoki
     * */
    public TvAdbCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String url_name = new Regex(parameter, "adobe\\.com/watch/(.+)").getMatch(0).replace("/", "_");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404 || !br.containsHTML("/player/player\\.swf")) {
            // No need to randomise URL when exporting offline links!
            // Please always use directhttp && property OFFLINE from decrypters! Using 'OFFLINE' property, ensures when the user checks
            // online status again it will _always_ return offline status.
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "([a-z0-9\\-]+)/?$").getMatch(0) + ".mp4");
            offline.setProperty("OFFLINE", true);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String embedurl = br.getRegex("tv\\.adobe\\.com/embed/([^<>\"]*?)\"").getMatch(0);
        if (embedurl == null) {
            return null;
        }
        br.getPage("https://tv.adobe.com/embed/" + embedurl);
        final String html5player = br.getRegex("var bridge = (\\{.*?\\});").getMatch(0);
        if (html5player == null) {
            return null;
        }
        // parse for qualities
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(html5player);
        final ArrayList<Object> sources = (ArrayList) entries.get("sources");
        String name = (String) entries.get("title");
        if (name == null) {
            name = url_name;
        }
        for (final Object videoo : sources) {
            entries = (LinkedHashMap<String, Object>) videoo;
            final String q = Long.toString(JavaScriptEngineFactory.toLong(entries.get("bitrate"), -1));
            final String u = (String) entries.get("fsrc");
            if (q == null || u == null || !u.startsWith("http")) {
                continue;
            }
            final DownloadLink dl = createDownloadlink("directhttp://" + u);
            dl.setFinalFileName(name + " - " + q + u.substring(u.lastIndexOf(".")));
            decryptedLinks.add(dl);
        }
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

}