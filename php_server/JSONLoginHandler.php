<?php

include_once 'JSONHandler.php';
include_once 'Session.php';


class LoginHandler extends JSONHandler {
	public function call($jsonArray, $jsonRPCId) {
		if (strcmp($jsonArray['method'], "login") != 0)
			return false;
		$params = $jsonArray["params"];
		if (!isset($params['upi']) || !isset($params['password']))
			return false;

		return $this->login($params['upi'], $params['password'], $jsonRPCId);
	}

	protected function loginReturn($jsonId, $error, $message) {
		return $this->makeJSONRPCReturn($jsonId, array('error' => $error, 'message' => $message));
	}


	private function login($username, $password, $jsonId) {
		if ($username == "" || $password == "")
			return $this->loginReturn($jsonId, -1, "Check your login data...");

		// connect to ldap server
		$ldapConnection = ldap_connect("ldaps://uoa.auckland.ac.nz:636");
		if (!$ldapConnection)
			return $this->loginReturn($jsonId, -1, "failed to connect to LDAP server...");

		ldap_set_option($ldapConnection, LDAP_OPT_PROTOCOL_VERSION, 3);

		$ldapBind = ldap_bind($ldapConnection, "cn=${username},ou=people,dc=uoa,dc=auckland,dc=ac,dc=nz", $password);

		// do some more checks if the user is real
		if ($ldapBind) {
			$ldapSearch = ldap_search($ldapConnection, "ou=people,dc=uoa,dc=auckland,dc=ac,dc=nz", "cn=${username}");
			if ($ldapSearch) {
				//$ldapResult = ldap_get_entries($ldapConnection, $ldapSearch);
				//print_r($ldapResult);
			}
			else
				$ldapBind = null;
		}
		// verify binding
		if ($ldapBind) {
			Session::get()->setUser($username);
			return $this->loginReturn($jsonId, 0, "login successful");
		} else {
			return $this->loginReturn($jsonId, -1, "LDAP bind failed...");
		}
	}
}

?>
