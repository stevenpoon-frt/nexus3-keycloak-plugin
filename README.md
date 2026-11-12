Nexus3 Keycloak Plugin
===============================

Frontiers build for Nexus Repository OSS 3.69.0-02
---------------------------------------------------

This tree keeps the upstream git history, Apache-2.0 license, and attribution of
[flytreeleft/nexus3-keycloak-plugin](https://github.com/flytreeleft/nexus3-keycloak-plugin)
(archived). It is compiled against Nexus Repository OSS **3.69.0-02** on Java 8.

**Version string:** `0.6.0-nexus3.69.0`

Maven coordinate:

```
org.github.flytreeleft:nexus3-keycloak-plugin:0.6.0-nexus3.69.0
```

Karaf start level 200 entry, same shape as the image that installed `0.4.0-SNAPSHOT`:

```
reference:file:/org/github/flytreeleft/nexus3-keycloak-plugin/0.6.0-nexus3.69.0/nexus3-keycloak-plugin-0.6.0-nexus3.69.0.jar = 200
```

### What changed versus upstream

Upstream `0.4.0` and `0.6.0-SNAPSHOT` are built against Nexus `3.15.3-01`.
Login through `KeycloakAuthenticatingRealm` can succeed on Nexus 3.69, and the UI
then crashes:

```
java.lang.AbstractMethodError
    at org.sonatype.nexus.security.internal.DefaultSecuritySystem.findUser(DefaultSecuritySystem.java:353)
    at org.sonatype.nexus.security.internal.DefaultSecuritySystem.currentUser(DefaultSecuritySystem.java:377)
```

Line 353 is `userManager.getUser(userId, roleIds)`. `UserManager` gained that
overload after 3.15. `AbstractReadOnlyUserManager` in 3.69 does not implement it,
and `KeycloakUserManager` did not either. `currentUser` passes a null role set.
The missing method is a binary-compatibility gap, not a Keycloak outage.

`AuthorizationManager` also gained `getPrivilegeByName` and `getPrivileges`.
`AbstractReadOnlyAuthorizationManager` in 3.69 does not implement those, so the
plugin now does. Privileges are still not stored in Keycloak: lookup by name
throws `NoSuchPrivilegeException`, and `getPrivileges` returns an empty list,
matching `listPrivileges()`.

`getUser(String, Set)` follows Nexus `UserManagerImpl`:

- `roleIds == null` loads the user and roles from Keycloak. This is the `getState` / `currentUser` path.
- a non-null set is applied as this source's role ids and does not call Keycloak for roles again.

Nexus 3.69 `UserManager` does not declare `isConfigured()`. That method appears in
3.70 and is intentionally not added here.

Community forks were checked before adapting anything:

- `inside-m2m-gp` (parent `3.69.0-02`) adds `getUser(String, Set)` but ignores the null-versus-set contract and reformats the sources.
- `nsakellariou-cite` (parent `3.68.1-02`) always delegates to `getUser(String)`, which is correct for the null role set used by `currentUser`, and returns an empty privilege list.
- `rverchere` changes the parent to `3.69.0-02` without adding `getUser(String, Set)`. A later commit adds `isConfigured()`, which does not exist on the 3.69 interface.

Only the 3.69 method gap is taken from those forks. Behavior of role mapping, SSO headers, and Keycloak 9 client calls is unchanged.

### Build

JDK 8 and Maven. The parent POM is `org.sonatype.nexus.plugins:nexus-plugins:3.69.0-02`.

```
export JAVA_HOME=/usr/lib/jvm/temurin-8-jdk-amd64
mvn -PbuildKar clean verify
```

Artifacts:

- `target/nexus3-keycloak-plugin-0.6.0-nexus3.69.0.jar`
- `target/nexus3-keycloak-plugin-0.6.0-nexus3.69.0-bundle.kar`

The JAR inlines Keycloak 9.0.2 (`org.keycloak.*`), same as the 0.4.0 bundle. The KAR
contains only that bundle. It does not install a second Jackson or BouncyCastle.
OSGi `Bundle-Version` is `0.6.0.nexus3_69_0`. Paths and `startup.properties` use the
Maven version `0.6.0-nexus3.69.0`.

`mvn -PbuildKar clean verify` compiles against the 3.69 security API and runs the
unit tests, including a reflection check that every abstract `UserManager` and
`AuthorizationManager` method is implemented by the plugin classes and the
`auth0` / `auth1` / `auth2` subclasses.

Not run here: a live Nexus 3.69 process, Keycloak, or any Frontiers host.
Do not deploy this artifact to a production Nexus from this repository's scripts.

### Install into an image `FROM sonatype/nexus3:3.69.0`

Build the jar first. `docker/build.sh` copies it beside `docker/Dockerfile` and
builds a local image. It does not start a container and does not copy `keycloak.json`
(`docker/.dockerignore` excludes that file). Mount the Office realm file at runtime
from a secret store. Do not bake a client secret into the image.

The image registers the bundle at Karaf level 200 with `reference:file:`, which is
how `0.4.0-SNAPSHOT` was installed. The jar path under the Nexus system directory is:

```
/opt/sonatype/nexus/system/org/github/flytreeleft/nexus3-keycloak-plugin/0.6.0-nexus3.69.0/nexus3-keycloak-plugin-0.6.0-nexus3.69.0.jar
```

Equivalent `mvn:` startup line, if the image uses Maven URLs instead of `reference:file:`:

```
mvn:org.github.flytreeleft/nexus3-keycloak-plugin/0.6.0-nexus3.69.0 = 200
```

The KAR can be dropped in `/opt/sonatype/nexus/deploy/` instead of editing
`startup.properties`. Use one install method, not both, or the bundle will be
started twice.

### Rollback

Put the previous level-200 line back and leave the old jar on disk:

```
reference:file:/org/github/flytreeleft/nexus3-keycloak-plugin/0.4.0-SNAPSHOT/nexus3-keycloak-plugin-0.4.0-SNAPSHOT.jar = 200
```

Remove the `0.6.0-nexus3.69.0` line from `$install_dir/etc/karaf/startup.properties`
(or rebuild the image `FROM sonatype/nexus3:3.69.0` without this plugin). Restart
Nexus. Delete the new jar only after the previous login path is confirmed.

### Upstream documentation

The rest of this file is the upstream README. Its sample `keycloak.json` uses a
placeholder secret and is not a Frontiers credential.

This plugin adds a Keycloak realm to Sonatype Nexus OSS and enables you to
authenticate with Keycloak Realm Users and authorize with Keycloak realm/client roles or groups.

Upstream claimed support for Nexus 3.x and Keycloak 3.x ~ 14.x. This fork is verified
at compile time against Nexus 3.69.0-02 only. Keycloak client libraries stay at 9.0.2,
which is what the installed 0.4.0-SNAPSHOT bundle embedded.

## Features

- Supports to map the **Realm-Roles**, **Client-Roles** and **Realm-Groups** of Keycloak.
- Supports to map multiple Keycloak realms.
- Supports to login with the Service Account of Keycloak.
- Supports Keycloak Single Sign On (SSO): sign-in and sign-out are all supported.

## Warning

If you are using the version 0.2.x of this plugin, and want to upgrade the plugin to 0.3.x,
you should create some new roles in Nexus3, because the new version of the plugin supports to
map the **Realm-Roles**, **Client-Roles** and **Realm-Groups** of Keycloak to the Nexus3 roles.

But don't worry about your data or the existing role-mappings, they will still work well.
The latest version of the plugin is compatible with the old version.
In the new version, the original Keycloak role/group name will be prepended with
`RealmRole:` (If it's a Realm-Role), `ClientRole:` (If it's a Client-Role) or `RealmGroup:` (If it's a Realm-Group).

If something goes wrong when you upgraded the plugin to the new version.
**DO NOT** save or update your configurations again, just go back to the old version and restart Nexus3,
then [create](https://github.com/flytreeleft/nexus3-keycloak-plugin/issues/new) an issue to report your problem.

## Development

Read the details in [develop/README.md](./develop/README.md).

## Prerequisites

* JDK 8+ is installed
* Apache Maven is installed
* Sonatype Nexus OSS 3.x is installed

## Installation

When Nexus gets downloaded and unzipped, there are typically two directories created:
* nexus-3.3.2-02
* sonatype-work/nexus3

To avoid confusion, the conventions of the Sonatype reference will be used in the following descriptions:
* nexus-3.3.2-02 will be referred to as **$install_dir**
* sonatype-work/nexus3 will be referred to as **$data_dir**

See [https://help.sonatype.com/repomanager3/installation/directories](https://help.sonatype.com/repomanager3/installation/directories)
for reference.

#### 1. Build the plugin

Build and install the into your local maven repository using the following commands:
```
git clone https://github.com/flytreeleft/nexus3-keycloak-plugin.git
cd nexus3-keycloak-plugin
mvn clean install -Dmaven.test.skip=true
```

Note: You can download the compiled jar from the [release page](https://github.com/flytreeleft/nexus3-keycloak-plugin/releases)
directly, just choose the latest version.

#### 2. Copy all needed jars into nexus system folder

```
PLUGIN_VERSION=0.6.0-nexus3.69.0
jars="org/github/flytreeleft/nexus3-keycloak-plugin/$PLUGIN_VERSION/nexus3-keycloak-plugin-$PLUGIN_VERSION.jar"

for jar in $(echo $jars | sed 's/ /\n/g'); do
    mkdir -p $install_dir/system/$(dirname $jar)
    cp ~/.m2/repository/$jar $install_dir/system/$jar
done
```

#### 3. Add bundle to startup properties

Append the following line to *startup.properties* file found in `$install_dir/etc/karaf`.

Please replace `[PLUGIN_VERSION]` by the current plugin version.
```
mvn\:org.github.flytreeleft/nexus3-keycloak-plugin/[PLUGIN_VERSION] = 200
```

Or running the following command to do that automatically:
```
echo "mvn\\:org.github.flytreeleft/nexus3-keycloak-plugin/$PLUGIN_VERSION = 200" >> $install_dir/etc/karaf/startup.properties
```

From the plugin version `0.4.0-SNAPSHOT`, it supports to build a new bundle format
[KAR](https://help.sonatype.com/repomanager3/bundle-development/installing-bundles)
which helps us no need to specify the plugin path in `startup.properties` for Nexus v3.15+.

If you are using Nexus v3.15+ and want to use `KAR` bundle, just run
`mvn -PbuildKar clean install -Dmaven.test.skip=true`, and it will build two packages
`nexus3-keycloak-plugin-<version>-bundle.kar` and `nexus3-keycloak-plugin-<version>.jar`.
Then just put the `*.kar` into the directory `$install_dir/deploy/`, and there is no need
to change `startup.properties` anymore.

Note:
- The `*.kar` file can be downloaded from the [release page](https://github.com/flytreeleft/nexus3-keycloak-plugin/releases).
- The `*.jar` file can be used for Nexus v3.14 and earlier directly.

#### 4. Configure Keycloak realm client

Login to your Keycloak, and navigate to
"[Choose your realm] -> Clients -> [Choose the existing client or create a new client, e.g. 'nexus3']".

Activate the `Settings` tab, choose `confidential` for `Access Type`,
then enable `Service Accounts Enabled` and `Authorization Enabled`,
click `Save` button to make configuration effective.

![](./docs/images/enable-service-accounts.png)

Then Activate the `Service Account Roles` tab, choose `realm-management` for `Client Roles`,
then select `view-realm`, `view-clients` and `view-users` in `Available Roles`,
click `Add selected` button to add them to `Assigned Roles`.

![](./docs/images/choose-service-account-roles.png)

#### 5. Create keycloak.json

Create a *keycloak.json* file in `$install_dir/etc`.

Login to your Keycloak, and navigate to
"[Choose your realm] -> Clients -> nexus3 -> Installation -> [Choose 'Keycloak OIDC JSON' option]".

Copy the json content to `$install_dir/etc/keycloak.json`:
```
echo '{
  "realm": "default",
  "auth-server-url": "http://localhost:8080/auth",
  "ssl-required": "external",
  "resource": "nexus3",
  "credentials": {
    "secret": "a203feab-4bcf-4e73-b66e-05adc1da0da6"
  },
  "policy-enforcer": {}
}' > $install_dir/etc/keycloak.json
```

## Usage

#### 1. Activate Plugin

After installation you have to activate the plugin in the administration frontend.
You have to login with an administrative nexus account to do so. The default admin credentials are
* username: *admin*
* password: *admin123* (don't forget to change it!)

After login you can navigate to the realm administration.
Activate the `Keycloak Authentication Realm` plugin by dragging it to the right hand side.

![](./docs/images/enable-keycloak-auth-realm.png)

#### 2. Map Keycloak Realm Client Roles to Nexus Roles

As a last step you have to map your Keycloak realm client roles to nexus internal roles.

![](./docs/images/keycloak-realm-client-roles.png)

A good starting point is mapping one Keycloak realm client role to *nx-admin* role,
so you can start managing Nexus with your Keycloak Login.
* Choose a Keycloak realm client role (once the plugin is enabled you can check what keycloak roles are assigned to user using the Log Viewer)
* Think up a new unique name for the mapped role
* Add *nx-admin* to the contained roles

![](./docs/images/map-keycloak-client-role-to-nexus.png)

That's it. Now you can login your keycloak account.

## Development

#### 1. Start nexus with console

Move into your `$install_dir`. Edit the file `bin/nexus.vmoptions` to contain the following line
```
-Dkaraf.startLocalConsole=true
```

After that (re-)start nexus. It will then startup with an interactive console enabled.
(If the console doesn't show up, you may hit the Enter key after startup).
Your console should look like this afterwards:
```
karaf@root()>
```

#### 2. Install plugin bundle

Within the console just type
```
bundle:install -s file://[ABSOLUTE_PATH_TO_YOUR_JAR]
```

## Multiple Keycloak Realms

From the version 0.3.3, the plugin supports multiple Keycloak realms. You can map at least 4 different Keycloak realms' roles/groups to Nexus3.

If you want to enable this feature, just export the `Keycloak OIDC JSON` from the Keycloak realm to the file `$install_dir/etc/keycloak.{index}.json`,
and the `{index}` is a number which can be `0`, `1` or `2`.

Then, enable the corresponding Keycloak authentication realm in Nexus3:

![](./docs/images/enable-multi-keycloak-auth-realm.png)

Finally, you just need to map the new realm roles/groups to Nexus3 as the default one.

## Single Sign On (SSO)

From the plugin version 0.3.4, you can login Nexus3 using Keycloak as SSO (Single Sign On) provider.

Before enable SSO, you need to put a reverse gateway (Nginx, Httpd etc.) before your Nexus3 server.

Then, the reverse gateway should do authentication via Keycloak and pass the HTTP header
`X-Keycloak-Sec-Auth: <username>:<auth token>` to your Nexus3 server.

> You also can pass username and auth token via the HTTP header
> `X-Auth-Username: <username>` and `X-Auth-Token: <auth token>` from the plugin version 0.4.0.

You can check the code of [develop/nginx-gateway/vhost.d/nexus3.conf](./develop/nginx-gateway/vhost.d/nexus3.conf)
for more details.

Note, if you login as Administrator, and refresh the administration page, you will see the 'Sign in' window:

![](./docs/images/enable-keycloak-sso-will-cause-popup-nexus-sign-in-window-after-reload-page.png)

Don't worry about it, you still login, just close the window.

**Note**: After you login, you can click the logout button to logout and it will redirect to the Keycloak login page.

## Docker

You can execute command `bash docker/build.sh` to build a local Nexus image with this plugin.
It expects `target/nexus3-keycloak-plugin-0.6.0-nexus3.69.0.jar` from the build above and uses
`FROM sonatype/nexus3:3.69.0`. It does not start the container. Do not use it against a live host.
Put `keycloak.json` on a runtime mount, not in the image. `docker/run.sh` is the upstream
local-run helper and is not part of the Frontiers install path.

After the image has been built, you should [prepare](#5-create-keycloakjson) your `keycloak.json`
and put it into the directory `docker/`, then execute command `bash docker/run.sh` to create and run a Nexus3 container.
If you want to change the default port/volume mappings, just edit `docker/run.sh` before running the container.

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Contributing

[![GitHub contributors](https://img.shields.io/github/contributors/flytreeleft/nexus3-keycloak-plugin.svg)](https://github.com/flytreeleft/nexus3-keycloak-plugin/graphs/contributors)

Thanks to all contributors who helped to get this up and running.

## Thanks

* [nexus3-crowd-plugin](https://github.com/pingunaut/nexus3-crowd-plugin.git) by [@pingunaut](https://github.com/pingunaut)
* [keycloak-authz-client](https://github.com/keycloak/keycloak/tree/master/authz/client)
