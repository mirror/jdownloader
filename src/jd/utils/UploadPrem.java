package jd.utils;

import java.io.File;
import java.net.URL;

import jd.plugins.Form;
import jd.plugins.Plugin;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;

public class UploadPrem {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			String username = "57310";
			String password = "qryhivz";
			File file = new File("/home/dwd/wallpaper-1280x1024-007.jpg");
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
			String link = new Regexp(Plugin.getRequest(new URL("http://uploaded.to/home"), cookie, null, true).getHtmlCode(), "http://uploaded.to/\\?id=[A-Za-z0-9]+").getFirstMatch();
			System.out.println(link);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
