//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.plugins.Form;
import jd.plugins.HTTPPost;
import jd.plugins.Plugin;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;


public class Upload {
   //private static Logger logger= JDUtilities.getLogger();
   public static String toPastebinCom(String str,String name){
       RequestInfo requestInfo=null;
       try {
         requestInfo = Plugin.postRequestWithoutHtmlCode(new URL("http://jd_"+JDUtilities.getMD5(str)+".pastebin.com/pastebin.php"), null, null, "parent_pid=&format=text&code2="+URLEncoder.encode(str,"UTF-8")+"&poster="+URLEncoder.encode(name,"UTF-8")+"&paste=Send&expiry=f&email=", false);
     }
     catch (MalformedURLException e1) {
         
         e1.printStackTrace();
     }
     catch (IOException e1) {
       
         e1.printStackTrace();
     }
     if(requestInfo!=null &&requestInfo.isOK()){
         
             return requestInfo.getLocation();
       
    

     }else{
         return null;
     }
   }
   
   public static String toPastebinCa(String str,String name,String desc,String pw){
       try {
           //Logger logger = JDUtilities.getLogger();
           //RequestInfo ri = Plugin.getRequest(new URL("http://uploaded.to"));
           Form[] forms = Form.getForms("http://pastebin.ca/index.php");
           Form form=null;
           for( int i=0; i<forms.length;i++){
              
               if(forms[i].vars.containsKey("content")){
                   form=forms[i];
                   break;
               }
           }
           if(form==null)return null;
           //logger.info("iiiii"+form);
           
           form.vars.put("content", str);
           form.vars.put("description", desc);
           form.vars.put("name", name);
           form.vars.put("type", "1");
           form.vars.put("encryptpw", pw);
           if(pw==null){
               form.vars.remove("encrypt");
           }
           form.action="http://pastebin.ca/index.php";
           RequestInfo ri = form.getRequestInfo();
           //
           if(!ri.containsHTML("Ihr Paste wurde angenommen"))return null;
           
           String ret=Plugin.getSimpleMatch(ri.getHtmlCode(), "Die URL lautet:</p><p><a href=\"/°\">http://pastebin", 0);
     
      return "http://pastebin.ca/"+ret;
       }catch(Exception e){
           
       }
       
       return null;
   }
   
