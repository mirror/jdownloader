package org.jdownloader.container.sft;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class DelphiFormBinLoader {

    public static final String DFM_MAGIC_HEADER = "TPF0";

    protected DelphiFormEntry  root;
    protected DataInputStream  inputStream;

    public DelphiFormBinLoader(DataInputStream inputStream) throws Exception {
        this.setInput(inputStream);
        this.run();
    }

    public void setInput(DataInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void releaseInput() {
        this.inputStream = null;
    }

    public void run() throws Exception {
        if (this.inputStream == null) throw new NullPointerException("invalid dfm stream");

        byte[] sftMagic = new byte[4];
        this.inputStream.read(sftMagic, 0, 4);
        if (new String(sftMagic, "UTF-8").equals(DFM_MAGIC_HEADER))
            this.readObject(null);
        else
            throw new UnsupportedOperationException("unsupported dfm format");
    }

    protected String readString() throws IOException {
        if (this.inputStream.available() == 0) return null;

        int len = this.inputStream.readByte() & 0xFF;
        if ((len == 0) || (this.inputStream.available() < len)) return null;
        byte[] rawstring = new byte[len];
        this.inputStream.read(rawstring);
        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
        return new String(rawstring);
    }

    protected String readUTFString() throws IOException {
        if (this.inputStream.available() == 0) return null;

        byte[] rawlen = new byte[4];
        this.inputStream.read(rawlen);
        int len = ((rawlen[3] & 0xFF) << 24) | ((rawlen[2] & 0xFF) << 16) | ((rawlen[1] & 0xFF) << 8) | (rawlen[0] & 0xFF);
        if ((len == 0) || (this.inputStream.available() < len)) return null;

        byte[] rawstring = new byte[len];
        this.inputStream.read(rawstring);
        /* CHECK: we should always use getBytes("UTF-8") or with wanted charset, never system charset! */
        byte[] encstring = new String(rawstring, "UTF-8").getBytes();

        return new String(encstring, "UTF-8");
    }

    protected long readNumber(int size) throws IOException {
        long propertyValue;
        switch (size) {
        case 1:
            propertyValue = this.inputStream.readByte() & 0xff;
            break;
        case 2:
            propertyValue = this.inputStream.readShort() & 0xffff;
            break;
        case 4:
            propertyValue = this.inputStream.readInt() & 0xffffffff;
            break;
        case 8:
            propertyValue = bigToLittleEndian(this.inputStream.readLong()) & 0x7fffffffffffffffL;
            break;
        default:
            throw new UnsupportedOperationException("unsupported integer size");
        }
        return propertyValue;
    }

    protected double readExtended() throws IOException {
        byte[] extended = new byte[10];
        byte[] normalize = new byte[8];
        this.inputStream.read(extended);

        long exponent = ((((long) extended[9] & 0x7F) << 8) | ((long) extended[8])) - 16383 + 1023;
        long double_builder = 0;
        double_builder |= ((long) extended[9] & 0x80) << 56;
        double_builder |= ((long) (exponent << 4) & 0xFFFF) << 48;
        double_builder |= ((long) extended[7] & 0x7F) << 45;
        double_builder |= ((long) extended[6] & 0xFF) << 37;
        double_builder |= ((long) extended[5] & 0xFF) << 29;
        double_builder |= ((long) extended[4] & 0xFF) << 21;
        double_builder |= ((long) extended[3] & 0xFF) << 13;
        double_builder |= ((long) extended[2] & 0xFF) << 5;
        double_builder |= ((long) extended[1] & 0xFF) >> 3;

        ByteBuffer.wrap(normalize).putLong(double_builder);
        return ByteBuffer.wrap(normalize).getDouble();
    }

    protected byte[] readBinary() throws IOException {
        if (this.inputStream.available() == 0) return null;

        byte[] rawlen = new byte[4];
        this.inputStream.read(rawlen);

        int len = ((rawlen[3] & 0xFF) << 24) | ((rawlen[2] & 0xFF) << 16) | ((rawlen[1] & 0xFF) << 8) | (rawlen[0] & 0xFF);
        if ((len == 0) || (this.inputStream.available() < len)) return null;
        byte[] raw = new byte[len];
        this.inputStream.read(raw);
        return raw;
    }

    protected ArrayList<String> readSet() throws IOException {
        ArrayList<String> list = new ArrayList<String>();

        String entry;
        while ((entry = readString()) != null)
            list.add(entry);

        return list;
    }

    protected boolean readObject(DelphiFormEntry parent) throws Exception {

        String objectType = readString();
        if (objectType == null) return false;
        String objectName = readString();

        DelphiFormEntry node = new DelphiFormEntryObject(parent, objectName, objectType);
        if (node.isRoot()) root = node;

        readPropertys(node, false);
        while (readObject(node))
            ;

        return true;
    }

    protected void readCollection(DelphiFormEntry parent) throws Exception {
        while ((this.inputStream.readByte() & 0xFF) == 1) {
            readPropertys(new DelphiFormEntryCollectionItem(parent), false);
        }
    }

    protected void readPropertys(DelphiFormEntry parent, boolean isList) throws Exception {
        while (true) {
            String propertyName = null;
            if (isList == false) {
                propertyName = readString();
                if (propertyName == null) return;
            }

            int propertyType = this.inputStream.readByte() & 0xFF;

            switch (propertyType) {
            case 0:
                if (isList) return;
                break;
            case 1:
                readPropertys(new DelphiFormEntryList(parent, propertyName), true);
                break;
            case 2:
                new DelphiFormEntryNumber(parent, propertyName, readNumber(1), propertyType);
                break;
            case 3:
                new DelphiFormEntryNumber(parent, propertyName, readNumber(2), propertyType);
                break;
            case 4:
                new DelphiFormEntryNumber(parent, propertyName, readNumber(4), propertyType);
                break;
            case 5:
                new DelphiFormEntryExtended(parent, propertyName, readExtended());
                break;
            case 6:
                new DelphiFormEntryString(parent, propertyName, readString());
                break;
            case 7:
                new DelphiFormEntryEnumValue(parent, propertyName, readString());
                break;
            case 8:
                new DelphiFormEntryBoolean(parent, propertyName, false);
                break;
            case 9:
                new DelphiFormEntryBoolean(parent, propertyName, true);
                break;
            case 11:
                new DelphiFormEntrySet(parent, propertyName, readSet());
                break;
            case 12:
                new DelphiFormEntryBinary(parent, propertyName, readBinary());
                break;
            case 14:
                readCollection(new DelphiFormEntryCollection(parent, propertyName));
                break;
            case 19:
                new DelphiFormEntryNumber(parent, propertyName, readNumber(8), propertyType);
                break;
            case 20:
                new DelphiFormEntryString(parent, propertyName, readUTFString());
                break;
            default:
                throw new UnsupportedOperationException("unsupported entry type " + propertyType);
            }
        }
    }

    public DelphiFormEntry getRoot() {
        return this.root;
    }

    public static long bigToLittleEndian(long bigendian) {
        ByteBuffer buf = ByteBuffer.allocate(8);

        buf.order(ByteOrder.BIG_ENDIAN);
        buf.putLong(bigendian);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf.getLong(0);
    }
}
