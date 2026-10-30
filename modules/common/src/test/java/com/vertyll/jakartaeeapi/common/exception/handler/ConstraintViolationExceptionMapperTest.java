package com.vertyll.jakartaeeapi.common.exception.handler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vertyll.jakartaeeapi.common.problem.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConstraintViolationExceptionMapperTest {
    @Mock
    private UriInfo uriInfo;

    @InjectMocks
    private ConstraintViolationExceptionMapper mapper;

    @Test
    void aViolationIsReportedUnderTheLastPartOfItsPath() {
        ProblemDetail problem = problemOf(Set.of(violation("register.command.email", "{errors.email}")));

        assertThat(problem.status()).isEqualTo(400);
        assertThat(problem.code()).isEqualTo("errors.validation.failed");
        assertThat(problem.errors()).containsEntry("email", List.of("errors.email"));
    }

    @Test
    void aFieldWithoutAPathKeepsItsName() {
        ProblemDetail problem = problemOf(Set.of(violation("email", "{errors.email}")));

        assertThat(problem.errors()).containsOnlyKeys("email");
    }

    @Test
    void aMessageThatIsNoTemplateStaysAsWritten() {
        ProblemDetail problem = problemOf(Set.of(violation("email", "must be an address")));

        assertThat(problem.errors()).containsEntry("email", List.of("must be an address"));
    }

    @Test
    void everyViolationOfOneFieldIsKept() {
        Set<ConstraintViolation<?>> violations = new LinkedHashSet<>();
        violations.add(violation("email", "{errors.email}"));
        violations.add(violation("email", "{errors.tooLong}"));

        assertThat(problemOf(violations).errors().get("email"))
            .containsExactlyInAnyOrder("errors.email", "errors.tooLong");
    }

    private ProblemDetail problemOf(Set<ConstraintViolation<?>> violations) {
        when(uriInfo.getPath()).thenReturn("/users");
        try (Response response = mapper.toResponse(new ConstraintViolationException(violations))) {
            return (ProblemDetail) response.getEntity();
        }
    }

    private static ConstraintViolation<?> violation(String path, String template) {
        Path propertyPath = mock(Path.class);
        when(propertyPath.toString()).thenReturn(path);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(propertyPath);
        when(violation.getMessageTemplate()).thenReturn(template);
        return violation;
    }
}
