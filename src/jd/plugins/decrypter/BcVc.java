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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bc.vc" }, urls = { "http://(www\\.)?bc\\.vc/(?!advertising)([A-Za-z0-9\\-]+)?" }, flags = { 0 })
public class BcVc extends PluginForDecrypt {

    public BcVc(PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * Important note: Via browser the videos are streamed via RTMP (maybe even in one part) but with this method we get HTTP links which is
     * fine.
     */
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.getURL().equals("http://bc.vc/") || br.containsHTML("top\\.location\\.href = \"http://bc\\.vc/\"") || br.containsHTML(">404 Not Found<") || br.containsHTML(">Sorry the page you are looking for does not exist")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Unable to connect to database")) {
            logger.info("Link can't be decrypted because of server problems: " + parameter);
            return decryptedLinks;
        }
        final String[] matches = br.getRegex("aid\\:(.*?)\\,lid\\:(.*?)\\,oid\\:(.*?)\\,ref\\: ?\\'(.*?)\\'\\}").getRow(0);
        if (matches == null || matches.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        LinkedHashMap<String, String> data = new LinkedHashMap<String, String>();
        data.put("opt", "check_log");
        data.put(Encoding.urlEncode("args[aid]"), matches[0]);
        data.put(Encoding.urlEncode("args[lid]"), matches[1]);
        data.put(Encoding.urlEncode("args[oid]"), matches[2]);
        data.put(Encoding.urlEncode("args[ref]"), "");

        br.postPage("http://bc.vc/fly/ajax.fly.php", data);
        // waittime is 5 seconds. but somehow this often results in an error.
        // we use 5.5 seconds to avoid them
        Thread.sleep(5500);

        data.put("opt", "make_log");
        br.postPage("http://bc.vc/fly/ajax.fly.php", data);
        String url = br.getRegex("\"url\"\\:\"(.*)\"").getMatch(0);
        if (url == null) {
            // maybe we have to wait even longer?
            Thread.sleep(2000);
            br.postPage("http://bc.vc/fly/ajax.fly.php", data);
            url = br.getRegex("\"url\"\\:\"(.*)\"").getMatch(0);
        }

        url = url.replace("\\", "");
        decryptedLinks.add(createDownloadlink(url));
        return decryptedLinks;
    }

    private String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}