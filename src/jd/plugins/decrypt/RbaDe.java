//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RbaDe extends PluginForDecrypt {

    private static final Pattern BATTLE_REL_PATH = Pattern.compile("(index\\.php\\?ID=4101&(amp;)?BATTLE=\\d+(&sid=\\w+)?)", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SUPPORTED_BATTLE = Pattern.compile("http://[\\w\\.]*?r-b-a\\.de/" + BATTLE_REL_PATH, Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_SUPPORTED_USER = Pattern.compile("http://[\\w\\.]*?r-b-a\\.de/index\\.php\\?ID=4100(&direction=last)?&MEMBER=\\d+(&sid=\\w+)?", Pattern.CASE_INSENSITIVE);

    private static final Pattern REGEX_DOWNLOADLINK = Pattern.compile("(download\\.php\\?FILE=(\\d+)-(\\d)\\.mp3&(amp;)?PATH=\\d)");

    public RbaDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink,ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.clearCookies(getHost());
        br.getPage(cryptedLink.getCryptedUrl());

        String[] links = null;
        if (new Regex(cryptedLink, PATTERN_SUPPORTED_BATTLE).matches()) {
            links = br.getRegex(REGEX_DOWNLOADLINK).getColumn(0);
        } else if (new Regex(cryptedLink, PATTERN_SUPPORTED_USER).matches()) {
            links = br.getRegex(BATTLE_REL_PATH).getColumn(0);
        }
        if (links == null) return null;

        for (String link : links) {
            decryptedLinks.add(createDownloadlink("http://www.r-b-a.de/" + link));
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
