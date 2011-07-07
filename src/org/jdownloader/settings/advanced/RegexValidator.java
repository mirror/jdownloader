package org.jdownloader.settings.advanced;


public class RegexValidator extends Validator {

    private String pattern;

    public String getPattern() {
        return pattern;
    }

    @Override
    public String toString() {
        return "Valid for '" + pattern + "' matches";
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public RegexValidator(String value) {
        this.pattern = value;
    }

}
