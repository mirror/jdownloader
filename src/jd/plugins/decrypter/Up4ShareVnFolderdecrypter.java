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
import jd.http.Request;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "up.4share.vn" }, urls = { "http://(?:www\\.)?(?:up\\.)?4share\\.vn/(?:d|dlist)/[a-f0-9]{16}" }, flags = { 0 })
public class Up4ShareVnFolderdecrypter extends PluginForDecrypt {

    public Up4ShareVnFolderdecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("up.4share.vn/", "4share.vn/");
        br.setConnectTimeout(2 * 60 * 1000);
        br.getPage(parameter);
        if ((br.containsHTML(">Error: Not valid ID") && !br.containsHTML("up\\.4share\\.vn/f/")) || br.containsHTML("\\[Empty Folder\\]")) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setAvailable(false);
            decryptedLinks.add(offline);
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<b>Thư mục:\\s*(.*?)\\s*</b>").getMatch(0);
        final String[] filter = br.getRegex("<tr>\\s*<td>.*?</td></tr>").getColumn(-1);
        if (filter != null && filter.length > 0) {
            for (final String f : filter) {
                final String url = new Regex(f, "('|\")((?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/.*?))\\1").getMatch(1);
                if (url == null) {
                    continue;
                }
                String name = new Regex(f, ">\\s*([^<]+)\\s*</a>").getMatch(0);
                if (name == null) {
                    name = url.substring(url.lastIndexOf("/") + 1);
                }
                final String size = new Regex(f, ">\\s*(\\d+(?:\\.\\d+)?\\s*(?:B(?:yte)?|KB|MB|GB))\\s*<").getMatch(0);
                final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
                if (name != null) {
                    dl.setName(name.trim());
                    dl.setAvailableStatus(AvailableStatus.TRUE);
                }
                if (size != null) {
                    dl.setDownloadSize(SizeFormatter.getSize(size));
                }
                decryptedLinks.add(dl);
            }
        }
        if (decryptedLinks.isEmpty()) {
            // fail over
            final String[] links = br.getRegex("(?:https?://(?:up\\.)?4share\\.vn)?/(?:d/[a-f0-9]{16}|f/[a-f0-9]{16}/[^<>\"]{1,})").getColumn(-1);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links) {
                dl = Request.getLocation(dl, br.getRequest()) + dl;
                final String fid = new Regex(dl, "f/([a-f0-9]{16})/").getMatch(0);
                if (fid != null) {
                    final DownloadLink dll = createDownloadlink(dl);
                    try {
                        dll.setLinkID(fid);
                    } catch (Throwable e) {
                        /* Not available in old 0.9.581 Stable */
                        dll.setProperty("LINKDUPEID", fid);
                    }
                    decryptedLinks.add(dll);
                }
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}