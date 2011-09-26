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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "anime-loads.org" }, urls = { "http://[\\w\\.]*?anime-loads\\.org/redirect/\\d+/[a-z0-9]+|http://[\\w\\.]*?anime-loads\\.org/media/\\d+" }, flags = { 0 })
public class NmLdsrg extends PluginForDecrypt {

    public NmLdsrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * Note: FilePackage gets overridden when crypt-it.com (link protection
     * service) used. Old posts + streaming links still get caught.
     */

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> links = new ArrayList<String>();
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.anime-loads.org");
        String parameter = param.toString();
        String fpName = null;
        br.setCookiesExclusive(true);
        if (parameter.contains("/redirect/")) {
            links.add(parameter);
        } else {
            br.getPage(parameter);
            if (br.containsHTML("Link existiert nicht")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            fpName = br.getRegex(":: (.*?)</span></h2>").getMatch(0);
            if (fpName == null) fpName = "No Title";
            String[] continueLinks = br.getRegex("\"(http://(www\\.)?anime-loads\\.org/redirect/\\d+/[a-z0-9]+)\"").getColumn(0);
            if (continueLinks == null || continueLinks.length == 0) return null;
            if (continueLinks != null && continueLinks.length != 0) {
                for (String singlelink : continueLinks) {
                    links.add(singlelink);
                }
            }
        }
        progress.setRange(links.size());
        for (String link : links) {
            br.getPage(link);
            String dllink = Encoding.htmlDecode(br.getRegex("<meta http-equiv=\"refresh\" content=\"5;URL=(.*?)\" />").getMatch(0));
            DownloadLink dl = createDownloadlink(dllink);
            dl.setSourcePluginPasswordList(passwords);
            dl.setDecrypterPassword(passwords.get(0));
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        if (links.size() > 0 && decryptedLinks.size() == 0) return null;
        return decryptedLinks;
    }

}
