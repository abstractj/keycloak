Keycloak and FreeIPA initial integration
================================================================

This module relates to the integration bits required to integrate FreeIPA, SSSD and Keycloak. It's considered a work in progress.

Meanwhile, everything will be documented here for people willing to give it a try.

## FreeIPA modules

SSSD, PAM and DBus modules are not tied to the FreeIPA installation. Although, they are mandatory to get the SSSD Federation provider working with PAM.

### [keycloak-dbus-java](https://github.com/abstractj/keycloak/tree/KEYCLOAK-3036/federation/freeipa/keycloak-dbus-java)

Contains all the classes from [dbus-java](https://dbus.freedesktop.org/doc/dbus-java/) with some small fixes.

### [keycloak-libpam4j](https://github.com/abstractj/keycloak/tree/KEYCLOAK-3036/federation/freeipa/keycloak-libpam4j)

A Java binding for libpam API responsible to authenticate username/password. Unfortunately the [latest release](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.kohsuke%22%20AND%20a%3A%22libpam4j%22) of this library is a bit dated and does not contain all the fixes. For this reason, I decided to get the [sources](https://github.com/kohsuke/libpam4j) and include it as a module.

### [keycloak-sssd-api](https://github.com/abstractj/keycloak/tree/KEYCLOAK-3036/federation/freeipa/keycloak-sssd-api)

It just contains all the objects representing the same objects from the SSSD DBus interface, into other words the stubs to represent remote objects from [Infopipe](https://fedorahosted.org/sssd/wiki/DesignDocs/DBusResponder). Plus, there's an utility class to make easy some conversions.

The Infopipe is the only way to retrive attributes like username, e-mail, address...from SSSD cache.

### [keycloak-sssd-federation](https://github.com/abstractj/keycloak/tree/KEYCLOAK-3036/federation/freeipa/keycloak-sssd-federation)

User Federation provider implementation baked by SSSD DBus information and PAM for authentication.

### [libunix-dbus-java](https://github.com/abstractj/libunix-dbus-java)

The Java implementation of D-Bus make use of native code, more specifically C, to allow you to read and write to Unix Sockets.

## Appendix: API usage

In order to make use of the API, follow these steps:

- Download the package

  ```
  $ wget -c https://github.com/abstractj/libunix-dbus-java/releases/download/libunix-dbus-java-0.8.0/libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm
  ```

- Check the signature

  ```
  $ rpm -K libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm

  libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm:
      Header V4 RSA/SHA256 Signature, key ID 84dc9914: OK
      Header SHA1 digest: OK (d17bb7ebaa7a5304c1856ee4357c8ba4ec9c0b89)
      V4 RSA/SHA256 Signature, key ID 84dc9914: OK
      MD5 digest: OK (770c2e68d052cb4a4473e1e9fd8818cf)
  ```

- Install the package

  ```
  dnf install libunix-dbus-java-0.8.0-1.fc24.x86_64.rpm
  ```

## FreeIPA server installation

Please refer to the [FreeIPA website](http://www.freeipa.org/page/Main_Page) for instructions about how to properly install and configure it.

### Setting up an user account for testing

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

## Note

This is a work in progress, feedback, bug reports and patches are always welcome.
