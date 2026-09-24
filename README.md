# Valora

**Valora** is a decision-support application for comparing supplier quotations by their
**effective acquisition cost**: the purchase price adjusted by the tax credits that apply to each
operation, according to rules configured by the administrator.

It runs as a local desktop application on Windows and Linux, and it can also run as a web
application during development. Everything runs on the user's machine, with no external services.

> **Version:** 0.1.1

---

## Overview

When comparing suppliers, the lowest price on a quotation is not always the best option. Depending
on the supplier, the product and the operation, the buyer may be entitled to tax credits that
change the real cost of the purchase.

Valora structures this analysis:

- **Supplier comparison:** register suppliers and products, and collect quotation options from
  several suppliers for the same product.
- **Effective acquisition cost:** for each option, Valora calculates the effective cost by applying
  the tax rules and calculation parameters currently configured, treating each tax credit
  individually.
- **Quotation management:** quotations keep their options and a history of comparisons. A new
  comparison can be run at any time with the current configuration.
- **Configurable rules and parameters:** tax rules and calculation parameters are stored in the
  database and managed by an administrator from within the application. Changing a rule does not
  require changing the code, and a new installation starts with **no preconfigured tax rules**.
  When a required rule or parameter is missing, the calculation is reported as incomplete instead
  of assuming a value.
- **Local/offline operation:** the desktop installers bundle the application, the backend, a Java
  runtime and a PostgreSQL database, so everything runs locally.

Valora is not an accounting, tax or ERP system, and it is not intended to replace one.

---

## Key Features

- **Initial setup and authentication:** on first launch, the first administrator account is created
  through the application; there is no default user or password. Sessions use JWT.
- **Roles:**
  - **ADMIN:** manages users, suppliers, products, NF-e imports, tax rules and calculation
    parameters.
  - **USER:** views the registered data, creates quotations, runs comparisons and downloads
    results.
- **User management:** administrators create additional users (USER or ADMIN).
- **Suppliers and products:** create, edit and deactivate.
- **NF-e XML import:** manual upload of Brazilian electronic invoice (NF-e) XML files. The backend
  parses the document and stores the invoice and item data (issuer, products, quantities, values
  and tax information), which can then be used as reference data in quotations. Automatic retrieval
  from tax authority services is **not** part of this release.
- **Tax rules:** create, edit and deactivate rules. A rule can be restricted by supplier type,
  supplier, product, origin and destination state, merchandise origin and CFOP (operation code). A
  dedicated view shows which rules apply to a given operation.
- **Calculation parameters:** general parameters used by the calculation, managed by the
  administrator.
- **Quotations and comparison:** create a quotation for a product, add supplier options (optionally
  based on an imported NF-e item) and compare them. Options are ranked by effective cost, and each
  comparison stores the full calculation, including the rules, rule versions and parameters used.
- **Export:** download a comparison as a CSV file.

The user interface is currently available in Brazilian Portuguese.

---

## Architecture

```text
Desktop app (Tauri + Rust)  or  web browser
                 │
                 ▼
      Frontend (React + TypeScript)
                 │  REST API (JSON, JWT)
                 ▼
      Backend (Java 21 + Spring Boot)
                 │  JPA / Flyway
                 ▼
            PostgreSQL 17
```

- **Backend:** Java 21 with Spring Boot (Web MVC, Security with a JWT resource server, Data JPA,
  Validation), built with Maven. All business rules and calculations run in the backend.
  **Flyway** manages the database schema.
- **Database:** PostgreSQL 17.
- **Frontend:** React 19 with TypeScript, built with Vite, using Mantine (UI components), React Router
  and TanStack Query. The frontend presents the data and results provided by the API and does not
  implement calculations.
- **Desktop shell:** Tauri 2 (Rust). In the packaged application, the Tauri process starts and stops
  a bundled PostgreSQL server (`127.0.0.1:54329`) and the backend (`127.0.0.1:8080`), and opens the
  same React frontend in a native window. Database passwords are generated randomly for each
  installation.

More details are available in `docs/ARQUITETURA.md` (in Portuguese).

---

## Technology Stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring Security (JWT), Spring Data JPA, Bean Validation, Flyway, Maven |
| Database | PostgreSQL 17 (Docker Compose for development; a portable server is bundled in the desktop installers) |
| Frontend | React 19, TypeScript, Vite, Mantine, React Router, TanStack Query |
| Desktop | Tauri 2, Rust |
| Testing | JUnit and Spring Boot Test, Vitest, React Testing Library, Playwright |

---

## Requirements

### End-user requirements

- **Windows:** Windows x64 (validated on Windows 11). The installer bundles everything needed to run
  Valora: the desktop application, the backend, a Java 21 runtime and a PostgreSQL database. You do
  **not** need to install Java, PostgreSQL, Maven, Node.js, npm, Docker, WSL or Git. Valora uses
  Microsoft WebView2, which ships with Windows 11. If WebView2 is missing, the installer is
  configured to download it from Microsoft, which requires an internet connection during
  installation.
