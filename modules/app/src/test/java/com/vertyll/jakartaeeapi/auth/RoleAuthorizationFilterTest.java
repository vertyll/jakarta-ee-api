package com.vertyll.jakartaeeapi.auth;

import java.lang.reflect.Method;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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

    private static RoleAuthorizationFilter.Rule rule(Class<?> type, String name) throws NoSuchMethodException {
        Method method = type.getDeclaredMethod(name);
        return RoleAuthorizationFilter.rule(method, type);
    }

    static class Secured {
        void plain() {
        }
    }

    @PermitAll
    static class Public {
        void open() {
        }

        @RolesAllowed("ADMIN")
        void adminOnly() {
        }
    }
}
