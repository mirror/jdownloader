package jd;

import jd.http.Browser;

public class Tester {

    public static void main(String[] args) throws Exception {
        Browser.setVerbose(true);
        Browser.init();
        Browser br = new Browser();

        br.postPage("http://www.themis-media.com/global/castfire/m4v/767", "version=ThemisMedia1%2E2&format=a3645f5941f09e957707648bb9e1b24c");
        System.out.println(br + "");
    }

}
