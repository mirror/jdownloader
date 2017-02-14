package org.jdownloader.controlling;

import java.util.HashSet;
import java.util.Set;

public class UniqueAlltimeID extends org.appwork.utils.UniqueAlltimeID {

    public UniqueAlltimeID() {
        super();
    }

    public UniqueAlltimeID(long id2) {
        super(id2);
    }

    public static Set<UniqueAlltimeID> createSet(long... ids) {
        final Set<UniqueAlltimeID> ret = new HashSet<UniqueAlltimeID>();
        if (ids != null) {
            for (final long id : ids) {
                ret.add(new UniqueAlltimeID(id));
            }
        }
        return ret;
    }
}
