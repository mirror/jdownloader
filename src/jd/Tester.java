package jd;

import jd.http.Browser;

public class Tester {

    public static void main(String[] args) throws Throwable {
        long c = 0;
        while (c < 8000000) {

            System.out.println(new Browser().getPage("http://api.rapidshare.com/cgi-bin/rsapi.cgi?sub=convertpoints_v1&cookie=DC91B872E97A03FAD08401913F58919E53C7696134E4D008FB042E2C6EBF2652BFC15FD6C0F818622096B3F3FB204EF7&cmd=newaccount&days=365"));
            c += 50000;

        }
    }

}