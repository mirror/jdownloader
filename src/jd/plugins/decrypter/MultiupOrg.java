//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiup.org" }, urls = { "http://(www\\.)?multiup\\.org/(\\?lien=.{2,}|fichiers/download/.{2,})" }, flags = { 0 })
public class MultiupOrg extends PluginForDecrypt {

    public MultiupOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("The file does not exist any more\\.<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String quest = br.getRegex("name=\"data\\[Fichier\\]\\[indiceQuestion\\]\" value=\"(.*?)\"").getMatch(0);
        final Regex additionValues = br.getRegex("What is the result of (\\d+) \\+ (\\d+) :");
        final Regex multiplyValues = br.getRegex("What is the result of (\\d+) \\* (\\d+) : </th>");
        if (quest == null || (additionValues.getMatches().length < 1 && multiplyValues.getMatches().length < 1)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        int result = 0;
        if (additionValues.getMatches().length > 1) {
            result = (Integer.parseInt(additionValues.getMatch(0)) + Integer.parseInt(additionValues.getMatch(1)));
        } else {
            result = (Integer.parseInt(multiplyValues.getMatch(0)) * Integer.parseInt(multiplyValues.getMatch(1)));
        }
        if (br.containsHTML("(Sorry but your file does not exist or no longer exists|The file does not exist any more|It was deleted either further to a complaint or further to a not access for several weeks|<h2>Not Found</h2>)")) return decryptedLinks;
        Thread.sleep(1000l);
        br.postPage(br.getURL(), "_method=POST&data%5BFichier%5D%5Bsecurity_code%5D=" + result + "&data%5BFichier%5D%5BindiceQuestion%5D=" + quest);
        boolean decrypt = false;
        String[] links = br.getRegex("<a target=\"_blank\" onMouseDown=\"\" href=\"([^<>\"\\']+)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("href=\"([^<>\"\\']+)\">Download</a>").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("p\\}\\((.*?)\\.split\\('\\|'\\)").getColumn(0);
            decrypt = true;
        }
        if (links == null || links.length == 0) return null;
        int failCounter = 0;
        for (String singleLink : links) {
            String finallink = null;
            if (decrypt) {
                finallink = decodeLink(singleLink);
                if (finallink == null) {
                    failCounter++;
                    continue;
                }
            } else {
                finallink = singleLink;
            }
            if (!finallink.contains("multiup.org/")) decryptedLinks.add(createDownloadlink(finallink));
        }
        if (failCounter == links.length) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    private String decodeLink(String s) {
        String decoded = null;

        try {
            Regex params = new Regex(s, "\\'(.*?[^\\\\])\\',(\\d+),(\\d+),\\'(.*?)\\'");

            String p = params.getMatch(0).replaceAll("\\\\", "");
            int a = Integer.parseInt(params.getMatch(1));
            int c = Integer.parseInt(params.getMatch(2));
            String[] k = params.getMatch(3).split("\\|");

            while (c != 0) {
                c--;
                if (k[c].length() != 0) p = p.replaceAll("\\b" + Integer.toString(c, a) + "\\b", k[c]);
            }

            decoded = p;
        } catch (Exception e) {
            return null;
        }
        return new Regex(decoded, "<a target=\"[^\"\\']+\" href=\"(.*?)\"").getMatch(0);
    }
}
