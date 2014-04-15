package jd.plugins;

import jd.controlling.packagecontroller.AbstractNode;
import jd.parser.Regex;

public class PartInfo {

    public int getNum() {
        return num;
    }

    private final int num;

    public PartInfo(/* storable */) {
        this(-1);
    }

    public PartInfo(int num) {
        this.num = Math.max(-1, num);
    }

    public PartInfo(AbstractNode node) {
        int num = -1;
        if (node != null) {
            try {
                String name = node.getName();
                if (node instanceof DownloadLink) name = ((DownloadLink) node).getView().getDisplayName();
                String partID = new Regex(name, "\\.r(\\d+)$").getMatch(0);
                if (partID == null) partID = new Regex(name, "\\.pa?r?t?\\.?(\\d+).*?\\.rar$").getMatch(0);
                if (partID != null) {
                    num = Integer.parseInt(partID);
                }
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        this.num = num;
    }
}
