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

import java.awt.Color;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.ProgressControllerEvent;
import jd.controlling.ProgressControllerListener;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shareapic.net" }, urls = { "http://[\\w\\.]*?shareapic\\.net/([0-9]+|(Zoom|View)-[0-9]+|content\\.php\\?id=[0-9]+)" }, flags = { 0 })
public class ShareaPicNet extends PluginForDecrypt implements ProgressControllerListener {

    private boolean abort = false;

    public ShareaPicNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        progress.getBroadcaster().addListener(this);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        // content links are kinda the same as View and can be handled easily so
        // i try to use them as much as i can in the plugin
        if (parameter.contains("content")) {
            String picid = new Regex(parameter, "shareapic\\.net/content\\.php\\?id=(\\d+)").getMatch(0);
            parameter = "http://www.shareapic.net/View-" + picid;
        }
        br.setFollowRedirects(false);
        parameter = parameter.replaceAll("(View|Zoom)", "Zoom") + ".html";
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("Access Denied - This is a non-public gallery") || br.containsHTML("error404") || br.containsHTML("Image has been removed from the server")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));

        /* Single pictures handling */
        if (parameter.contains("Zoom") || parameter.contains("View")) {
            String finallink = br.getRegex("<img src=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink("directhttp://" + finallink));
            return decryptedLinks;
        }

        /* Gallery handling */
        String fpName = br.getRegex("<title>(.*?)\\| Shareapic\\.net</title>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("NAME=\"Abstract\" CONTENT=\"(.*?)\">").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("Description\" CONTENT=\"(.*?)\">").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("font-stretch: normal; -x-system-font: none;\">(.*?)</h1>").getMatch(0);
                }
            }
        }
        String pagepiece = br.getRegex("<textarea(.*?)</textarea>").getMatch(0);
        if (pagepiece == null) return null;
        String[] links = new Regex(pagepiece, "\"(http://www.shareapic\\.net/View-[0-9]+)").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            if (abort) {
                progress.setColor(Color.RED);
                progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                progress.doFinalize(5000l);
                return new ArrayList<DownloadLink>();
            }
            link = link.replace("View", "Zoom");
            br.getPage(link + ".html");
            String finallink = br.getRegex("<img src=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink("directhttp://" + finallink);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }
    }
}
