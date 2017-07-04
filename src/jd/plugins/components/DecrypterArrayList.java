package jd.plugins.components;

import java.util.ArrayList;

import jd.http.Browser;

/**
 *
 * used in classes which extends multiple times, for instance <br />
 * real plugin <> pornembedparser <> antiddosfordecrypt <> pluginfordecrypt<br />
 * you can't override createDownloadlink from pluginfordecrypt within pornembededparser then it will nuke actions within real plugin and
 * have fallout.<br />
 *
 * @author raztoki
 *
 * @param <E>
 */
public abstract class DecrypterArrayList<E> extends ArrayList<E> {

    /**
     *
     */
    private static final long serialVersionUID = -7880168426363450777L;

    public abstract boolean add(final String link);

    public abstract boolean add(final String link, final Browser br);

}