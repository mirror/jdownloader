package jd.captcha.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import jd.plugins.Plugin;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Diese Klasse lädt uploade.to Captchas
 * 
 * 
 * @author coalado
 */
public class UploadedTo {
    /**
     * @param args
     */
    public static void main(String args[]) {

        UploadedTo main = new UploadedTo();
        main.go();
    }

    private RequestInfo ri;

    private int         counter = 0;

    private long        initTime;

    private Logger      logger  = JDUtilities.getLogger();

    private String      hoster;

    private void go() {
        String link = "http://uploaded.to/?id=v1zo22";
        hoster = "Uploaded.to";

        initTime = new Date().getTime();
        while (true) {
            load(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath() + "/jd/captcha/methods" + "/" + hoster + "/captchas/"), link);

            counter++;

        }

    }

    private String getCaptchaName() {
        return hoster + "_" + initTime + "_captcha_" + counter + ".jpg";
    }

    private void load(File file, String link) {

        try {
            ri = Plugin.getRequest(new URL(link));

            String captchaURL = "http://uploaded.to/" + Plugin.getSimpleMatch(ri.getHtmlCode(), "<img name=\"img_captcha\" src=\"°\"", 0);
            JDUtilities.download(new File(file, getCaptchaName()), captchaURL);

            logger.info(captchaURL+" - "+new File(file, getCaptchaName()));
        }
        catch (MalformedURLException e) {
            // TODO Auto-generated catch block
             e.printStackTrace();
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
             e.printStackTrace();
        }
    }

}