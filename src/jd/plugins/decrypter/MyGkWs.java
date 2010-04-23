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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mygeek.ws" }, urls = { "http://[\\w\\.]*?mygeek\\.ws/go/folder/id/[a-z0-9]+" }, flags = { 0 })
public class MyGkWs extends PluginForDecrypt implements ProgressControllerListener {

    public MyGkWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    private boolean abort = false;

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        progress.getBroadcaster().addListener(this);
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String[] links = br.getRegex("<td><a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) links = br.getRegex("\"(http://mygeek\\.ws/go/link/id/[a-z0-9]+)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String dl : links) {
            if (abort) {
                progress.setColor(Color.RED);
                progress.setStatusText(progress.getStatusText() + ": " + JDL.L("gui.linkgrabber.aborted", "Aborted"));
                progress.doFinalize(5000l);
                return new ArrayList<DownloadLink>();
            }
            br.getPage(dl);
            String goOn = br.getRegex("document\\.location='(/.*?)';").getMatch(0);
            if (goOn == null) {
                goOn = br.getRegex("klicken Sie bitte <a href=\"(/.*?)\"").getMatch(0);
                if (goOn == null) {
                    goOn = br.getRegex("(\"|')(/go/link/id/[0-9a-z]+/q1/\\d+/q2/\\d+)(\"|')").getMatch(1);
                }
            }
            if (goOn == null) return null;
            goOn = "http://mygeek.ws" + goOn;
            br.getPage(goOn);
            String finallink = br.getRegex("document\\.location='(.*?)';").getMatch(0);
            if (finallink == null) {
                finallink = br.getRegex("klicken Sie bitte <a href=\"(.*?)\"").getMatch(0);
            }
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

    public void onProgressControllerEvent(ProgressControllerEvent event) {
        if (event.getID() == ProgressControllerEvent.CANCEL) {
            abort = true;
        }
    }
}
