package com.vertyll.jakartaeeapi.common.problem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailTest {
    @Test
    void aProblemStartsFromItsStatusAndOwnsNoCode() {
        ProblemDetail problem = Problems.of(Response.Status.NOT_FOUND, "errors.missing", "/users/1");

        assertThat(problem.type()).isEqualTo("about:blank");
        assertThat(problem.status()).isEqualTo(404);
        assertThat(problem.title()).isEqualTo("Not Found");
        assertThat(problem.detail()).isEqualTo("errors.missing");
        assertThat(problem.instance()).isEqualTo("/users/1");
        assertThat(problem.code()).isNull();
        assertThat(problem.errors()).isNull();
    }

    @Test
    void aCodeWithoutArgumentsLeavesTheArgumentsOut() {
        ProblemDetail problem = ProblemDetail.of(400, "Bad Request", "detail", "/").withCode("errors.bad", List.of());

        assertThat(problem.code()).isEqualTo("errors.bad");
        assertThat(problem.args()).isNull();
    }

    @Test
    void aCodeKeepsItsArgumentsAndWhatTheProblemHadBefore() {
        ProblemDetail problem = ProblemDetail.of(400, "Bad Request", "detail", "/")
            .withErrors(Map.of("email", List.of("errors.email")))
            .withCode("errors.bad", List.of("value"));

        assertThat(problem.args()).containsExactly("value");
        assertThat(problem.errors()).containsOnlyKeys("email");
    }

    @Test
    void aLaterChangeToTheCallersListDoesNotReachTheProblem() {
        List<Object> arguments = new ArrayList<>(List.of("value"));
        ProblemDetail problem = ProblemDetail.of(400, "Bad Request", "detail", "/").withCode("errors.bad", arguments);

        arguments.add("later");

        assertThat(problem.args()).containsExactly("value");
    }

    @Test
    void theResponseCarriesTheProblemAsProblemJson() {
        ProblemDetail problem = ProblemDetail.of(409, "Conflict", "detail", "/");

        try (Response response = Problems.response(problem)) {
            assertThat(response.getStatus()).isEqualTo(409);
            assertThat(response.getMediaType()).hasToString(ProblemDetail.MEDIA_TYPE);
            assertThat(response.getEntity()).isEqualTo(problem);
        }
    }
}
