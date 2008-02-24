package jd.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jd.config.Configuration;
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
         Vector<Vector<String>> matches = Plugin.getAllSimpleMatches(code,"name=\"upload_form\" °_filesize' value='°' />° <input type='hidden' name='°_filename' value='°' />");
                 
      Vector<String> match = matches.get(0);
    
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
    public static boolean uploadToCollector(Plugin plugin, File Captcha)
    {	
    	JDUtilities.getLogger().info("File:"+Captcha);
        if (!JDUtilities.getConfiguration().getBooleanProperty(Configuration.USE_CAPTCHA_COLLECTOR, true) || !plugin.collectCaptchas || (JDUtilities.getController() != null && JDUtilities.getController().getWaitingUpdates() != null && JDUtilities.getController().getWaitingUpdates().size() > 0)) 
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
			HttpURLConnection connection = (HttpURLConnection) new URL("http://jdcc.ath.cx").openConnection();
			int responseCode = HttpURLConnection.HTTP_NOT_IMPLEMENTED;
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
}