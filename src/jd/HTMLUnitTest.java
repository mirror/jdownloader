package jd;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlButtonInput;
import com.gargoylesoftware.htmlunit.html.HtmlImage;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class HTMLUnitTest {
    public static void main(String[] args) throws Throwable {
        // testUploaded();

        testRecaptcha();
    }

    private static void testRecaptcha() throws FailingHttpStatusCodeException, MalformedURLException, IOException, InterruptedException, DialogClosedException, DialogCanceledException {
        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
        final WebClient webClient = new WebClient(BrowserVersion.CHROME) {
            public WebResponse loadWebResponse(final WebRequest webRequest) throws IOException {
                System.out.println("Load " + webRequest.getUrl());

                WebResponse ret = super.loadWebResponse(webRequest);
                // System.out.println(ret.getContentAsString());
                return ret;
            }
        };

        final HtmlPage page = webClient.getPage("https://www.google.com/recaptcha/demo/ajax");

        List<DomElement> buttons = page.getElementsByTagName("input");
        ((HtmlButtonInput) buttons.get(0)).click();
        Thread.sleep(10000);
        System.out.println(1);
        String pageAsXml = page.asXml();
        HtmlImage image = (HtmlImage) page.getElementById("recaptcha_challenge_image");
        File file = new File("recaptcha_" + System.currentTimeMillis() + ".png");
        image.saveAs(file);

        Dialog.getInstance().showConfirmDialog(0, "Imagee", "", new ImageIcon(ImageIO.read(file)), null, null);
        System.out.println(1);
        //
        // pageAsText = page.asText();
        // HtmlImage image = (HtmlImage) page.getElementById("recaptcha_challenge_image");
        // image.saveAs(new File("D:\\recaptcha" + System.currentTimeMillis() + ".png"));
        // // Page img = webClient.getPage(image.getSrcAttribute());
        // webClient.closeAllWindows();
    }

    private static void testUploaded() throws Throwable {
        System.getProperties().put("org.apache.commons.logging.simplelog.defaultlog", "trace");
        final WebClient webClient = new WebClient(BrowserVersion.CHROME) {
            public WebResponse loadWebResponse(final WebRequest webRequest) throws IOException {
                System.out.println("Load " + webRequest.getUrl());
                WebResponse ret = super.loadWebResponse(webRequest);
                System.out.println(ret.getContentAsString());
                return ret;
            }
        };
        final HtmlPage page = webClient.getPage("http://uploaded.net/fi....");

        String pageAsXml = page.asXml();

        String pageAsText = page.asText();
        DomNodeList<DomElement> buttons = page.getElementsByTagName("button");
        for (int i = 0; i < buttons.size(); i++) {
            HtmlButton bt = (HtmlButton) buttons.get(i);
            Page res = bt.click();
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(1);
            break;
        }

        pageAsXml = page.asXml();

        pageAsText = page.asText();
        HtmlImage image = (HtmlImage) page.getElementById("recaptcha_challenge_image");
        image.saveAs(new File("D:\\recaptcha" + System.currentTimeMillis() + ".png"));
        // Page img = webClient.getPage(image.getSrcAttribute());
        webClient.closeAllWindows();
    }

}
