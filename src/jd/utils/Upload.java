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

package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPPost;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.HTTP;
import jd.plugins.RequestInfo;

public class Upload {
    public static String toJDownloader(String str, String desc) {
        try {
            RequestInfo ri = HTTP.postRequest(new URL("http://service.jdownloader.org/tools/log.php"), "upload=1&desc=" + Encoding.urlEncode(desc) + "&log=" + Encoding.urlEncode(str));

            return "http://service.jdownloader.org/tools/log.php?id=" + ri.getHtmlCode();
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;

    }

    // private static Logger logger= JDUtilities.getLogger();
    public static String toPastebinCom(String str, String name) {
        RequestInfo requestInfo = null;
        try {
            requestInfo = HTTP.postRequestWithoutHtmlCode(new URL("http://jd_" + JDUtilities.getMD5(str) + ".pastebin.com/pastebin.php"), null, null, "parent_pid=&format=text&code2=" + URLEncoder.encode(str, "UTF-8") + "&poster=" + URLEncoder.encode(name, "UTF-8") + "&paste=Send&expiry=f&email=", false);
        } catch (MalformedURLException e1) {

            e1.printStackTrace();
        } catch (IOException e1) {

            e1.printStackTrace();
        }
        if (requestInfo != null && requestInfo.isOK()) {

            return requestInfo.getLocation();

        } else {
            return null;
        }
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

   

    /*
     * public static boolean uploadToCollector(Plugin plugin, File Captcha) {
     * JDUtilities.getLogger().info("File:"+Captcha); if (
     * !plugin.collectCaptchas() || (JDUtilities.getController() != null &&
     * JDUtilities.getController().getWaitingUpdates() != null &&
     * JDUtilities.getController().getWaitingUpdates().size() > 0)) return
     * false; String Methodhash = "";
     * 
     * try { File f = new File(new File(new
     * File(JDUtilities.getJDHomeDirectoryFromEnvironment(),
     * JDUtilities.getJACMethodsDirectory()), plugin.getHost()), "letters.mth");
     * JDUtilities.getLogger().info("Methode:"+f); Methodhash =
     * JDUtilities.getLocalHash(f); } catch (Exception e) { // TODO: handle
     * exception } if(Methodhash==null || Methodhash=="") return false; try {
     * //http://jdcc.ath.cx HTTPConnection connection = new HTTPConnection( new
     * URL("http://ns2.km32221.keymachine.de/jdownloader/web/uploadcaptcha.php").openConnection());
     * int responseCode = HTTPConnection.HTTP_NOT_IMPLEMENTED; try {
     * responseCode = connection.getResponseCode(); } catch (IOException e) { }
     * RequestInfo requestInfo = new RequestInfo("<form action=\"\"
     * method=\"post\" enctype=\"multipart/form-data\">\n<input type=\"hidden\"
     * name=\"host\" value=\""+plugin.getHost()+"\">\n<input type=\"hidden\"
     * name=\"hash\" value=\""+Methodhash+"\">\n<input type=\"file\"
     * name=\"captcha\">\n</form>","http://jdcc.ath.cx", "", null,
     * responseCode); requestInfo.setConnection(connection); Form form =
     * requestInfo.getForm(); form.fileToPost=Captcha; try {
     * if(form.getRequestInfo().getHtmlCode().contains("true")) return true; }
     * catch (Exception e) { // TODO: handle exception } } catch
     * (MalformedURLException e1) { e1.printStackTrace(); } catch (IOException
     * e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
     * 
     * return false; } public static boolean sendToCaptchaExchangeServer(Plugin
     * plugin, String PixelString, String Character) { // if
     * (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_CAPTCHA_EXCHANGE_SERVER,
     * true) || (JDUtilities.getController() != null &&
     * JDUtilities.getController().getWaitingUpdates() != null &&
     * JDUtilities.getController().getWaitingUpdates().size() > 0)) // return
     * false; String Methodhash = ""; File f=null; try { f = new File(new
     * File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(),
     * JDUtilities.getJACMethodsDirectory()), plugin.getHost()), "letters.mth");
     * JDUtilities.getLogger().info("Methode:"+f); Methodhash =
     * JDUtilities.getLocalHash(f); } catch (Exception e) { // TODO: handle
     * exception } //if(Methodhash==null || Methodhash=="") // return false; try {
     * //http://jdcc.ath.cx HTTPConnection connection = new HTTPConnection(new
     * URL("http://ns2.km32221.keymachine.de/jdownloader/update/").openConnection());
     * int responseCode = HTTPConnection.HTTP_NOT_IMPLEMENTED; try {
     * responseCode = connection.getResponseCode(); } catch (IOException e) { }
     * JDUtilities.getLogger().info("Upload "+Character); RequestInfo
     * requestInfo = new RequestInfo("<form action=\"captchaexchange.php\"
     * method=\"post\">\n<input type=\"hidden\" name=\"character\"
     * value=\""+Character+"\">\n<input type=\"hidden\" name=\"version\"
     * value=\""+1+"\">\n<input type=\"hidden\" name=\"pixelstring\"
     * value=\""+PixelString+"\">\n<input type=\"hidden\" name=\"host\"
     * value=\""+plugin.getHost()+"\">\n<input type=\"hidden\" name=\"hash\"
     * value=\""+Methodhash+"\">\n</form>","http://ns2.km32221.keymachine.de/jdownloader/update/",
     * "", null, responseCode); requestInfo.setConnection(connection); Form form =
     * requestInfo.getForm(); String ret=form.getRequestInfo().getHtmlCode(); //
     * JDUtilities.getLogger().info(ret); if(ret.indexOf("ERROR")<0&&ret.indexOf("jDownloader")>0){
     * JDUtilities.writeLocalFile(f, ret); JDUtilities.getLogger().info("Updated
     * Method "+f); } } catch (MalformedURLException e1) { // TODO
     * Auto-generated catch block e1.printStackTrace(); } catch (IOException e1) { //
     * TODO Auto-generated catch block e1.printStackTrace(); }
     * 
     * return false; }
     */

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

            form.setFileToPost(file);
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