package org.jdownloader.images;

import org.appwork.resources.Theme;

/**
 * New JDownloader Icon Theme Support
 * 
 * @author thomas
 * 
 */
public class NewTheme extends Theme {
    private static final NewTheme INSTANCE = new NewTheme();

    /**
     * get the only existing instance of NewTheme.I(). This is a singleton
     * 
     * @return
     */
    public static NewTheme getInstance() {
        return NewTheme.INSTANCE;
    }

    public static NewTheme I() {
        return NewTheme.INSTANCE;
    }

    /**
     * Create a new instance of NewTheme.I(). This is a singleton class. Access
     * the only existing instance by using {@link #getInstance()}.
     */
    private NewTheme() {
        super();
    }

    @Override
    protected String getNameSpace() {
        return "org/jdownloader/";
    }

}
