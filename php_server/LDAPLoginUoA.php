<?php

class LDAPLoginUoA {
	private $ldapConnection;
	private $ldapBind;
	private $error = "";
	
	public function __construct($server = null) {
		if ($server === null)
			$server = "ldaps://uoa.auckland.ac.nz:636";
		$this->ldapConnection = ldap_connect($server);
		if (!$this->ldapConnection)
			$this->error = "failed to connect to LDAP server...";
		else
			ldap_set_option($this->ldapConnection, LDAP_OPT_PROTOCOL_VERSION, 3);
	}

	public function getLastError() {
		return $this->error;
	}

	public function login($username, $password) {
		if ($this->ldapConnection == null)
			return false;

		if ($username == "" || $password == "") {
			$this->error = "bad login data...";
			return false;
		}

		$this->ldapBind = ldap_bind($this->ldapConnection, "cn=${username},ou=people,dc=uoa,dc=auckland,dc=ac,dc=nz", $password);

		// do some more checks if the user is real
		if ($this->ldapBind) {
			$ldapSearch = ldap_search($this->ldapConnection, "ou=people,dc=uoa,dc=auckland,dc=ac,dc=nz", "cn=${username}");
			if ($ldapSearch) {
				//$ldapResult = ldap_get_entries($this->ldapConnection, $ldapSearch);
				//print_r($ldapResult);
			}
			else
				$this->ldapBind = null;
		}
		// verify binding
		if ($this->ldapBind) {
			return true;
		} else {
			$this->error = "invalid credentials";
			return false;
		}
	}	
}

?>
