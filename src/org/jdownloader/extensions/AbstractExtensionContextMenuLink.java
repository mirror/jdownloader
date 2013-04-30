package org.jdownloader.extensions;

import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;

import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.gui.views.SelectionInfo;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class AbstractExtensionContextMenuLink<T extends AbstractExtension<?, ?>> extends MenuItemData implements MenuLink {

    protected T _getExtension() {
        try {
            ParameterizedTypeImpl sc = (ParameterizedTypeImpl) getClass().getGenericSuperclass();
            Class<T> clazz = (Class<T>) sc.getActualTypeArguments()[0];
            LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);
            if (ex._isEnabled()) { return ((T) ex._getExtension()); }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public JComponent addTo(JComponent root, SelectionInfo<?, ?> selection) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        T ext = _getExtension();
        if (ext == null || !ext.isEnabled()) return null;
        link(root, selection, ext);
        return null;
    }

    abstract protected void link(JComponent root, SelectionInfo<?, ?> selection, T extension);

}
