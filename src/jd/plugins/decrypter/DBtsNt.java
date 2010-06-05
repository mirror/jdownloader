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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.*;

import java.io.IOException;
import java.util.ArrayList;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "audiobeats.net" }, urls = { "http://[\\w\\.]*?audiobeats\\.net/(liveset|link|event|artist)\\?id=\\d+" }, flags = { 0 })
public class DBtsNt extends PluginForDecrypt {
    
    private String fpName;
    private ArrayList<DownloadLink> decryptedLinks;

    public DBtsNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        decryptedLinks = new ArrayList<DownloadLink>();

        //TODO: beim hinzufügen von Events oder Artisten etc. sollte der Linkgrabber gleiche links auch als gleiche Links erkennen

        Regex urlInfo = new Regex(parameter, "http://[\\w\\.]*?audiobeats\\.net/(liveset|link|event|artist)\\?id=(\\d+)");
        String type = urlInfo.getMatch(0);
        String id = urlInfo.getMatch(1);

        if (type.equals("link")) {

            progress.increase(1);            
            String link = "/link?id=" + id;
            if (!decryptSingleLink(parameter.toString(), progress, decryptedLinks, link)) return null;
        } else if(type.equals("liveset")) {

            if (fpName == null) fpName = br.getRegex("<title>AudioBeats\\.net - (.*?)</title>").getMatch(0);
            
            if (!decryptLiveset(parameter.toString(), progress, decryptedLinks)) return null;
        } else if(type.equals("event") || type.equals("artist")) {
            
            br.getPage(parameter.toString());
            String[] allLivesets = br.getRegex("\"(/liveset\\?id=\\d+)\"").getColumn(0);

            if (allLivesets == null || allLivesets.length == 0) return null;
            if (fpName == null) fpName = br.getRegex("<title>AudioBeats\\.net - (.*?)</title>").getMatch(0);

            progress.setRange(allLivesets.length);
            for (String aLiveset : allLivesets) {
                if (!decryptLiveset(aLiveset, progress, decryptedLinks)) return null;
                Thread.sleep(1000);
            }
        }

        return decryptedLinks;
    }

    private boolean decryptLiveset(String parameter, ProgressController progress, ArrayList<DownloadLink> decryptedLinks) throws IOException {
        int i = 0;
        do{
            br.getPage(parameter);

            if(br.containsHTML("Error 403")) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            } else {

                String[] allLinks = br.getRegex("\"(/link\\?id=\\d+)\"").getColumn(0);

                if (allLinks == null || allLinks.length == 0) return false;
                progress.setRange(progress.getMax() + allLinks.length);
                for (String aLink : allLinks) {
                    if (!decryptSingleLink(parameter, progress, decryptedLinks, aLink)) return false;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {}
                }

                String fpName = br.getRegex("<title>AudioBeats\\.net - (.*?)</title>").getMatch(0);
                if (fpName == null) fpName = br.getRegex("rel=\"alternate\" type=\".*?\" title=\"(.*?)\"").getMatch(0);

                setPackageName();
                break;
            }

        } while(i<5);

        return true;
    }

    private void setPackageName() {
        if (fpName != null) {

            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
    }

    private boolean decryptSingleLink(String parameter, ProgressController progress, ArrayList<DownloadLink> decryptedLinks, String aLink) throws IOException {
        br.setFollowRedirects(false);
        br.getPage(aLink);
        if (br.getRedirectLocation() == null) {
            logger.warning("Decrypter must be defect, link = " + parameter);
            return false;
        }
        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        progress.increase(1);

        return true;
    }

}
