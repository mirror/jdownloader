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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dlbase.ws" }, urls = { "http://[\\w\\.]*?dlbase\\.ws/(season|game|program|music|xxx|movie|gamereport)_details\\.php\\?id=[0-9]+" }, flags = { 0 })
public class DlBseWs extends PluginForDecrypt {

    public DlBseWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        // "gamereport"-links handling
        if (parameter.contains("gamereport")) {
            parameter = br.getRegex("\"(game_details\\.php\\?id=.*?)\"").getMatch(0);
            if (parameter == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            parameter = "http://dlbase.ws/" + parameter;
            br.getPage(parameter);
        }
        String fpName = br.getRegex("\"top\"><strong>(.*?)</strong>").getMatch(0).trim();
        fp.setName(fpName);
        // Container handling
        if (br.containsHTML(">Download DLC<")) {
            decryptedLinks = loadcontainer(br, parameter);
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        String[] dlinks = br.getRegex("\"((season|game|program|music|xxx|movie)_details\\.php\\?.*?&download=.*?)\"").getColumn(0);
        if (dlinks == null || dlinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (String dlink : dlinks) {
            dlink = "http://dlbase.ws/" + dlink;
            br.getPage(dlink);
            // Password handling
            String password = br.getRegex("<br>Passwort: <input type=\"text\" style=\"color:red;\" value=\"(.*?)\"").getMatch(0).trim();
            String[] links = br.getRegex("\"(go\\.php\\?.*?)\"").getColumn(0);
            if (links == null || links.length == 0) return null;
            progress.setRange(links.length);
            for (String link : links) {
                link = "http://dlbase.ws/" + link;
                br.getPage(link);
                /* captcha handling */
                String downlink = null;
                if (!br.containsHTML("kreiscaptcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                for (int i = 0; i <= 5; i++) {
                    File file = this.getLocalCaptchaFile();
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://dlbase.ws/kreiscaptcha.php"));
                    int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                    if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                    Form captchaForm = new Form();
                    captchaForm.setMethod(Form.MethodType.POST);
                    captchaForm.setAction(link);
                    captchaForm.put("button", "Send");
                    captchaForm.put("button.x", p[0] + "");
                    captchaForm.put("button.y", p[1] + "");
                    br.submitForm(captchaForm);

                    if (br.containsHTML("kreiscaptcha\\.php")) continue;
                    downlink = br.getRegex("URL=(.*?)\"></fieldset").getMatch(0);
                    if (downlink != null) break;
                }
                if (downlink == null && br.containsHTML("kreiscaptcha\\.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
                if (downlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                DownloadLink dl_link = createDownloadlink(downlink);
                if (password != null && !password.equals("-kein Passwort-") && !password.equals("keins") && !password.equals("n/a") && password.length() != 0) {
                    dl_link.addSourcePluginPassword(password);
                }
                decryptedLinks.add(dl_link);
                progress.increase(1);
            }
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    // by jiaz
    private ArrayList<DownloadLink> loadcontainer(Browser br, String parameter) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String category = new Regex(parameter, "dlbase\\.ws/(.*?)_details").getMatch(0);
        String id = "&id=";
        if (category.equals("game") || category.equals("season") || category.equals("movie")) {
            id = "sid=";
        }
        String containerid = new Regex(parameter, "dlbase\\.ws/.*?_details\\.php\\?id=(\\d+)").getMatch(0);
        String containerlink = "http://dlbase.ws/container_download.php?type=" + category + id + containerid + "&dl=dlc";
        String test = Encoding.htmlDecode(containerlink);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(test);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/dlbase/" + test.replaceAll("(http://dlbase.ws|/|\\?)", "") + ".dlc");
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
        return null;
    }

}