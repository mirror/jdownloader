package jd.http.ext.events;

import jd.http.ext.HtmlFrameController;

public class JSInteraction extends HtmlFrameControllerEvent {
    public static enum AnswerTypes {
        CANCEL, OK, SKIP
    }

    public static enum Types {
        ALERT, CONFIRM, PROMPT
    }

    private AnswerTypes answer = AnswerTypes.CANCEL;

    private String answerString = null;

    private String message = null;

    private Types type = null;

    public JSInteraction(HtmlFrameController caller, Types type, String msg) {
        this(caller, type, msg, null);
    }

    public JSInteraction(HtmlFrameController caller, Types type, String msg, String def) {
        super(caller, 0, null);
        this.type = type;
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
        return type;
    }

    public void setAnswer(AnswerTypes answer) {
        this.answer = answer;
    }

    public void setAnswerString(String answerString) {
        this.answerString = answerString;
    }

}
