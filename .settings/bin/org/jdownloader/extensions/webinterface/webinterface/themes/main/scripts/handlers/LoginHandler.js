/* LOGIN START */
var LoginHandler = {
	start : function jdstart(ip,port,u,p) {
			$.jd.setOptions({
				apiServer : "http://"+ip+":"+port+"/",
				debug: true
			}).send('jd/version', LoginHandler.connect(u,p));
	},
	connect : function jdstart(user,pass) {
			$.jd.setOptions({
			user : user,
			pass : pass
		}).startSession(LoginHandler.auth);
	},
	auth : function jdauth(resp) {
		if (resp.status == $.jd.e.sessionStatus.REGISTERED) {
			CaptchaHandler.getCaptchas();
			$.jd.subscribe("captcha", CaptchaHandler.onCaptcha);
			$.jd.startPolling();
		} else {
			LoginHandler.error("auth");
		}
	},
	error : function jderror(mod) {
		alert("Your mode "+mod+" failed. Please Contact the Supportteam (http://board.jdownloader.org) with the latest steps you was doing.");
	}
};