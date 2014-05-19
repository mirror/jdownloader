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

import java.text.DecimalFormat;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fotolog.com.br" }, urls = { "http://(www\\.)?fotolog\\.com(\\.br)?/[a-z0-9\\-_]+/(\\d+|mosaic|archive)" }, flags = { 0 })
public class FotoLogComBr extends PluginForDecrypt {

    public FotoLogComBr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String domain = "http://(www\\.)?fotolog\\.com(\\.br)?";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // don't support calendar, always switch to Mosaic
        String parameter = param.toString().replace("/archive", "/mosaic");
        if (!parameter.endsWith("/")) {
            parameter += "/";
        }
        br.clearCookies(null);
        br.setCookie(getHost(parameter), "foto-lang", "en");
        br.getPage(parameter);
        if (br.containsHTML(">Error 404 :") || br.getRedirectLocation() != null || br.containsHTML(">Account closed or deactivated<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        if (parameter.matches(domain + "/[a-z0-9\\-_]+/\\d+/")) {
            // single image urls
            final String finallink = br.getRegex("itemprop=\"image\" content=\"(http://[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
        } else {
            // mosaic / album urls
            if (br.containsHTML(">Latest popular photos<")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            String fpName = br.getRegex("<title>([^<>\"]*?)\\- Fotolog</title>").getMatch(0);
            if (fpName == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            fpName = Encoding.htmlDecode(fpName.trim()).replace(":", "-");
            if (!parameter.endsWith("/")) {
                parameter += "/";
            }
            int counter = 1;
            final DecimalFormat df = new DecimalFormat("0000");
            int offset = 0;
            int maxOffset = 30;
            int picsPerPageMax = 30;
            int picsPerPageGrabbed = picsPerPageMax;
            final String lastPage = br.getRegex("/(\\d+)\">Last ›</a>").getMatch(0);
            if (lastPage != null) {
                maxOffset = Integer.parseInt(lastPage);
            }
            while (offset != (maxOffset + picsPerPageMax) && picsPerPageGrabbed == picsPerPageMax) {
                try {
                    if (this.isAbort()) {
                        logger.info("Decryption aborted by user: " + parameter);
                        return decryptedLinks;
                    }
                } catch (final Throwable e) {
                    // Not available in old 0.9.581 Stable
                }
                logger.info("Decrypting offset " + offset + " / " + maxOffset);
                if (offset > 0) {
                    br.getPage(parameter + offset + "/");
                }
                final String[] links = br.getRegex("class=\"wall_img_container\" href=\"http://[^<>\"]*?\"><img alt=\"[^<>\"]*?\" src=\"(http://[^<>\"]*?_t\\.jpg)\"").getColumn(0);
                if (links == null || links.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                } else {
                    for (final String singleLink : links) {
                        final DownloadLink dl = createDownloadlink("directhttp://" + singleLink.replace("_t.jpg", "_f.jpg"));
                        dl.setFinalFileName(fpName + "_" + df.format(counter) + ".jpg");
                        dl.setAvailable(true);
                        decryptedLinks.add(dl);
                        try {
                            distribute(dl);
                        } catch (final Throwable e) {
                            // Not available in old 0.9.581 Stable
                        }
                        counter++;
                    }
                    picsPerPageGrabbed = links.length;
                    offset += picsPerPageMax;
                    logger.info("Decrypted offset " + offset + " / " + maxOffset);
                }
            }
            if (!decryptedLinks.isEmpty()) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName);
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private String getHost(final String s) {
        final String t = new Regex(s, domain.replaceFirst("http://(www\\.)?", "")).getMatch(-1);
        return t;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}