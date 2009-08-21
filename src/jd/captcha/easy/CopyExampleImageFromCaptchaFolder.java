package jd.captcha.easy;

import java.io.File;
import java.io.FilenameFilter;

import jd.nutils.io.JDIO;

public class CopyExampleImageFromCaptchaFolder {

    /**
     * kopiert ein exampleimage aus dem Captchaordner in den methodenordner
     * @param args
     */
    public static void main(String[] args) {
        EasyMethodeFile[] list = EasyMethodeFile.getMethodeList();
        for (EasyMethodeFile easyMethodeFile : list) {
            File exf = easyMethodeFile.getExampleImage();
            if (exf == null || !exf.exists()) {
                File[] listF = easyMethodeFile.getCaptchaFolder().listFiles(new FilenameFilter() {

                    public boolean accept(File dir, String name) {
                        return name.matches("(?is).*\\.(jpg|png|gif)");
                    }
                });
                if(listF!=null && listF.length>1)
                {
                    JDIO.copyFile(listF[0], new File(easyMethodeFile.file,"example."+easyMethodeFile.getCaptchaType(false)));
                    System.out.println(easyMethodeFile.file);

                }
            }
        }
    }

}
