package jd.plugins;

import org.appwork.storage.Storable;

public class PartInfo implements Storable {

    private String partID;

    public String getPartID() {
        return partID;
    }

    public void setPartID(String partID) {
        this.partID = partID;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }

    private int num;

    private PartInfo(/* storable */) {
    }

    public PartInfo(String partId, int num) {
        this.partID = partId;
        this.num = num;
    }

}
