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
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "freedisc.pl" }, urls = { "http://(www\\.)?freedisc\\.pl/[A-Za-z0-9_\\-]+,d\\-\\d+([A-Za-z0-9_,\\-]+)?" }) 
public class FreeDiscPlFolder extends PluginForDecrypt {

    public FreeDiscPlFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FOLDER = "http://(www\\.)?freedisc\\.pl/[A-Za-z0-9\\-_]+,d-\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        final String fpName = br.getRegex(">([^>]+)</h1>").getMatch(0);
        final String[] entries = br.getRegex("div class=\"dir-item\"><div.*?</div></div></div>").getColumn(-1);
        final String fileEntry = "class=('|\"|)[\\w -]+\\1><a href=\"(/[^<>\"]*?,f\\-[^<>\"]*?)\"[^>]*>(.*?)</a>";
        final String folderEntry = "class=('|\"|)[\\w -]+\\1><a href=\"(/?[A-Za-z0-9\\-_]+,d\\-\\d+[^<>\"]*?)\"";
        if (entries != null && entries.length > 0) {
            for (final String e : entries) {
                final String folder = new Regex(e, folderEntry).getMatch(1);
                if (folder != null) {
                    decryptedLinks.add(createDownloadlink(Request.getLocation(folder, br.getRequest())));
                    continue;
                }
                final String link = new Regex(e, fileEntry).getMatch(1);
                final String filename = new Regex(e, fileEntry).getMatch(2);
                final String filesize = new Regex(e, "info\">Rozmiar :(.*?)<").getMatch(0);
                final DownloadLink dl = createDownloadlink(Request.getLocation(link, br.getRequest()));
                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize.replace("Bajty", "Bytes")));
                dl.setAvailableStatus(AvailableStatus.TRUE);
                decryptedLinks.add(dl);
            }
        } else {
            // fail over
            final String[] links = br.getRegex(fileEntry).getColumn(0);
            final String[] folders = br.getRegex(folderEntry).getColumn(1);
            if ((links == null || links.length == 0) && (folders == null || folders.length == 0) && br.containsHTML("class=\"directoryText previousDirLinkFS\"")) {
                decryptedLinks.add(createOfflinelink(parameter));
                return decryptedLinks;
            }
            if (links != null && links.length > 0) {
                for (String singleLink : links) {
                    singleLink = Request.getLocation(singleLink, br.getRequest());
                    decryptedLinks.add(createDownloadlink(singleLink));
                }
            }
            if (folders != null && folders.length > 0) {
                for (final String singleLink : folders) {
                    decryptedLinks.add(createDownloadlink("http://freedisc.pl" + singleLink));
                }
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}