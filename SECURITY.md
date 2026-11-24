# Security Policy

## Supported versions

Only the latest release of CXP Exporter receives security fixes. Fixes are published as a new release.

## Reporting a vulnerability

Do not open a public issue for security problems. Report them privately through [GitHub Security Advisories](https://github.com/sameaslooks/android-cxp-exporter/security/advisories).

Please include:

- the app version and the Android and Google Play services versions;
- steps to reproduce the problem;
- the impact you expect, and whether exported files or memory contents are affected.

## Scope

In scope:

- the CXP Exporter app code, including CXF parsing and the KDBX, CXF JSON and CSV export files it writes;
- the build and release workflows in this repository.

Out of scope (report these to the respective project):

- Google Password Manager, Credential Manager and Google Play services;
- KeePassXC and KeePassDX;
- problems caused by a compromised device or by files that were shared or stored insecurely after export.
