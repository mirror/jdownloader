(function() {
	var initData = reloadResponse;

	ret= {};
	ret.contentType=initData[4][1][0];
	ret.x=initData[4][1][4];
	ret.y=initData[4][1][3];
	ret.challengeType=initData[5];
	if(	typeof(initData[4][1][1]) !== 'undefined' ){
		ret.explainUrl=[4][1][1];
	}


	return ret;
	
})()