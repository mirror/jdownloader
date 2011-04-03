package org.jdownloader.extensions.captchapush;

import java.io.File;

import org.appwork.utils.IO;

public class CaptchaSolveRequest {

    private static final int STRICT_LENGTH = 100;

    private final String     host;
    private final String     suggest;
    private final String     explain;
    private final byte[]     captchaBytes;

    public CaptchaSolveRequest(String host, String suggest, String explain, File captchaFile) throws Exception {
        this.host = (host == null) ? "" : host;
        this.suggest = (suggest == null) ? "" : suggest;
        this.explain = (explain == null) ? "" : explain;
        this.captchaBytes = IO.readFile(captchaFile);
    }

    public CaptchaSolveRequest(byte[] data) {
        byte[] temp = new byte[100];

        System.arraycopy(data, 0 * STRICT_LENGTH, temp, 0, STRICT_LENGTH);
        this.host = new String(temp).trim();

        System.arraycopy(data, 1 * STRICT_LENGTH, temp, 0, STRICT_LENGTH);
        this.suggest = new String(temp).trim();

        System.arraycopy(data, 2 * STRICT_LENGTH, temp, 0, STRICT_LENGTH);
        this.explain = new String(temp).trim();

        this.captchaBytes = new byte[data.length - 3 * STRICT_LENGTH];
        System.arraycopy(data, 3 * STRICT_LENGTH, captchaBytes, 0, captchaBytes.length);
    }

    public String getHost() {
        return host;
    }

    public String getSuggest() {
        return suggest;
    }

    public String getExplain() {
        return explain;
    }

    public byte[] getCaptchaBytes() {
        return captchaBytes;
    }

    public byte[] getByteArray() {
        byte[] bHost = host.getBytes();
        byte[] bSuggestion = suggest.getBytes();
        byte[] bExplain = explain.getBytes();
        byte[] bCaptcha = captchaBytes;

        byte[] message = new byte[3 * STRICT_LENGTH + bCaptcha.length];

        System.arraycopy(bHost, 0, message, 0 * STRICT_LENGTH, Math.min(bHost.length, STRICT_LENGTH));

        System.arraycopy(bSuggestion, 0, message, 1 * STRICT_LENGTH, Math.min(bSuggestion.length, STRICT_LENGTH));

        System.arraycopy(bExplain, 0, message, 2 * STRICT_LENGTH, Math.min(bExplain.length, STRICT_LENGTH));

        System.arraycopy(bCaptcha, 0, message, 3 * STRICT_LENGTH, bCaptcha.length);

        return message;
    }

    @Override
    public String toString() {
        return host + " - " + suggest + " - " + explain;
    }

}
