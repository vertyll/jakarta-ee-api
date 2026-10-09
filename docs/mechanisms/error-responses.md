# Error responses

What the API answers when it refuses a request, and how a client turns that into text.

Every refusal is an RFC 9457 problem document (`application/problem+json`, `common/problem`), and none carries a
sentence a person reads:

| Field                                 | Holds                                                                |
|---------------------------------------|----------------------------------------------------------------------|
| `type`, `title`, `status`, `instance` | `about:blank`, the status's reason phrase, the HTTP status, the path |
| `code`                                | a message key, e.g. `errors.auth.accessDenied`                       |
| `detail`                              | the same key                                                         |
| `args`                                | the arguments of that key, in order                                  |
| `errors`                              | in a validation error, the message keys of each invalid field        |

| Refusal                       | `code`                                                                   |
|-------------------------------|--------------------------------------------------------------------------|
| a business exception          | the key of the `BaseBusinessException` subclass                          |
| no caller, or the wrong roles | `errors.auth.authenticationRequired`, `errors.auth.accessDenied`         |
| a bean validation failure     | `errors.validation.failed`; each field gets its constraint's message key |
| an error JAX-RS raises itself | `errors.status.{status}`                                                 |
| anything unexpected           | `errors.unexpected`, without internals                                   |

The server keeps no translation catalog: the client maps `code` and `args` to text in its own language.
