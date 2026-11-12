package org.github.flytreeleft.nexus3.keycloak.plugin.internal;

import org.github.flytreeleft.nexus3.keycloak.plugin.KeycloakAuthenticatingRealm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sonatype.nexus.security.user.User;
import org.sonatype.nexus.security.user.UserManager;
import org.sonatype.nexus.security.user.UserNotFoundException;
import org.sonatype.nexus.security.user.UserSearchCriteria;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KeycloakUserManagerTest {
    private KeycloakUserManager m;
    private NexusKeycloakClient mockedClient;

    @Before
    public void before() {
        this.mockedClient = mock(NexusKeycloakClient.class);
        when(this.mockedClient.getSource()).thenReturn("Keycloak");
        when(this.mockedClient.findRoleIdsByUserId(anyString())).thenReturn(Collections.<String>emptySet());
        this.m = new KeycloakUserManager(this.mockedClient);
    }

    @Test
    public void testGetAuthenticationRealmName() {
        Assert.assertEquals(KeycloakAuthenticatingRealm.NAME, m.getAuthenticationRealmName());
    }

    @Test
    public void testGetSource() {
        Assert.assertEquals("Keycloak", m.getSource());
    }

    @Test
    public void testListUsers() {
        when(this.mockedClient.findUsers()).thenReturn(mockedUsers());
        Assert.assertEquals(2, this.m.listUsers().size());
    }

    private Set<User> mockedUsers() {
        Set<User> u = new HashSet<>();
        User u1 = new User();
        User u2 = new User();
        u1.setUserId("1");
        u2.setUserId("2");
        u.add(u1);
        u.add(u2);
        return u;
    }

    @Test
    public void testListUserIds() {
        when(this.mockedClient.findAllUserIds()).thenReturn(mockedUsers().stream()
                                                                         .map(User::getUserId)
                                                                         .collect(Collectors.toSet()));
        Assert.assertEquals(2, this.m.listUserIds().size());
    }

    @Test
    public void testSearchUsers() {
        UserSearchCriteria usc = new UserSearchCriteria("1");
        when(this.mockedClient.findUserByCriteria(usc)).thenReturn(mockedUsers());
        Assert.assertEquals(2, this.m.searchUsers(usc).size());
    }

    @Test(expected = UserNotFoundException.class)
    public void testNotFoundUser() throws Exception {
        this.m.getUser("42");
    }

    @Test
    public void testGetUser() throws UserNotFoundException {
        User u = user("1");
        when(this.mockedClient.findUserByUserId("1")).thenReturn(u);
        when(this.mockedClient.findRoleIdsByUserId("1")).thenReturn(Collections.singleton("ClientRole:nexus:nx-admin"));
        User found = this.m.getUser("1");
        Assert.assertEquals("1", found.getUserId());
        Assert.assertEquals("Keycloak", found.getSource());
        Assert.assertEquals(Collections.singleton("ClientRole:nexus:nx-admin"), roleIds(found));
    }

    /**
     * Nexus 3.69 {@code DefaultSecuritySystem.findUser} calls this with a null role
     * set from {@code currentUser}. That is the call that threw AbstractMethodError
     * against the 0.4.0-SNAPSHOT bundle.
     */
    @Test
    public void testGetUserWithNullRoleIdsLoadsKeycloakRoles() throws UserNotFoundException {
        User u = user("steven.poon");
        when(this.mockedClient.findUserByUserId("steven.poon")).thenReturn(u);
        when(this.mockedClient.findRoleIdsByUserId("steven.poon")).thenReturn(Collections.singleton("RealmRole:nx-user"));

        UserManager manager = this.m;
        User found = manager.getUser("steven.poon", null);

        Assert.assertEquals("steven.poon", found.getUserId());
        Assert.assertEquals("Keycloak", found.getSource());
        Assert.assertEquals(Collections.singleton("RealmRole:nx-user"), roleIds(found));
    }

    @Test
    public void testGetUserWithKnownRoleIdsDoesNotRefetchRoles() throws UserNotFoundException {
        when(this.mockedClient.findUserByUserId("1")).thenReturn(user("1"));

        User found = this.m.getUser("1", new HashSet<String>(Arrays.asList("RealmRole:a", "ClientRole:nexus:b")));

        Assert.assertEquals(new HashSet<String>(Arrays.asList("RealmRole:a", "ClientRole:nexus:b")), roleIds(found));
        verify(this.mockedClient, never()).findRoleIdsByUserId("1");
    }

    @Test(expected = UserNotFoundException.class)
    public void testNotFoundUserWithKnownRoleIds() throws Exception {
        this.m.getUser("42", new HashSet<String>());
    }

    private static User user(String userId) {
        User user = new User();
        user.setUserId(userId);
        return user;
    }

    private static Set<String> roleIds(User user) {
        return user.getRoles().stream().map(role -> role.getRoleId()).collect(Collectors.toSet());
    }
}
