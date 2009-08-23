package jd.captcha.easy.load;

import jd.parser.html.Form;

public class LoadAgain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        LoadImage li = LoadImage.loadFile("protect-it.org");
        try {
            // von einer anderen url laden für Plugins nützlich
             li.baseUrl="http://protect-it.org/?id=224692747cc7f9e";
            System.out.println(li.load("protect-it.org").file);
            Form form = li.br.getForm(0);
            form.getInputField("pass").setValue("test");
            System.out.println(form);
//            System.out.println(form);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
