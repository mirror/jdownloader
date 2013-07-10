package jd.controlling.packagecontroller;

import java.util.List;

public interface PackageControllerModifyVetoListener<PackageType, ChildType> {
    public boolean onAskToRemovePackage(PackageType pkg);

    public boolean onAskToRemoveChildren(List<ChildType> children);
}
