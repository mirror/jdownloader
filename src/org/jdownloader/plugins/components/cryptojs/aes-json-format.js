/**
* AES JSON formatter for CryptoJS
*
* @author BrainFooLong (bfldev.com)
* @link https://github.com/brainfoolong/cryptojs-aes-php
*/

var CryptoJSAesJson = {
    stringify: function (cipherParams) {
        var j = {ct: cipherParams.ciphertext.toString(CryptoJS.enc.Base64)};
        if (cipherParams.iv) j.iv = cipherParams.iv.toString();
        if (cipherParams.salt) j.s = cipherParams.salt.toString();
        return JSON.stringify(j);
    },
    parse: function (jsonStr) {
        var j = JSON.parse(jsonStr);
        var cipherParams = CryptoJS.lib.CipherParams.create({ciphertext: CryptoJS.enc.Base64.parse(j.ct)});
        if (j.iv) cipherParams.iv = CryptoJS.enc.Hex.parse(j.iv);
        if (j.s) cipherParams.salt = CryptoJS.enc.Hex.parse(j.s);
        return cipherParams;
    }
}