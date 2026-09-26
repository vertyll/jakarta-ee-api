package com.vertyll.jakartaeeapi.common.problem;

import jakarta.ws.rs.core.Response;

public final class Problems {

    private Problems() {
    }

    public static ProblemDetail of(Response.StatusType status, String detail, String instance) {
        return ProblemDetail.of(status.getStatusCode(), status.getReasonPhrase(), detail, instance);
    }

    public static Response response(ProblemDetail problem) {
        return Response.status(problem.status()).type(ProblemDetail.MEDIA_TYPE).entity(problem).build();
    }
}
