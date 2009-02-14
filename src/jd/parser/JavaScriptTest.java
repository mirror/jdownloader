package jd.parser;

import java.io.IOException;

import jd.http.Browser;

import org.xml.sax.SAXException;

public class JavaScriptTest {
    public static String getRSCOMServer(String link) throws Exception, SAXException {
        Browser b = new Browser();
        b.getPage(link);
        b.submitForm(b.getForm(0));
        JavaScript js = b.getJavaScript();
        js.javaScript = "function f() {return document.getElementsByName(\"dlf\")[0].action;} f();";
        return js.runJavaScript();
    }

    private static String getZshare(String string) throws Exception, SAXException {
        Browser b = new Browser();
        b.getPage(string);
        b.submitForm(b.getForm(0));
        JavaScript js = b.getJavaScript();
        return "http://" + b.getHost() + "/" + js.getVar("link");
    }

    public static String getSendspace(String string) throws IOException, SAXException {
        Browser b = new Browser();
        b.getPage(string);
        JavaScript js = b.getJavaScript();
        return js.getVar("link_dec");
    }

    public static void dwdstest() throws IOException, SAXException {
        Browser b = new Browser();
        System.out.println("---------- HTML Seite vor dem aufruf der js funktion Aendern");
        b.getPage("http://dwdhome.ath.cx/test.html");
        System.out.println(b);
        System.out.println("---------- HTML Seite nach dem aufruf der js funktion Aendern");
        JavaScript js = b.getJavaScript();
        js.callFunction("Aendern");
        System.out.println(js);
    }

    /**
     * TODO muss noch überarbeitet werden
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {

            System.out.println("---------- rs.com downloadlinkgrabber");
            System.out.println(getRSCOMServer("http://rapidshare.com/files/157971450/Om_Jai_Laxmi_Mata.pdf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            System.out.println("---------- zshare linkdecyption");
            System.out.println(getZshare("http://www.zshare.net/download/12230432d2e1bc81/"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            System.out.println("---------- sendspace linkdecyption");
            System.out.println(getSendspace("http://www.sendspace.com/file/ueknde"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

            dwdstest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
