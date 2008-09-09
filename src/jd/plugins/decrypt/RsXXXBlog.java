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
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class RsXXXBlog extends PluginForDecrypt {
    static private final String host = "rs.xxx-blog.org";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?xxx-blog\\.org/[a-zA-Z0-9]{1,4}-[a-zA-Z0-9]{10,40}/.*", Pattern.CASE_INSENSITIVE);

    public RsXXXBlog(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        parameter = parameter.substring(parameter.lastIndexOf("http://"));
        br.getPage(parameter.replaceFirst("http://[\\w\\.]*?xxx-blog.org", "http://xxx-blog.org/frame"));
        if (br.getRedirectLocation() == null) return null;
        DownloadLink dl_link = createDownloadlink(br.getRedirectLocation());
        dl_link.addSourcePluginPassword("xxx-blog.dl.am");
        dl_link.addSourcePluginPassword("xxx-blog.org");
        decryptedLinks.add(dl_link);

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
