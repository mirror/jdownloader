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

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ShareRocktEs extends PluginForDecrypt {
    private static final String CODER = "ToKaM";
    private static final String HOST = "share.rockt.es";

    private static final Pattern patternSupported_go = Pattern.compile("http://[\\w\\.]*?share\\.rockt\\.es/\\?go=(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_v = Pattern.compile("http://[\\w\\.]*?share\\.rockt\\.es/\\?v=\\w+", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported = Pattern.compile(patternSupported_v.pattern() + "|" + patternSupported_go.pattern(), Pattern.CASE_INSENSITIVE);
    private Browser br;

    public ShareRocktEs() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        br = new Browser();
        String page = br.getPage(cryptedLink);
        br.setFollowRedirects(false);
        String[] matches;
        if (new Regex(cryptedLink, patternSupported_v).matches()) {
            matches = new Regex(page, Pattern.compile("window\\.open\\('\\?go=(.*?)','_blank'\\)")).getMatches(1);
        } else {
            String match = new Regex(cryptedLink, patternSupported_go).getFirstMatch();
            if (match == null) {
                return null;
            }
            matches = new String[] { match };
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String followlink = null;
        String linksite = null;
        for (String match : matches) {
            linksite = br.getPage("http://share.rockt.es/?go=" + match);
            boolean gotLink = false;
            while (!gotLink) {
                if ((followlink = new Regex(linksite, Pattern.compile("document\\.location='\\?go=(.*?)'")).getFirstMatch()) != null) {
                    linksite = br.getPage("http://share.rockt.es/?go=" + followlink);
                } else {
                    gotLink = true;
                    String link = new Regex(linksite, Pattern.compile("<iframe src='(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    if (link != null) {
                        decryptedLinks.add(createDownloadlink(link));
                    } else {
                        link = new Regex(linksite, Pattern.compile("document\\.location='(.*?)'", Pattern.CASE_INSENSITIVE)).getFirstMatch();
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
    public boolean doBotCheck(File file) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision: 2086 $", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
