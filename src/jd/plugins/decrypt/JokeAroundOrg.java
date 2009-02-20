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

package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class JokeAroundOrg extends PluginForDecrypt {

    public JokeAroundOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String wait = br.getRegex(";sec=(\\d+);var").getMatch(0);
        if (wait != null) {
            this.sleep(Integer.parseInt(wait) * 1000l, param);
        }
        String forward = br.getRegex("document.location='(/!.*?-.*?/)';").getMatch(0);
        if (forward != null) {
            decryptedLinks.add(createDownloadlink("http://joke-around.org" + forward));
            return decryptedLinks;
        }
        Form form = br.getForm(0);
        form.setAction(br.getRegex("action=\"(.*?)\"").getMatch(0));
        br.submitForm(form);
        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}
