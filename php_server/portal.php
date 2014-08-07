<?php
error_reporting(E_ALL);
ini_set('display_errors', true);
ini_set('safe_mode', false);
ini_set('allow_url_fopen', true);
ini_set('allow_url_include', true);

ini_set('allow_url_include', 'on');

set_error_handler("var_dump");

?>

<?php
session_start();

include_once 'JSONLoginHandler.php';
include_once 'JSONUploadHandler.php';


function finish($message) {
	die($message);
}

function dispatchJSON($json) {
	$jsonArray = json_decode($json, true);

	if (!isset($jsonArray['method']))
		finish("no json RPC method!");
	if (!isset($jsonArray['id']))
		finish("no json RPC id!");
	$requestId = $jsonArray['id'];
	
	// setup handlers
	$handlers = array();
	$handlers[] = new LoginHandler();
	if (Session::get()->getUser() !== null)
		$handlers[] = new UploadHandler();

	$handled = false;
	foreach ($handlers as $handler) {
		$response = $handler->call($jsonArray, $requestId);
		if ($response !== false) {
			$handled = true;
			break;
		}
	}
	if (!$handled)
		finish(JSONHandler::makeJSONRPCReturn($requestId, array('error' => 0, 'message' => "json request not handled")));

	finish($response);
}


if (!isset($_REQUEST['json']))
	finish("not a json request");

dispatchJSON($_REQUEST['json']);

?>
