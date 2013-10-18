package org.jdownloader.extensions;

import java.lang.reflect.Type;

import jd.controlling.packagecontroller.AbstractPackageChildrenNode;
import jd.controlling.packagecontroller.AbstractPackageNode;

import org.appwork.exceptions.WTFException;
import org.jdownloader.actions.AbstractSelectionContextAction;
import org.jdownloader.gui.views.SelectionInfo;

import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

public abstract class AbstractExtensionAction<T extends AbstractExtension<?, ?>, PackageType extends AbstractPackageNode<ChildrenType, PackageType>, ChildrenType extends AbstractPackageChildrenNode<PackageType>> extends AbstractSelectionContextAction<PackageType, ChildrenType> {

    public AbstractExtensionAction(SelectionInfo<PackageType, ChildrenType> si) {
        super(si);
    }

    private T extension;
    {

        Class<?> myClass = getClass();
        main: while (myClass != null && extension == null) {
            try {
                Type supClass = myClass.getGenericSuperclass();
                if (supClass instanceof ParameterizedTypeImpl) {

                    ParameterizedTypeImpl sc = ((ParameterizedTypeImpl) supClass);
                    for (Type t : sc.getActualTypeArguments()) {
                        if (t instanceof Class && AbstractExtension.class.isAssignableFrom((Class<?>) t)) {
                            Class<? extends AbstractExtension> clazz = (Class<? extends AbstractExtension>) t;
                            LazyExtension ex = ExtensionController.getInstance().getExtension(clazz);

                            extension = ((T) ex._getExtension());
                            break main;
                        }
                    }

                    myClass = ((ParameterizedTypeImpl) supClass).getRawType();

                } else if (supClass instanceof Class) {
                    myClass = (Class<?>) supClass;
                } else {
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
                throw new WTFException(e);

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
