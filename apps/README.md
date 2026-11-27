# apps

Client applications (05.01 §2.1, ADR-047). Since C-017 (D-78) they are built together from increment 1,
and every story delivers its screens in every client of its role.

| Folder | Client | Roles |
| --- | --- | --- |
| `gate/` | Guard console, React 19 + Vite + TypeScript | GUARD |
| `portal/` | Owners, drivers and guests, React | OWNER, DRIVER |
| `admin/` | Administration, finance and supervision, React | ADMIN, FINANCE_ADMIN, SUPERVISOR |
| `mobile/` | Owner and driver app, React Native + Expo | OWNER, DRIVER |
| `desktop/` | Gatehouse desktop console, Electron (packages `gate/`) | GUARD |

Web screens are responsive from 360 px to 1920 px (NFR-UX-03, QAS-34). The apps are created by SCRUM-75,
SCRUM-93 and SCRUM-94.
