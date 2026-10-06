## Context

The current DEV screenshots cover Messages, Files, GPS, Remote Access, Reports, and Detailed Information. They show literal translation keys in page subtitles, GPS controls detached from their map context, a Reports dashboard that leaves uneven whitespace, and inconsistent search/action layouts. The attached DOM also shows Files being included with an old cache token even though the current DEV `index.html` references newer assets. The latest browser diagnostic reports Quirks Mode, and code inspection found that the `localized` directive does not rerender when saved language settings load asynchronously. HTTP success alone therefore does not prove that the browser rendered the current UI.

The deployment target is the `source/volumes/webapps/ROOT/` tree, which is served by the DEV Tomcat volume. The source webapp is under `server-source/server/src/main/webapp/`; corresponding source and served assets must remain aligned without replacing DEV-only template functionality (notably the expanded Reports view). The available integrated browser is currently unauthenticated.

The enrollment screenshots are a separate concern: the `modelo-default` profile visibly has launcher 6.36 set to “Não instalar” and launcher 1.3 set to “Instalar/Atualizar”, and another profile is described as “Enroll com 6.36 + OTA para 1.3”. These are read-only evidence, not authorization to edit enrollment. The approved 6.36 APK and profile data are non-goals for this change.

## UX/UI Assessment

The review is based on the operator's screenshots and attached DOM context, using task visibility, information hierarchy, consistency, responsive behavior, accessibility, and error recovery as criteria.

| Screen | Evidence and user impact | UX/UI correction and acceptance |
|---|---|---|
| Messages | The operator reported that the send area did not appear. Hiding the whole compose flow until device selection or failing to explain permissions leaves no discoverable action. | Keep device selection and message entry visible to authorized viewers before selection; clearly explain disabled sending and device-load failures. Device labels must use only valid primitive fields. |
| Files | The user requires Cards/List only here. The attached DOM showed raw `files.page.subtitle` and old include token, while the screen already had both view controls. | Preserve the two view controls only on Files; verify translated subtitle/button names, accessible pressed state, and no loss of search or loaded files when switching. |
| GPS | The screenshot places group/refresh controls between the heading and map and spreads history period, timeline, stops, and exports across the page. This breaks spatial grouping and makes device-specific controls ambiguous. | Align page-level controls with the heading; group history actions and timeline inside the map area, next to the device list, with wrapping at narrow widths. |
| Remote Access | The reported action rail competes with the preview and risks compressed/clipped action labels. | Keep the preview and action rail distinct; labels and touch targets remain legible, and the rail stacks below the preview when side-by-side layout is too narrow. |
| Reports | The screenshot shows four cards in the first row and two in the next, with broad unused regions. | Use a deliberate three/two/one-column responsive grid, align cards to their own content, preserve every panel and filter, and avoid horizontal page scrolling. |
| Detailed Information | The user needs more than a device-number-only lookup; typeahead and asynchronous requests can otherwise show stale details or unclear empty/error states. | Search across supported identifying fields, require a valid selected result for details, clear old details when the query changes, and announce loading, no results, and request failures. |

The assessment finds task completion, not decoration, to be the primary risk: entry points and feedback must be visible, related controls grouped, text readable at the tested viewport, and stale results/errors made explicit. Keyboard focus and accessible names/pressed states are retained for interactive controls.

The follow-up diagnostic identifies two additional root causes to correct before visual acceptance: the served shell lacks `<!DOCTYPE html>`, placing the application in Quirks Mode; and ordinary `localized` elements translate only once, before the user's saved locale may have resolved. The fix adds the HTML5 doctype and subscribes the directive to locale changes, with listener cleanup on scope destruction.

## Goals / Non-Goals

**Goals:**

- Deliver the same current, cache-busted UI in source and DEV for the six reported screens.
- Remove literal translation keys, preserve the Messages composer, and keep device labels safe and readable.
- Keep Cards/List exclusively in Files.
- Improve responsive placement and density for GPS, Remote Access, and Reports without removing existing actions or report data.
- Keep Detailed Information loading, empty, selection, and error states explicit.
- Perform a screenshot-based UX/UI review and record which checks can and cannot be verified without an authenticated browser.

