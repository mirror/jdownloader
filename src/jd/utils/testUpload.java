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
		Upload.uploadToCollector(JDUtilities.getPluginForHost("rapidshare.com"), new File("/home/dwd/.jd_home/captchas/rapidshare.com/24.02.2008_19.17.58_DOWNLOAD+VIA+DEUTSCHE+TELEKOM_IIII_BAD.jpg"));

	}

}
