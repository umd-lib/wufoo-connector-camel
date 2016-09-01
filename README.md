Wufoo Connector - Camel Java Router Project
===========================================
Apache Camel is used in integrating Wufoo and SysAid. On Successful completion of Wufoo form, Wufoo/SysAid Connector will create a service request in SysAid with the information received from WuFoo form.

The Integration uses Wufoo's webhook to send request submitted from WuFoo form to apache Camel listener. The Camel Listener receives the information through Camel Jetty Component and transforms the information into JSON format that SysAid requires for creating a service request. Rest API for SysAid is used to connect and create the service request in SysAid.

To build this project use

    mvn install

To run this project from within Maven use

    mvn camel:run

For more help see the Apache Camel documentation

    http://camel.apache.org/
    
For Local Development, Use UltraHook to forward the request to localhost in your desktop.
UltraHook makes it super easy to connect public webhook endpoints with development environments.
```sh
$ gem install ultrahook
$ ultrahook stripe 5000
Authenticated as senvee
Forwarding activated...
http://stripe.senvee.ultrahook.com -> http://localhost:5000
```
