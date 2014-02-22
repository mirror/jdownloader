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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wuala.com" }, urls = { "https://(www\\.)?www\\.wuala\\.com/[^<>\"/]+/[^<>\"/]+(/[^<>\"/]+)?" }, flags = { 0 })
public class WualaComFolder extends PluginForDecrypt {

    public WualaComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_SINGLE = "https?://(www\\.)?www\\.wuala\\.com/[^<>\"/]+/[^<>\"/]+/(?!\\?key=)[^<>\"/]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(TYPE_SINGLE)) {
            decryptedLinks.add(createDownloadlink(parameter.replace("wuala.com/", "wualadecrypted.com/")));
        } else {
            br.getHeaders().put("Accept", "*/*");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www.wuala.com/sharing/wuala-server-list");
            final String api_server = br.getRegex("\"apiservers\":\\[\"([a-z0-9]+)\"\\]\\}").getMatch(0);
            if (api_server == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            String parameters = "?il=0&ff=1";
            String link_part = new Regex(parameter, "wuala\\.com/(.+)").getMatch(0);
            final String key = new Regex(parameter, "\\?key=([A-Za-z0-9]+)").getMatch(0);
            if (key != null) {
                link_part = link_part.replace("?key=" + key, "");
                parameters += "&key=" + key;
            }
            br.getPage("https://" + api_server + ".wuala.com/previewSorted/" + link_part + parameters);
            if (br.getRequest().getHttpConnection().getResponseCode() == 404) {
                final DownloadLink offline = createDownloadlink("https://www.wualadecrypted.com/linkdown/linkdown/linkdown" + System.currentTimeMillis() + new Random().nextInt(10000000));
                offline.setAvailable(false);
                offline.setProperty("offline", true);
                offline.setName(new Regex(parameter, "wuala\\.com/(.+)").getMatch(0));
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<name>([^<>\"]*?)</name>").getMatch(0);
            final String[] items = br.getRegex("(<item contentType.*?</item>)").getColumn(0);
            if (items == null || items.length == 0 || fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String item : items) {
                final String name = new Regex(item, "name=\"([^<>]*?)\"").getMatch(0);
                final String filesize = new Regex(item, "size=\"(\\d+)\"").getMatch(0);
                String url = new Regex(item, "url=\"(https?://www\\.wuala\\.com/[^<>\"]*?)(/)?\"").getMatch(0);
                if (name == null || filesize == null || url == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                url = url.replace("http://", "https://").replace("wuala.com/", "wualadecrypted.com/");
                if (key != null) url += "?key=" + key;
                final DownloadLink dl = createDownloadlink(url);
                dl.setName(encodeUnicode(Encoding.htmlDecode(name.trim())));
                dl.setDownloadSize(Long.parseLong(filesize));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }
}
