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
            if(easyMethodeFile.copyExampleImage())
                System.out.println(easyMethodeFile.file);
        }
    }

}
