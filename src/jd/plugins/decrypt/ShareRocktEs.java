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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ShareRocktEs extends PluginForDecrypt {

    private static final Pattern PATTERN_SUPPORTED_GO = Pattern.compile("http://[\\w\\.]*?share\\.rockt\\.es/\\?go=(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTEREN_SUPPORTED_V = Pattern.compile("http://[\\w\\.]*?share\\.rockt\\.es/\\?v=\\w+", Pattern.CASE_INSENSITIVE);
    private static final String CODER = "JD-Team";
    public ShareRocktEs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        String parameter = param.toString();

        String page = br.getPage(parameter);
        br.setFollowRedirects(false);
        String[] matches;
        if (new Regex(parameter, PATTEREN_SUPPORTED_V).matches()) {
            matches = new Regex(page, Pattern.compile("window\\.open\\('\\?go=(.*?)','_blank'\\)")).getColumn(0);
        } else {
            String match = new Regex(parameter, PATTERN_SUPPORTED_GO).getMatch(0);
            if (match == null) { return null; }
            matches = new String[] { match };
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String followlink = null;
        String linksite = null;
        for (String match : matches) {
            linksite = br.getPage("http://share.rockt.es/?go=" + match);
            boolean gotLink = false;
            while (!gotLink) {
                if ((followlink = new Regex(linksite, Pattern.compile("document\\.location='\\?go=(.*?)'")).getMatch(0)) != null) {
                    linksite = br.getPage("http://share.rockt.es/?go=" + followlink);
                } else {
                    gotLink = true;
                    String link = new Regex(linksite, Pattern.compile("<iframe src='(.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0);
                    if (link != null) {
                        decryptedLinks.add(createDownloadlink(link));
                    } else {
                        link = new Regex(linksite, Pattern.compile("document\\.location='(.*?)'", Pattern.CASE_INSENSITIVE)).getMatch(0);
                        if (link != null) {
                            decryptedLinks.add(createDownloadlink(link));
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
