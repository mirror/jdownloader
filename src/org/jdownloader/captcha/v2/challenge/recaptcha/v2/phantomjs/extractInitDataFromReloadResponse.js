(function() {
	try {
		var initData = reloadResponse;
		logger("TYPE "+initData[5]);
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
	
		ret = {};
	
		ret.contentType = initData[4][1][0];
	
		ret.x = initData[4][1][4];
	
		ret.y = initData[4][1][3];

		ret.challengeType = initData[5];
		
		if (typeof (initData[4][1][1]) !== 'undefined') {
			ret.explainUrl = initData[4][1][1];
		}

		return ret;
	} catch (err) {
		logger(err);
		ret = {};
		ret.x = -1;
		ret.y = -1;
		ret.challengeType = "unknown";
		
		return ret;
	}

})()