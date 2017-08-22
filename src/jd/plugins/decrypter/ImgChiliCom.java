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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgchili.com" }, urls = { "http://((www\\.)?imgchili\\.(com|net)/show/|(i|t)\\d+\\.imgchili\\.(com|net)/)\\d+/[a-z0-9_\\.\\(\\)%\\-]+|http://(www\\.)?imgchili\\.(com|net)/album/[a-z0-9]{32}" })
public class ImgChiliCom extends PluginForDecrypt {
    public ImgChiliCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // http://((www\\.)?imgchili\\.(com|net)/show/|[it]\\d+\\.imgchili\\.(com|net)/)\\d+/[a-z0-9_\\.\\(\\)]+
    private static final String ALBUMLINK       = "http://(www\\.)?imgchili\\.(com|net)/album/[a-z0-9]{32}";
    private static final String SINGLEFINALLINK = "http://(www\\.)?[it]\\d+\\.imgchili\\.(com|net)/\\d+/[a-z0-9_\\.\\(\\)%\\-]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(ALBUMLINK)) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            br.setFollowRedirects(false);
            final DownloadLink offline = this.createOfflinelink(parameter);
            offline.setFinalFileName(new Regex(parameter, "([a-z0-9]+)$").getMatch(0));
            if (br.containsHTML("The album does not exist")) {
                logger.info("Link offline: " + parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            } else if (br.containsHTML("This album is empty\\. <br/>")) {
                logger.info("Link offline (empty album): " + parameter);
                decryptedLinks.add(offline);
                return decryptedLinks;
            }
            final String fpName = br.getRegex("<title>imgChili \\&raquo; ([^<>\"]*?)</title>").getMatch(0);
            final String[] thumbs = br.getRegex("<img src=\"(http://t\\d+\\.imgchili\\.net/\\d+/[A-Za-z0-9\\-_\\.\\(\\)%\\-]+\\.jpg)\"").getColumn(0);
            if (thumbs == null || thumbs.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : thumbs) {
                singleLink = "directhttp://" + singleLink.replace("http://t", "http://i");
                final DownloadLink dl = createDownloadlink(singleLink);
                dl.setProperty("Referer", parameter);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else if (parameter.matches(SINGLEFINALLINK)) {
            final String finallink = parameter.replace("http://t", "http://i");
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            final String ref = parameter.replaceFirst("http://(www\\.)?[it]\\d+\\.(imgchili\\.(com|net))", "http://$2/show").replaceFirst("(\\.jpe?g).*", "$1").replace("%28", "(").replace("%29", ")");
            dl.setContainerUrl(ref);
            dl.setProperty("Referer", ref);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        } else {
            parameter = "http://imgchili.net/show" + new Regex(parameter, "(com|net)(/show)?(/.+)").getMatch(2);
            param.setCryptedUrl(parameter);
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.containsHTML("does not exist\\. <|id=\"shaceship\"")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            String finallink = br.getRegex("onclick=\"scale\\(this\\);\"[\t\n\r ]+src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("id=\"show_image\"\\s*?src=\"(https?://i\\d+\\.imgchili\\.(com|net)/\\d+/[a-z0-9_\\.\\(\\)%\\-]+)\"").getMatch(0);
            }
            if (finallink == null) {
                finallink = br.getRegex("\"(https?://i\\d+\\.imgchili\\.(com|net)/\\d+/[a-z0-9_\\.\\(\\)%\\-]+)\"").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setProperty("Referer", parameter);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
}
