//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anime-loads.org" }, urls = { "http://[\\w\\.]*?anime-loads\\.org/Weiterleitung/\\?cryptid=[0-9a-z]{32}|http://[\\w\\.]*?anime-loads\\.org/page.php\\?id=[0-9]+|http://[\\w\\.]*?anime-loads\\.org/media/\\d+" }, flags = { 0 })
public class NmLdsrg extends PluginForDecrypt {

    public NmLdsrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.anime-loads.org");
        String parameter = param.toString();

        br.setCookiesExclusive(true);
        if (parameter.contains("Weiterleitung")) {
            links.add(parameter);
        } else {
            br.getPage(parameter);
            Thread.sleep(200);
            br.getPage(parameter);
            String[] calls = br.getRegex("(\\||')([a-z0-9]{32})").getColumn(1);
            for (String call : calls) {
                links.add(getLink(call));
            }
            calls = br.getRegex("(http://[\\w\\.]*?anime-loads\\.org/[^/]*?/\\d+/[a-zA-Z]*?\\.0)").getColumn(0);
            for (String call : calls) {
                links.add(call);
            }
        }

        for (String link : links) {
            br.getPage(link);
            String dllink = Encoding.htmlDecode(br.getRegex("<meta http-equiv=\"refresh\" content=\"5; url=(.*?)\">").getMatch(0));
            DownloadLink dl = createDownloadlink(dllink);
            dl.setSourcePluginPasswordList(passwords);
            dl.setDecrypterPassword(passwords.get(0));
            decryptedLinks.add(dl);
        }
        if (links.size() > 0 && decryptedLinks.size() == 0) return null;
        return decryptedLinks;
    }

    public String getLink(String a) {
        String d = "";
        for (int i = a.length() - 1; i >= 0; i--) {
            d += a.charAt(i);
        }
        return "http://anime-loads.org/Weiterleitung/?cryptid=" + d;
    }

}
