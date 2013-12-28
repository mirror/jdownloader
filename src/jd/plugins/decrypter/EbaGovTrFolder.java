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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "eba.gov.tr" }, urls = { "http://(www\\.)?eba\\.gov\\.tr/(video/(?!izle/)[a-z0-9\\-_]+|dergi/goster/\\d+)" }, flags = { 0 })
public class EbaGovTrFolder extends PluginForDecrypt {

    public EbaGovTrFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String FILELINK = "http://(www\\.)?eba\\.gov\\.tr/dergi/goster/\\d+";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        if (parameter.matches(FILELINK)) {
            final DownloadLink main = createDownloadlink(parameter.replace("eba.gov.tr/", "ebadecrypted.gov.tr/"));
            String filename = null;
            final FilePackage fp = FilePackage.getInstance();
            try {
                br.getPage(parameter);
                if (br.containsHTML(">Aradığınız Sayfa Bulunamadı<|>Bu sayfa kaldırılmış olabilir\\.<")) {
                    main.setAvailable(false);
                } else {
                    final String titlefirst = br.getRegex("class=\"active\">([^<>\"]*?)</li>").getMatch(0);
                    filename = br.getRegex("<h3>[^<>\"]+<small>([^<>\"]*?)</small></h3>").getMatch(0);
                    if (titlefirst != null && filename != null) {
                        filename = Encoding.htmlDecode(titlefirst.trim() + " - " + Encoding.htmlDecode(filename.trim()));
                        main.setName(filename);
                        fp.setName(filename);
                        main._setFilePackage(fp);
                        main.setAvailable(true);
                    }
                }
            } catch (final Throwable e) {
            }
            decryptedLinks.add(main);
            final String coverbig = br.getRegex("<ul class=\"media\\-grid\">[\t\n\r ]+<li>[\t\n\r ]+<a href=\"(https?://[^<>\"]*?\\.PNG)\"").getMatch(0);
            if (coverbig != null) {
                final DownloadLink cover = createDownloadlink("directhttp://" + coverbig);
                if (filename != null) {
                    String coverfilename = null;
                    String oldext = null;
                    if (filename.contains(".")) oldext = filename.substring(filename.lastIndexOf("."));
                    if (oldext != null) {
                        coverfilename = filename.replace(oldext, ".png");
                    } else {
                        coverfilename = filename + ".png";
                    }
                    cover.setFinalFileName(coverfilename);
                    cover._setFilePackage(fp);
                    cover.setAvailable(true);
                    decryptedLinks.add(cover);
                }
            }
        } else {
            br.getPage(parameter);
            if (br.containsHTML("class=\"alert\\-message warning\"")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<h1>Video <small>([^<>\"]*?)</small></h1>").getMatch(0);
            if (fpName == null || fpName.equals("")) fpName = new Regex(parameter, "([a-z0-9\\-_]+)").getMatch(0);
            int maxPage = 1;
            final String[] pages = br.getRegex("\"/video/[A-Za-z0-9\\-_]+/\\d+\">(\\d+)</a>").getColumn(0);
            if (pages != null && pages.length != 0) {
                for (final String apage : pages) {
                    final int currentint = Integer.parseInt(apage);
                    if (currentint > maxPage) maxPage = currentint;
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            for (int i = 1; i <= maxPage; i++) {
                logger.info("Decrypting page " + i + " of " + maxPage);
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption process aborted at page " + i + " of " + maxPage);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                if (i > 1) {
                    br.getPage(parameter + "/" + i);
                }
                final String[][] linkInfo = br.getRegex("\"(/video/izle/[a-z0-9]+)\" title=\"[^<>\"]+\">[\t\n\r ]+<h5>([^<>\"]*?)</h5>").getMatches();
                if (linkInfo == null || linkInfo.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                for (final String singleLinkInfo[] : linkInfo) {
                    final DownloadLink dl = createDownloadlink("http://www.eba.gov.tr" + singleLinkInfo[0]);
                    dl.setName(Encoding.htmlDecode(singleLinkInfo[1].trim()) + ".mp4");
                    dl.setAvailable(true);
                    dl._setFilePackage(fp);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        // Not available in old 0.9.581 Stable
                    }
                    decryptedLinks.add(dl);
                }
            }
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}