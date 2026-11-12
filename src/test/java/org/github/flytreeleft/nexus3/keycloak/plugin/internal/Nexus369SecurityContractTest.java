package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth0.KeycloakAuthenticatingRealm;
import org.junit.Assert;
import org.junit.Test;
import org.sonatype.nexus.security.authz.AuthorizationManager;
import org.sonatype.nexus.security.privilege.NoSuchPrivilegeException;
import org.sonatype.nexus.security.user.UserManager;

/**
 * Fails the build when a Nexus 3.69 security interface method is left abstract on a
 * plugin class. That is the linkage error behind {@code AbstractMethodError} in
 * {@code DefaultSecuritySystem.findUser}.
 */
public class Nexus369SecurityContractTest {
    private static final List<Class<?>> USER_MANAGERS = Arrays.<Class<?>>asList(
            KeycloakUserManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth0.KeycloakUserManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth1.KeycloakUserManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth2.KeycloakUserManager.class
    );

    private static final List<Class<?>> AUTHORIZATION_MANAGERS = Arrays.<Class<?>>asList(
            KeycloakAuthorizationManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth0.KeycloakAuthorizationManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth1.KeycloakAuthorizationManager.class,
            org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth2.KeycloakAuthorizationManager.class
    );

    @Test
    public void userManagersImplementEvery369Method() {
        Assert.assertNotNull(UserManager.class.getResource("UserManager.class"));
        for (Class<?> implementation : USER_MANAGERS) {
            assertImplements(UserManager.class, implementation);
        }
    }

    @Test
    public void authorizationManagersImplementEvery369Method() {
        for (Class<?> implementation : AUTHORIZATION_MANAGERS) {
            assertImplements(AuthorizationManager.class, implementation);
        }
    }

    @Test
    public void keycloakDoesNotExposePrivileges() {
        KeycloakAuthorizationManager manager = new KeycloakAuthorizationManager(client());
        try {
            manager.getPrivilegeByName("nx-admin");
            Assert.fail("expected NoSuchPrivilegeException");
        } catch (NoSuchPrivilegeException expected) {
            Assert.assertNotNull(expected);
        }
        Assert.assertEquals(Collections.emptyList(), manager.getPrivileges(Collections.singleton("nx-admin")));
        Assert.assertTrue(manager.listPrivileges().isEmpty());
    }

    @Test
    public void realmsStayConcrete() {
        Assert.assertFalse(Modifier.isAbstract(KeycloakAuthenticatingRealm.class.getModifiers()));
        Assert.assertFalse(Modifier.isAbstract(
                org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth1.KeycloakAuthenticatingRealm.class.getModifiers()));
        Assert.assertFalse(Modifier.isAbstract(
                org.github.flytreeleft.nexus3.keycloak.plugin.ext.auth2.KeycloakAuthenticatingRealm.class.getModifiers()));
    }

    private static NexusKeycloakClient client() {
        return new NexusKeycloakClient("Keycloak");
    }

    private static void assertImplements(Class<?> api, Class<?> implementation) {
        Assert.assertFalse(implementation.getName() + " is abstract",
                           Modifier.isAbstract(implementation.getModifiers()));
        for (Method method : api.getMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) {
                continue;
            }
            Method implemented;
            try {
                implemented = implementation.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                Assert.fail(implementation.getName() + " does not implement " + method);
                return;
            }
            Assert.assertFalse(implementation.getName() + " leaves abstract " + method,
                               Modifier.isAbstract(implemented.getModifiers()));
        }
    }
}
