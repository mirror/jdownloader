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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginUtils;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "relink.us", "relink.us" }, urls = { "http://[\\w\\.]*?relink\\.us/(go\\.php\\?id=[\\w]+|f/[\\w]+)", "http://[\\w\\.]*?relink\\.us/view\\.php\\?id=\\w+" }, flags = { 0, 0 })
public class Rlnks extends PluginForDecrypt {

    ProgressController progress;

    public Rlnks(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean decryptContainer(String page, String cryptedLink, String containerFormat, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        String containerURL = new Regex(page, "(download\\.php\\?id=[a-zA-z0-9]+\\&" + containerFormat + "=\\d+)").getMatch(0);
        if (containerURL != null) {
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "." + containerFormat);
            Browser browser = br.cloneBrowser();
            browser.getHeaders().put("Referer", cryptedLink);
            browser.getDownload(container, "http://relink.us/" + Encoding.htmlDecode(containerURL));
            decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
            container.delete();
            return true;
        }
        return false;
    }

    private void decryptLinks(ArrayList<DownloadLink> decryptedLinks) throws IOException {
        br.setFollowRedirects(false);
        String[] matches = br.getRegex("getFile\\('(cid=\\w*?&lid=\\d*?)'\\)").getColumn(0);
        try {
            progress.addToMax(matches.length);
            for (String match : matches) {
                try {
                    for (int i = 0; i < 3; i++) {
                        Browser brc = br.cloneBrowser();
                        brc.getPage("http://www.relink.us/frame.php?" + match);
                        if (brc.getRedirectLocation() != null && brc.getRedirectLocation().contains("relink.us/getfile")) brc.getPage(brc.getRedirectLocation());
                        if (brc.getRedirectLocation() != null) {
                            decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(brc.getRedirectLocation())));
                            break;
                        } else {
                            String url = brc.getRegex("iframe.*?src=\"(.*?)\"").getMatch(0);
                            if (url != null) {
                                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(url)));
                                break;
                            }
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {
                        }
                    }
                    progress.increase(1);
                } catch (Exception e) {
                }
            }
        } finally {
            br.setFollowRedirects(true);
        }
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.progress = progress;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        String page = br.getPage(parameter);
        boolean okay = true;
        for (int i = 0; i < 4; i++) {
            if (br.containsHTML("(das richtige Passwort)|(haben ein falsches Passwort)")) {
                okay = false;
                Form form = br.getForm(1);
                String pw = PluginUtils.askPassword(this);
                form.put("password", pw);
                page = br.submitForm(form);
            } else {
                okay = true;
                break;
            }
        }
        if (okay == false) throw new DecrypterException(DecrypterException.CAPTCHA);
        progress.setRange(0);
        decryptLinks(decryptedLinks);
        String more_links[] = new Regex(page, Pattern.compile("<a href=\"(go\\.php\\?id=[a-zA-Z0-9]+\\&seite=\\d+)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String link : more_links) {
            br.getPage("http://relink.us/" + link);
            decryptLinks(decryptedLinks);
        }

        if (decryptedLinks.size() == 0) {
            if (!decryptContainer(page, parameter, "dlc", decryptedLinks)) {
                if (!decryptContainer(page, parameter, "ccf", decryptedLinks)) {
                    decryptContainer(page, parameter, "rsdf", decryptedLinks);
                }
            }
        }

        return decryptedLinks;
    }

}
