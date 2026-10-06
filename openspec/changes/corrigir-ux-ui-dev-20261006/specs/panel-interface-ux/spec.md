## ADDED Requirements

### Requirement: Current localized assets are rendered in DEV
The DEV interface SHALL load the current version of each changed page template and its localization resources rather than a previously cached parent or child template. All changed assets SHALL use a consistent cache-busting token through the initial page, application shell, page includes, and dynamically loaded plugin resources.

#### Scenario: Open Files after a UI deployment
- **WHEN** an operator opens or reloads Files after the DEV UI update
- **THEN** the loaded include references the current template and dictionary resources, and the subtitle and Cards/List labels render as localized text instead of translation keys

#### Scenario: Open Applications after a UI deployment
- **WHEN** an operator opens Applications after the DEV UI update
- **THEN** the page subtitle renders as localized text instead of `applications.page.subtitle`

### Requirement: Reports cards align to content in a responsive grid
The aggregate Reports dashboard SHALL display no more than three columns on wide viewports, two columns at medium widths, and one column on narrow viewports. Each report card SHALL size to its own content and SHALL NOT stretch vertically to match an unrelated card in the same row.

#### Scenario: Wide viewport with six aggregate panels
- **WHEN** an operator opens the aggregate Reports dashboard on a wide viewport
- **THEN** all six panels appear in rows of at most three columns without a fourth narrow column or large blank card regions caused by row stretching

#### Scenario: Narrow viewport
- **WHEN** the Reports viewport is reduced to a narrow width
- **THEN** the panels stack in one column without horizontal page overflow and retain their data and filters

### Requirement: GPS history stays in the map context
The GPS page SHALL align group and refresh controls with the page heading, and SHALL contain history period, playback, export, timeline, and stop controls within the map area to which they apply.

#### Scenario: Open GPS history for a selected device
- **WHEN** an operator enables history for a selected device
- **THEN** the history controls, timeline, and stops appear within the map panel beside the device list, while the page-level group and refresh controls remain aligned with the page heading

### Requirement: Remote action controls remain legible
The Remote Access action rail SHALL remain visually separate from the device preview and SHALL preserve complete labels and usable targets at wide and narrow viewport widths.

#### Scenario: Wide and narrow Remote Access layouts
- **WHEN** Remote Access is rendered on a wide viewport or a narrow viewport
- **THEN** action labels do not overlap or become clipped, and the action rail moves below the preview when the available width cannot support a side-by-side layout
