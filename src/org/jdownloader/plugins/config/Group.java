package org.jdownloader.plugins.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storable;
import org.appwork.storage.config.handler.KeyHandler;

public class Group implements Storable {
    public Group(/* Storable */) {
    }

    private String title;
    private String iconKey;

    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(String iconKey) {
        this.iconKey = iconKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    private String                regex;
    public final List<KeyHandler> handler = new ArrayList<KeyHandler>();

    private Pattern               _pattern;

    public Group(String title, String regex, String iconKey) {
        super();
        this.title = title;
        this.iconKey = iconKey;
        this.regex = regex;
    }

    public boolean matches(KeyHandler m) {
        return _getPattern().matcher(m.getGetMethod().getName()).matches();
    }

    private Pattern _getPattern() {
        if (_pattern == null) {
            _pattern = Pattern.compile(regex);
        }
        return _pattern;
    }

    public void add(KeyHandler m) {
        handler.add(m);
    }

    public static final org.appwork.storage.TypeRef<Group> TYPE = new org.appwork.storage.TypeRef<Group>(Group.class) {
    };

    public Group createClone() {
        return JSonStorage.restoreFromString(JSonStorage.serializeToJson(this), Group.TYPE);
    }
}
