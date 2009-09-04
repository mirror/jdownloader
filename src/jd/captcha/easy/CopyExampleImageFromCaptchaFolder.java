package jd.captcha.easy;

public class CopyExampleImageFromCaptchaFolder {

    /**
     * kopiert ein exampleimage aus dem Captchaordner in den methodenordner
     * @param args
     */
    public static void main(String[] args) {
        EasyMethodFile[] list = EasyMethodFile.getMethodeList();
        for (EasyMethodFile easyMethodeFile : list) {
            if(easyMethodeFile.copyExampleImage())
                System.out.println(easyMethodeFile.file);
        }
    }

}
