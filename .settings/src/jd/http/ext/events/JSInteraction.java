package jd.http.ext.events;

import jd.http.ext.ExtBrowser;
import jd.http.ext.FrameController;

public class JSInteraction extends ExtBrowserEvent {
    public static enum AnswerTypes {
        CANCEL, OK, SKIP
    }

    public static enum Types {
        ALERT, CONFIRM, PROMPT
    }

    private AnswerTypes answer = AnswerTypes.CANCEL;

    private String answerString = null;

    private String message = null;

    public JSInteraction(ExtBrowser extBrowser, FrameController caller, Types type, String msg) {
        this(extBrowser, caller, type, msg, null);
    }

    public JSInteraction(ExtBrowser owner, FrameController caller, Types type, String msg, String def) {
        super(owner, caller, type.ordinal(), null);

        this.message = msg;
        answerString = def;
    }

    public AnswerTypes getAnswer() {
        return answer;
    }

    public String getAnswerString() {
        return answerString;
    }

    public String getMessage() {
        return message;
    }

    public Types getType() {
        return Types.values()[this.getEventID()];
    }

    public void setAnswer(AnswerTypes answer) {
        this.answer = answer;
    }

    public void setAnswerString(String answerString) {
        this.answerString = answerString;
    }

}
