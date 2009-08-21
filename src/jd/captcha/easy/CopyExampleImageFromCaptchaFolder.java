package jd.captcha.easy;

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
