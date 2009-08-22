package jd.captcha.easy.load;

public class LoadAgain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoadImage li = LoadImage.loadFile("teradepot.com");
        try {
            // von einer anderen url laden für Plugins nützlich
            // li.baseUrl="http://www.teradepot.com/palo3ff7klxt/S.A.S_P.v4.27.1000.Multilingual_WA.I.Key.CRD.rar.html";
            System.out.println(li.load("teradepot.com").file);
            System.out.println(li.br);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
