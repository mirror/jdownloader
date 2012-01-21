//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 13393 $", interfaceVersion = 2, names = { "mirrorstack.com" }, urls = { "http://(www\\.)?mirrorstack\\.com/([a-z0-9]{2}_)?[a-z0-9]{12}" }, flags = { 0 })
public class MirStkCm extends PluginForDecrypt {

    /*
     * DEV NOTES: - provider has issues at times, and doesn't unhash stored data
     * values before exporting them into redirects. I've noticed this with
     * mediafire links for example http://mirrorstack.com/mf_dbfzhyf2hnxm will
     * at times return http://www.mediafire.com/?HASH(0x15053b48), you can then
     * reload a couple times and it will work in jd.. provider problem not
     * plugin. Other example links I've used seem to work fine.
     */

    public MirStkCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString().replace("/www.", "/");
        br.getPage(parameter);
        if (br.containsHTML(">Not Found</h2>")) {
            logger.warning("Invalid URL, either removed or never existed :" + parameter);
            return null;
        }
        if (parameter.matches("http://(www\\.)?mirrorstack\\.com/[a-z0-9]{2}_[a-z0-9]{12}")) {
            String finallink = br.getRedirectLocation();
            decryptedLinks.add(createDownloadlink(finallink));
        }

        if (parameter.matches("http://(www\\.)?mirrorstack\\.com/[a-z0-9]{12}")) {
            String[] redirectLinks = br.getRegex(Pattern.compile("<a href=\\'(http://mirrorstack.com/[a-z0-9]{2}_[a-z0-9]{12})\\'")).getColumn(0);
            if (redirectLinks == null || redirectLinks.length == 0) {
                logger.warning("Couldn't find any link... :" + parameter);
                return null;
            }
            progress.setRange(redirectLinks.length);
            for (String redirectLink : redirectLinks) {
                br.getPage(redirectLink);
                String finallink = br.getRedirectLocation();
                if (finallink == null) {
                    continue;
                }
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }
}
