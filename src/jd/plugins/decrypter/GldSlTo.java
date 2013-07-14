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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "goldesel.to" }, urls = { "http://(www\\.)?goldesel\\.to/download/\\d+/.{1}" }, flags = { 0 })
public class GldSlTo extends PluginForDecrypt {

    public GldSlTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // Important: Does not work without this cookie!
        br.setCookie("http://goldesel.to/", "goldesel_in", "1");
        br.getPage(parameter);
        if (!br.containsHTML("class=\"entry_box_head\">Direct\\-Downloads</div>") && !br.containsHTML("class=\"entry_box_head\">Streams \\-")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final Browser br2 = br.cloneBrowser();
        br2.getPage("http://goldesel.to/script/main1.js");
        final String reCaptchaID = br2.getRegex("Recaptcha\\.create\\(\"([^<>\"]*?)\"").getMatch(0);
        String[] postInfo = getAjaxPost(br2);
        if (br.containsHTML("class=\"recaptcha_only_if_image\"") && reCaptchaID != null) {
            br.getHeaders().put("Referer", "");
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setId(reCaptchaID);
            for (int i = 1; i <= 5; i++) {
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                br.postPage(parameter, "recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c + "&Module=CCap");
                if (br.containsHTML("class=\"recaptcha_only_if_image\"")) {
                    continue;
                }
                break;
            }
            if (br.containsHTML("class=\"recaptcha_only_if_image\"")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }

        String fpName = br.getRegex("class=\"content_box_head\">Detailansicht von \"(.*?)\"</div>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("width=\"14\" height=\"14\" align=\"absbottom\" />\\&nbsp;\\&nbsp;(.*?)</span><div").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("style=\"float:left; margin:0px;\"><strong>\"(.*?)\"</strong>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<title>(.*?) \\(Download\\) \\- GoldEsel</title>").getMatch(0);
                }
            }
        }
        String[] decryptIDs = br.getRegex("onClick=\"window\\.open\\(\\'http://goldesel\\.to/dl/\\', \\'(\\d+)\\'").getColumn(0);
        if (decryptIDs == null || decryptIDs.length == 0) {
            decryptIDs = br.getRegex("goD\\(\\'(\\d+)\\'\\);").getColumn(0);
            if (decryptIDs == null || decryptIDs.length == 0) {
                decryptIDs = br.getRegex("href=\"http://goldesel\\.to/dl/\" target=\"(.*?)\"").getColumn(0);
            }
        }
        if (decryptIDs == null || decryptIDs.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] streamIDs = br.getRegex("onClick=\"load_Stream\\(\\'(\\d+)\\'\\)").getColumn(0);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        for (final String decryptID : decryptIDs) {
            br.postPage(postInfo[0], postInfo[1] + "=" + decryptID);
            String finallink = br.toString();
            if (finallink.contains("No input file specified")) {
                continue;
            }
            if (!finallink.startsWith("http") || finallink.length() > 500) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        if (streamIDs != null && streamIDs.length != 0) {
            for (final String streamID : streamIDs) {
                br.postPage("http://goldesel.to/ajax/streams.php", "Stream=" + streamID);
                String finallink = br.getRegex("<a href=\"(http[^<>\"]*?)\"").getMatch(0);
                if (finallink == null || finallink.length() > 500) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            }
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private String[] getAjaxPost(final Browser br) {
        String[] postInfo = new String[2];
        final Regex postInfoRegex = br.getRegex("function [A-Za-z0-9\\-_]+\\(([A-Z0-9]+)\\) \\{ \\$\\.post\\(\"(ajax[^<>\"]*?)\"");
        if (postInfoRegex.getMatches().length != 0) {
            postInfo[0] = "http://goldesel.to/" + postInfoRegex.getMatch(1);
            postInfo[1] = postInfoRegex.getMatch(0);
        } else {
            postInfo[0] = "http://goldesel.to/ajax/jDL.php";
            postInfo[1] = "LNK";
        }
        return postInfo;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}