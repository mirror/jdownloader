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


package jd.captcha.loader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import jd.parser.SimpleMatches;
import jd.plugins.HTTP;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * Diese Klasse lädt uploade.to Captchas
 * 
 * 
 * @author JD-Team
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
            load(new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/jd/captcha/methods" + "/" + hoster + "/captchas/"), link);

            counter++;

        }

    }

    private String getCaptchaName() {
        return hoster + "_" + initTime + "_captcha_" + counter + ".jpg";
    }

    private void load(File file, String link) {

        try {
            ri = HTTP.getRequest(new URL(link));

            String captchaURL = "http://uploaded.to/" + ri.getRegexp("<img name=\"img_captcha\" src=\"(.*?)\"");
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