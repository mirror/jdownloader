package org.jdownloader.captcha.v2.challenge.recaptcha.v2.phantomjs;

import java.util.ArrayList;

import org.jdownloader.captcha.v2.ValidationResult;

public class TileContent {

    private Payload  payload;

    private boolean  asyncJsStuffInProgress = false;
    public final int y;
    public final int x;

    public boolean isAsyncJsStuffInProgress() {
        return asyncJsStuffInProgress;
    }

    public void setAsyncJsStuffInProgress(boolean asyncJsStuffInProgress) {
        this.asyncJsStuffInProgress = asyncJsStuffInProgress;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
        synchronized (marks) {
            marks.clear();
        }
    }

    public TileContent(int x, int y, Payload payload) {
        this.x = x;
        this.y = y;
        this.payload = payload;
    }

    ArrayList<Response> marks = new ArrayList<Response>();

    public boolean mark(Response mark, boolean selected, int responses) {

        synchronized (marks) {
            if (selected) {
                if (marks.size() == 0) {
                    return true;
                } else if (marks.size() == 1) {
                    marks.get(0).getResponse().setValidation(ValidationResult.INVALID);
                    marks.clear();
                    return false;
                } else {
                    mark.getResponse().setValidation(ValidationResult.INVALID);
                    marks.clear();
                    return false;
                }

            } else {
                marks.add(mark);
                return marks.size() == responses;
            }

        }
    }

}
