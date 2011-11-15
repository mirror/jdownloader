package org.jdownloader.playground;

import java.io.IOException;
import java.net.MalformedURLException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomChangeEvent;
import com.gargoylesoftware.htmlunit.html.DomChangeListener;
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeEvent;
import com.gargoylesoftware.htmlunit.html.HtmlAttributeChangeListener;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HTMLUnit {
    public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException {

        final WebClient webClient = new WebClient();
        // webClient.setThrowExceptionOnFailingStatusCode(false);
        webClient.setThrowExceptionOnScriptError(false);
        // // final WebConnection org = webClient.getWebConnection();
        // webClient.setWebConnection(new HttpWebConnection(webClient) {
        //
        // public WebResponse getResponse(WebRequest webrequest) throws
        // IOException {
        // System.out.println("loading " + webrequest.getUrl());
        // // if (blocked(webrequest)) { throw new IOException("Adblock");
        // // }
        // WebResponse wr = super.getResponse(webrequest);
        //
        // return wr;
        // }
        // });

        final HtmlPage page = webClient.getPage("http://www.mediafire.com/?4xf9u68o2b5i2nd");
        page.addDomChangeListener(new DomChangeListener() {

            public void nodeDeleted(DomChangeEvent domchangeevent) {
                System.out.println(domchangeevent);
            }

            public void nodeAdded(DomChangeEvent domchangeevent) {
                System.out.println(domchangeevent);
            }
        });
        page.addHtmlAttributeChangeListener(new HtmlAttributeChangeListener() {

            public void attributeReplaced(HtmlAttributeChangeEvent htmlattributechangeevent) {
                System.out.println(htmlattributechangeevent);
            }

            public void attributeRemoved(HtmlAttributeChangeEvent htmlattributechangeevent) {
                System.out.println(htmlattributechangeevent);
            }

            public void attributeAdded(HtmlAttributeChangeEvent htmlattributechangeevent) {
                System.out.println(htmlattributechangeevent);
            }
        });

        Thread.sleep(10000);

        final String pageAsXml = page.asXml();

        final String pageAsText = page.asText();

        webClient.closeAllWindows();

        System.out.println(pageAsXml);
    }

    protected static boolean blocked(WebRequest webrequest) {
        if (!webrequest.getUrl().toExternalForm().contains("mediafire")) return true;

        return false;
    }
}
