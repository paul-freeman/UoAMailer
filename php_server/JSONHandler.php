<?php

include_once 'Session.php';


function finish($message) {
	die($message);
}

function dispatchJSON($json) {
	$jsonArray = json_decode($json, true);

	if (!isset($jsonArray['method']))
		finish("no json RPC method!");
	if (!isset($jsonArray['id']))
		finish("no json RPC id!");

	$handler = null;

	if (Session::get()->getUser() === null)
		$handler = new LoginHandler();
	else
		$handler = new UploadHandler();

	$response = $handler->call($jsonArray, $jsonArray['id']);
	if ($response == false)
		finish("json request not handled");
	finish($response);
}



class JSONHandler {
	public function call($jsonArray, $jsonRPCId) {
		return false;
	}

	private function makeJSONRPCReturn($id, $result) {
		$array = array('jsonrpc' => "2.0", 'result' => $result, 'id' => $id);
		return json_encode($array);
	}
}

class LoginHandler extends JSONHandler {
	public function call($jsonArray, $jsonRPCId) {
		if (strcmp($jsonArray['method'], "login") != 0)
			return false;
		$params = $jsonArray["params"];
		if (!isset($params['upi']) || !isset($params['password']))
			return false;

		return $this->login($params['upi'], $params['password'], $jsonRPCId);
	}

	protected function loginReturn($jsonId, $ok, $message) {
		return $this->makeJSONRPCReturn($jsonId, array('login' => $ok, 'message' => $message));
	}


	private function login($username, $password, $jsonId) {
		// connect to ldap server
		$ldapConnection = ldap_connect("uoa.auckland.ac.nz");
		if (!$ldapConnection)
			return $this->loginReturn($jsonId, false, "failed to connect to LDAP server...");

		$ldapBind = ldap_bind($ldapConnection, "cn=${username},ou=People,dc=uoa,dc=auckland,dc=ac,dc=nz", $password);

		// verify binding
		if ($ldapBind) {
			Session::get()->setUser($username);
			return $this->loginReturn($jsonId, true, "login successful");
		} else {
			return $this->loginReturn($jsonId, false, "LDAP bind failed...");
		}
	}
}

class UploadHandler extends JSONHandler {
	public function call($jsonArray, $jsonRPCId) {

		if (strcmp($jsonArray['method'], "upload") != 0)
			return false;
		$params = $jsonArray["params"];
		if (!isset($params['files']))
			return false;

		return $this->upload($params['files'], $jsonRPCId);
	}

	private function upload($files, $jsonRPCId) {
		return $this->makeJSONRPCReturn($jsonRPCId, array('upload' => true, 'files' => $files, 'message' => "done"));
	}
} 

?>
