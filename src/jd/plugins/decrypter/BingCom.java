//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bing.com" }, urls = { "http://(www\\.)?bing\\.com/videos/watch/video/.*?/[a-z0-9]+" }, flags = { 0 })
public class BingCom extends PluginForDecrypt {

    public static final Pattern PATTERN_JAVASCRIPT_HEX = Pattern.compile("\\\\x([a-f0-9]{2})", Pattern.CASE_INSENSITIVE);

    public BingCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String filename = br.getRegex("<title>(.*?) \\- Bing Videos</title>").getMatch(0);
        boolean setFilename = false;
        if (filename != null) {
            setFilename = true;
            filename = filename.replace(":", "-");
        }
        final String[] regexes = { "formatCode: 1003, url: \\'(http.*?\\.flv)\\'", "formatCode: 1002, url: \\'(http.*?\\.wmv)\\'", "formatCode: 101, url: \\'(http.*?\\.mp4)\\'" };
        for (String regex : regexes) {
            String finallink = br.getRegex(regex).getMatch(0);
            if (finallink != null) {
                finallink = decodeJavascriptHex(finallink);
                DownloadLink fnllink = createDownloadlink(finallink);
                if (setFilename) {
                    String ext = finallink.substring(finallink.lastIndexOf("."));
                    if (ext == null || ext.length() > 5) ext = ".flv";
                    fnllink.setFinalFileName(filename + ext);
                }
                decryptedLinks.add(fnllink);
            }
        }
        return decryptedLinks;
    }

    public String decodeJavascriptHex(final String javascriptHexString) {
        StringBuffer sb = new StringBuffer();
        Matcher m = PATTERN_JAVASCRIPT_HEX.matcher(javascriptHexString);
        while (m.find()) {
            m.appendReplacement(sb, JDHexUtils.toString(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
