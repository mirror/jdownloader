package jd.plugins.optional.remoteserv.remotecall.client;

public class TypeConverterFactory {

    private static final StringTypConverter  CONVERTER_STRING  = new StringTypConverter();
    private static final IntegerTypConverter CONVERTER_INT     = new IntegerTypConverter();
    private static final DefaultTypConverter CONVERTER_DEFAULT = new DefaultTypConverter();

    public static TypeConverter getInstance(final Class<?> returnType) {
        if (returnType.isAssignableFrom(int.class)) {
            return TypeConverterFactory.CONVERTER_INT;
        } else if (returnType.isAssignableFrom(String.class)) {
            return TypeConverterFactory.CONVERTER_STRING;
        } else {
            return TypeConverterFactory.CONVERTER_DEFAULT;
        }

    }

}
