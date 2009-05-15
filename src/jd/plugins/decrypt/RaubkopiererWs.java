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

package jd.plugins.decrypt;

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.HTMLEntities;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;

public class RaubkopiererWs extends PluginForDecrypt {
    
    ProgressController progress;

    public RaubkopiererWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(false);

        br.getPage(parameter);
        if ((br.getRedirectLocation() != null && br.getRedirectLocation().contains("error")) || br.containsHTML("class=\"error_msg\"")) {
            return null;
        }
        String fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0).trim();
        if (fpName != null)
            fp.setName(HTMLEntities.unhtmlentities(fpName));
        String fpPass = br.getRegex("Passwort:</b></th>\\s+<td>(.*?)</td>").getMatch(0);
        if (fpPass != null)
            fp.setPassword(fpPass);

        Form form = br.getFormbyProperty("name", "go_captcha");
        ArrayList<InputField> mirrors = form.getInputFieldsByType("submit");
        if (form == null || mirrors == null)
            return null;
        progress.setRange(mirrors.size());
        long partcount = 1;
        for (int i = 0; i <= mirrors.size()-1; i++) {
            for (int retry = 1; retry <= 5; retry++) {
                String captchaURL = "/captcha" + form.getRegex("<img\\ssrc=\"/captcha(.*?)\"").getMatch(0);
                if (captchaURL == null)
                    return null;
                URLConnectionAdapter con = br.openGetConnection(captchaURL);
                File captchaFile = this.getLocalCaptchaFile();
                Browser.download(captchaFile, con);
                String code = getCaptchaCode(captchaFile, param);
                br.postPage(parameter, "captcha=" + code + "&" + mirrors.get(i).getKey() + "=");
                if (!br.containsHTML("Fehler: Der Sicherheits-Code")) {
                    break;
                } else {
                    logger.log(java.util.logging.Level.WARNING, JDLocale.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
                }
            }
            String[] parts = br.getRegex("<a\\shref=\"(/\\w+?/dl/.+?/goto-[a-z0-9]+?/?)\"").getColumn(0);
            if (parts.length != 0) {
                partcount = parts.length;
                int percent = progress.getPercent();
                progress.setRange(parts.length * mirrors.size());
                progress.setStatus((long)Math.round(progress.getMax() * ((double)percent/10000)));
                for (String part : parts) {
                    br.getPage(part.replace("goto", "frame"));
                    DownloadLink dlink = createDownloadlink(br.getRedirectLocation());
                    dlink.addSourcePluginPassword(fpPass);
                    fp.add(dlink);
                    decryptedLinks.add(dlink);
                    progress.increase(1);
                }
            } else progress.increase(partcount);
        }

        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}