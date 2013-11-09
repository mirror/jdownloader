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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livemixtapes.com" }, urls = { "http://(www\\.)?(livemixtap\\.es/[a-z0-9]+|(\\w+\\.)?livemixtapes\\.com/(download(/mp3)?|mixtapes)/\\d+/.*?\\.html)" }, flags = { 0 })
public class LiveMistapesComDecrypter extends PluginForDecrypt {

    public LiveMistapesComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String REDIRECTLINK           = "http://(www\\.)?livemixtap\\.es/[a-z0-9]+";
    private static final String MUSTBELOGGEDIN         = ">You must be logged in to access this page";
    private static final String ONLYREGISTEREDUSERTEXT = "Download is only available for registered users";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replace("/mixtapes/", "/download/");
        br.getHeaders().put("Accept-Encoding", "gzip,deflate");

        /** If link is a short link correct it */
        if (parameter.matches(REDIRECTLINK)) {
            br.setFollowRedirects(false);
            br.getPage(parameter);
            String correctLink = br.getRedirectLocation();
            if (correctLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(correctLink);
            correctLink = br.getRedirectLocation();
            if (correctLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            parameter = correctLink;
            br.setFollowRedirects(true);
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
        }
        if (br.getURL().contains("error/login.html")) {
            logger.info("Login needed to decrypt link: " + parameter);
            return decryptedLinks;
        }
        // Check for embedded video(s)
        if (br.containsHTML("function videoEmbed")) {
            final String finallink = br.getRegex("videoEmbed\\(\\'(http://[^<>\"]*?)\\'").getMatch(0);
            if (finallink != null) {
                decryptedLinks.add(createDownloadlink(finallink));
                return decryptedLinks;
            }
        }
        final DownloadLink mainlink = createDownloadlink(parameter.replace("livemixtapes.com/", "livemixtapesdecrypted.com/"));
        String filename = null, filesize = null;
        if (br.containsHTML(MUSTBELOGGEDIN)) {
            final Regex fileInfo = br.getRegex("<td height=\"35\"><div style=\"padding\\-left: 8px\">([^<>\"]*?)</div></td>[\t\n\r ]+<td align=\"center\">([^<>\"]*?)</td>");
            filename = fileInfo.getMatch(0);
            filesize = fileInfo.getMatch(1);
            if (filename == null || filesize == null) {
                // mainlink.getLinkStatus().setStatusText(ONLYREGISTEREDUSERTEXT);
                mainlink.setAvailable(true);
            }
        } else {
            final String timeRemaining = br.getRegex("TimeRemaining = (\\d+);").getMatch(0);
            if (timeRemaining != null) {
                // mainlink.getLinkStatus().setStatusText("Not yet released, cannot download");
                mainlink.setName(Encoding.htmlDecode(br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0)));
                mainlink.setAvailable(true);
                decryptedLinks.add(mainlink);
                return decryptedLinks;
            }

            final Regex fileInfo = br.getRegex("<td height=\"35\"><div[^>]+>(.*?)</div></td>[\t\n\r ]+<td align=\"center\">((\\d+(\\.\\d+)? ?(KB|MB|GB)))</td>");
            filename = fileInfo.getMatch(0);
            filesize = fileInfo.getMatch(1);
        }
        if (filename == null || filesize == null) {
            mainlink.setAvailable(false);
        } else {
            mainlink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            mainlink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        decryptedLinks.add(mainlink);

        return decryptedLinks;
    }

}
