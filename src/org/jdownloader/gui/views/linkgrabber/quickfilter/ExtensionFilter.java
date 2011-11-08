package org.jdownloader.gui.views.linkgrabber.quickfilter;

import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.utils.Files;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.images.NewTheme;

public abstract class ExtensionFilter<E extends AbstractPackageNode<V, E>, V extends AbstractPackageChildrenNode<E>> extends Filter<E, V> {

    private Pattern pattern;

    public ExtensionFilter(String string, ImageIcon icon, boolean b) {
        super(string, icon);
    }

    public ExtensionFilter(ExtensionsFilterInterface filter) {
        super(filter.getDesc(), NewTheme.I().getIcon(filter.getIconID(), 16));
        pattern = filter.compiledAllPattern();
    }

    @Override
    public boolean isFiltered(V link) {
        String ext = Files.getExtension(link.getName());
        return isFiltered(ext);
    }

    public boolean isFiltered(String ext) {
        if (ext == null) return false;
        if (pattern == null) return false;
        return pattern.matcher(ext).matches();
    }

    @Override
    public boolean isFiltered(E link) {
        return false;
    }

}
