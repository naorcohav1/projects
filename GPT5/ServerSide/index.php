<?php
include 'session.php';
include 'conn.php';
?>




<html>
<style>
  th,
  td {
    border: 1px solid;
  }
</style>
<table>
  <tr>
    <th>Hostname</th>
    <th>Ip Address</th>
    <th>Operating System</th>
    <th>Action</th>
  </tr>
  <?php

  $botQuery = $mysql->query("select * from Victims");

  while ($row = $botQuery->fetch_assoc()) {
    $hostname = $row["hostname"];
    $operatingsystem = $row["operatingsystem"];
    $ipaddress = $row["ipaddress"];
    $Action = "<a href='/manage.php?bot=" . $hostname . "'>Manage</a>";

    echo "<tr>";
    echo "<td>" . $hostname . "</td>";
    echo "<td>" . $ipaddress . "</td>";
    echo "<td>" . $operatingsystem . "</td>";
    echo "<td>" . $Action . "</td>";
    echo "</tr>";
  }
  ?>


  <table>

</html>