package jd.crypt;



public final class Base16Decoder extends BaseDecoder {

    public Base16Decoder() {
        super(new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        });
    }
}
