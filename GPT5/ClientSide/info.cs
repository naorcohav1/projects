using System;
using System.Diagnostics;
using System.Net;

namespace VIRUSPROG
{
    class GeneralInfo
    {
        public string oSystem;//string variable for storing the operating system
        public string uName;//string variable for storing the user's name
        public string cDirectory;//string variable for storing the current directory
        public string pName;//string variable for stroing the process name
        public string ePath;//string variable for storing the exacutable path 
        public string ipv4Address;//string variable for ipv4 address
        public string hostName;//string for storing host name
        public int pId;//string variable for storing the process id
        public bool isAdmin;//bool variable for checking if the user is admin
    
    public GeneralInfo(){//general info
        oSystem=Environment.OSVersion.ToString();
        uName=Environment.UserName; 
        cDirectory=Environment.CurrentDirectory;
        pName=Process.GetCurrentProcess().ProcessName;
        pId=Process.GetCurrentProcess().Id;
        hostName=Dns.GetHostName();
        ePath=Process.GetCurrentProcess().MainModule.FileName;
        ipv4Address=Dns.GetHostByName(hostName).AddressList[1].ToString();

        using var identity =  System.Security.Principal.WindowsIdentity.GetCurrent();
        var principal=new System.Security.Principal.WindowsPrincipal(identity);
        isAdmin =principal.IsInRole(System.Security.Principal.WindowsBuiltInRole.Administrator);

    }
    }
}