(function() {
	try {
		var initData = reloadResponse;
		if ("multicaptcha" == initData[5]) {
			console.log("TYPE multicaptcha");
			ret = {};
			ret.x = initData[4][5][0][0][4];
			ret.y = initData[4][5][0][0][3];
			ret.challengeType = initData[5];
			return ret;
		} else if ("nocaptcha" == initData[5]) {
			console.log("TYPE NOCAPTCHA");
			ret = {};
			ret.x = -1;
			ret.y = -1;
			ret.challengeType = initData[5];
			return ret;
		}
		console.log("TYPE "+initData[4][1][0]);
		ret = {};
		ret.contentType = initData[4][1][0];
		ret.x = initData[4][1][4];
		ret.y = initData[4][1][3];
		ret.challengeType = initData[5];
		
		if (typeof (initData[4][1][1]) !== 'undefined') {
			ret.explainUrl = [4][1][1];
		}

		return ret;
	} catch (err) {
		ret = {};
		ret.x = -1;
		ret.y = -1;
		ret.challengeType = "unknown";
		return ret;
	}
//	  var rq = function(a) {
//			debugger;
//	        switch (a) {
//	            case "default":
//	                return new Vp;
//	            case "nocaptcha":
//	                return new aq;
//	            case "imageselect":
//	                return new Wp;
//	            case "tileselect":
//	                return new Wp("tileselect");
//	            case "dynamic":
//	                return new wq;
//	            case "audio":
//	                return new Np;
//	            case "text":
//	                return new cq;
//	            case "multicaptcha":
//	                return new tq;
//	            case "canvas":
//	                return new hq;
//	            case "coref":
//	                return new Qp
//	        }
//	    };
})()