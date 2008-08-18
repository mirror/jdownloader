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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class AnimeANet extends PluginForDecrypt {
    final static String host = "animea.net";
    private Pattern patternSupported_Series = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported_Episode = Pattern.compile("http://[\\w\\.]*?animea\\.net/download/[\\d]+-[\\d]+/(.*?)\\.html", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported = Pattern.compile(patternSupported_Series.pattern() + "|" + patternSupported_Episode.pattern(), Pattern.CASE_INSENSITIVE);

    public AnimeANet() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        parameter = parameter.replaceAll(" ", "+");

        if (patternSupported_Series.matcher(parameter).matches()) {
            logger.info(parameter + " gematcht Nr1");
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("<a href=\"/download/(.*?)\\.html\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(links.length);
            logger.info(links.length + "");
            for (String element : links) {
                logger.info(element);
                decryptedLinks.add(createDownloadlink("http://www.animea.net/download/" + element + ".html"));
                progress.increase(1);
            }
        } else {
            logger.info(parameter + " gematcht Nr2");
            String[] links = new Regex(br.getPage(parameter), Pattern.compile("<a href=\"(.*?)\" rel=\"nofollow\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
            progress.setRange(links.length);
            logger.info(links.length + "");
            for (String element : links) {
                logger.info(element);
                decryptedLinks.add(createDownloadlink(element));
                progress.increase(1);
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}