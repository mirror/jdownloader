//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rs.hoerbuch.in" }, urls = { "http://(rs\\.hoerbuch\\.in/[A-Za-z0-9]+|hoerbuch\\.in/download_\\d+)\\.html" }, flags = { 0 })
public class RsHrbchn extends PluginForDecrypt {

    public RsHrbchn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> passwords = new ArrayList<String>();
        passwords.add("www.hoerbuch.in");
        String parameter = param.toString();
        br.getPage(parameter);
        if (parameter.contains("hoerbuch.in/download_")) {
            String password = br.getRegex("<B>Passwort:</B> (.*?) \\| <B>Uploader").getMatch(0);
            if (password != null && !passwords.contains(password)) passwords.add(password);
            String fpName = br.getRegex("\">Download von \"<b>(.*?)</b>").getMatch(0);
            String[] allLinks = br.getRegex("\"http://(www\\.)?hoerbuch\\.in/cj/out\\.php\\?pct=\\d+\\&url=(http://rs\\.hoerbuch\\.in/.*?)\"").getColumn(1);
            if (allLinks == null || allLinks.length == 0) allLinks = br.getRegex("(http://rs\\.hoerbuch\\.in/([A-Za-z0-9-]+/)?(\\d+/)?[A-Za-z0-9_\\.-]+)\"").getColumn(0);
            if (allLinks == null || allLinks.length == 0) return null;
            progress.setRange(allLinks.length);
            for (String singleLink : allLinks) {
                br.getPage(singleLink);
                String finallink = getSingleLink(singleLink);
                if (finallink == null) {
                    if (br.containsHTML("(<title>404 Not Found</title>|<h1>Not Found</h1>)")) {
                        logger.info("Found a broken link on mainlink: " + singleLink);
                        logger.info("Found a broken link on detailed link: " + singleLink);
                        continue;
                    }
                    logger.warning("Error happened during decryption loop for mainlink: " + parameter);
                    logger.warning("Error happened during decryption loop for detailed link: " + singleLink);
                    return null;
                }
                if (finallink.contains("ul.to")) finallink = finallink.replace("ul.to", "uploaded.to");
                DownloadLink dl = createDownloadlink(finallink);
                dl.setSourcePluginPasswordList(passwords);
                decryptedLinks.add(dl);
                progress.increase(1);
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        } else {
            String dLink = getSingleLink(parameter);
            if (dLink == null) return null;
            DownloadLink dl = createDownloadlink(dLink);
            dl.setSourcePluginPasswordList(passwords);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

    private String getSingleLink(String parameter) {
        Form form = br.getForm(0);
        if (form == null) return null;
        String dLink;
        if (form.hasInputFieldByName("uri")) {
            dLink = "http://rapidshare." + new Regex(parameter, "in/(\\w{2,3})-").getMatch(0) + Encoding.htmlDecode(form.getInputFieldByName("uri").getValue());
        } else {
            dLink = form.getAction(null);
        }
        return dLink;
    }

}
