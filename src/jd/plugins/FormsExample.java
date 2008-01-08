package jd.plugins;

public class FormsExample {

    /**
     * @param args
     */
    public static void main(String[] args) {
 
            //Suche auf Glück bei Google
            System.out.println("Suche auf Glück bei Google");
            Form[] forms = Form.getForms("http://www.google.de/");
            for (int i = 0; i < forms.length; i++) {
                System.out.println(forms[i].toString());
            }
            Form form = Form.getForms("http://www.google.de/")[0];
            form.vars.remove("btnG");
            form.vars.put("q", "jDownloader");
            System.out.println(form.getRequestInfo().getLocation());
            RequestInfo req = form.getRequestInfo();

            System.out.println(req.getConnection().getURL().toString());
            System.out.println(req.getLocation()+"\n\n");
            
            //Eintrag auf Pastebin
            System.out.println("Eintrag auf Pastebin");
            forms = Form.getForms("http://jdownloader.pastebin.com/");
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


    }

}
