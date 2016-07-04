Keycloak, FreeIPA and SSSD
================================================================

This module relates to the integration bits required to integrate FreeIPA, SSSD and Keycloak. It's considered a work in progress.

Meanwhile, everything will be documented here for people willing to give it a try.

## Required setup

SSSD and DBus modules are not tied to the FreeIPA installation. Although, makes more sense to have it installed and configured, once that's target of this project.

### libunix-dbus-java

The Java implementation of D-Bus make use of native code, more specifically C, to allow you to read and write Unix Sockets.

In order to install it, follow these steps:

1. Download the package

  ```
  $ wget -c https://github.com/abstractj/libunix-dbus-java/releases/download/libunix-dbus-java-0.8.0/libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm
  ```

2. Check the signature

  ```
  $ rpm -K libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm

  libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm:
      Header V4 RSA/SHA256 Signature, key ID 84dc9914: OK
      Header SHA1 digest: OK (d17bb7ebaa7a5304c1856ee4357c8ba4ec9c0b89)
      V4 RSA/SHA256 Signature, key ID 84dc9914: OK
      MD5 digest: OK (770c2e68d052cb4a4473e1e9fd8818cf)
  ```

3. Install the package

  ```
  dnf install libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm
  ```

### FreeIPA installation

Please refer to the [FreeIPA website](http://www.freeipa.org/page/Main_Page) for instructions about how to properly install and configure it.

#### Setting up an user account for testing

```
$ ipa user-add john --first=John --last=Smith --email=john@smith.com --phone=042424242 --street="Testing street" \      --city="Testing city" --state="Testing State" --postalcode=0000000000
```

### sssd.conf

In order to be able to retrieve and cache attributes
[SSSD infopipe](https://jhrozek.fedorapeople.org/sssd/1.12.0/man/sssd-ifp.5.html) must be configured. This follows a similar approach from [mod_lookup_identity](https://www.adelton.com/apache/mod_lookup_identity/), it's pretty much possible to provide scripts for an automated setup.

```
[domain/your-hostname.local]
...
ldap_user_extra_attrs = mail:mail, sn:sn, givenname:givenname, telephoneNumber:telephoneNumber

[sssd]
services = nss, sudo, pam, ssh, ifp
...

[ifp]
allowed_uids = root, yourOSUsername
user_attributes = +mail, +telephoneNumber, +givenname, +sn
```

After that, please make sure to reestart SSSD service:

```
$ sudo systemctl restart sssd-dbus
```

### Testing it

```
String[] attr = {"mail", "givenname", "sn" };
InfoPipe infoPipe = Sssd.infopipe();
Map<String, Variant> attributes = infoPipe.getUserAttributes("john", Arrays.asList(attr));

System.out.println(attributes);

List<String> groups = infoPipe.getUserGroups("john");

System.out.println(groups);

Sssd.disconnect();
```

It should just work with no additional setup required.

### Note

This is a work in progress, feedback, bug reports and patches are always welcome.
