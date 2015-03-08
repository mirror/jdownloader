package jd.controlling.packagecontroller;

import java.util.List;

public interface PackageControllerModifyVetoListener<PackageType, ChildType> {
    public List<ChildType> onAskToRemovePackage(Object asker, PackageType pkg, List<ChildType> children);

    public List<ChildType> onAskToRemoveChildren(Object asker, List<ChildType> children);
}
