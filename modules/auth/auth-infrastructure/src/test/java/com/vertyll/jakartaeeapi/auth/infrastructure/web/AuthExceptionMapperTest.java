package com.vertyll.jakartaeeapi.auth.infrastructure.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;

import com.vertyll.jakartaeeapi.auth.domain.AuthException;
import com.vertyll.jakartaeeapi.common.problem.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthExceptionMapperTest {
    private final AuthExceptionMapper mapper = new AuthExceptionMapper(uriInfo("/users/me"));

    @Test
    void aRefusedTokenIsUnauthorizedWithItsKey() {
        try (Response response = mapper.toResponse(AuthException.rejected(AuthException.TOKEN_INVALID))) {
            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getMediaType()).hasToString(ProblemDetail.MEDIA_TYPE);
            ProblemDetail problem = (ProblemDetail) response.getEntity();
            assertThat(problem.code()).isEqualTo(AuthException.TOKEN_INVALID);
            assertThat(problem.instance()).isEqualTo("/users/me");
        }
    }

    @Test
    void anUnreachableKeycloakIsServiceUnavailable() {
        try (Response response = mapper.toResponse(AuthException.unavailable())) {
            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(((ProblemDetail) response.getEntity()).code())
                .isEqualTo(AuthException.IDENTITY_PROVIDER_UNAVAILABLE);
        }
    }

    private static UriInfo uriInfo(String path) {
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getPath()).thenReturn(path);
        return uriInfo;
    }
}
