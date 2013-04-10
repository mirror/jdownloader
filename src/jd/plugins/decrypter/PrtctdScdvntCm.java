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
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protected.socadvnet.com" }, urls = { "http://(www\\.)?protected\\.socadvnet\\.com/\\?[a-z0-9-]+" }, flags = { 0 })
public class PrtctdScdvntCm extends PluginForDecrypt {

    private String MAINPAGE = "http://protected.socadvnet.com/";

    public PrtctdScdvntCm(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /*
     * At the moment this decrypter only decrypts: turbobit.net, hotfile.com links as "protected.socadvnet.com" only allows crypting links
     * of this host!
     */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setDebug(true);
        final String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        final String postvar = new Regex(parameter, "protected\\.socadvnet\\.com/\\?(.+)").getMatch(0);
        if (postvar == null) { return null; }
        br.getPage(parameter);
        if ((MAINPAGE + "index.php").equals(br.getRedirectLocation())) { throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore.")); }
        if (br.getRedirectLocation() != null) {
            br.getPage(br.getRedirectLocation());
        }
        final String cpPage = br.getRegex("\"(plugin/.*?)\"").getMatch(0);
        final String sendCaptcha = "ksl.php";
        final String getList = "llinks.php";
        br.postPage(MAINPAGE + getList, "LinkName=" + postvar);
        final String[] linksCount = br.getRegex("(moc\\.tenvdacos\\.detcetorp//:ptth)").getColumn(0);
        if (linksCount == null || linksCount.length == 0) { return null; }
        final int linkCounter = linksCount.length;
        if (cpPage != null) {
            for (int i = 0; i <= 3; i++) {
                final String equals = this.getCaptchaCode(MAINPAGE + cpPage, param);
                br.postPage(MAINPAGE + sendCaptcha, "res_code=" + equals);
                if (!br.toString().trim().equals("1") && !br.toString().trim().equals("0")) {
                    logger.warning("Error in doing the maths for link: " + parameter);
                    return null;
                }
                if (!br.toString().trim().equals("1")) {
                    continue;
                }
                break;
            }
            if (!br.toString().trim().equals("1")) { throw new DecrypterException(DecrypterException.CAPTCHA); }
        }
        logger.info("Found " + linkCounter + " links, decrypting now...");
        for (int i = 0; i <= linkCounter - 1; i++) {
            br.getHeaders().put("Referer", parameter);
            final String actualPage = MAINPAGE + getList + "?out_name=" + postvar + "&&link_id=" + i;
            br.getPage(actualPage);
            if (br.containsHTML("This file is either removed due to copyright claim or is deleted by the uploader")) {
                logger.info("Found one offline link for link " + parameter + " linkid:" + i);
                continue;
            }
            String finallink = br.getRegex("http-equiv=\"refresh\" content=\"0;url=(http.*?)\"").getMatch(0);
            if (finallink == null) {
                // Handlings for more hosters will come soon i think
                if (br.containsHTML("turbobit\\.net")) {
                    final String singleProtectedLink = MAINPAGE + "plugin/turbobit.net.free.php?out_name=" + postvar + "&link_id=" + i;
                    br.getPage(singleProtectedLink);
                    if (br.getRedirectLocation() == null) {
                        logger.warning("Redirect location for this link is null: " + parameter);
                        return null;
                    }
                    final String turboId = new Regex(br.getRedirectLocation(), "http://turbobit\\.net/download/free/(.+)").getMatch(0);
                    if (turboId == null) {
                        logger.warning("There is a problem with the link: " + actualPage);
                        return null;
                    }
                    finallink = "http://turbobit.net/" + turboId + ".html";
                } else if (br.containsHTML("hotfile\\.com")) {
                    finallink = br.getRegex("style=\"margin:0;padding:0;\" action=\"(.*?)\"").getMatch(0);
                    if (finallink.equals("plugin/hotfile.com.free.php")) {
                        final String singleProtectedLink = MAINPAGE + finallink;
                        br.postPage(singleProtectedLink, "out_name=" + postvar + "&link_id=" + i);
                    }
                    finallink = br.getRegex("href=\"(.*?)\"").getMatch(0).trim();
                    if (finallink == null) {
                        logger.warning("There is a problem with the link: " + actualPage);
                        return null;
                    }
                    if (finallink.contains("/get/")) {
                        br.getPage(finallink);
                        if (br.containsHTML("Invalid link")) {
                            finallink = br.getRegex("href=\"(.*?)\"").getMatch(0);
                        }
                    }
                }
            }
            if (finallink == null) {
                logger.warning("Finallink for the following link is null: " + parameter);
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}