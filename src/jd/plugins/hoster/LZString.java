/*
 * LZString4Java By Rufus Huang
 * https://github.com/rufushuang/lz-string4java
 * MIT License
 *
 * Port from original JavaScript version by pieroxy
 * https://github.com/pieroxy/lz-string
 */

package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class LZString {

    protected static class Context {
        HashMap<String, Integer> dictionary         = new HashMap<String, Integer>();
        HashSet<String>          dictionaryToCreate = new HashSet<String>();
        String                   c                  = "";
        String                   wc                 = "";
        String                   w                  = "";
        // Compensate for the first entry which should not count
        double                   enlargeIn          = 2d;
        int                      dictSize           = 3;
        int                      numBits            = 2;
        Data                     data               = new Data();
    }

    protected static class Data {
        int          val;
        int          position;
        StringBuffer string = new StringBuffer();
    }

    protected static void writeBit(char value, Data data) {
        data.val = (char) ((data.val << 1) | value);
        if (data.position == 15) {
            data.position = 0;
            data.string.append((char) data.val);
            data.val = 0;
        } else {
            data.position++;
        }
    }

    protected static void writeBits(int numBits, char value, Data data) {
        for (int i = 0; i < numBits; i++) {
            writeBit((char) (value & 1), data);
            value = (char) (value >> 1);
        }
    }

    protected static void produceW(Context context) {
        if (context.dictionaryToCreate.contains(context.w)) {
            char firstChar = context.w.charAt(0);
            if (firstChar < 256) {
                writeBits(context.numBits, (char) 0, context.data);
                writeBits(8, firstChar, context.data);
            } else {
                writeBits(context.numBits, (char) 1, context.data);
                writeBits(16, firstChar, context.data);
            }
            decrementEnlargeIn(context);
            context.dictionaryToCreate.remove(context.w);
        } else {
            writeBits(context.numBits, (char) context.dictionary.get(context.w).intValue(), context.data);
        }
        decrementEnlargeIn(context);
    }

    protected static void decrementEnlargeIn(Context context) {
        context.enlargeIn--;
        if ((int) context.enlargeIn == 0) {
            context.enlargeIn = Math.pow(2, context.numBits);
            context.numBits++;
        }
    }

    public static String compress(String uncompressedStr) {
        int i, value;
        HashMap<String, Integer> context_dictionary = new HashMap<String, Integer>();
        HashSet<String> context_dictionaryToCreate = new HashSet<String>();
        String context_c = "";
        String context_wc = "";
        String context_w = "";
        double context_enlargeIn = 2d; // Compensate for the first entry which should not count
        int context_dictSize = 3;
        int context_numBits = 2;
        StringBuffer context_data_string = new StringBuffer(uncompressedStr.length() / 3);
        int context_data_val = 0;
        int context_data_position = 0;
        int ii;

        char[] uncompressed = uncompressedStr.toCharArray();
        for (ii = 0; ii < uncompressed.length; ii += 1) {
            context_c = String.valueOf(uncompressed[ii]);
            if (!context_dictionary.containsKey(context_c)) {
                context_dictionary.put(context_c, context_dictSize++);
                context_dictionaryToCreate.add(context_c);
            }

            context_wc = context_w + context_c;
            if (context_dictionary.containsKey(context_wc)) {
                context_w = context_wc;
            } else {
                if (context_dictionaryToCreate.contains(context_w)) {
                    if (context_w.charAt(0) < 256) {
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1);
                            if (context_data_position == 15) {
                                context_data_position = 0;
                                context_data_string.append((char) context_data_val);
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                        }
                        value = context_w.charAt(0);
                        for (i = 0; i < 8; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == 15) {
                                context_data_position = 0;
                                context_data_string.append((char) context_data_val);
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = value >> 1;
                        }
                    } else {
                        value = 1;
                        for (i = 0; i < context_numBits; i++) {
                            context_data_val = (context_data_val << 1) | value;
                            if (context_data_position == 15) {
                                context_data_position = 0;
                                context_data_string.append((char) context_data_val);
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = 0;
                        }
                        value = context_w.charAt(0);
                        for (i = 0; i < 16; i++) {
                            context_data_val = (context_data_val << 1) | (value & 1);
                            if (context_data_position == 15) {
                                context_data_position = 0;
                                context_data_string.append((char) context_data_val);
                                context_data_val = 0;
                            } else {
                                context_data_position++;
                            }
                            value = value >> 1;
                        }
                    }
                    context_enlargeIn--;
                    if (context_enlargeIn == 0) {
                        context_enlargeIn = Math.pow(2, context_numBits);
                        context_numBits++;
                    }
                    context_dictionaryToCreate.remove(context_w);
                } else {
                    value = context_dictionary.get(context_w);
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == 15) {
                            context_data_position = 0;
                            context_data_string.append((char) context_data_val);
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }

                }
                context_enlargeIn--;
                if (context_enlargeIn == 0) {
                    context_enlargeIn = Math.pow(2, context_numBits);
                    context_numBits++;
                }
                // Add wc to the dictionary.
                context_dictionary.put(context_wc, context_dictSize++);
                context_w = context_c;
            }
        }

        // Output the code for w.
        if (!context_w.isEmpty()) {
            if (context_dictionaryToCreate.contains(context_w)) {
                if (context_w.charAt(0) < 256) {
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1);
                        if (context_data_position == 15) {
                            context_data_position = 0;
                            context_data_string.append((char) context_data_val);
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 8; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == 15) {
                            context_data_position = 0;
                            context_data_string.append((char) context_data_val);
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }
                } else {
                    value = 1;
                    for (i = 0; i < context_numBits; i++) {
                        context_data_val = (context_data_val << 1) | value;
                        if (context_data_position == 15) {
                            context_data_position = 0;
                            context_data_string.append((char) context_data_val);
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = 0;
                    }
                    value = context_w.charAt(0);
                    for (i = 0; i < 16; i++) {
                        context_data_val = (context_data_val << 1) | (value & 1);
                        if (context_data_position == 15) {
                            context_data_position = 0;
                            context_data_string.append((char) context_data_val);
                            context_data_val = 0;
                        } else {
                            context_data_position++;
                        }
                        value = value >> 1;
                    }
                }
                context_enlargeIn--;
                if (context_enlargeIn == 0) {
                    context_enlargeIn = Math.pow(2, context_numBits);
                    context_numBits++;
                }
                context_dictionaryToCreate.remove(context_w);
            } else {
                value = context_dictionary.get(context_w);
                for (i = 0; i < context_numBits; i++) {
                    context_data_val = (context_data_val << 1) | (value & 1);
                    if (context_data_position == 15) {
                        context_data_position = 0;
                        context_data_string.append((char) context_data_val);
                        context_data_val = 0;
                    } else {
                        context_data_position++;
                    }
                    value = value >> 1;
                }

            }
            context_enlargeIn--;
            if (context_enlargeIn == 0) {
                context_enlargeIn = Math.pow(2, context_numBits);
                context_numBits++;
            }
        }

        // Mark the end of the stream
        value = 2;
        for (i = 0; i < context_numBits; i++) {
            context_data_val = (context_data_val << 1) | (value & 1);
            if (context_data_position == 15) {
                context_data_position = 0;
                context_data_string.append((char) context_data_val);
                context_data_val = 0;
            } else {
                context_data_position++;
            }
            value = value >> 1;
        }

        // Flush the last char
        while (true) {
            context_data_val = (context_data_val << 1);
            if (context_data_position == 15) {
                context_data_string.append((char) context_data_val);
                break;
            } else {
                context_data_position++;
            }
        }
        return context_data_string.toString();
    }

    protected static int readBit(DecData data) {
        int res = data.val & data.position;
        data.position >>= 1;
            if (data.position == 0) {
                data.position = 32768;
                data.val = data.string[data.index++];
            }
            return res > 0 ? 1 : 0;
    }

    protected static int readBits(int numBits, DecData data) {
        int res = 0;
        double maxpower = Math.pow(2, numBits);
        int power = 1;
        while (power != Math.round(maxpower)) {
            res |= readBit(data) * power;
            power <<= 1;
        }
        return res;
    }

    protected static class DecData {
        public char[] string;
        public char   val;
        public int    position;
        public int    index;
    }

    public static String decompress(String compressed) {
        ArrayList<String> dictionary = new ArrayList<String>();
        int next;
        double enlargeIn = 4d;
        int dictSize = 4;
        int numBits = 3;
        String entry = "";
        StringBuffer result = new StringBuffer(compressed.length() * 4);
        String w;
        int c;
        int errorCount = 0;
        DecData data = new DecData();
        data.string = compressed.toCharArray();
        data.val = data.string[0];
        data.position = 32768;
        data.index = 1;

        for (int i = 0; i < 3; i += 1) {
            dictionary.add(i, String.valueOf((char) i));
        }

        next = readBits(2, data);
        switch (next) {
        case 0:
            c = readBits(8, data);
            break;
        case 1:
            c = readBits(16, data);
            break;
        case 2:
            return "";
        default:
            throw new RuntimeException("unexpected next = " + next);
        }
        dictionary.add(3, String.valueOf((char) c));
        w = String.valueOf((char) c);
        result.append(w);

        while (true) {
            c = readBits(numBits, data);

            switch (c) {
            case 0:
                if (errorCount++ > 10000) {
                    return "Error";
                }
                c = readBits(8, data);
                dictionary.add(dictSize++, String.valueOf((char) c));
                c = dictSize - 1;
                enlargeIn--;
                break;
            case 1:
                c = readBits(16, data);
                dictionary.add(dictSize++, String.valueOf((char) c));
                c = dictSize - 1;
                enlargeIn--;
                break;
            case 2:
                return result.toString();
            }

            if (Math.round(enlargeIn) == 0) {
                enlargeIn = Math.pow(2, numBits);
                numBits++;
            }

            if (c < dictionary.size() && dictionary.get(c) != null) {
                entry = dictionary.get(c);
            } else {
                if (c == dictSize) {
                    entry = w + w.charAt(0);
                } else {
                    return null;
                }
            }
            result.append(entry);

            // Add w+entry[0] to the dictionary.
            dictionary.add(dictSize++, w + entry.charAt(0));
            enlargeIn--;

            w = entry;

            if (Math.round(enlargeIn) == 0) {
                enlargeIn = Math.pow(2, numBits);
                numBits++;
            }

        }
        // return result.toString(); // Exists in JS ver, but unreachable code.
    }

    public static final String                           _keyStr          = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    protected static final char[]                        _keyChars        = _keyStr.toCharArray();
    protected static final HashMap<Character, Character> _keyCharIndexMap = new HashMap<Character, Character>(_keyStr.length());
    static {
        for (int i = 0; i < _keyChars.length; i++) {
            _keyCharIndexMap.put(_keyChars[i], (char) i);
        }
    }

    public static String compressToBase64(String inputStr) {
        if (inputStr == null) {
            return "";
        }
        StringBuffer output = new StringBuffer(inputStr.length() / 3);
        char chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        boolean chr2NaN = false, chr3NaN = false;
        int i = 0;

        char[] input = LZString.compress(inputStr).toCharArray();
        while (i < input.length * 2) {

            if (i % 2 == 0) {
                chr1 = (char) (input[i / 2] >> 8);
                chr2 = (char) (input[i / 2] & 255);
                chr2NaN = false;
                if (i / 2 + 1 < input.length) {
                    chr3 = (char) (input[i / 2 + 1] >> 8);
                    chr3NaN = false;
                } else {
                    chr3 = 0;
                    chr3NaN = true;
                }
            } else {
                chr1 = (char) (input[(i - 1) / 2] & 255);
                if ((i + 1) / 2 < input.length) {
                    chr2 = (char) (input[(i + 1) / 2] >> 8);
                    chr3 = (char) (input[(i + 1) / 2] & 255);
                    chr2NaN = false;
                    chr3NaN = false;
                } else {
                    chr2 = chr3 = 0;
                    chr2NaN = true;
                    chr3NaN = true;
                }
            }
            i += 3;

            enc1 = (char) (chr1 >> 2);
            enc2 = (char) (((chr1 & 3) << 4) | (chr2 >> 4));
            enc3 = (char) (((chr2 & 15) << 2) | (chr3 >> 6));
            enc4 = (char) (chr3 & 63);

            if (chr2NaN) {
                enc3 = enc4 = 64;
            } else if (chr3NaN) {
                enc4 = 64;
            }

            output.append(_keyChars[enc1]);
            output.append(_keyChars[enc2]);
            output.append(_keyChars[enc3]);
            output.append(_keyChars[enc4]);
        }

        return output.toString();
    }

    public static String decompressFromBase64(String inputStr) {
        if (inputStr == null) {
            return "";
        }
        StringBuffer output = new StringBuffer(inputStr.length() * 3);
        int ol = 0;
        char output_ = 0;
        char chr1, chr2, chr3;
        char enc1, enc2, enc3, enc4;
        int i = 0;

        inputStr = inputStr.replaceAll("[^A-Za-z0-9\\+\\/\\=]", "");
        char[] input = inputStr.toCharArray();

        while (i < input.length) {
            // enc1 = (char) LZString._keyStr.indexOf(String.valueOf(input[i++]));
            // enc2 = (char) LZString._keyStr.indexOf(String.valueOf(input[i++]));
            // enc3 = (char) LZString._keyStr.indexOf(String.valueOf(input[i++]));
            // enc4 = (char) LZString._keyStr.indexOf(String.valueOf(input[i++]));

            enc1 = _keyCharIndexMap.get(input[i++]);
            enc2 = _keyCharIndexMap.get(input[i++]);
            enc3 = _keyCharIndexMap.get(input[i++]);
            enc4 = _keyCharIndexMap.get(input[i++]);

            chr1 = (char) ((enc1 << 2) | (enc2 >> 4));
            chr2 = (char) (((enc2 & 15) << 4) | (enc3 >> 2));
            chr3 = (char) (((enc3 & 3) << 6) | enc4);

            if (ol % 2 == 0) {
                output_ = (char) (chr1 << 8);

                if (enc3 != 64) {
                    output.append((char) (output_ | chr2));
                }
                if (enc4 != 64) {
                    output_ = (char) (chr3 << 8);
                }
            } else {
                output.append((char) (output_ | chr1));

                if (enc3 != 64) {
                    output_ = (char) (chr2 << 8);
                }
                if (enc4 != 64) {
                    output.append((char) (output_ | chr3));
                }
            }
            ol += 3;
        }

        return LZString.decompress(output.toString());

    }

    public static String compressToUTF16(String inputStr) {
        if (inputStr == null) {
            return "";
        }
        StringBuffer output = new StringBuffer(inputStr.length() / 3);
        int current = 0;
        int status = 0;

        inputStr = LZString.compress(inputStr);
        char[] input = inputStr.toCharArray();

        for (int i = 0; i < input.length; i++) {
            char c = input[i];
            switch (status++) {
            case 0:
                output.append((char) ((c >> 1) + 32));
                current = (c & 1) << 14;
                break;
            case 1:
                output.append((char) ((current + (c >> 2)) + 32));
                current = (c & 3) << 13;
                break;
            case 2:
                output.append((char) ((current + (c >> 3)) + 32));
                current = (c & 7) << 12;
                break;
            case 3:
                output.append((char) ((current + (c >> 4)) + 32));
                current = (c & 15) << 11;
                break;
            case 4:
                output.append((char) ((current + (c >> 5)) + 32));
                current = (c & 31) << 10;
                break;
            case 5:
                output.append((char) ((current + (c >> 6)) + 32));
                current = (c & 63) << 9;
                break;
            case 6:
                output.append((char) ((current + (c >> 7)) + 32));
                current = (c & 127) << 8;
                break;
            case 7:
                output.append((char) ((current + (c >> 8)) + 32));
                current = (c & 255) << 7;
                break;
            case 8:
                output.append((char) ((current + (c >> 9)) + 32));
                current = (c & 511) << 6;
                break;
            case 9:
                output.append((char) ((current + (c >> 10)) + 32));
                current = (c & 1023) << 5;
                break;
            case 10:
                output.append((char) ((current + (c >> 11)) + 32));
                current = (c & 2047) << 4;
                break;
            case 11:
                output.append((char) ((current + (c >> 12)) + 32));
                current = (c & 4095) << 3;
                break;
            case 12:
                output.append((char) ((current + (c >> 13)) + 32));
                current = (c & 8191) << 2;
                break;
            case 13:
                output.append((char) ((current + (c >> 14)) + 32));
                current = (c & 16383) << 1;
                break;
            case 14:
                output.append((char) ((current + (c >> 15)) + 32));
                output.append((char) ((c & 32767) + 32));
                status = 0;
                break;
            }
        }
        output.append((char) (current + 32));
        return output.toString();
    }

    public static String decompressFromUTF16(String inputStr) {
        if (inputStr == null) {
            return "";
        }
        StringBuffer output = new StringBuffer(inputStr.length() / 3);
        int status = 0;
        int i = 0;
        int current = 0;

        char[] input = inputStr.toCharArray();

        while (i < input.length) {
            char c = (char) (input[i] - 32);

            switch (status++) {
            case 0:
                current = c << 1;
                break;
            case 1:
                output.append((char) (current | (c >> 14)));
                current = (c & 16383) << 2;
                break;
            case 2:
                output.append((char) (current | (c >> 13)));
                current = (c & 8191) << 3;
                break;
            case 3:
                output.append((char) (current | (c >> 12)));
                current = (c & 4095) << 4;
                break;
            case 4:
                output.append((char) (current | (c >> 11)));
                current = (c & 2047) << 5;
                break;
            case 5:
                output.append((char) (current | (c >> 10)));
                current = (c & 1023) << 6;
                break;
            case 6:
                output.append((char) (current | (c >> 9)));
                current = (c & 511) << 7;
                break;
            case 7:
                output.append((char) (current | (c >> 8)));
                current = (c & 255) << 8;
                break;
            case 8:
                output.append((char) (current | (c >> 7)));
                current = (c & 127) << 9;
                break;
            case 9:
                output.append((char) (current | (c >> 6)));
                current = (c & 63) << 10;
                break;
            case 10:
                output.append((char) (current | (c >> 5)));
                current = (c & 31) << 11;
                break;
            case 11:
                output.append((char) (current | (c >> 4)));
                current = (c & 15) << 12;
                break;
            case 12:
                output.append((char) (current | (c >> 3)));
                current = (c & 7) << 13;
                break;
            case 13:
                output.append((char) (current | (c >> 2)));
                current = (c & 3) << 14;
                break;
            case 14:
                output.append((char) (current | (c >> 1)));
                current = (c & 1) << 15;
                break;
            case 15:
                output.append((char) (current | c));
                status = 0;
                break;
            }
            i++;
        }

        return LZString.decompress(output.toString());
        // return output;

    }

}
