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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "http://(www\\.)?(uploaded|ul)\\.(to|net)/folder/[a-z0-9]+" }, flags = { 0 })
public class UploadedToFolder extends PluginForDecrypt {

    public UploadedToFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("ul.to/", "uploaded.net/");
        parameter = parameter.replace("uploaded.to/", "uploaded.net/");
        br.setFollowRedirects(true);
        br.setCookie("http://uploaded.net", "lang", "de");
        br.getPage(parameter);
        if (br.containsHTML("(title=\"enthaltene Dateien\" style=\"cursor:help\">\\(0\\)</span>|<i>enthält keine Dateien</i>)")) {
            logger.info("Folder empty: " + parameter);
            return decryptedLinks;
        }
        if (br.getURL().contains("uploaded.net/404") || br.containsHTML("(<h1>Seite nicht gefunden<br|>Error: 404<|<title>uploaded.*?\\- where your files have to be uploaded to</title>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String fpName = br.getRegex("<h1><a href=\"folder/[a-z0-9]+\">(.*?)</a></h1>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
        final String[] links = br.getRegex("\"(file/[a-z0-9]+)/from/").getColumn(0);
        final String[] folders = br.getRegex("\"(/folder/[a-z0-9]+)\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (final String dl : links)
                decryptedLinks.add(createDownloadlink("http://uploaded.net/" + dl));
        }
        if (folders != null && folders.length != 0) {
            final String fid = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
            for (final String dl : folders)
                if (!dl.contains(fid)) decryptedLinks.add(createDownloadlink("http://uploaded.net" + dl));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
