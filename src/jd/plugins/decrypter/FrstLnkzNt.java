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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

//by pspzockerscene| dlc handling by jiaz
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1st-linkz.net" }, urls = { "http://[\\w\\.]*?1st-linkz\\.net/[a-z0-9]+\\.html" }, flags = { 0 })
public class FrstLnkzNt extends PluginForDecrypt {

    public FrstLnkzNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        Form captchaForm = br.getForm(0);
        if (captchaForm != null) {
            // there is no real captcha handling needed because you can see the
            // letters in the html code (twice)

            // // Captcha handling
            // String captchainput =
            // br.getRegex("src=\"captcha/(.*?).jpg\" border").getMatch(0);
            // if (captchainput == null) throw new
            // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // captchaForm.put("captcha", captchainput);
            br.submitForm(captchaForm);
            Form checkform = br.getForm(0);
            if (checkform != null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // // Wrong-captcha errorhandling
            // if
            // (br.containsHTML("Bitte überprüfen Sie die Eingabe des Captchas"))
            // throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            // }
        }
        // container handling (if no containers found, use webprotection)
        // ccf is prefered because this site often got buggy dlc and rsdf
        // containers!
        if (br.containsHTML("type=ccf")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("type=dlc")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("type=rsdf")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        // Webprotection decryption
        String[] links = br.getRegex("onclick=\"winopen\\('(.*?)'\\)").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            String link2 = link.replace("&amp;", "&");
            br.getPage(link2);
            System.out.print(br.toString());
            String finallink = br.getRedirectLocation();
            if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String[] dlclinks = br.getRegex("(http://www\\.1st-linkz\\.net/container/dl\\.php\\?type=" + format + "&id=(\\d+))\"").getColumn(0);
        if (dlclinks == null || dlclinks.length == 0) return null;
        for (String link : dlclinks) {
            String test = Encoding.htmlDecode(link);
            File file = null;
            URLConnectionAdapter con = brc.openGetConnection(link);
            if (con.getResponseCode() == 200) {
                file = JDUtilities.getResourceFile("tmp/1stlinkz/" + test.replaceAll("(http://www.1st-linkz.net/|\\?)", "") + "." + format);
                if (file == null) return null;
                file.deleteOnExit();
                brc.downloadConnection(file, con);
            } else {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            if (file != null && file.exists() && file.length() > 100) {
                ArrayList<DownloadLink> decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                if (decryptedLinks.size() > 0) return decryptedLinks;
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return null;
    }

}
