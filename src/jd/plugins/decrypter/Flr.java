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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filer.net" }, urls = { "http://(www\\.)?filer.net/folder/[a-z0-9]+" }, flags = { 0 })
public class Flr extends PluginForDecrypt {

    public Flr(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", "JDownloader");
        final String folderID = new Regex(parameter, "([a-z0-9]+)$").getMatch(0);
        br.getPage("http://api.filer.net/api/folder/" + folderID + ".json");
        if (getJson("code", br.toString()).equals("506")) {
            logger.info("Folderlink offline: " + parameter);
            return decryptedLinks;
        }
        if (getJson("count", br.toString()).equals("0")) {
            logger.info("Folderlink empty: " + parameter);
            return decryptedLinks;
        }
        if (getJson("count", br.toString()).equals("599")) {
            logger.info("unknown file error for link: " + parameter);
            return decryptedLinks;
        }
        if (getJson("code", br.toString()).equals("201")) {
            for (int i = 1; i <= 3; i++) {
                final String passCode = getUserInput("Password?", param);
                br.getPage("http://api.filer.net/api/folder/" + folderID + ".json?password=" + Encoding.urlEncode(passCode));
                if (getJson("code", br.toString()).equals("201")) continue;
                break;
            }
            if (getJson("code", br.toString()).equals("201")) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String fpName = getJson("name", br.toString());
        if (fpName == null) fpName = "filer.net folder: " + folderID;
        final String allLinks = br.getRegex("\"files\":\\[(.*?)\\]").getMatch(0);
        final String[] linkInfo = new Regex(allLinks, "\\{(.*?)\\}").getColumn(0);
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Decrypter broken for link:" + parameter);
            return decryptedLinks;
        }

        for (final String singleLinkInfo : linkInfo) {
            final DownloadLink link = createDownloadlink(getJson("link", singleLinkInfo).replace("\\", ""));
            link.setFinalFileName(getJson("name", singleLinkInfo).replace("\\", ""));
            link.setDownloadSize(SizeFormatter.getSize(getJson("size", singleLinkInfo).replace("\\", "")));
            link.setAvailable(true);
            decryptedLinks.add(link);
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}