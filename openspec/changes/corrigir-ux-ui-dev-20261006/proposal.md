## Why

The DEV screenshots show unresolved usability and localization defects across six operational screens, including literal translation keys and a stale Files template still loaded by the browser. The current evidence disproves the prior assumption that HTTP 200 responses alone meant the UI was fixed; a traceable, user-visible correction is needed before DEV can be considered ready.

## What Changes

- Correct the Messages composer and device identity display, Files translations and view controls, GPS map/history layout, Remote action layout, Reports grid, and Detailed Information search states.
- Ensure the served DEV assets and their cache-busting references select the same corrected templates and dictionaries as the source.
- Preserve the Cards/List selector exclusively in Files and preserve all existing data and actions.
- Keep enrollment/APK and device-profile configuration outside this UI change; the user-approved enrollment APK remains version 6.36.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `panel-interface-ux`: clarify the expected responsive, localized behavior of the six screens and the Files-only view selector.

## Impact

Affected areas are the webapp templates, controllers, stylesheets, and localization bundles in `server-source/` and the DEV-served `source/volumes/webapps/ROOT/` tree, plus the OpenSpec UX specification. No API, database, dependency, APK, enrollment version, or device-profile changes are in scope.
