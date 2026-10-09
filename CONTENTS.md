# Contents

Every document in this repository, the module it belongs to, and what it covers. Terms are defined in
[GLOSSARY.md](GLOSSARY.md), and the specifications behind them are in [STANDARDS.md](STANDARDS.md).

## Start here

| Document                    | Module | Kind              | Covers                                                         |
|-----------------------------|--------|-------------------|----------------------------------------------------------------|
| [jakarta-ee-api](README.md) | —      | repository README | What the repository is, its stack and where to start.          |
| [Glossary](GLOSSARY.md)     | —      | reference         | Every term the docs use, and where it is explained.            |
| [Standards](STANDARDS.md)   | —      | reference         | The RFCs and specifications the code implements or depends on. |

## Overview

| Document                                       | Module | Kind     | Covers                                                       |
|------------------------------------------------|--------|----------|--------------------------------------------------------------|
| [Development Setup](docs/development-setup.md) | —      | overview | Running the infrastructure, Liberty dev mode and the checks. |
| [Architecture](docs/architecture.md)           | —      | overview | Modules, routes, access rules and accounts.                  |
| [Authentication](docs/authentication.md)       | —      | overview | Sign-in, token authorization and sign-out.                   |

## Mechanisms

| Document                                                      | Module | Kind      | Covers                                                                                      |
|---------------------------------------------------------------|--------|-----------|---------------------------------------------------------------------------------------------|
| [Cross-site requests](docs/mechanisms/cross-site-requests.md) | —      | mechanism | Why a forged request from another site cannot act with the user's session.                  |
| [Error responses](docs/mechanisms/error-responses.md)         | —      | mechanism | What the API answers when it refuses a request, and how a client turns that into text.      |
| [Token refresh](docs/mechanisms/token-refresh.md)             | —      | mechanism | How the session keeps a valid access token without signing the user out when requests race. |
