package jd.utils;

import java.io.File;

import jd.JDInit;

public class testUpload {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
        JDInit init = new JDInit();
        init.loadConfiguration();
        init.initPlugins();
       	new Thread(new Runnable(){

			public void run() {

				Upload.uploadToCollector(JDUtilities.getPluginForHost("rapidshare.com"), new File("/home/dwd/.jd_home/captchas/rapidshare.com/24.02.2008_22.36.42_DOWNLOAD+VIA+DEUTSCHE+TELEKOM_ZIII_BAD.jpg"));
				
			}}).start();
	}

}
