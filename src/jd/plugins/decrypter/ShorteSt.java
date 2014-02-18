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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shorte.st" }, urls = { "http://(www\\.)?sh\\.st/[a-z0-9]+" }, flags = { 0 })
public class ShorteSt extends PluginForDecrypt {

    public ShorteSt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">page not found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String timer = getJs(br, "seconds");
        final String cb = getJs(br, "callbackUrl");
        final String sid = getJs(br, "sessionId");
        if (cb == null || sid == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int t = 5;
        if (timer != null) t = Integer.parseInt(timer);
        sleep(t * 1001, param);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("Accept", "application/json, text/javascript");
        br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.postPage(cb, "sessionId=" + sid + "&browserToken=" + new Regex(String.valueOf(new Random().nextLong()), "(\\d{10})$").getMatch(0));
        String finallink = getJs(br2, "destinationUrl");
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        finallink = finallink.replaceAll("\\\\/", "/");
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    private String getJs(Browser ibr, String s) {
        // js
        String test = ibr.getRegex(s + ":\\s*(\"|')(.*?)\\1").getMatch(1);
        // json(finallink)
        if (test == null) test = ibr.getRegex("\"" + s + "\":\"(.*?)\"").getMatch(0);
        // int/long/boolean
        if (test == null) test = ibr.getRegex(s + ":\\s*(\\d+|true|false)").getMatch(0);
        return test;
    }

}
