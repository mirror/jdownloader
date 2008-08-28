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

import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ftp2share extends PluginForDecrypt {
    static private final String host = "ftp2share.net";
    private static final Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/file/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);

    static private final Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public ftp2share() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        
        try {
            Browser br = new Browser();

            if (parameter.matches(patternSupported_Folder.pattern())) {
                if (!parameter.contains("?system")) {
                    parameter = parameter + "?system=*";
                }

                br.getPage(parameter);

                String links[][] = new Regex(br, Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE)).getMatches();
                for (String[] element : links) {
                    String link = Encoding.Base64Decode(Encoding.filterString(element[0], "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                    decryptedLinks.add(createDownloadlink(link));
                }
            } else if (parameter.matches(patternSupported_File.pattern())) {

                br.getPage(parameter);
                Form[] forms = br.getForms();
                if (forms.length > 1) {
                    br.submitForm(forms[1]);

                }
                String links[][] = br.getRegex(Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE)).getMatches();
                for (String[] element : links) {
                    String link = Encoding.Base64Decode(Encoding.filterString(element[0], "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                    decryptedLinks.add(createDownloadlink(link));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
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
