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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "workupload.com" }, urls = { "https?://(?:www\\.)?workupload\\.com/archive/[A-Za-z0-9]+" })
public class WorkuploadComFolder extends PluginForDecrypt {
    public WorkuploadComFolder(PluginWrapper wrapper) {
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
        String fpName = br.getRegex("<h1>([^<>\"]+)</h1>").getMatch(0);
        final String[] htmls = br.getRegex("<div class=\"frame\">.*?class=\"filedownload\"").getColumn(-1);
        if (htmls == null || htmls.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleHTML : htmls) {
            final String url = new Regex(singleHTML, "(/file/[^\"]+)").getMatch(0);
            final String filename = new Regex(singleHTML, "class=\"filename[^\"]*?\">\\s*?<p>([^<>\"]+)<").getMatch(0);
            final String filesize = new Regex(singleHTML, "class=\"filesize[^\"]*?\">([^<>\"]+)<").getMatch(0);
            if (url == null) {
                continue;
            }
            final DownloadLink dl = createDownloadlink("https://" + this.getHost() + url);
            boolean setAvailable = false;
            if (filename != null) {
                dl.setName(filename);
                setAvailable = true;
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
                setAvailable = true;
            }
            if (setAvailable) {
                dl.setAvailable(true);
            }
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
