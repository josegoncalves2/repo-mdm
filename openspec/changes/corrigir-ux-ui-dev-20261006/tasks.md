## 1. UX/UI Review and Scope

- [x] 1.1 Review all supplied screenshots and DOM evidence for Messages, Files, GPS, Remote Access, Reports, and Detailed Information; record usability findings and acceptance criteria in the design.
- [x] 1.2 Confirm Files is the only screen in scope with Cards/List controls and explicitly exclude APK/profile mutations from this change.

## 2. Screen Corrections

- [x] 2.1 Verify Messages keeps the composer visible to authorized viewers before device selection, preserves send permission checks, and displays primitive-only device labels and load errors in the implementation.
- [x] 2.2 Verify Files preserves search/data across Cards/List modes and localizes the subtitle and both accessible view controls; keep the selector exclusive to Files.
- [x] 2.3 Correct and verify GPS page controls align with the heading and all history controls/timeline/stops remain within the map area in the implementation.
- [x] 2.4 Correct and verify the Remote Access action rail remains readable and usable beside or below the preview at wide and narrow widths in the responsive styles.
- [x] 2.5 Correct and verify Reports uses no more than three columns on wide screens, two on medium, and one on narrow screens, with content-sized cards and all existing data retained.
- [x] 2.6 Verify Detailed Information supports multi-field lookup, valid device selection, stale-result clearing, and explicit loading/empty/error states in the implementation.

## 3. Asset and Localization Delivery

- [x] 3.1 Ensure all page and plugin strings needed on first render exist in the eagerly loaded English and Portuguese dictionaries, and plugin locale bundles remain synchronized.
- [x] 3.2 Align the changed source and DEV-served ROOT assets without removing ROOT-only functionality; apply one new cache token across changed initial assets, shell templates, child templates, and dynamic plugin resources.
- [x] 3.3 Confirm the DEV Tomcat-served resources expose the new references and corrected templates/dictionaries, and no active page include points to the stale cache token.

## 4. Validation and Release Evidence

- [x] 4.1 Run targeted JavaScript syntax checks, JSON parsing, OpenSpec strict validation, and `git diff --check`; verify Cards/List selector occurrences are limited to Files.
- [ ] 4.2 Verify the six screens visually in an authenticated DEV session at desktop and narrow widths, recording any remaining failures before UX acceptance. **Blocked:** the available DEV browser redirects to `#/login`; authenticated visual acceptance remains pending.
- [x] 4.3 Reconfirm no APK, enrollment version, QR data, or device profile was modified; report the observed `modelo-default` 6.36/1.3 actions separately without saving profile changes.
