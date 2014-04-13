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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pony.fm" }, urls = { "https?://(www\\.)?pony\\.fm/tracks/[a-z0-9\\-_]+" }, flags = { 0 })
public class PonyFm extends PluginForDecrypt {

    public PonyFm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "pony\\.fm/tracks/(\\d+)").getMatch(0);
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        br.getPage("https://pony.fm/api/web/tracks/" + fid + "?log=true");
        if (br.containsHTML("\"Track not found")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String song_name = getJson("title");
        final String linktext = br.getRegex("\"formats\":\\[(\\{.*?\\})\\]").getMatch(0);
        if (linktext == null || song_name == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = linktext.split("\\},\\{");
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String linkinfo : links) {
            final String url = getJson("url", linkinfo);
            final String fsize = getJson("size", linkinfo);
            final String ext = getJson("extension", linkinfo);
            final DownloadLink fina = createDownloadlink("directhttp://" + url.replace("\\", ""));
            fina.setFinalFileName(song_name + "." + ext);
            fina.setDownloadSize(SizeFormatter.getSize(fsize));
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }

        /* Add covers */
        final String covertext = br.getRegex("\"covers\":(\\{.*?\\})").getMatch(0);
        if (covertext != null) {
            final String[][] covers = new Regex(covertext, "\"([a-z0]+)\":\"(http[^<>\"]*?)\"").getMatches();
            if (covers != null && covers.length != 0) {
                for (final String linkinfo[] : covers) {
                    final String type = linkinfo[0];
                    final String url = linkinfo[1].replace("\\", "");
                    final String ext = url.substring(url.lastIndexOf("."));
                    final DownloadLink fina = createDownloadlink("directhttp://" + url.replace("\\", ""));
                    fina.setFinalFileName(song_name + "_cover_" + type + "." + ext);
                    fina.setAvailable(true);
                    decryptedLinks.add(fina);
                }
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(song_name);
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = br.getRegex("\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?([0-9\\.]+)").getMatch(1);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":([\t\n\r ]+)?\"([^<>\"]*?)\"").getMatch(1);
        return result;
    }

}
