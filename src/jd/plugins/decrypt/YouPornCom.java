//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class YouPornCom extends PluginForDecrypt {

    private static final Pattern DOWNLOADFILE = Pattern.compile("var player_url = '(.*?)';", Pattern.CASE_INSENSITIVE);

    public YouPornCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);

        br.postPage(parameter, "user_choice=Enter");
        String matches = br.getRegex(DOWNLOADFILE).getMatch(0);
        String filename = br.getRegex("<meta name=\"title\" content=\"YOUPORN - (.*?)\" />").getMatch(0);
        if (matches == null) { return null; }
        matches = matches.replaceAll("&xml=1", "");
        DownloadLink dlink = createDownloadlink(matches);
        if (filename != null) dlink.setStaticFileName(filename.trim().replaceAll(" ", "-") + ".flv");
        dlink.setBrowserUrl(parameter);
        decryptedLinks.add(dlink);

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

}
