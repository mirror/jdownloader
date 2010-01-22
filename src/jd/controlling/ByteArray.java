package jd.controlling;

public class ByteArray {

    private final byte[] buffer;
    private int mark = 0;

    /**
     * create new ByteArray instance with given size in bytes
     * 
     * @param size
     */
    public ByteArray(final int size) {
        buffer = new byte[size];
    }

    /**
     * get this byte[]
     * 
     * @return
     */
    final public byte[] getBuffer() {
        return buffer;
    }

    /**
     * set mark how much usefull bytes are in this byte[]
     * 
     * @param filled
     */
    public void setMark(final int mark) {
        this.mark = mark;
    }

    /**
     * returns how much usefull bytes are in this byte[]
     * 
     * @return
     */
    final public int getMark() {
        return mark;
    }

}
