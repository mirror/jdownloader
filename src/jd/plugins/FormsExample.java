package jd.plugins;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class FormsExample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            //Suche auf Glück bei Google
            System.out.println("Suche auf Glück bei Google");
            RequestInfo req = Plugin.getRequest(new URL("http://www.google.de/"));
            Form[] forms = Form.getForms(req);
            for (int i = 0; i < forms.length; i++) {
                System.out.println(forms[i].toString());
            }
            Form form = forms[0];
            form.vars.remove("btnG");
            form.vars.put("q", "jDownloader");
            req = form.getRequestInfo();

            System.out.println(req.getConnection().getURL().toString());
            System.out.println(req.getLocation()+"\n\n");
            
            //Eintrag auf Pastebin
            System.out.println("Eintrag auf Pastebin");
            req = Plugin.getRequest(new URL("http://jdownloader.pastebin.com/"));
            forms = Form.getForms(req);
            for (int i = 0; i < forms.length; i++) {
                System.out.println(forms[i].toString());
            }
            form = forms[forms.length - 1];
            String str = form.toString();
            form.vars.put("format", "java");
            form.vars.put("code2", str);
            form.vars.put("poster", "jDownloader");
            req = form.getRequestInfo();
            System.out.println(req.getConnection().getURL().toString());

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
