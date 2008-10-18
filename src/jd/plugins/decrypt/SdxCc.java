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
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

public class SdxCc extends PluginForDecrypt {
    //TODO decrypter in arbeit wird heute 18.10 noch fertig gemacht! (gn8)
    //TODO könnte sogar schon funktionieren.
    private static final Pattern PATTERN_DOWNLOADLINK_TEAM     = Pattern.compile("<td align='center' valign='bottom'><b><a href='(file\\.php\\?did=\\d+&file_id=\\d+)' target='_blank'><u><h2>D O W N L O A D</h2></u></a></b>",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_DOWNLOADLINK_USER     = Pattern.compile("<td align='center' valign='bottom'><b><a href='\\.\\./\\.\\./(infusions/user_uploads/file\\.php\\?did=\\d+&file_id=\\d+)' target='_blank'><u><h2>D O W N L O A D</h2></u></a>");
    private static final Pattern PATTERN_DOWNLOADLINK_CENTER   = Pattern.compile("<center><div class='quote'><b><a href='(.+?)' target='_blank'>2\\.SFT</a></b></div></center><br />");
    private static final Pattern PATTERN_PASSWORD              = Pattern.compile("</tr><tr><td align='center' class='tbl2'>Passwort:<br>(.+?)</td></tr><tr>",Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FOLDER_NAME           = Pattern.compile("<script type='text/javascript' src='\\.\\./\\.\\./includes/forum\\.js'></script><font size='\\+\\d+'><b>(.+?)</b></font>");      
    public SdxCc(PluginWrapper wrapper) {
        super(wrapper);
    }
    /*
    public static void main(String[] args) throws IOException {
        Browser br = new Browser();
        br.setFollowRedirects(false);
        String page = br.getPage("http://www.sdx.cc/infusions/user_uploads/download.php?did=8902");
        String pw = new Regex(page,PATTERN_PASSWORD).getMatch(0);
        String name = new Regex(page,PATTERN_FOLDER_NAME).getMatch(0);
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_TEAM).getColumn(0)){
            br.getPage("http://www.sdx.cc/infusions/pro_download_panel/"+link);
            System.out.println(br.getRedirectLocation());
            System.out.println(pw);
            System.out.println(name);

        }
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_USER).getColumn(0)){
            System.out.println("http://www.sdx.cc/infusions/pro_download_panel/"+link);
            br.getPage("http://www.sdx.cc/"+link);
            System.out.println(br.getRedirectLocation());
            System.out.println(pw);
            System.out.println(name);
        }
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_CENTER).getColumn(0)){
            System.out.println(link);
        }   
    }*/

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = cryptedLink.getCryptedUrl();
        String page = br.getPage(url);
        String pw = new Regex(page,PATTERN_PASSWORD).getMatch(0);
        String name = new Regex(page,PATTERN_FOLDER_NAME).getMatch(0);
        name = name!=null?name.trim():"";
        pw   = pw  !=null?pw.trim()  :"sdx.cc";
        br.setFollowRedirects(false);
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_TEAM).getColumn(0)){
            br.getPage("http://www.sdx.cc/infusions/pro_download_panel/"+link);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));     
        }
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_USER).getColumn(0)){
            br.getPage("http://www.sdx.cc"+link);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        }
        for(String link:new Regex(page, PATTERN_DOWNLOADLINK_CENTER).getColumn(0)){
            decryptedLinks.add(createDownloadlink(link));
        }
        FilePackage fp = new FilePackage();
        fp.setName(name);
        for(DownloadLink dlLink:decryptedLinks){
            dlLink.setFilePackage(fp);
            dlLink.addSourcePluginPassword(pw);
            dlLink.setDecrypterPassword(pw);
        }
        return decryptedLinks.size()>0?decryptedLinks:null;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
