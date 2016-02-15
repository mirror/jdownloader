package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.awt.Image;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import jd.plugins.Plugin;

import org.appwork.utils.images.IconIO;
import org.jdownloader.captcha.v2.Challenge;

public abstract class ImageCaptchaChallenge<T> extends Challenge<T> {

    private volatile File imageFile;

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
        return "CaptchaChallenge by " + plugin.getHost() + "-" + getTypeID() + " File: " + getImageFile();
    }

    public Plugin getPlugin() {
        return plugin;
    }

    private final Plugin plugin;

    public synchronized File getImageFile() {
        if (imageFile == null && plugin != null) {
            imageFile = plugin.getLocalCaptchaFile();
        }
        return imageFile;
    }

    public void setImageFile(File imageFile) {

        this.imageFile = imageFile;

    }

    public Object getAPIStorable(String format) throws Exception {
        return IconIO.toDataUrl(ImageIO.read(getImageFile()));
    }

}
