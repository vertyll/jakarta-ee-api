package com.vertyll.jakartaeeapi.common.exception.handler;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.jakartaeeapi.common.problem.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericExceptionMapperTest {
    @Mock
    private UriInfo uriInfo;

    @InjectMocks
    private GenericExceptionMapper mapper;

    @BeforeEach
    void aRequest() {
        when(uriInfo.getPath()).thenReturn("/missing");
    }

    @Test
    void anHttpErrorKeepsItsStatusAndGetsAKeyFromIt() {
        try (Response response = mapper.toResponse(new NotFoundException())) {
            assertThat(response.getStatus()).isEqualTo(404);
            assertThat(((ProblemDetail) response.getEntity()).code()).isEqualTo("errors.status.404");
        }
    }

    @Test
    void anythingElseIsAnInternalErrorThatTellsTheCallerNothing() {
        try (Response response = mapper.toResponse(new IllegalStateException("database password in here"))) {
            assertThat(response.getStatus()).isEqualTo(500);
            ProblemDetail problem = (ProblemDetail) response.getEntity();
            assertThat(problem.code()).isEqualTo("errors.unexpected");
            assertThat(problem.detail()).doesNotContain("password");
        }
    }
}
