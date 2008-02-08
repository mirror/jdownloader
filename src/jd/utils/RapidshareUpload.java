package jd.utils;

import java.io.File;

import jd.plugins.Form;

public class RapidshareUpload {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Form form= Form.getForms("http://rapidshare.com/")[0];
			form.fileToPost=new File("/home/dwd/wallpaper-1280x1024-007.jpg");
			System.out.println(form.getRequestInfo().getHtmlCode());
			System.out.println(form.toString());
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

}
