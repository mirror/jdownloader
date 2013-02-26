package org.jdownloader.captcha.v2.challenge.stringcaptcha;

import java.io.File;

import jd.controlling.IOPermission;
import jd.plugins.Plugin;

import org.jdownloader.captcha.v2.Challenge;

public abstract class ImageCaptchaChallenge<T> extends Challenge<T> {

    private File         imageFile;
    private IOPermission ioPermission;

    public IOPermission getIoPermission() {
        return ioPermission;
    }

    public void setIoPermission(IOPermission ioPermission) {
        this.ioPermission = ioPermission;
    }

    public ImageCaptchaChallenge(File file, String method, String explain, Plugin plugin, IOPermission ioPermission) {
        super(method, explain);
        this.imageFile = file;
        this.plugin = plugin;
        this.ioPermission = ioPermission;
    }

    public String toString() {
        return "CaptchaChallenge by " + plugin.getHost() + " File: " + imageFile;
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

}
