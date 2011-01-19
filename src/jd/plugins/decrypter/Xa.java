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
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "luxport.eu" }, urls = { "http://[\\w\\.]*?(ex|luxport)\\.(ua|eu|ru)/((view|get|load)/[0-9]+(.+)?|(view/[0-9]+\\?r=[0-9]+|view/[0-9]+\\?r=[0-9,]+))" }, flags = { 0 })
public class Xa extends PluginForDecrypt {

    public Xa(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(".*?fs\\d+.*?(ex|luxport)\\.(ua|eu|ru)/get/\\d+.*?")) {
            String filename = new Regex(parameter, "/get/\\d+/(.+)").getMatch(0);
            String finallink = new Regex(parameter, "(.*?/get/\\d+)").getMatch(0);
            DownloadLink finalDlLink = createDownloadlink("directhttp://" + finallink);
            if (filename != null) {
                filename = Encoding.htmlDecode(filename);
                finalDlLink.setFinalFileName(filename.trim());
            }
            /*
             * hosts only allow 1 chunk (max 2 but not with 0.95xx
             * downloadsystem
             */
            /* see DirectHTTP for possible properties */
            finalDlLink.setProperty("forcenochunk", true);
            decryptedLinks.add(finalDlLink);
        } else {
            parameter = parameter.replace("ex.ru", "luxport.eu");
            parameter = parameter.replace("ex.ua", "luxport.eu");
            parameter = parameter.replace("ex.eu", "luxport.eu");
            parameter = parameter.replace("luxport.ru", "luxport.eu");
            parameter = parameter.replace("luxport.ua", "luxport.eu");
            parameter = parameter.replace("/load/", "/view/");
            br.setFollowRedirects(false);
            br.getPage(parameter);
            // To get and check more than 1 redirect after another
            for (int i = 0; i <= 3; i++) {
                if (br.getRedirectLocation() != null && br.getRedirectLocation().equals("http://luxport.eu/")) {
                    logger.info("The following link was added but is offline: " + parameter);
                    throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
                }
                if (br.getRedirectLocation() != null) {
                    br.getPage(br.getRedirectLocation());
                    continue;
                }
                break;
            }
            if (br.getRedirectLocation() != null) return null;
            String fpName = br.getRegex("align='left' style='margin-right: 16px;' alt='(.*?)'").getMatch(0);
            if (fpName == null || fpName.trim().equals("kein titel")) fpName = br.getRegex("<h1>(.*?)</h1><br>").getMatch(0);
            if (parameter.contains("/view/")) {
                String[] linksandmd5 = br.getRegex("'(/get/[0-9]+.*?md5:[0-9a-z]+)'").getColumn(0);
                if (linksandmd5 != null && linksandmd5.length != 0) {
                    progress.setRange(linksandmd5.length);
                    for (String pagepiece : linksandmd5) {
                        String md5hash = new Regex(pagepiece, "md5:([a-z0-9]+)").getMatch(0);
                        String cryptedlink = new Regex(pagepiece, "(/get/[0-9]+)").getMatch(0);
                        br.getPage("http://www.luxport.eu" + cryptedlink);
                        String finallink = br.getRedirectLocation();
                        if (finallink == null) return null;
                        DownloadLink decryptedlink = createDownloadlink(finallink);
                        if (md5hash != null) {
                            decryptedlink.setMD5Hash(md5hash.trim());
                        }
                        // Errorhandling for offline links, adding them makes no
                        // sense!
                        if (!finallink.contains("http://luxport.eu/online")) {
                            decryptedLinks.add(decryptedlink);
                        }
                        progress.increase(1);
                    }

                } else {
                    String[] links = br.getRegex("'(/get/[0-9]+)'").getColumn(0);
                    if (links.length == 0) return null;
                    progress.setRange(links.length);
                    for (String cryptedlink : links) {
                        br.getPage("http://luxport.eu" + cryptedlink);
                        String finallink = br.getRedirectLocation();
                        if (finallink == null) return null;
                        decryptedLinks.add(createDownloadlink(finallink));
                        progress.increase(1);
                    }
                }
                if (fpName != null && !fpName.trim().equals("kein titel")) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(decryptedLinks);
                }

            } else {
                String finallink = br.getRedirectLocation();
                if (finallink == null) return null;
                decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            }
        }
        return decryptedLinks;
    }
}
