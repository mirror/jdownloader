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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "klipsiz.org" }, urls = { "http://(www\\.)?klipsiz\\.(net|org)/(?!index\\.php|radyo\\-listeleri\\.php)(([a-zA-Z0-9-.]+|[a-z]{1}/[0-9]+)\\.php|mp3-dinle-indir/[0-9]+-[0-9]+/)" }, flags = { 0 })
public class LlpszNt extends PluginForDecrypt {

    public LlpszNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final Pattern manylinks = Pattern.compile("klipsiz\\.net/[a-z]{1}/[0-9]+\\.php");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("klipsiz.net/", "klipsiz.org/");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://www.klipsiz.net") || br.containsHTML("(>404: Not Found<|<title>Bedava Yabancı Müzik Mp3 dinle indir şarkı sözü lyrics \\- klipsiz\\.org\\&nbsp;\\-\\&nbsp;Bedava Yabancı Müzik Mp3 dinle indir şarkı sözü lyrics \\- klipsiz\\.org</title>)")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        String[] links = br.getRegex("<p class=\"MsoNormal\"><strong><a href=\"([^<>\"/]*?)\"").getColumn(0);
        if (links != null && links.length != 0) {
            final String fpName = br.getRegex("<h1 style=\"float:left;\">([^<>\"]*?)</h1>").getMatch(0);
            if (links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String cryptedlink : links) {
                br.getPage("http://www.klipsiz.org/" + cryptedlink);
                String finallink = decryptSingleLink();
                if (finallink == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            String finallink = decryptSingleLink();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    private String decryptSingleLink() {
        String finallink = br.getRegex("<param name=\"zrodlo\" value=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (finallink != null) finallink = Encoding.urlDecode((finallink), true);
        return finallink;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}