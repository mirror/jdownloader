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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "imgchili.com" }, urls = { "http://((www\\.)?imgchili\\.(com|net)/show/|t\\d+\\.imgchili\\.(com|net)/)\\d+/[a-z0-9_\\.\\(\\)]+|http://(www\\.)?imgchili\\.(com|net)/album/[a-z0-9]{32}" }, flags = { 0 })
public class ImgChiliCom extends PluginForDecrypt {

    public ImgChiliCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // http://((www\\.)?imgchili\\.(com|net)/show/|[it]\\d+\\.imgchili\\.(com|net)/)\\d+/[a-z0-9_\\.\\(\\)]+

    private static final String ALBUMLINK       = "http://(www\\.)?imgchili\\.(com|net)/album/[a-z0-9]{32}";
    private static final String SINGLEFINALLINK = "http://(www\\.)?[it]\\d+\\.imgchili\\.(com|net)/\\d+/[a-z0-9_\\.\\(\\)]+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(ALBUMLINK)) {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            br.setFollowRedirects(false);

            if (br.containsHTML("The album does not exist")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }

            final String fpName = br.getRegex("<title>imgChili \\&raquo; ([^<>\"]*?)</title>").getMatch(0);
            final String[] thumbs = br.getRegex("<img src=\"(http://t\\d+\\.imgchili\\.net/\\d+/[A-Za-z0-9\\-_\\.\\(\\)]+\\.jpg)\"").getColumn(0);
            if (thumbs == null || thumbs.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String singleLink : thumbs) {
                singleLink = "directhttp://" + singleLink.replace("http://t", "http://i");
                final DownloadLink dl = createDownloadlink(singleLink);
                final String finalfilename = new Regex(singleLink, "imgchili\\.net/\\d+/\\d+_(.+)").getMatch(0);
                if (finalfilename != null) dl.setFinalFileName(finalfilename);
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
            final String finalfilename = new Regex(finallink, "imgchili\\.net/\\d+/\\d+_(.+)").getMatch(0);
            final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setFinalFileName(finalfilename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        } else {
            parameter = "http://imgchili.com/show" + new Regex(parameter, "(com|net)(/show)?(/.+)").getMatch(2);
            param.setCryptedUrl(parameter);
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.containsHTML("does not exist\\. <")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String finallink = br.getRegex("onclick=\"scale\\(this\\);\"[\t\n\r ]+src=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("\"(http://i\\d+\\.imgchili\\.(com|net)/\\d+/[a-z0-9_]+\\.[a-z]{1,5})\"").getMatch(0);
            }
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final String finalfilename = new Regex(parameter, "imgchili\\.(com|net)/show/\\d+/(.+)").getMatch(1);
            final DownloadLink dl = createDownloadlink(finallink);
            dl.setFinalFileName(finalfilename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }

        return decryptedLinks;
    }

}
