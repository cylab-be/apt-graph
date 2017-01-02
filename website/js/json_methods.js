// jscs:disable
function json_post(url, jsonbody) {
	// Sending and receiving data in JSON format using POST method
	//
	var request = new XMLHttpRequest();
	request.open('POST', url, true);
	request.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
	var body = JSON.stringify(jsonbody);
	request.send(body);
	return request;
}
