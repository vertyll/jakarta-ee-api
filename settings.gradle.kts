rootProject.name = "jakarta-ee-api"

include(
    "modules:common",
    "modules:session-store",
    "modules:auth:auth-api",
    "modules:auth:auth-domain",
    "modules:auth:auth-application",
    "modules:auth:auth-infrastructure",
    "modules:user:user-domain",
    "modules:user:user-application",
    "modules:user:user-infrastructure",
    "modules:app"
)
