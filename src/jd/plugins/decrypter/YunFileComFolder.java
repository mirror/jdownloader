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

import java.net.SocketTimeoutException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser.BrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yunfile.com" }, urls = { "http://(www\\.)?(page\\d+\\.)?(yunfile|filemarkets|yfdisk)\\.com/ls/[a-z0-9]+/([a-z0-9]+)?" }, flags = { 0 })
public class YunFileComFolder extends PluginForDecrypt {

    public YunFileComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("http://(www\\.)?(yunfile|filemarkets|yfdisk)\\.com/", "http://yunfile.com/");
        br.setCookie("http://yunfile.com/", "language", "en_us");
        br.setFollowRedirects(true);
        try {
            br.getPage(parameter);
        } catch (final BrowserException e) {
            logger.info("Decrypt failed (server error): " + parameter);
            return decryptedLinks;
        } catch (final SocketTimeoutException e) {
            logger.info("Decrypt failed (timeout): " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("\\[ The uploader has no shared file lists\\.\\! \\]")) {
            logger.info("This link contains no downloadable content: " + parameter);
            return decryptedLinks;
        }
        final String[] links = br.getRegex("\"(http://(www\\.)?(page\\d+\\.)?yunfile\\.com/file/[a-z0-9]+/[a-z0-9]+/?)\"").getColumn(0);
        final String[] folders = br.getRegex("(http://(www\\.)?(page\\d+\\.)?yunfile\\.com/ls/[a-z0-9]+/[a-z0-9]+)").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String singleLink : links)
                decryptedLinks.add(createDownloadlink(singleLink));
        }
        if (folders != null && folders.length != 0) {
            for (String singleFolder : folders)
                decryptedLinks.add(createDownloadlink(singleFolder));
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(new Regex(parameter, "http://(www\\.)?yunfile\\.com/ls/([a-z0-9]+)/([a-z0-9]+)?").getMatch(0));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}