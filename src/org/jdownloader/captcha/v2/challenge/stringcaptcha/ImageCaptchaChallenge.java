package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.appwork.utils.images.IconIO;
import org.appwork.utils.net.httpserver.responses.FileResponse;
import org.jdownloader.captcha.v2.Challenge;

import jd.plugins.Plugin;

public abstract class ImageCaptchaChallenge<T> extends Challenge<T> {

    private File imageFile;

    public ImageCaptchaChallenge(File file, String method, String explain, Plugin plugin) {
        super(method, explain);
        this.imageFile = file;
        this.plugin = plugin;

    }

    public byte[] getImageBytes() throws IOException {
        return IconIO.toJpgBytes(getImage());
    }

    public Image getImage() throws IOException {
        return ImageIO.read(getImageFile());
    }

    public byte[] getAnnotatedImageBytes() throws IOException {
        return IconIO.toJpgBytes(getAnnotatedImage());
    }

    /**
     * The AnnotatedImage may contain instructions that are not in the original image.
     *
     *
     * @return
     * @throws IOException
     */
    public Image getAnnotatedImage() throws IOException {
        return getImage();
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

    public Object getAPIStorable(String format) throws Exception {
        String mime = FileResponse.getMimeType(getImageFile().getName());
        return IconIO.toDataUrl(ImageIO.read(getImageFile()));
    }

}
