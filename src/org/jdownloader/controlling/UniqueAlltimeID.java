package org.jdownloader.controlling;

import java.util.HashSet;
import java.util.Set;

public class UniqueAlltimeID extends org.appwork.utils.UniqueAlltimeID {
    public UniqueAlltimeID() {
        super();
    }

    private Long idLong = null;

    public UniqueAlltimeID(long id2) {
        super(id2);
    }

    public void setID(long ID) {
        super.setID(ID);
        this.idLong = Long.valueOf(ID);
    }

    public Long getIDLong() {
        final Long ret = idLong;
        if (ret == null) {
            setID(getID());
            return getIDLong();
        } else {
            return ret;
        }
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