   public static String toRamzahlCom(File file) {
       try {
           Form form= Form.getForms("http://ramzal.com/upload")[0];
           form.fileToPost=file;
           return new Regexp(form.getRequestInfo(), "URL:</b> <a href=(http://ramzal.com//?upload_files/.*?)>http://ramzal.com//?upload_files/.*?</a></font>").getFirstMatch();
           //
       } catch (Exception e) {
           // TODO: handle exception
       }
return "";
   }
   public static String toRapidshareCom(File file) {
       try {
           Form form= Form.getForms("http://rapidshare.com/")[0];
           form.fileToPost=file;
           return new Regexp(form.getRequestInfo(), ":</td><td><a href=\"(http://rapidshare.com/files/.*?)\" target=\"_blank\">http://rapidshare.com/files/").getFirstMatch();
       } catch (Exception e) {
           // TODO: handle exception
       }
return "";
   }
   
   
   public static String toUploadedToPremium(File file,String username,String password) {
       try {
     
          
           Form form= Form.getForms("http://uploaded.to/login")[0];
           form.put("email", username);
           form.put("password", password);
           form.withHtmlCode=false;
           String cookie = form.getRequestInfo(false).getCookie();
           RequestInfo reqestinfo = Plugin.getRequest(new URL("http://uploaded.to/home"), cookie, null, true);
           form = Form.getForms(reqestinfo)[0];
           form.fileToPost=file;
           form.setRequestPopertie("Cookie", cookie);
           form.action=new Regexp(reqestinfo.getHtmlCode(), "document..*?.action = \"(http://.*?.uploaded.to/up\\?upload_id=)\";").getFirstMatch()+Math.round(10000*Math.random())+"0"+Math.round(10000*Math.random());
           reqestinfo = form.getRequestInfo();
           return new Regexp(Plugin.getRequest(new URL("http://uploaded.to/home"), cookie, null, true).getHtmlCode(), "http://uploaded.to/\\?id=[A-Za-z0-9]+").getFirstMatch();
       } catch (Exception e) {
           e.printStackTrace();
       }
       return "";

   }

   
   
   
    public static String toUploadedTo(String str,String name){
        try {
            //RequestInfo ri = Plugin.getRequest(new URL("http://uploaded.to"));
            Form[] forms = Form.getForms("http://uploaded.to");
            if(forms.length>0){
                Iterator<Entry<String, String>> it = forms[0].formProperties.entrySet().iterator();
               String action=null;
                while(it.hasNext()){
                    String value;
                    if((value=it.next().getKey()).endsWith("upload_id")){
                        action=value;
                        break;
                    }
                   
                }
                if(action==null||action.split("http").length<2){
                   
                    return null;
                }
           action="Http"+action.split("http")[1]+"=";
           String uid=Math.round(10000*Math.random())+"0"+Math.round(10000*Math.random());
           
           HTTPPost up = new HTTPPost(action+uid, true);
           up.setBoundary("---------------------------19777693011381");
           up.doUpload();
           up.connect();
           up.sendVariable("uploadcount", forms[0].vars.get("uploadcount"));
           up.sendVariable("x", "42");
           up.sendVariable("y", "13");
           up.sendVariable("lang", forms[0].vars.get("lang"));
           up.setForm("file1x");
           up.sendBytes(name, str.getBytes(), str.getBytes().length);
           up.sendVariable("la_id", forms[0].vars.get("la_id"));
           up.sendVariable("domain_id", forms[0].vars.get("domain_id"));
           up.sendVariable("inline", "on");
           up.close();
           String code = up.getRequestInfo().getHtmlCode();          
           ArrayList<ArrayList<String>> matches = Plugin.getAllSimpleMatches(code,"name=\"upload_form\" °_filesize' value='°' />° <input type='hidden' name='°_filename' value='°' />");
                 
           ArrayList<String> match = matches.get(0);
    
      String id = match.get(match.size()-2);
      String dlLinkID=id.substring(0,6);
      return "http://uploaded.to/?id="+dlLinkID;
            }else{
                return null;
            }
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        
        
        return null;
    }
    /*
    public static boolean uploadToCollector(Plugin plugin, File Captcha)
    {	
    	JDUtilities.getLogger().info("File:"+Captcha);
        if ( !plugin.collectCaptchas() || (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0)) 
            return false;
        String Methodhash = "";

        try {
        	File f = new File(new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), JDUtilities.getJACMethodsDirectory()), plugin.getHost()), "letters.mth");
        	JDUtilities.getLogger().info("Methode:"+f);
        	Methodhash = JDUtilities.getLocalHash(f);
		} catch (Exception e) {
			// TODO: handle exception
		}
		if(Methodhash==null || Methodhash=="")
			return false;
		try {
		    //http://jdcc.ath.cx
		    HTTPConnection connection = new HTTPConnection( new URL("http://ns2.km32221.keymachine.de/jdownloader/web/uploadcaptcha.php").openConnection());
			int responseCode = HTTPConnection.HTTP_NOT_IMPLEMENTED;
			try {
				responseCode = connection.getResponseCode();
			} catch (IOException e) {
			}
			RequestInfo requestInfo = new RequestInfo("<form action=\"\" method=\"post\" enctype=\"multipart/form-data\">\n<input type=\"hidden\" name=\"host\" value=\""+plugin.getHost()+"\">\n<input type=\"hidden\" name=\"hash\" value=\""+Methodhash+"\">\n<input type=\"file\" name=\"captcha\">\n</form>","http://jdcc.ath.cx", "", null, responseCode);
			requestInfo.setConnection(connection);
			Form form = requestInfo.getForm();
			form.fileToPost=Captcha;
			try {
				if(form.getRequestInfo().getHtmlCode().contains("true"))
					return true;
			} catch (Exception e) {
				// TODO: handle exception
			}

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		return false;
    	
    }
    public static boolean sendToCaptchaExchangeServer(Plugin plugin, String PixelString, String Character)
    {	
//        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_CAPTCHA_EXCHANGE_SERVER, true) || (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0)) 
//            return false;
        String Methodhash = "";
        File f=null;
        try {
        	f = new File(new File(new File(JDUtilities.getJDHomeDirectoryFromEnvironment(), JDUtilities.getJACMethodsDirectory()), plugin.getHost()), "letters.mth");
        	JDUtilities.getLogger().info("Methode:"+f);
        	Methodhash = JDUtilities.getLocalHash(f);
		} catch (Exception e) {
			// TODO: handle exception
		}
		//if(Methodhash==null || Methodhash=="")
		//	return false;
		try {
		    //http://jdcc.ath.cx
		    HTTPConnection connection = new HTTPConnection(new URL("http://ns2.km32221.keymachine.de/jdownloader/update/").openConnection());
			int responseCode = HTTPConnection.HTTP_NOT_IMPLEMENTED;
			try {
				responseCode = connection.getResponseCode();
			} catch (IOException e) {
			}
			JDUtilities.getLogger().info("Upload "+Character);
			RequestInfo requestInfo = new RequestInfo("<form action=\"captchaexchange.php\" method=\"post\">\n<input type=\"hidden\" name=\"character\" value=\""+Character+"\">\n<input type=\"hidden\" name=\"version\" value=\""+1+"\">\n<input type=\"hidden\" name=\"pixelstring\" value=\""+PixelString+"\">\n<input type=\"hidden\" name=\"host\" value=\""+plugin.getHost()+"\">\n<input type=\"hidden\" name=\"hash\" value=\""+Methodhash+"\">\n</form>","http://ns2.km32221.keymachine.de/jdownloader/update/", "", null, responseCode);
			requestInfo.setConnection(connection);
			Form form = requestInfo.getForm();
			String ret=form.getRequestInfo().getHtmlCode();
			//	JDUtilities.getLogger().info(ret);
			if(ret.indexOf("ERROR")<0&&ret.indexOf("jDownloader")>0){
				JDUtilities.writeLocalFile(f, ret);
				JDUtilities.getLogger().info("Updated Method "+f);
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
		return false;
    	
    }
    */
}