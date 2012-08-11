//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

// this plugin supports 'folders' and 'other files of this uploader'.

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crocko.com" }, urls = { "https?://(www\\.)?(easy\\-share|crocko)\\.com/(f/[A-Z0-9]+/(.+)?|o/[0-9]+)" }, flags = { 0 })
public class SShrFldr extends PluginForDecrypt {

    public SShrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        parameter = parameter.replaceFirst("easy-share", "crocko");
        br.setCookie("http://www.crocko.com", "language", "en");
        br.setCustomCharset("utf-8");
        br.getPage(parameter);
        if (br.containsHTML(">Error 404: Page not found<|No files in this folder|Folder not found|>the page you\\'re looking for <")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Either incorrect URL, folder or owner no longer exists, empty folder."));
        if (br.containsHTML("Please wait a few seconds and try again")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Server error, please try again."));
        if (br.containsHTML("<label>Password:</label> <input")) {
            for (int i = 0; i <= 3; i++) {
                final String passCode = getUserInput("Enter password for: " + parameter, param);
                br.postPage(br.getURL(), "f=%7Bf%7D&pass=" + passCode);
                if (br.containsHTML("<label>Password:</label> <input")) continue;
                break;
            }
            if (br.containsHTML("<label>Password:</label> <input")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        final String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
        String[] links = br.getRegex("class=\"w331 fl h18 l \"><a href=\"(.*?)\">").getColumn(0);
        if (links == null || links.length == 0) {
            links = br.getRegex("\"(https?://(www\\.)?crocko\\.com/f/[A-Z0-9]+/[^\"\\'<>]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                links = br.getRegex("class=\"last\"><a href=\"(.*?)\">").getColumn(0);
            }
        }
        String[] folders = br.getRegex("\"(https?://(www\\.)?crocko\\.com/f/[A-Z0-9]+)/.+\"").getColumn(0);
        if ((links == null || links.length == 0) && (folders == null || folders.length == 0)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (links != null && links.length != 0) {
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
        }
        if (folders != null && folders.length != 0) {
            final String id = new Regex(parameter, ".*?crocko\\.com/(f|o)/([A-Z0-9]+)").getMatch(1);
            for (String aFolder : folders)
                if (!aFolder.contains(id)) decryptedLinks.add(createDownloadlink(aFolder));
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
