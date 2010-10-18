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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "badongo.com" }, urls = { "http://[\\w\\.]*?badongo\\.com/.*(file|vid|audio)/[0-9]+" }, flags = { 0 })
public class BdngCm extends PluginForDecrypt {
    static private String host = "badongo.com";

    public BdngCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setCookiesExclusive(true);
        br.clearCookies(host);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.setFollowRedirects(false);
        br.getPage(parameter);
        parameter = parameter.replaceFirst("badongo\\.com", "badongo.viajd");
        if (!br.containsHTML("Diese Datei wurde gesplittet")) {
            /* For single video links */
            //if (!parameter.endsWith("/1")) parameter = parameter + "/1";
            /* For audio files */
            if (parameter.contains("audio/")) parameter = parameter.replace("audio/", "file/");
            DownloadLink dlLink = createDownloadlink(Encoding.htmlDecode(parameter));
            dlLink.setProperty("type", "single");
            decryptedLinks.add(dlLink);
        } else {
            /* Get CaptchaCode */
            for (int i = 0; i <= 5; i++) {
                br.getPage(param.toString() + "?rs=displayCaptcha&rst=&rsrnd=" + System.currentTimeMillis() + "&rsargs[]=yellow");
                Form form = br.getForm(0);
                String cid = br.getRegex("cid\\=(\\d+)").getMatch(0);
                String code = getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid, param);
                form.setAction(br.getRegex("action=.\"(.+?).\"").getMatch(0));
                form.put("user_code", code);
                form.put("cap_id", br.getRegex("cap_id.\"\\svalue=.\"(\\d+).\"").getMatch(0));
                form.put("cap_secret", br.getRegex("cap_secret.\"\\svalue=.\"([a-z0-9]+).\"").getMatch(0));
                br.submitForm(form);
                if (br.getRedirectLocation() == null) break;
            }
            /* Collect Splitfiles */
            String[] partLinks = br.getRegex("<a\\shref=\"(http://www\\.badongo\\.com/de/c?(vid|file)/\\d+(/\\d)?/\\w\\w)\"").getColumn(0);
            if (partLinks == null || partLinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            progress.setRange(partLinks.length);
            for (int i = 0; i <= partLinks.length - 1; i++) {
                DownloadLink dlLink = createDownloadlink(partLinks[i].replaceFirst("badongo\\.com", "badongo.viajd"));
                dlLink.setName(dlLink.getName() + "." + (i + 1));
                dlLink.setProperty("type", "split");
                dlLink.setProperty("part", i + 1);
                dlLink.setProperty("parts", partLinks.length);
                decryptedLinks.add(dlLink);
                progress.increase(1);
            }
        }
        return decryptedLinks;
    }
}