- **Linux:** a Debian/Ubuntu-based x64 (`amd64`) distribution with glibc 2.39 or newer (for example,
  Ubuntu 24.04 or later). The package depends on `libwebkit2gtk-4.1-0` and `libgtk-3-0`, which the
  package manager provides. Java and PostgreSQL are bundled in the package.
- Local ports `8080` and `54329` must be free (both are bound to `127.0.0.1` only).

### Development requirements

- Java 21 (JDK)
- Maven
- Node.js 20.19 or later, with npm
- Docker and Docker Compose (for the development PostgreSQL)
- Git
- For the desktop application: Rust (stable, via [rustup](https://rustup.rs)) and the Tauri system
  dependencies. On Ubuntu or WSL2, install
  `build-essential pkgconf libwebkit2gtk-4.1-dev libxdo-dev libssl-dev`.
- For building the installers, see [Building the installers](#building-the-installers).

---

## Installation

The installers are published on the [GitHub Releases](https://github.com/usrafaelmendes/valora/releases)
page.

### Windows

File: `Valora_0.1.1_x64-setup.exe`

1. Open the [Valora v0.1.1 release page](https://github.com/usrafaelmendes/valora/releases/tag/v0.1.1).
2. Download `Valora_0.1.1_x64-setup.exe`.
3. Run the installer. It installs for the current user only, under
   `%LOCALAPPDATA%\Programs\Valora`, and does not require administrator privileges.
4. Complete the installation.
5. Open **Valora** from the Start menu. The first launch takes a few seconds while the local
   database is created.
6. Complete the initial setup by creating the first administrator account (name, e-mail and
   password).
7. Log in with that account.

Notes:

- The installer is not digitally signed yet, so Windows SmartScreen may show a warning. If it does,
  select **More info → Run anyway**.
- Application data (database, configuration and logs) is stored in `%LOCALAPPDATA%\Valora`,
  separately from the program folder.
- **Uninstalling:** use **Settings → Apps → Installed apps → Valora → Uninstall**. Uninstalling
  removes the program but **keeps** `%LOCALAPPDATA%\Valora`, and a later installation reuses it. To
  permanently delete the data, remove that folder manually after uninstalling.

### Linux

File: `Valora_0.1.1_amd64.deb`, intended for compatible Debian/Ubuntu systems.

1. Download `Valora_0.1.1_amd64.deb` from the
   [Valora v0.1.1 release page](https://github.com/usrafaelmendes/valora/releases/tag/v0.1.1).
2. Install it from the download directory:

   ```bash
   sudo dpkg -i Valora_0.1.1_amd64.deb
   ```

   If `dpkg` reports missing dependencies, resolve them with:

   ```bash
   sudo apt-get install -f
   ```

3. Open **Valora** from the applications menu, or run `valora` in a terminal.
4. Complete the initial setup by creating the first administrator account, then log in.

The package installs the application under `/usr/lib/Valora` and the `/usr/bin/valora` command. User
data is stored in `~/.local/share/Valora`.

To remove the package:

```bash
sudo dpkg -r valora
```

Removing the package does **not** delete the user data in `~/.local/share/Valora`. To delete it
permanently, remove that folder manually.

> **Validation note:** the Linux package of this release was validated on **WSL (Ubuntu 26.04)**,
> not on a native Linux installation. Other distributions and package formats (such as AppImage)
> are not supported yet.

---

## Usage

1. **Initial setup:** on first launch, create the first administrator account.
2. **Log in** with your e-mail and password.
3. **Users (ADMIN):** create additional users with the USER or ADMIN role.
4. **Suppliers (ADMIN):** register suppliers with their tax ID (CNPJ), state and supplier type
   (manufacturer or wholesaler).
5. **Products (ADMIN):** register the products you want to quote.
6. **NF-e import (ADMIN, optional):** upload NF-e XML files to use invoice items as reference data
   for quotation options.
7. **Tax rules and calculation parameters (ADMIN):** configure the rules and parameters that apply
   to your operation. Until they are configured, calculations are reported as incomplete.
8. **Quotations:** create a quotation for a product and add the supplier options, with payment terms
   and notes when applicable.
9. **Comparison:** run the comparison. Options are ranked by effective acquisition cost, showing the
   price, each tax credit and the rules used for each option. You can run a new comparison later,
   and previous comparisons remain in the history.
10. **Export:** download the comparison as a CSV file.

---

## Development

### Clone and configure

```bash
git clone https://github.com/usrafaelmendes/valora.git
cd valora
cp .env.example .env
```

Edit `.env` with your local values and never commit it:

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: credentials of the development database,
  used by Docker Compose and by the backend;
- `JWT_SECRET`: at least 32 characters (for example, generated with `openssl rand -base64 48`);
- `JWT_EXPIRACAO`: token lifetime (default `8h`).

No administrator credentials are configured in `.env`. The first administrator is created through
the application.

### Start PostgreSQL

```bash
docker compose up -d
docker compose ps
```

### Start the backend

```bash
cd backend
mvn spring-boot:run        # http://localhost:8080
```

The backend reads the root `.env` file, and Flyway creates or updates the schema on startup.

### Start the frontend (web)

```bash
cd frontend
npm install
npm run dev                # http://localhost:5173
```

The Vite development server forwards `/api/*` requests to the backend.

### Desktop application (development)

```bash
cd frontend
npm run tauri dev          # starts Vite in desktop mode and opens the Valora window
```

In development, start PostgreSQL and the backend separately, as described above. Tauri starts them
only in the packaged application.

### Building the installers

The installers are built from Linux or WSL:

```bash
cd frontend
npm run runtime:windows    # assembles src-tauri/runtime/windows-x86_64 (Java runtime, backend, PostgreSQL)
npm run empacotar:windows  # builds Valora_<version>_x64-setup.exe (NSIS, cross-compiled)
npm run runtime:linux      # assembles src-tauri/runtime/linux-x86_64
npm run empacotar:linux    # builds Valora_<version>_amd64.deb
```

In addition to the development requirements, the build machine needs `python3`, `curl`, Docker (to
extract the Linux PostgreSQL binaries) and, for Windows, `cargo-xwin` with the
`nsis lld llvm clang` packages. The runtime downloads are pinned and verified by SHA-256 in
`frontend/src-tauri/scripts/preparar-runtime.sh`. The generated runtimes and installers are never
committed.

---

## Project Structure

```text
valora/
├── backend/                  # Spring Boot application: API, business rules, calculation, Flyway migrations
├── frontend/                 # React + TypeScript application
│   ├── e2e/                  # Playwright end-to-end tests
│   ├── src/                  # Pages, components and API client
│   └── src-tauri/            # Tauri desktop shell (Rust), packaging configuration and scripts
├── docs/                     # Project documentation (in Portuguese)
│   ├── ARQUITETURA.md        # Architecture and technical decisions
│   ├── CASOS_DE_TESTE.md     # Test cases
│   ├── REGRAS_TRIBUTARIAS.md # Calculation model and configurable tax rules
│   └── REQUISITOS.md         # Functional and non-functional requirements
├── test-data/
│   └── nfe/                  # Local folder for NF-e XML samples (contents are not versioned)
├── .env.example              # Template for the local development configuration
├── compose.yaml              # PostgreSQL for development
└── README.md
```

---

## Testing

```bash
# Backend: unit, controller and persistence tests
cd backend && mvn test

# Frontend
cd frontend
npm test                   # unit and component tests (Vitest)
npm run typecheck
npm run lint
npm run format:check

# End-to-end: PostgreSQL + backend + frontend (Playwright)
npx playwright install chromium   # first time only
npm run e2e
```

The backend concurrency test for the initial setup uses a temporary database on the development
PostgreSQL. `npm run e2e` creates a temporary database, generates a random JWT secret and
administrator password for the run, uses only fictitious data and drops the database at the end.
Ports `8080` and `5173` must be free.

Results for version 0.1.1:

| Suite | Result |
|---|---|
| Backend tests | 361/361 passed |
| Frontend tests | 211/211 passed |
| End-to-end tests | 17/17 passed |
| Typecheck | passed |
| Lint | passed |
| Format check | passed |

The 0.1.0 installers were tested for installation, first launch, initial setup, login, closing and
reopening, data persistence, recovery after a forced shutdown and uninstallation on Windows 11 x64.
The same checks, including package removal, were run for the `.deb` package on WSL (Ubuntu 26.04).
Version 0.1.1 adds the Windows taskbar icon fix, which was verified on Windows 11.

---

## Releases

Official installers are distributed through
[GitHub Releases](https://github.com/usrafaelmendes/valora/releases). Each release provides:

- a Windows installer (`.exe`);
- a Debian/Ubuntu package (`.deb`).

Installers and other build outputs are not stored in the Git repository.

---

## Security and Privacy

- Data imported into Valora, such as invoices, supplier information and prices, may be commercially
  sensitive. It is stored only in the local database on the user's machine.
- Never commit real NF-e XML files or other real business documents to the repository. Use
  fictitious data for tests and examples.
- Never commit credentials. Local configuration belongs in `.env`, which is ignored by Git.
- The desktop application generates its own random database passwords and JWT secret, and its
  services accept connections from the local machine only.

---

## Contributing

Issues and pull requests are welcome. Before submitting a change:

- keep business rules and calculations in the backend;
- do not hard-code tax rules, since they are configured in the application;
- add or update tests, and make sure all test suites and the typecheck, lint and format checks pass;
- do not include real invoices, credentials or other sensitive data.

---

## License

No license has been defined yet. Until one is published, all rights are reserved.
