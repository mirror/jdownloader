package jd.captcha.pixelobject;

public class PixelObjectColor implements Comparable<PixelObjectColor> {
    public int color = 0;
    public int count = 1;

    public PixelObjectColor(int color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj instanceof PixelObjectColor && ((PixelObjectColor) obj).color == color;
    }

    public int compareTo(PixelObjectColor o) {
        return new Integer(o.count).compareTo(count);
    }
    @Override
    public String toString() {
        return "color:#"+Integer.toHexString(color)+" count:"+count;
    }
}