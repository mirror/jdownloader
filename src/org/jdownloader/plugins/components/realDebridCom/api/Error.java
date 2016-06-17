package org.jdownloader.plugins.components.realDebridCom.api;

public enum Error {
    INTERNAL(-1, "Internal error"),
    MISSING_PARAMETER(1, "Missing parameter"),
    BAD_PARAMETER_VALUE(2, "Bad parameter value"),
    UNKNOWN_METHOD(3, "Unknown method"),
    METHOD_NOT_ALLOWED(4, "Method not allowed"),
    SLOW_DOWN(5, "Slow down"),
    RESOURCE_UNREACHABLE(6, "Ressource unreachable"),
    RESOURCE_NOT_FOUND(7, "Resource not found"),
    BAD_TOKEN(8, "Bad token"),
    PERMISSION_DENIED(9, "Permission denied"),
    AUTH_PENDING(10, "Authorization pending"),
    TWO_FACTOR_AUTH_REQUIRED(11, "Two-Factor authentication needed"),
    TWO_FACTOR_AUTH_PENDING(12, "Two-Factor authentication pending"),
    BAD_LOGIN(13, "Invalid login"),
    ACCOUNT_LOCKED(14, "Account locked"),
    ACCOUNT_NOT_ACTIVATED(15, "Account not activated"),
    UNSUPPORTED_HOSTER(16, "Unsupported hoster"),
    HOSTER_IN_MAINTENANCE(17, "Hoster in maintenance"),
    HOSTER_LIMIT_REACHED(18, "Hoster limit reached"),
    HOSTER_TEMP_UNAVAILABLE(19, "Hoster temporarily unavailable"),
    HOSTER_PREMIUM_ONLY(20, "Hoster not available for free users"),
    TOO_MANY_ACTIVE_DOWNLOADS(21, "Too many active downloads"),
    IP_ADRESS_FORBIDDEN(22, "IP Address not allowed"),
    TRAFFIC_EXHAUSTED(23, "Traffic exhausted"),
    FILE_UNAVAILABLE(24, "File unavailable"),
    SERVICE_UNAVAILABLE(25, "Service unavailable"),
    UPLOAD_TOO_BIG(26, "Upload too big"),
    UPLOAD_ERROR(27, "Upload error"),
    FILE_NOT_ALLOWED(28, "File not allowed"),
    TORRENT_TOO_BIG(29, "Torrent too big"),
    TORRENT_FILE_INVALID(30, "Torrent file invalid"),

    ACTION_ALREADY_DONE(31, "Action already done"),

    IMAGE_RESOLUTION_ERROR(32, "Image resolution error"),
    // DUmmy Code:
    UNKNOWN(-99, "Unknown Error ID");
    public static Error getByCode(long id) {
        for (Error e : values()) {
            if (id == e.code) {
                return e;
            }
        }
        return UNKNOWN;
    };

    private int    code;
    private String msg;

    Error(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}