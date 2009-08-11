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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 7182 $", interfaceVersion = 2, names = { "ddl-music.org" }, urls = { "http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+|http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+" }, flags = { 0 })
public class DDLMscrg extends PluginForDecrypt {
    static public final String DECRYPTER_DDLMSC_MAIN = "http://[\\w\\.]*?ddl-music\\.org/index\\.php\\?site=view_download&cat=.+&id=\\d+";
    static public final String DECRYPTER_DDLMSC_CRYPT = "http://[\\w\\.]*?ddl-music\\.org/captcha/ddlm_cr\\d\\.php\\?\\d+\\?\\d+";

    public DDLMscrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(DECRYPTER_DDLMSC_CRYPT)) {
            int add = 0;
            for (int i = 1; i < 5; i++) {
                br.getPage(parameter);
                try {
                    Thread.sleep(3000 + add);
                } catch (InterruptedException e) {
                }
                Form captchaForm = br.getForm(0);
                String[] calc = br.getRegex(Pattern.compile("method=\"post\">[\\s]*?(\\d*?) (\\+|-) (\\d*?) =", Pattern.DOTALL)).getRow(0);
                String inputname = captchaForm.getInputFieldsByType("text").get(0).getKey();
                if (calc[1].equals("+")) {
                    captchaForm.put(inputname, String.valueOf(Integer.parseInt(calc[0]) + Integer.parseInt(calc[2])));
                } else {
                    captchaForm.put(inputname, String.valueOf(Integer.parseInt(calc[0]) + Integer.parseInt(calc[2])));
                }
                br.submitForm(captchaForm);
                if (!br.containsHTML("Du bist ein Angeber") && !br.containsHTML("Mein Gott, wo bist denn du zur Schule gegangen!")) {
                    decryptedLinks.add(createDownloadlink(br.getRegex(Pattern.compile("<form action=\"(.*?)\" method=\"post\">", Pattern.CASE_INSENSITIVE)).getMatch(0)));
                    break;
                }
                add += 500;
            }
        } else if (parameter.matches(DECRYPTER_DDLMSC_MAIN)) {
            br.getPage(parameter);

            String password = br.getRegex(Pattern.compile("<td class=\"normalbold\"><div align=\"center\">Passwort</div></td>.*?<td class=\"normal\"><div align=\"center\">(.*?)</div></td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
            if (password != null && password.contains("kein Passwort")) {
                password = null;
            }

            String ids[] = br.getRegex(Pattern.compile("<a href=\"(.*?)\" target=\"_blank\" onMouseOut=\"MM_swapImgRestore", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(ids.length);
            for (String id : ids) {
                if (id.startsWith("/captcha/")) id = "http://ddl-music.org" + id;
                DownloadLink dLink = createDownloadlink(id);
                dLink.addSourcePluginPassword(password);
                decryptedLinks.add(dLink);
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    // @Override

}
