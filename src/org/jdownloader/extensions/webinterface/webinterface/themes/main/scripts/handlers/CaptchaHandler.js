/* CAPTCHA START */
var CaptchaHandler = {
	_queue : [],
	addCaptcha : function addCaptcha(captcha) {
		if (captcha.type !== "NORMAL") {
			alert("Warning: Captcha type " + captcha.type + " (currently) is not supported.");
		}
		try {
			CaptchaHandler._queue.push(captcha);
		} catch(e) {
			console.log("Ou of Captchas");	
		}
		if (CaptchaHandler._queue.length == 1) {
			CaptchaHandler.showNext();
		}
	},
	removeCaptcha: function removeCaptcha(captcha) {
		for(var i = 0; i < CaptchaHandler._queue.length; i++) {
			if(CaptchaHandler._queue[i].id === captcha.id) {
				if(i===0) {
					CaptchaHandler._queue.shift();
					if(CaptchaHandler._queue.length > 0) {
						CaptchaHandler.showNext();
					}
				} else {
					CaptchaHandler._queue.splice(i,1);
				}
				break;
			}
		}
	},
	showNext : function showNext() {
		$("#captacha_img").attr("src", $.jd.getURL("captcha/get", CaptchaHandler._queue[0].id));
        document.captchaform.captchaInput.value="";
		$("#jd-captcha-dialog").dialog("option", {modal: true,resizable: false}).dialog("open");
		$("#jd-captcha-dialog" ).bind( "dialogclose", function(event, ui) {
			$.jd.send("captcha/abort", [ CaptchaHandler._queue.shift().id, "BLOCKTHIS" ]);
			$.jGrowl($.i18n._('captcha.closed'), {header: "Captcha", position: "bottom-right"});
		});
	},
	onCaptcha : function onCaptcha(event) {
				switch(event.message) {
					case "new":
						CaptchaHandler.addCaptcha(event.data);
						break;
					case "expired":
						CaptchaHandler.removeCaptcha(event.data);
						break;
				}
	},
	solve : function solve() {
		$.jd.send("captcha/solve", [ CaptchaHandler._queue.shift().id, $("#captchaInput").val() ]);
		if (CaptchaHandler._queue.length > 0) {
			CaptchaHandler.showNext();
		}
		return false;
	},
	getCaptchas : function getCaptchas() {
		$.jd.send("captcha/list", function onCaptchaList(captchas) {
			$.each(captchas, function(i, captcha) {
				CaptchaHandler.addCaptcha(captcha);
			});
		});
	}
};

/* CAPTCHA END */