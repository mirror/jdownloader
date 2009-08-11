package jd;

import java.io.File;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.parser.html.Form.MethodType;

public class Tester {

    public static void main(String s[]) throws Exception {

        try {
            
            File file = new File("");
            Browser br = new Browser();
            String data = br.getPage("http://rapidshare.com");
            
            Form form = new Form();
            
form.setAction("http://jdownloader.org/");
form.setEncoding("multipart/form-data");
form.setMethod(MethodType.POST);
form.addInputField(new InputField("a","avalue"));
form.addInputField(new InputField("b","bvalue"));
form.addInputField(new InputField("c","cvalue"));
  br.submitForm(form);
            data=data;
        } catch (Exception e) {
            JDLogger.exception(e);
            
        }
    }

}
