package jd.plugins.optional.jdunrar;

import java.util.regex.Pattern;

public class Signature {

    private String id;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Pattern getSignatur() {
        return signatur;
    }

    public void setSignatur(Pattern signatur) {
        this.signatur = signatur;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private Pattern signatur;
    private String desc;
    private Pattern extension;

    public Signature(String id, String signaturPattern, String desc,String ext) {
       this.id=id;
       this.signatur=signaturPattern!=null?Pattern.compile(signaturPattern,Pattern.CASE_INSENSITIVE):null;
       this.extension=ext!=null?Pattern.compile(ext,Pattern.CASE_INSENSITIVE):null;
       this.desc=desc;
    }

    public Pattern getExtension() {
        return extension;
    }

    public void setExtension(Pattern extension) {
        this.extension = extension;
    }

    public boolean matches(String sig) {    
        return signatur.matcher(sig).matches();     
    }

}
