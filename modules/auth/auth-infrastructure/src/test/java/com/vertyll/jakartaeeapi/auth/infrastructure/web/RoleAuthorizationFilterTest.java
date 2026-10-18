package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vertyll.jakartaeeapi.auth.contracts.SignedInPerson;
import com.vertyll.jakartaeeapi.auth.contracts.SignedInPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoleAuthorizationFilterTest {
    @Test
    void aMethodWithoutRulesRequiresSigningIn() throws NoSuchMethodException {
        assertThat(rule(Secured.class, "plain").kind()).isEqualTo(RoleAuthorizationFilter.Kind.AUTHENTICATED);
    }

    @Test
    void theMethodRuleWinsOverTheClassRule() throws NoSuchMethodException {
        RoleAuthorizationFilter.Rule rule = rule(Public.class, "adminOnly");

        assertThat(rule.kind()).isEqualTo(RoleAuthorizationFilter.Kind.ROLES);
        assertThat(rule.roles()).containsExactly("ADMIN");
    }

    @Test
    void theClassRuleAppliesToItsMethods() throws NoSuchMethodException {
        assertThat(rule(Public.class, "open").kind()).isEqualTo(RoleAuthorizationFilter.Kind.PERMIT_ALL);
    }

    @Test
    void aRuleRefusesWhoItDoesNotLetIn() {
        SecurityContext admin = caller("ADMIN");
        RoleAuthorizationFilter.Rule admins =
                new RoleAuthorizationFilter.Rule(RoleAuthorizationFilter.Kind.ROLES, List.of("ADMIN"));
        RoleAuthorizationFilter.Rule anyone =
                new RoleAuthorizationFilter.Rule(RoleAuthorizationFilter.Kind.PERMIT_ALL, List.of());
        RoleAuthorizationFilter.Rule nobody =
                new RoleAuthorizationFilter.Rule(RoleAuthorizationFilter.Kind.DENY_ALL, List.of());
        RoleAuthorizationFilter.Rule signedIn =
                new RoleAuthorizationFilter.Rule(RoleAuthorizationFilter.Kind.AUTHENTICATED, List.of());

        assertThat(RoleAuthorizationFilter.refusal(anyone, null)).isEmpty();
        assertThat(RoleAuthorizationFilter.refusal(nobody, admin)).map(RoleAuthorizationFilter.Refusal::status)
            .contains(Response.Status.FORBIDDEN);
        assertThat(RoleAuthorizationFilter.refusal(signedIn, null)).map(RoleAuthorizationFilter.Refusal::status)
            .contains(Response.Status.UNAUTHORIZED);
        assertThat(RoleAuthorizationFilter.refusal(signedIn, admin)).isEmpty();
        assertThat(RoleAuthorizationFilter.refusal(admins, null)).map(RoleAuthorizationFilter.Refusal::status)
            .contains(Response.Status.UNAUTHORIZED);
        assertThat(RoleAuthorizationFilter.refusal(admins, caller("USER"))).map(RoleAuthorizationFilter.Refusal::status)
            .contains(Response.Status.FORBIDDEN);
        assertThat(RoleAuthorizationFilter.refusal(admins, admin)).isEmpty();
    }

    @Test
    void aRefusedRequestIsAbortedWithAProblem() throws NoSuchMethodException {
        ResourceInfo resource = mock(ResourceInfo.class);
        when(resource.getResourceMethod()).thenReturn(Secured.class.getDeclaredMethod("plain"));
        doReturn(Secured.class).when(resource).getResourceClass();
        RoleAuthorizationFilter filter = new RoleAuthorizationFilter(resource);
        ContainerRequestContext context = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn("/users/me");
        when(context.getUriInfo()).thenReturn(uriInfo);

        filter.filter(context);

        ArgumentCaptor<Response> response = ArgumentCaptor.forClass(Response.class);
        verify(context).abortWith(response.capture());
        try (Response refusal = response.getValue()) {
            assertThat(refusal.getStatus()).isEqualTo(401);
        }
    }

    private static SecurityContext caller(String role) {
        return new CallerSecurityContext(
            new SignedInPrincipal(new SignedInPerson("subject", "ada@example.test", "ada", List.of(role))),
            true
        );
    }

    private static RoleAuthorizationFilter.Rule rule(Class<?> type, String name) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(name);
        return RoleAuthorizationFilter.rule(method, type);
    }

    static class Secured {
        void plain() {
            throw new UnsupportedOperationException();
        }
    }

    @PermitAll
    static class Public {
        void open() {
            throw new UnsupportedOperationException();
        }

        @RolesAllowed("ADMIN")
        void adminOnly() {
            throw new UnsupportedOperationException();
        }
    }
}
