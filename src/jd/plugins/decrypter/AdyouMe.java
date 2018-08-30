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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.components.PluginJSonUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "adyou.me" }, urls = { "https?://(?:\\w+\\.)?adyou\\.me/[a-zA-Z0-9]{4,}$" })
public class AdyouMe extends PluginForDecrypt {
    public AdyouMe(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String finallink = br.getRegex("message:\\s*\"Content from: (.*?) disallows").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("//top\\.location\\.href\\s*=\\s*\"(.*?)\";").getMatch(0);
            if (finallink == null) {
                // we can get via ajax also
                final String data = br.getRegex("data:\\s*(\\{.*?\\}),[\r\n]").getMatch(0);
                final String[] args = new Regex(data, "_args:\\s*\\{'([a-f0-9]{32})'\\s*:\\s*(\\d+)\\s*\\}").getRow(0);
                if (data != null) {
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    final String postarg = "_args[" + args[0] + "]=" + args[1] + "&benid=0";
                    Browser ajax = br.cloneBrowser();
                    ajax.postPage(br.getURL() + "/skip_timer", postarg);
                    sleep(6000l, param);
                    ajax = br.cloneBrowser();
                    ajax.postPage(br.getURL() + "/skip_timer", postarg);
                    finallink = PluginJSonUtils.getJsonValue(ajax, "url");
                }
            }
        }
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));
        return decryptedLinks;
    }
}
