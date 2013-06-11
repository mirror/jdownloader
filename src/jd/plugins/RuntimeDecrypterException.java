package jd.plugins;

public class RuntimeDecrypterException extends RuntimeException {

    public RuntimeDecrypterException(DecrypterException decrypterException) {
        super(decrypterException.getMessage());
    }
}
