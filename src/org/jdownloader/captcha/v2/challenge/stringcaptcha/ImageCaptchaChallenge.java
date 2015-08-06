package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import javax.imageio.ImageIO;

import jd.plugins.Plugin;

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

    public Object getAPIStorable() throws Exception {
        String mime = FileResponse.getMimeType(getImageFile().getName());
        return IconIO.toDataUrl(ImageIO.read(getImageFile()), mime);
    }

}
