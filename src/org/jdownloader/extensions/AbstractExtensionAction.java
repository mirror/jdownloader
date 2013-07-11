package org.jdownloader.extensions;

import java.lang.reflect.Type;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.exceptions.WTFException;
import org.jdownloader.actions.SelectionAppAction;
import org.jdownloader.gui.views.SelectionInfo;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class AbstractExtensionAction<T extends AbstractExtension<?, ?>, PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends SelectionAppAction<PackageType, ChildrenType> {

    public AbstractExtensionAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);
    }

    private T extension;
    {

        Class<?> myClass = getClass();
        while (true) {
            try {
                Type supClass = myClass.getGenericSuperclass();
                if (supClass instanceof ParameterizedTypeImpl) {

                    ParameterizedTypeImpl sc = ((ParameterizedTypeImpl) supClass);
                    if (sc.getRawType() != AbstractExtensionAction.class) {
                        myClass = sc.getRawType();
                        continue;
                    }
                    Class<T> clazz = (Class<T>) sc.getActualTypeArguments()[0];
                    LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);

                    extension = ((T) ex._getExtension());

                    break;
                } else if (supClass instanceof Class) {
                    myClass = (Class<?>) supClass;
                } else {
                    break;
                }

            } catch (Exception e) {
                throw new WTFException();

            }
        }
        if (extension == null) {

        throw new WTFException();

        }
    }

    protected T _getExtension() {
        return extension;
    }
}
