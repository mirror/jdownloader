package jd;

import java.io.File;

import jd.parser.Regex;

public class Tester {

    public static void main(String[] args) throws Exception {
        String sfv = ";Kommentare können das\r\n;Ergebnis verfälschen!\r\nTest.pdf \t\t\t 123456789aBcDef";
        System.out.println(sfv);
        sfv = sfv.replaceAll(";(.*?)[\r\n]{1,2}", "");
        System.out.println(sfv);

        String file = "C:\\Downloads\\Test.pdf";
        String name = new File(file).getName();
        String crc = "123456789AbCdEf";
        String control = name + "\\s*" + crc;
        System.out.println(control);

        System.out.println(new Regex(sfv, control).matches());
    }
}