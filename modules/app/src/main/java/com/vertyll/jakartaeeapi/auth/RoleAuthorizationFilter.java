package com.vertyll.jakartaeeapi.auth;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import org.jspecify.annotations.Nullable;

import com.vertyll.jakartaeeapi.common.problem.Problems;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class RoleAuthorizationFilter implements ContainerRequestFilter {
    private static final String AUTHENTICATION_REQUIRED = "errors.auth.authenticationRequired";
    private static final String ACCESS_DENIED = "errors.auth.accessDenied";

    @Context
    private ResourceInfo resource;

    @Override
    public void filter(ContainerRequestContext context) {
        Method method = resource.getResourceMethod();
        Class<?> type = resource.getResourceClass();
        if (method == null || type == null) {
            return;
        }
        Refusal refusal = refusal(rule(method, type), context.getSecurityContext());
        if (refusal != null) {
            context.abortWith(
                Problems.response(
                    Problems.of(refusal.status(), refusal.messageKey(), context.getUriInfo().getPath())
                        .withCode(refusal.messageKey(), List.of())
                )
            );
        }
    }

    static @Nullable Refusal refusal(Rule rule, @Nullable SecurityContext security) {
        boolean signedIn = security != null && security.getUserPrincipal() != null;
        return switch (rule.kind()) {
            case PERMIT_ALL -> null;
            case DENY_ALL -> new Refusal(Response.Status.FORBIDDEN, ACCESS_DENIED);
            case AUTHENTICATED -> signedIn ? null : new Refusal(Response.Status.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
            case ROLES -> rolesRefusal(rule, security, signedIn);
        };
    }

    private static @Nullable Refusal rolesRefusal(Rule rule, @Nullable SecurityContext security, boolean signedIn) {
        if (security == null || !signedIn) {
            return new Refusal(Response.Status.UNAUTHORIZED, AUTHENTICATION_REQUIRED);
        }
        return rule.roles().stream().anyMatch(security::isUserInRole) ? null
                : new Refusal(Response.Status.FORBIDDEN, ACCESS_DENIED);
    }

    static Rule rule(Method method, Class<?> type) {
        Rule onMethod = ruleOf(method);
        if (onMethod != null) {
            return onMethod;
        }
        Rule onType = ruleOf(type);
        return onType != null ? onType : new Rule(Kind.AUTHENTICATED, List.of());
    }

    private static @Nullable Rule ruleOf(AnnotatedElement element) {
        if (element.isAnnotationPresent(DenyAll.class)) {
            return new Rule(Kind.DENY_ALL, List.of());
        }
        RolesAllowed roles = element.getAnnotation(RolesAllowed.class);
        if (roles != null) {
            return new Rule(Kind.ROLES, List.of(roles.value()));
        }
        if (element.isAnnotationPresent(PermitAll.class)) {
            return new Rule(Kind.PERMIT_ALL, List.of());
        }
        return null;
    }

    enum Kind {
        PERMIT_ALL,
        DENY_ALL,
        ROLES,
        AUTHENTICATED
    }

    record Refusal(Response.Status status, String messageKey) {
    }

    record Rule(Kind kind, List<String> roles) {
        Rule {
            roles = List.copyOf(roles);
        }
    }
}
