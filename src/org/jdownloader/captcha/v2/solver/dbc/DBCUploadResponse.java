package org.jdownloader.captcha.v2.solver.dbc;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;

public class DBCUploadResponse implements Storable {

    public static final TypeRef<DBCUploadResponse> TYPE = new TypeRef<DBCUploadResponse>() {
    };

    private DBCUploadResponse(/* Storable */) {

    }

    private int status;

    public int getStatus() {
        return status;
    }

    public boolean isSolved() {
        return isIs_correct() && StringUtils.isNotEmpty(getText());
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getCaptcha() {
        return captcha;
    }

    public void setCaptcha(int captcha) {
        this.captcha = captcha;
    }

    public boolean isIs_correct() {
        return is_correct;
    }

    public void setIs_correct(boolean is_correct) {
        this.is_correct = is_correct;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private int     captcha;
    private boolean is_correct;
    private String  text;
}
