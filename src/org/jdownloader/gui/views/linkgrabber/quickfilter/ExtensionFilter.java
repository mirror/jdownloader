package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.HashMap;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLink;

import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.images.NewTheme;

public abstract class ExtensionFilter extends Filter {

    private Pattern                        pattern;
    private String                         description = null;
    private final HashMap<String, Boolean> fastCheck   = new HashMap<String, Boolean>();

    public ExtensionFilter(String string, ImageIcon icon, boolean b) {
        super(string, icon);
    }

    public ExtensionFilter(ExtensionsFilterInterface filter) {
        super(filter.getDesc(), NewTheme.I().getIcon(filter.getIconID(), 16));
        description = filter.getDesc();
        pattern = filter.compiledAllPattern();

    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean isFiltered(CrawledLink link) {
        return isFiltered(link.getExtension());
    }

    protected synchronized boolean isFiltered(String ext) {
        if (ext == null) return false;
        Boolean ret = fastCheck.get(ext);
        if (ret != null) return ret.booleanValue();
        if (pattern == null) return false;
        ret = Boolean.valueOf(pattern.matcher(ext).matches());
        fastCheck.put(ext, ret);
        return ret.booleanValue();
    }

}
