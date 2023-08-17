package jd.plugins;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.simplejson.JSonParser;

public class MinimalMemoryJSonParser extends JSonParser {
    public MinimalMemoryJSonParser(String json) {
        super(json);
    }

    @Override
    protected Map<String, ? extends Object> createJSonObject() {
        return new org.appwork.storage.simplejson.MinimalMemoryMap<String, Object>();
    }

    @Override
    protected Number parseFixedNumber(CharSequence charSequence) {
        try {
            return super.parseFixedNumber(charSequence);
        } catch (NumberFormatException e) {
            return new BigInteger(charSequence.toString());
        }
    }

    @Override
    protected Number parseFloatNumber(CharSequence charSequence) {
        try {
            return super.parseFloatNumber(charSequence);
        } catch (NumberFormatException e) {
            return new BigDecimal(charSequence.toString());
        }
    }

    @Override
    protected String dedupeString(String string) {
        return JSonStorage.dedupeString(string);
    }
}
