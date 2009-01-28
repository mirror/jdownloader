package jd.parser;

import java.io.IOException;

import org.xml.sax.SAXException;

import jd.http.Browser;

public class JavaScriptTest {
    public static String getRSCOMServer(String link) throws IOException, SAXException {
        Browser b = new Browser();
        b.getPage(link);
        b.submitForm(b.getForm(0));
        JavaScript js = b.getJavaScript();
        js.javaScript = "function f() {return document.getElementsByName(\"dlf\")[0].action;} f();";
        return js.runJavaScript();
    }

    /**
     * TODO muss noch überarbeitet werden
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            System.out.println(getRSCOMServer("http://rapidshare.com/files/157971450/Om_Jai_Laxmi_Mata.pdf"));
            System.out.println("---------- HTML Seite vor dem aufruf der js funktion Aendern");
            Browser b = new Browser();
            b.getPage("http://dwdhome.ath.cx/test.html");
            System.out.println(b);
            System.out.println("---------- HTML Seite nach dem aufruf der js funktion Aendern");
            JavaScript js = b.getJavaScript();
            js.callFunction("Aendern");
            System.out.println(js);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
