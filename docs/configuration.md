# Wufoo SysAid Configuration

Apache Camel is used in integrating Wufoo and SysAid. On Successful completion
of Wufoo form, Wufoo/SysAid Connector will create a service request in SysAid
with the information received from WuFoo form.

The Integration uses Wufoo's webhook to send request submitted from WuFoo form
to apache Camel listener. The Camel Listener receives the information through
Camel Jetty Component and transforms the information into JSON format that
SysAid requires for creating a service request. Rest API for SysAid is used to
connect and create the service request in SysAid.

### Setup Camel Listener

```sh
mvn camel:run
```

Execute the above command to start the camel listener. Three routes will be
created by the camel application. Route with id **WufooListener** is used for
linking wufoo and sysaid copy the url associated with this route.

### Wufoo Configuration

In Wufoo form settings add webhook integration by navigating to edit >add
integration > webhook settings.

- Input wufooListener address from camel into url field
- Input a unique value into handshake key field

### Modify edu.umd.lib.wufooconnectorcamel.cfg

- Input SysAid Url for the property key **sysaid.url**
- Input SysAid username for the property key **sysaid.username**
- Input SysAid password for the property key **sysaid.password**
- Input handshake key entered in wufoo forms for the property key
  **wufoo.handshake_key**

### Wufoo SysAid Field Mapping

Each wufoo form is uniquely identified by its hash value. For each wufoo form
new mapping file can be provided with corresponding wufoo and sysaid field
mappings. Two or more wufoo forms can use the same mapping file, but for each
form new entry should be made into the property key **wufoo.sysaid.mapping**

```sh
wufoo.sysaid.mapping = form1-hashvalue=/path/to/mapping/file1, form2-hashvalue=/path/to/mapping/file2
```

The mapping file uses YAML format to define a list of SysAid fields. Each field
must specifiy the `name` and `template` values

```yaml
- name: SYSAID_FIELD_NAME
  template: TEMPLATE_STRING
  mappedField:
    - name: mapped_field_name
      wufooField: name_of_wufoo_field
      mapping:
        key1: val1
        key2: val2
```

#### name

The name field would specify the name of the SysAid field. The name field is
required.

#### template

The template field specifies the templated value to be sent to the SysAid during
ticket creation. It can be a plain text value or a complex template containing
one or more Wufoo fields or Mapped fields. The templates are rendered using the
stringtemplate4 library.

- The Wufoo fields and Mapped fields in the template string should be enclosed
  in angle brackets.
- The template can conditionally display text based in presence of a Wufoo
  value.
- The template can list a number of fields within a singe pair of brackets and
  define a separtor value. The rendered string would contain all on the
  non-empty fields in the list separated by the specified separator. Eg.
  `<[Field1,Field2]; separator=", ">`
- The template value can span across multiple lines. See [Yaml docs][2].
  - The pipe `|` character is used to begin a value that contains multiple
    logical lines. (The rendered value will have multiple lines)
  - To break a single logical line to multiple physcial lines, enclose the value
    with double quotes and use "\" to break lines. (The rendered value will have
    single line)
- If the value includes a `:` character enclose the value with double quotes.

See [stringtemplate4 docs][1] for more information on supported features.

#### mappedFields

The mappedFields field is an optional field. It is only necessary if the
template includes mappedFields. The mappedFields are used to map a wufoo
submission value to different value. For example, if the wufoo field Field1 has
text value "SSDR" but the SysAid ticket should use the value "Software Systems
Development & Research", a mapping field can be defined and the template can
include the mapped field instead of directly using the wufoo field as shown
below. A single wufoo field can be the source of multiple mapped fields if
necessary.

```yaml
- name: title
  template: "Department: <mapped_dept_field>"
  mappedFields:
    - name: mapped_dept_field
      wufooField: Field1
      mapping:
        ssdr: Software System Development & Research
```

Use the [sample configuration file][3] as the starting point to create a new
mapping file.

[1]: https://github.com/antlr/stringtemplate4/blob/master/doc/templates.md
[2]: http://yaml.org/spec/1.2/spec.html#id2760844
[3]: ../src/main/resources/wufoo-sysaid-field-mapping-sample.yml

Note: The older `.properties` based mapping configuration is deprecated.

### SysAid Configuration

The Values for SysAid Properties can be found under system admin menu in SysAID.
Steps to find SysAid Configuration information.

- Login to SysAid
- Click on Gear icon on the top header.
- Click on Customize Menu on left menu.
- Choose Web forms from the child menu.
- The Page provides list of forms available.
- Click on any form and view the source code.
- Find the hidden input field with name formID for form ID value

```
 <input type="hidden" name="formID" value="8cb90ec:1565644c31d:-7ffb" />
```

- Find the form action tag with name 'frm'. This value should be used for
  webform url value.

```
 <form action="https://libticketingdev.umd.edu/webformsubmit?pageEncoding=utf-8" method="post" name="frm">
```

- Click on your name on the top header and select about from the menu options.
- Account ID can be found with field title Account Name.
- SysAid url can be found with field title server url.
