package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.controlling.linkcrawler.CrawledLink;

import org.appwork.utils.Files;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.images.NewTheme;

public abstract class ExtensionFilter extends Filter {

    private Pattern pattern;
    private String  description = null;

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
        String ext = Files.getExtension(link.getName());
        return isFiltered(ext);
    }

    public boolean isFiltered(String ext) {
        if (ext == null) return false;
        if (pattern == null) return false;
        return pattern.matcher(ext).matches();
    }

    // @Override
    // public boolean isFiltered(CrawledPackage link) {
    // return false;
    // }

}
