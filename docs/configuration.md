# Wufoo SysAid Configuration

Apache Camel is used in integrating Wufoo and SysAid. On Successful completion of Wufoo form, Wufoo/SysAid Connector will create a service request in SysAid with the information received from WuFoo form.

The Integration uses Wufoo's webhook to send request submitted from WuFoo form to apache Camel listener. The Camel Listener receives the information through Camel Jetty Component and transforms the information into JSON format that SysAid requires for creating a service request. Rest API for SysAid is used to connect and create the service request in SysAid.

### Setup Camel Listener
```sh
mvn camel:run
```
Execute the above command to start the camel listener. Three routes will be created by the camel application. Route with id **WufooListener** is used for linking wufoo and sysaid copy the url associated with this route.

### Wufoo Configuration

In Wufoo form settings add webhook integration by navigating to edit >add integration > webhook settings.
* Input wufooListener address from camel into url field
* Input a unique value into handshake key field

### Modify edu.umd.lib.wufooconnectorcamel.cfg

* Input SysAid Url for the property key **sysaid.url**
* Input SysAid username for the property key **sysaid.username**
* Input SysAid password for the property key **sysaid.password**
* Input handshake key entered in wufoo forms for the property key **wufoo.handshake_key**

### Wufoo SysAid Field Mapping

Each wufoo form is uniquely identified by its hash value. For each wufoo form new mapping file can be provided with corresponding wufoo and sysaid field mappings. Two or more wufoo forms can use the same mapping file, but for each form new entry should be made into the property key **wufoo.sysaid.mapping**

```sh
wufoo.sysaid.mapping = formhashvalue=mappingfilepath&location, form2hashvalue=mappingfilepath&location
```
The Configuration file consists of the mapping between Wufoo form and SysAid form name. Using this Mapping file the parameters from the Wufoo is converted into parameters with SysAid field names. Fields can have Many to One Field Mapping.
```sh
wufoo.fields=Name,Email,Comment or Question,Subject  
sysaid.fields=request_user,description,title,responsibility
```
Provide All the fields created in wuFoo Form including Hidden fields if any for the key 'wufoo.fields'. Provide the fields that will be impacted in SysAid including the fields which need defaults for the key 'sysaid.fields'.
```sh
FIELDNAME.fieldMapping = Provide the SysAid field the field Should be mapped
```
The SysAid integration also supports defaults.
When a default value should be populated in SysAid for all the entries from a wufoo form defaults for the SysAid field can be set in the configuration file.
```sh
Sysaid.defaultfield=description
sysaid.defaults.responsibility=1221
#responsiblity is sysaid field for which default of 1221 will be assigned.
```

### SysAid Configuration 
The Values for SysAid Properties can be found under system admin menu in SysAID.
Steps to find SysAid Configuration information.

* Login to SysAid
* Click on Gear icon on the top header.
* Click on Customize Menu on left menu.
* Choose Web forms from the child menu.
* The Page provides list of forms available. 
* Click on any form and view the source code.
* Find the hidden input field with name formID for form ID value
```
 <input type="hidden" name="formID" value="8cb90ec:1565644c31d:-7ffb" />

```
* Find the form action tag with name 'frm'. This value should be used for webform url value.
```
 <form action="https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8" method="post" name="frm">

```
* Click on your name on the top header and select about from the menu options.
* Account ID can be found with field title Account Name.
* SysAid url can be found with field title server url.
