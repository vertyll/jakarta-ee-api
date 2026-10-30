package com.vertyll.jakartaeeapi.common.exception.handler;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.jakartaeeapi.common.exception.BaseBusinessException;
import com.vertyll.jakartaeeapi.common.exception.HttpStatusProvider;
import com.vertyll.jakartaeeapi.common.exception.ValidationErrorProvider;
import com.vertyll.jakartaeeapi.common.problem.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseBusinessExceptionMapperTest {
    private static final String KEY = "errors.order.closed";

    @Mock
    private UriInfo uriInfo;

    @InjectMocks
    private BaseBusinessExceptionMapper mapper;

    @BeforeEach
    void aRequest() {
        when(uriInfo.getPath()).thenReturn("/orders/1");
    }

    @Test
    void aRefusalThatNamesNoStatusIsABadRequest() {
        try (Response response = mapper.toResponse(new Refused(KEY))) {
            assertThat(response.getStatus()).isEqualTo(400);
            ProblemDetail problem = (ProblemDetail) response.getEntity();
            assertThat(problem.code()).isEqualTo(KEY);
            assertThat(problem.instance()).isEqualTo("/orders/1");
        }
    }

    @Test
    void aRefusalAnswersWithTheStatusItNames() {
        try (Response response = mapper.toResponse(new Conflict(Response.Status.CONFLICT))) {
            assertThat(response.getStatus()).isEqualTo(409);
        }
    }

    @Test
    void aServerFailureKeepsItsStatus() {
        try (Response response = mapper.toResponse(new Conflict(Response.Status.SERVICE_UNAVAILABLE))) {
            assertThat(response.getStatus()).isEqualTo(503);
        }
    }

    @Test
    void theArgumentsOfTheMessageReachTheProblem() {
        try (Response response = mapper.toResponse(new Refused(KEY, "order-1"))) {
            assertThat(((ProblemDetail) response.getEntity()).args()).containsExactly("order-1");
        }
    }

    @Test
    void aRefusalWithFieldErrorsListsThem() {
        try (Response response = mapper.toResponse(new Invalid())) {
            assertThat(((ProblemDetail) response.getEntity()).errors()).containsEntry("email", List.of("errors.email"));
        }
    }

    private static class Refused extends BaseBusinessException {
        @Serial
        private static final long serialVersionUID = 1L;

        Refused(String key, Object... args) {
            super(key, args);
        }
    }

    private static final class Conflict extends Refused implements HttpStatusProvider {
        @Serial
        private static final long serialVersionUID = 1L;

        private final Response.Status status;

        Conflict(Response.Status status) {
            super(KEY);
            this.status = status;
        }

        @Override
        public Response.Status getHttpStatus() {
            return status;
        }
    }

    private static final class Invalid extends Refused implements ValidationErrorProvider {
        @Serial
        private static final long serialVersionUID = 1L;

        Invalid() {
            super(KEY);
        }

        @Override
        public Map<String, List<String>> getValidationErrors() {
            return Map.of("email", List.of("errors.email"));
        }
    }
}
