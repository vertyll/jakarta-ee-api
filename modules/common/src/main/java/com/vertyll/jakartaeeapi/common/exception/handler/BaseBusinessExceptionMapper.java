package com.vertyll.jakartaeeapi.common.exception.handler;

import java.util.Arrays;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import com.vertyll.jakartaeeapi.common.exception.BaseBusinessException;
import com.vertyll.jakartaeeapi.common.exception.HttpStatusProvider;
import com.vertyll.jakartaeeapi.common.exception.ValidationErrorProvider;
import com.vertyll.jakartaeeapi.common.problem.ProblemDetail;
import com.vertyll.jakartaeeapi.common.problem.Problems;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class BaseBusinessExceptionMapper implements ExceptionMapper<BaseBusinessException> {

    @Context
    private UriInfo uriInfo;

    @Override
    public Response toResponse(BaseBusinessException exception) {
        String path = uriInfo.getPath();
        Response.Status status = exception instanceof HttpStatusProvider provider ? provider.getHttpStatus()
                : Response.Status.BAD_REQUEST;

        if (status.getFamily() == Response.Status.Family.SERVER_ERROR) {
            log.error("Server error {} at path {}", exception.getMessageKey(), path, exception);
        } else {
            log.warn("Business exception {} at path {}", exception.getMessageKey(), path);
        }

        ProblemDetail problem = Problems.of(status, exception.getMessageKey(), path)
            .withCode(exception.getMessageKey(), Arrays.asList(exception.getArgs()));
        if (exception instanceof ValidationErrorProvider provider) {
            problem = problem.withErrors(provider.getValidationErrors());
        }
        return Problems.response(problem);
    }
}
