package org.jdownloader.gui.views.downloads.action;

import javax.swing.KeyStroke;

import org.appwork.utils.StringUtils;

public class Modifier {

    private int modifier;

    public int getModifier() {
        return modifier;
    }

    private Modifier(int modifier) {
        this.modifier = modifier;
    }

    @Override
    public String toString() {
        KeyStroke ks = getKeyStroke();

        return ks.toString();
    }

    public static Modifier create(KeyStroke keyStroke) {
        if (keyStroke.getModifiers() == 0) return null;
        return new Modifier(keyStroke.getModifiers());
    }

    public static Modifier create(String value) {
        if (StringUtils.isEmpty(value)) return null;

        KeyStroke keyStroke = KeyStroke.getKeyStroke(value);
        if (keyStroke.getModifiers() == 0) return null;
        return new Modifier(keyStroke.getModifiers());
    }

    public KeyStroke getKeyStroke() {
        return KeyStroke.getKeyStroke(0, modifier);
    }

}
