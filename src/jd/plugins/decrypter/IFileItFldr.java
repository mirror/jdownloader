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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecloud.io" }, urls = { "http://(www\\.)?(ifile\\.it|filecloud\\.io)/_[a-z0-9]+" }, flags = { 0 })
public class IFileItFldr extends PluginForDecrypt {

    public IFileItFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    // TODO: Implement API: http://code.google.com/p/filecloud/wiki/FetchTagDetails
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("ifile.it/", "filecloud.io/");
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost("filecloud.io");
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        String fpName = null;
        // Id we have an account we can use the API, if not we have to do it over the site
        if (aa != null) {
            final String akey = ((jd.plugins.hoster.IFileIt) hostPlugin).getUrlEncodedAPIkey(aa, hostPlugin, br);
            br.postPage("http://api.filecloud.io/api-fetch_tag_details.api", "akey=" + akey + "&tkey=" + new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
            fpName = br.getRegex("\"name\":\"([^<>\"]*?)\"").getMatch(0);
            final String[][] linkinformation = br.getRegex("\"size\":\"(\\d+)\",\"name\":\"([^<>\"]*?)\",\"ukey\":\"([^<>\"]*?)\"").getMatches();
            if (linkinformation == null || linkinformation.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String[] info : linkinformation) {
                final DownloadLink dl = createDownloadlink("http://filecloud.io/" + info[2]);
                dl.setAvailable(true);
                dl.setDownloadSize(SizeFormatter.getSize(info[0].trim()));
                dl.setName(info[1].trim());
                decryptedLinks.add(dl);
            }
        } else {
            br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
            br.setCookie("http://filecloud.io/", "lang", "en");
            br.getPage(parameter);
            if (br.containsHTML(">no such tag")) {
                logger.info("Invalid/Offline folderlink: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex("<title>([^<>\"]*?) \\- filecloud\\.io</title>").getMatch(0);
            String[][] linkinformation = br.getRegex("\"size\":\"(\\d+)\",\"name\":\"([^<>\"]*?)\",\"ukey\":\"([^<>\"]*?)\"").getMatches();
            boolean fail = false;
            if (linkinformation == null || linkinformation.length == 0) {
                fail = true;
                linkinformation = br.getRegex("ukey\":\"([^<>\"]*?)\"").getMatches();
            }
            if (linkinformation == null || linkinformation.length == 0) {
                if (br.containsHTML(">this tag belongs to another user<")) {
                    logger.info("No links to decrypt: " + parameter);
                    return decryptedLinks;
                }
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String[] info : linkinformation) {
                if (fail) {
                    decryptedLinks.add(createDownloadlink("http://filecloud.io/" + info[0]));
                } else {
                    final DownloadLink dl = createDownloadlink("http://filecloud.io/" + info[2]);
                    dl.setAvailable(true);
                    dl.setDownloadSize(SizeFormatter.getSize(info[0].trim()));
                    dl.setName(info[1].trim());
                    decryptedLinks.add(dl);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

}
