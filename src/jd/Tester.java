package jd;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class Tester {

    public static void main(String[] args) throws Exception {
        File file = new File("c:/test it");
        System.out.println(file);
        URL url = file.toURI().toURL();
        System.out.println(url);
        URI uri = url.toURI();
        url.getFile();
        System.out.println(uri);
        System.out.println(uri.getPath());
    }

}
