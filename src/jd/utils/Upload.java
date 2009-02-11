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

package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPPost;
import jd.nutils.JDHash;
import jd.parser.Form;
import jd.parser.Regex;

public class Upload {
    public static String toJDownloader(String str, String desc) {
        try {
            Browser br = new Browser();
            String ret=br.postPage("http://service.jdownloader.net/tools/log.php", "upload=1&desc=" + Encoding.urlEncode(desc) + "&log=" + Encoding.urlEncode(str));
            return "http://www.jdownloader.org/pastebin/" + ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    public static String toPastebinCom(String str, String name) {
        Browser br = new Browser();
        br.setFollowRedirects(false);
        try {
            br.postPage("http://jd_" + JDHash.getMD5(str) + ".pastebin.com/pastebin.php", "parent_pid=&format=text&code2=" + URLEncoder.encode(str, "UTF-8") + "&poster=" + URLEncoder.encode(name, "UTF-8") + "&paste=Send&expiry=f&email=");
            if (br.getHttpConnection().isOK()) return br.getRedirectLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String toRapidshareComPremium(File file, String userid, String pass) {
        try {
            Browser br = new Browser();

            String[] data = br.getPage("http://rapidshare.com/cgi-bin/upload.cgi?intsysdata=1").split("\\,");
            HTTPPost up = new HTTPPost("http://rs" + data[0].trim() + "cg.rapidshare.com/cgi-bin/upload.cgi", true);
            up.setBoundary("----------070308143019350");
            up.doUpload();
            up.connect();
            up.sendVariable("toolmode2", "1");
            up.setForm("filecontent");
            up.sendFile(file.getAbsolutePath(), file.getName());
            up.sendVariable("freeaccountid", userid);
            up.sendVariable("password", pass);
            up.close();
            String code = up.read();
            String[] lines = Regex.getLines(code);
            return lines[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String toUploadedToPremium(File file, String username, String password) {
        try {
            Browser br = new Browser();
            br.getPage("http://uploaded.to/login");
            Form form = br.getForm(0);

            form.put("email", username);
            form.put("password", password);
            br.submitForm(form);
            br.getPage("http://uploaded.to/home");
            form = br.getForm(0);

            form.setFileToPost(file,null);
            form.action = br.getRegex("document..*?.action = \"(http://.*?.uploaded.to/up\\?upload_id=)\";").getMatch(0) + Math.round(10000 * Math.random()) + "0" + Math.round(10000 * Math.random());
            br.submitForm(form);
            br.getPage("http://uploaded.to/home");
            return br.getRegex("http://uploaded.to/\\?id=[A-Za-z0-9]+").getMatch(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";

    }
}