**Non-Goals:**

- Change the enrollment APK, APK version, package metadata, device profiles, QR enrollment data, or production deployment.
- Change API contracts, databases, device behavior, or application permissions.
- Claim user-visible acceptance based only on HTTP 200 responses.

## Decisions

1. **Use the served ROOT tree as the DEV runtime truth and update its source counterpart.** The active DEV Tomcat volume serves ROOT directly. Updating only the Maven source would not correct DEV; copying whole source templates over ROOT is also unsafe because ROOT Reports contains additional functionality. Instead, make corresponding targeted edits while preserving existing template-specific behavior.

2. **Invalidate the complete static-resource chain with one new cache token.** Update initial script/style references and the parent/child template and plugin references together. This addresses the captured old `content.html` include URL; changing only `index.html` or only a child template can leave an already-cached parent selecting stale resources.

3. **Use eager global translations for first-render labels.** Global page subtitles and plugin search states referenced by the initial route are placed in the eagerly loaded module dictionary in both languages. Plugin resource bundles remain synchronized, but asynchronous plugin loading is not relied upon for the initial render.

4. **Fix layout at the owning component and responsive breakpoint.** Keep GPS refresh/group controls in the page toolbar and history/timeline/stops in the map panel; keep Remote actions in their existing action rail; constrain Reports to three desktop columns, two medium columns, and one narrow column, with cards aligned to their content rather than stretched to the tallest peer.

5. **Do not mutate enrollment data during UI deployment.** The 6.36 version is user-approved, but the screenshots reveal conflicting live profile actions. No profile save or APK/version change is safe without confirming the three tested profiles and the intended OTA policy. Record the observed discrepancy for a separate, explicitly approved enrollment correction.

6. **Treat authenticated visual inspection as a release gate, not a proxy.** Use the operator-provided authenticated screenshots as evidence for only the screens and viewport states they actually show. Do not infer unshown interactions, other screens, or narrow-width behavior from those images, and do not claim final UX approval until the remaining visual checks pass.

## Risks / Trade-offs

- [A browser or intermediary may retain an old shell despite the new asset token] → Change the token across the full include chain, confirm the exact DEV-served references, and require an authenticated hard reload before acceptance.
- [Source and DEV templates intentionally differ] → Preserve ROOT-only functionality and compare each changed component rather than wholesale-copying templates.
- [CSS overrides later in the cascade may reintroduce a four-column grid or control overflow] → Put the responsive overrides after existing shared rules and inspect all selectors at relevant breakpoints.
- [The live enrollment profile may continue to install 1.3 instead of the user-approved 6.36] → Keep this deployment strictly UI-only and clearly report the observed profile state; do not change enrollment configuration without explicit authorization.
- [No authenticated session is available for visual regression checks] → Report this as unverified; require the operator to validate the six screens in an authenticated session before marking UX acceptance complete.

## Migration Plan

1. Complete and validate the OpenSpec proposal, design, delta spec, and tasks before applying UI changes.
2. Update the source webapp and DEV-served ROOT files, preserving unrelated dirty files and existing ROOT-only behavior.
3. Run targeted syntax/JSON checks, formatting/diff checks, cache-token consistency checks, and DEV HTTP requests for every changed resource.
4. Because ROOT is volume-served, verify the new references directly from DEV; do not restart services unless these checks show a need.
5. Perform authenticated visual checks at wide and narrow widths when a valid session is available. Do not mark these complete on the unauthenticated login page.
6. If a change must be rolled back, restore only the specific files changed by this OpenSpec change and restore the prior cache references; do not revert unrelated workspace changes.

## Open Questions

- Which exact three profile names were used for the failed enrollment tests, and should any of them intentionally perform OTA from 6.36 to 1.3? This remains outside this UI change and must be resolved before changing a profile.
- An authenticated DEV browser session is needed to verify actual rendered translations, composer visibility, and responsive layouts end-to-end.
