package jd.http.ext;

import java.awt.Component;
import java.awt.Insets;
import java.net.URL;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.HtmlRendererContext;
import org.w3c.dom.Document;

public class ExtBrowserFrame implements BrowserFrame {

    private HtmlFrameController frameController;

    public ExtBrowserFrame(HtmlFrameController frameController) {
        this.frameController = frameController;
    }

    public Component getComponent() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public Document getContentDocument() {

        return frameController.getDocument();
    }

    public HtmlRendererContext getHtmlRendererContext() {
        // TODO Auto-generated method stub
        return frameController;
    }

    public void loadURL(URL url) {
        // gets called to load a frame or somthing like this.
        // we do not need this because HTMLFRameController cares about frame
        // process

    }

    public void setDefaultMarginInsets(Insets insets) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void setDefaultOverflowX(int overflowX) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void setDefaultOverflowY(int overflowY) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

}
