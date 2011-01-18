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

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "klipsiz.net" }, urls = { "http://[\\w\\.]*?klipsiz\\.net/(([a-zA-Z0-9-.]+|[a-z]{1}/[0-9]+)\\.php|mp3-dinle-indir/[0-9]+-[0-9]+/)" }, flags = { 0 })
public class LlpszNt extends PluginForDecrypt {

    public LlpszNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final Pattern manylinks = Pattern.compile("klipsiz\\.net/[a-z]{1}/[0-9]+\\.php");

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://www.klipsiz.net")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        if (Regex.matches(parameter, manylinks)) {
            String fpName = br.getRegex("\"componentheading\">(.*?)</div>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>(.*?)MP3'l").getMatch(0);
            }

            String[] links = br.getRegex("sectiontableentry.*?<a href=\"(.*?)\">").getColumn(0);
            if (links.length == 0) return null;
            progress.setRange(links.length);
            for (String cryptedlink : links) {
                br.getPage(cryptedlink);
                String decryptedlink = br.getRegex("addVariable\\('file'.*?(http.*?)'\\)").getMatch(0);
                if (decryptedlink == null) decryptedlink = br.getRegex("width='28px' align='right'><a href=\"(.*?)\"").getMatch(0);
                if (decryptedlink == null) return null;
                if (decryptedlink.equals("")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                decryptedlink = Encoding.urlDecode((decryptedlink), true);
                br.getPage(decryptedlink);
                String finallink = br.getRedirectLocation();
                if (finallink != null && finallink.contains("wrzuta.pl/sr")) {
                    br.getPage(finallink);
                    finallink = br.getRedirectLocation();
                }
                if (br.containsHTML("Nie odnaleziono pliku")) {
                    logger.warning("Found 1 offline link");
                } else if (finallink == null) {
                    logger.warning("There is a problem with the decrypter!");
                    return null;
                } else {
                    decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
                    progress.increase(1);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            String finallink = br.getRegex("<param name=\"zrodlo\" value=\"(.*?)\">").getMatch(0);
            if (finallink == null) return null;
            if (finallink.equals("")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            finallink = Encoding.urlDecode((finallink), true);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

}
