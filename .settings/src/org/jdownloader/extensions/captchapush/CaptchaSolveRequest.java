package org.jdownloader.extensions.captchapush;

import jd.controlling.captcha.CaptchaController;

import org.appwork.utils.IO;

public class CaptchaSolveRequest {

    private static final int STRICT_LENGTH = 100;

    private final int        id;
    private final String     explain;
    private final byte[]     captchaBytes;

    public CaptchaSolveRequest(CaptchaController controller) throws Exception {
        this.id = controller.getId();
        this.explain = (controller.getExplain() == null) ? "" : controller.getExplain();
        this.captchaBytes = IO.readFile(controller.getCaptchafile());
    }

    public CaptchaSolveRequest(byte[] data) {
        byte[] temp = new byte[4];

        System.arraycopy(data, 0, temp, 0, 4);
        this.id = convertByteArrayToInt(temp);

        temp = new byte[100];

        System.arraycopy(data, 4 + 0 * STRICT_LENGTH, temp, 0, STRICT_LENGTH);
        this.explain = new String(temp).trim();

        this.captchaBytes = new byte[data.length - 3 * STRICT_LENGTH];
        System.arraycopy(data, 4 + 1 * STRICT_LENGTH, captchaBytes, 0, captchaBytes.length);
    }

    public int getId() {
        return id;
    }

    public String getExplain() {
        return explain;
    }

    public byte[] getCaptchaBytes() {
        return captchaBytes;
    }

    public byte[] toByteArray() {
        byte[] bId = convertIntToByteArray(id);
        byte[] bExplain = explain.getBytes();
        byte[] bCaptcha = captchaBytes;

        byte[] message = new byte[4 + 1 * STRICT_LENGTH + bCaptcha.length];

        System.arraycopy(bId, 0, message, 0, 4);

        System.arraycopy(bExplain, 0, message, 4 + 0 * STRICT_LENGTH, Math.min(bExplain.length, STRICT_LENGTH));

        System.arraycopy(bCaptcha, 0, message, 4 + 1 * STRICT_LENGTH, bCaptcha.length);

        return message;
    }

    @Override
    public String toString() {
        return id + " - " + explain;
    }

    private static int convertByteArrayToInt(byte[] buffer) {
        int value = 0;

        value |= (0xFF & buffer[0]) << 24;
        value |= (0xFF & buffer[1]) << 16;
        value |= (0xFF & buffer[2]) << 8;
        value |= (0xFF & buffer[3]);

        return value;
    }

    private static byte[] convertIntToByteArray(int val) {
        byte[] buffer = new byte[4];

        buffer[0] = (byte) (val >>> 24);
        buffer[1] = (byte) (val >>> 16);
        buffer[2] = (byte) (val >>> 8);
        buffer[3] = (byte) val;

        return buffer;
    }

}
