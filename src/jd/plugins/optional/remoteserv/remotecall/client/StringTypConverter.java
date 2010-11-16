package jd.plugins.optional.remoteserv.remotecall.client;

public class StringTypConverter implements TypeConverter {

    public Object convert(final Object call) {
        return call == null ? null : call.toString();
    }

}
