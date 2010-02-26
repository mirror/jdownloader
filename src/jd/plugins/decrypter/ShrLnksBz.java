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

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "share-links.biz" }, urls = { "http://[\\w\\.]*?(share-links\\.biz/_[0-9a-z]+|s2l\\.biz/[a-z0-9]+)" }, flags = { 0 })
public class ShrLnksBz extends PluginForDecrypt {

    private static String host = "http://share-links.biz";

    public ShrLnksBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setFollowRedirects(false);
        br.getPage(parameter);
        String aha = br.getRedirectLocation();
        if (aha != null) {
            parameter = aha;
            br.getPage(aha);
            br.getPage(aha);
        }
        /* Error handling */
        if (br.containsHTML("Der Inhalt konnte nicht gefunden werden")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Folderpassword+Captcha handling
        if (br.containsHTML("id=\"folderpass\"")) {
            String latestPassword = this.getPluginConfig().getStringProperty("PASSWORD", null);
            for (int i = 0; i <= 3; i++) {
                Form pwform = br.getForm(0);
                if (pwform == null) return null;
                // First try the stored password, if that doesn't work, ask the
                // user to enter it
                if (latestPassword == null) latestPassword = Plugin.getUserInput("Password?", param);
                pwform.put("pass", latestPassword);
                br.submitForm(pwform);
                if (br.containsHTML("Das eingegebene Passwort ist falsch")) {
                    getPluginConfig().setProperty("PASSWORD", null);
                    getPluginConfig().save();
                    continue;
                }
                break;
            }
            if (br.containsHTML("Das eingegebene Passwort ist falsch")) {
                getPluginConfig().setProperty("PASSWORD", null);
                getPluginConfig().save();
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
            // Save actual password if it is valid
            getPluginConfig().setProperty("PASSWORD", latestPassword);
            getPluginConfig().save();
        }

        if (br.containsHTML("(/captcha/|captcha_container|\"Captcha\"|id=\"captcha\")")) {
            for (int i = 0; i <= 5; i++) {
                String Captchamap = br.getRegex("/><img src=\"(.*?)\" alt=\"Captcha\" id=\"captcha\" usemap=\"#captchamap\" />").getMatch(0);
                File file = this.getLocalCaptchaFile();
                Browser temp = br.cloneBrowser();
                temp.getDownload(file, "http://share-links.biz" + Captchamap);
                Point p = UserIO.getInstance().requestClickPositionDialog(file, "Share-links.biz", JDL.L("plugins.decrypt.shrlnksbz.desc", "Read the combination in the background and click the corresponding combination in the overview!"));
                String nexturl = getNextUrl(p.x, p.y);
                if (nexturl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.setFollowRedirects(true);
                br.getPage("http://share-links.biz/" + nexturl);
                if (br.containsHTML("Die getroffene Auswahl war falsch")) {
                    br.getPage(parameter);
                    // Usually a cookie is being set and you don't have to enter
                    // the password again but if you would, the password of the
                    // plugin configuration would always be the right one
                    if (br.containsHTML("id=\"folderpass\"")) {
                        Form pwform = br.getForm(0);
                        pwform.put("pass", this.getPluginConfig().getStringProperty("PASSWORD"));
                        br.submitForm(pwform);
                    }
                    continue;
                }
                break;
            }
            if (br.containsHTML("Die getroffene Auswahl war falsch")) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        
        // simulate browser
        Browser brc = br.cloneBrowser();
        brc.getPage("http://share-links.biz/scripts/download.js");
        brc.getPage("http://share-links.biz/template/images/flags/en.gif");
        brc.getPage("http://share-links.biz/template/images/flags/de.gif");
        
        
        // container handling
        if (br.containsHTML("'dlc'")) {
            decryptedLinks = loadcontainer(br, "dlc");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("'rsdf'")) {
            decryptedLinks = loadcontainer(br, "rsdf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }

        if (br.containsHTML("'ccf'")) {
            decryptedLinks = loadcontainer(br, "ccf");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;
        }
        if (br.containsHTML("'cnl'")) {
            decryptedLinks = loadcontainer(br, "cnl");
            if (decryptedLinks != null && decryptedLinks.size() > 0) return decryptedLinks;

        }
        /* File package handling */
        int pages = 1;
        String pattern = parameter.substring(parameter.lastIndexOf("/"), parameter.length());
        if (br.containsHTML("folderNav")) {
            pages = pages + br.getRegex(pattern + "\\?n=[0-9]++\"").getMatches().length;
        }
        LinkedList<String> links = new LinkedList<String>();
        for (int i = 1; i <= pages; i++) {
            br.getPage(host + pattern + "?n=" + i);
            String[] linki = br.getRegex("decrypt\\.gif\".*?_get\\('(.*?)'").getColumn(0);
            links.addAll(Arrays.asList(linki));
        }
        if (links.size() == 0) return null;
        progress.setRange(links.size());
        for (String link : links) {
            link = "http://share-links.biz/get/lnk/" + link;
            br.getPage(link);
            String clink0 = br.getRegex("unescape\\(\"(.*?)\"").getMatch(0);
            if (clink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            clink0 = Encoding.htmlDecode(clink0);
            String frm = br.getRegex("\"(http://share-links\\.biz/get/frm/.*?)\"").getMatch(0);
            br.getPage(frm);
            String b64 = br.getRegex("\\p{Punct}(aHR0.*?)\\p{Punct}").getMatch(0);
            String finalUrl = Encoding.Base64Decode(b64);
            if (finalUrl.equals(b64)) {
                finalUrl = Encoding.Base64Decode(b64 + "=");
                if (finalUrl.equals(b64 + "=")) {
                    finalUrl = Encoding.Base64Decode(b64 + "==");
                    if (finalUrl.equals(b64 + "==")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                }
            }
            DownloadLink dl = createDownloadlink(finalUrl);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    /** finds the correct shape area for the given point */
    private String getNextUrl(int x, int y) {
        String[][] results = br.getRegex("<area shape=\"rect\" coords=\"(\\d+),(\\d+),(\\d+),(\\d+)\" href=\"/(.*?)\"").getMatches();
        String hit = null;
        for (String[] ret : results) {
            int xmin = Integer.parseInt(ret[0]);
            int ymin = Integer.parseInt(ret[1]);
            int xmax = Integer.parseInt(ret[2]);
            int ymax = Integer.parseInt(ret[3]);
            if (x >= xmin && x <= xmax && y >= ymin && y <= ymax) {
                hit = ret[4];
                break;
            }
        }
        return hit;
    }

    /** by jiaz */
    private ArrayList<DownloadLink> loadcontainer(Browser br, String format) throws IOException, PluginException {
        Browser brc = br.cloneBrowser();
        String dlclinks = null;
        if (format.matches("dlc")) {
            dlclinks = br.getRegex("dlc container.*?onclick=\"javascript:_get\\('(.*?)'.*?'dlc'\\)").getMatch(0);
        } else if (format.matches("ccf")) {
            dlclinks = br.getRegex("ccf container.*?onclick=\"javascript:_get\\('(.*?)'.*?'ccf'\\)").getMatch(0);
        } else if (format.matches("rsdf")) {
            dlclinks = br.getRegex("rsdf container.*?onclick=\"javascript:_get\\('(.*?)'.*?'rsdf'\\)").getMatch(0);
        } else
            ;
        
        if (dlclinks == null) 
            return new ArrayList<DownloadLink>();
        dlclinks = "http://share-links.biz/get/" + format + "/" + dlclinks;
        String test = Encoding.htmlDecode(dlclinks);
        File file = null;
        URLConnectionAdapter con = brc.openGetConnection(dlclinks);
        if (con.getResponseCode() == 200) {
            file = JDUtilities.getResourceFile("tmp/sharelinks/" + test.replaceAll("(http://share-links.biz/|/|\\?)", "") + "." + format);
            if (file == null) return new ArrayList<DownloadLink>();
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
        return new ArrayList<DownloadLink>();
    }

}
