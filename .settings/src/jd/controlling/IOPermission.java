package jd.controlling;

public interface IOPermission {

    public static enum CAPTCHA {
        OK,
        BLOCKTHIS,
        BLOCKHOSTER,
        BLOCKALL
    }

    public boolean isCaptchaAllowed(String hoster);

    public void setCaptchaAllowed(String hoster, CAPTCHA mode);
}
