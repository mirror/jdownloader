package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import javax.imageio.ImageIO;

import jd.plugins.Plugin;

import org.appwork.storage.Storable;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.jdownloader.captcha.v2.Challenge;

public abstract class ImageCaptchaChallenge<T> extends Challenge<T> {

    private File imageFile;

    public ImageCaptchaChallenge(File file, String method, String explain, Plugin plugin) {
        super(method, explain);
        this.imageFile = file;
        this.plugin = plugin;

    }

    public String toString() {
        return "CaptchaChallenge by " + plugin.getHost() + "-" + getTypeID() + " File: " + imageFile;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    private Plugin plugin;

    public File getImageFile() {
        return imageFile;
    }

    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    public class APIData implements Storable {
        private String data;

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public APIData(/* Storable */) {
        }

    }

    public Storable getAPIStorable() throws Exception {
        String mime = FileResponse.getMimeType(getImageFile().getName());
        APIData ret = new APIData();
        ret.setData(IconIO.toDataUrl(ImageIO.read(getImageFile()), mime));

        return ret;
    }

}
