package jd.captcha.easy.load;

public class LoadAgain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoadImage li = LoadImage.loadFile("teradepot.com");
        try {
            System.out.println(li.load("teradepot.com").file);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
