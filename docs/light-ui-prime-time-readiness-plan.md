# Light Phone 3 UI Prime-Time Readiness Plan

## Purpose

Make Gadgetbridge feel intentionally designed for the Light Phone 3 while retaining
Gadgetbridge's protocol, synchronization, health-data, notification, and device-management
behavior. “Light UI conforming” is judged across the complete user journey, not just by using
black backgrounds or Compose. A screen is not considered migrated while its primary interaction
still unexpectedly opens an Android Preference, Material, or AppCompat screen.

This is a staged delivery plan, not a claim that the current app is release-ready. Preserve the
existing migration work and validate each phase on the LP3 before broadening it.

## Research basis and device constraints

The checked-in Light SDK is the implementation reference for this application:

- `../light-sdk/sdk/ui/src/main/kotlin/com/thelightphone/sdk/ui/LightGrid.kt` defines the
  27-column by 31-row layout grid and its width/height based unit conversions.
- `.../LightTheme.kt` defines the LightOS typography roles and scales design sizes against a
  600 dp vertical baseline.
- `.../LightFont.kt` resolves Akkurat from system fonts on Light Phone devices and avoids
  redistributing licensed font files.
- `.../LightTopBar.kt`, `LightBottomBar.kt`, `LightScrollView.kt`, and `LightProgressBar.kt`
  define the shared layout and interaction conventions.

The connected LP3 reports a physical display of **1080 × 1240 at 480 dpi**, or **360 × 413 dp**.
The 27-column horizontal unit therefore resolves to about **13.3 dp** on this device. The
connected system font-scale setting returned no explicit override (default scale). The system
font directory contains the AkkuratLLTT family (Light, Regular, Medium, Bold and companion
styles). Do not bundle those system font files into Gadgetbridge: use the system family where
available and a tested fallback elsewhere.

The app retains `minSdk 23`; use API-guarded font discovery and keep the fallback functional.
The Light SDK is a visual/design reference here; do not add its client/runtime to Gadgetbridge.
Android/system-owned permission pickers, Bluetooth pairing, document pickers, and OS settings
are expected native boundaries. Their return path must be clear and predictable.

## Current assessment

### Established Light foundations

- The app has a Light-styled device list, dashboard date header, settings landing pages, device
  settings DSL renderer, selected dashboard widgets, and shared monochrome components.
- Recent repairs cover key device-setting navigation, default values, several callbacks and
  summaries, notification filter edge cases, device refresh, accessible rows, and progress
  semantics.
- The app-local typography now resolves Akkurat from LP3 system fonts and adopts the SDK's
  screen-height-scaled title/heading/copy/button/detail roles, with a system-font fallback.
- Recent app builds, focused widget tests, APK installs, and application launches have passed.

### Important gaps before claiming conformance

- Many app journeys still use Android layouts, AppCompat toolbars, Material widgets, and
  `PreferenceFragmentCompat`, including notification management/filter editing, XML-only device
  settings, and numerous device/integration screens.
- Migrated strings, date/time/number formatting, RTL, very large font scales, and small-width
  behavior are not yet consistently validated.
- Some DSL control types are still approximations; sortable lists need a real reorder UI and
  exact persistence semantics. All summary, dependency, confirmation, action-result, and
  XML-subscreen behaviors need systematic parity checks.
- Dashboard chart/health widgets and legacy chart detail flows are not comprehensively migrated.
- Dashboard goals/metric color semantics and some legacy legend behavior need an explicit product
  decision and parity implementation; monochrome does not mean loss of useful data meaning.
- Visual evaluation must include screenshots and real interaction paths, not only a successful
  APK launch.
- A native-resolution Settings screenshot showed the Material/Android action bar stacked above
  the Light top bar, creating duplicate app identity and back navigation. LightSettingsActivity
  and LightDeviceSettingsActivity now hide that inherited action bar.
- The dashboard header's full “Previous” label wrapped at LP3 width. The visible affordance now
  uses a short “Prev” label while retaining the full accessible “Previous day” description.
- A review against SDK screens confirmed the Light back affordance is the dedicated left-chevron
  `LightIcons.BACK`, not a text “Back” button or Material arrow. All Light-hosted settings and
  device-settings screens now share that icon, its accessible name, and a 48 dp hit target.
- The primary app shell no longer uses `MaterialToolbar` or `BottomNavigationView`: the top bar and
  three destination controls now use the local Light components, while preserving calendar,
  dashboard-widget settings, page selection, and global Settings actions.
- LP3 screenshots after this shell pass show Dashboard/Devices titles, a calendar action, a
  dashboard Edit action, and the monochrome three-destination bar. Settings shows the SDK-style
  back chevron instead of a text back action; its 48 dp hit area was tapped successfully to return
  to the shell.
- The device-row “More” action now opens a full-screen Light action page built from the same
  connection/settings/alias/installer/remove menu and the user's configured device-card actions.
  The row also restores capability-gated quick actions for sync, find-device, and heart-rate
  measurement; heart-rate samples render in a Light result surface, and find-device selection and
  stop behavior use Light screens while preserving per-earbud targets.
- The LP3 dashboard screenshot revealed metrics flush against screen edges. Shared Light rows and
  metric rows now use a one-grid horizontal inset and a more even vertical rhythm; screenshots
  show the values, labels, and progress bars aligned to the same gutter.
- The device action surface is now a full-screen Light page with a scrolling action list, rather
  than a bounded popup that visually clipped its last item. Native-resolution LP3 review confirms
  the full-screen header/back affordance and scrollable action list.
- Welcome is now a three-step flow (intro, permissions, get started), not five independent pages.
  The intro presents a concise app summary without the native theme dropdown; the permissions
  step starts with a status count and request-all action, with the long per-permission list
  collapsed until explicitly reviewed. The back affordance is the Light chevron, and continuing
  past the permissions step records the user's choice not to be repeatedly prompted.
- First-run onboarding is reduced from five pager pages to three: a compact introduction, a
  permissions step with the full list collapsed by default, and the existing finish/actions page.
  Intro content now summarizes the dashboard, devices, and privacy benefits; the separate
  overview/open-source slides and onboarding theme dropdown are removed from the mandatory flow.
  The theme preference remains available in app settings.
- The onboarding permissions page now shows a granted-count summary and keeps per-permission
  controls behind “Review permissions”. “Continue” explicitly dismisses future permission
  pestering, while system permission requests and the existing “request all” sequence remain intact.

## Design and behavior rules

1. **One visual language per user journey.** A Light screen should not unexpectedly hand off to a
   stock Android settings list for an ordinary in-app task. When a platform boundary is necessary,
   label it, preserve state, and return to the same Light context.
2. **Use the LP3 grid, not ad hoc Android dimensions.** Use horizontal grid units for Light-aligned
   insets and SDK-derived vertical rhythm for content. Touch targets may exceed visual geometry to
   satisfy access needs without enlarging the drawn control.
3. **Use Akkurat and LightOS type roles.** Use semantic roles consistently; do not select text
   sizes screen-by-screen. Keep Android font scaling enabled and verify wrapping rather than
   clipping.
4. **Monochrome hierarchy must remain legible.** Use weight, spacing, dividers, and secondary
   contrast intentionally. Never encode state through color alone.
5. **Native Android boundaries remain native.** Permission, pairing, file/folder chooser, and
   other OS-owned experiences must use the platform UI and have predictable Light return paths.
6. **Data and action parity precede visual replacement.** Every migrated control must preserve
   defaults, persistence, callbacks, confirmation, dependency state, deep links, and device-service
   side effects.
7. **Accessibility is part of conformance.** Controls need names, roles, state/value semantics,
   keyboard/switch access where relevant, and adequate targets; headings and progress must be
   announced meaningfully.

## Delivery phases

### Phase 0 — Build a complete screen and route inventory

**Work**

- Enumerate launcher/onboarding, shell tabs, devices, device details/settings, dashboard,
  notifications, charts/health, workouts, integrations, app-management tools, global settings,
  import/export, and permission/system handoffs.
- For each activity/fragment, record callers, intent extras, return behavior, persistence surface,
  device-service effects, and whether it is Light, Android-native by policy, or still legacy.
- Add a lightweight route audit checklist to the migration docs. Search both manifest declarations
  and navigation call sites; do not infer reachability from class names alone.

**Exit criteria**

- Every user-reachable screen has an owner, migration state, and documented native-boundary reason
  or migration issue.
- Every route into a DSL/XML device setting, notification filter, and settings sub-screen is
  covered by an entry-point matrix.

### Phase 1 — Lock the design foundation and typography

**Work**

- Maintain one LP3 type scale, font resolver, monochrome palette, grid conversion, spacing scale,
  divider treatment, and control-state system; remove competing local defaults.
- Compare app-local type roles numerically and visually to the checked-in SDK. Confirm title,
  heading, body, action, and secondary/detail mappings against representative real screens.
- Test Akkurat discovery on LP3, non-LP3 system fallback, missing-font fallback, all font weights,
  Latin/diacritics, CJK, and RTL-script fallback.
- Test font scale 1.0, 1.3, and 2.0; adjust bars/rows to grow, wrap, or scroll. Do not clip titles
  or disable user scaling just to preserve a screenshot.
- Check line length, truncation, and number/date formatting at 360 × 413 dp; provide ellipsis only
  where the full accessible label remains available.

**Exit criteria**

- A typography specimen page has golden LP3 screenshots for normal and enlarged text.
- No high-priority user text is clipped, unreadable, or below the chosen LightOS role size without
  a documented dense-data exception.
- Visual type changes have been reviewed on the actual device (not only emulator density).

### Phase 2 — Shell, navigation, and platform boundaries

**Work**

- Finish the main shell and top-level destinations so navigation, selected state, back behavior,
  empty/loading/error states, and status/navigation chrome feel coherent with LightOS.
- Audit toolbar/menu overflow, action labels, back/up, bottom navigation, nested screens, and
  notification deep links for mixed Light/Android handoffs.
- Make system-boundary transitions explicit; validate that returning from system settings,
  app picker, file/folder picker, and Bluetooth flow restores the prior screen/state.
- Resolve manifest parent behavior versus task-stack behavior; test both in-app navigation and
  launches from shortcuts/search/tiles.

**Exit criteria**

- No dead-end, blank root, duplicate activity, wrong parent, or lost deep-link target in the route
  matrix.
- Android/system-owned screens are explicitly allow-listed; all other exposed top-level app screens
  have Light presentation.

### Phase 3 — Devices, pairing, and device settings parity

**Work**

- Finish device list row hierarchy, accessible overflow/menu targets, connected/busy/error states,
  sorting/grouping, empty state, refresh, add/remove, and actions.
- Complete the DSL renderer against every `DeviceSetting` node: switches and confirmations, static
  and state summaries, dependencies and refresh, list callbacks, seekbar ranges/formatting, text
  input, dates/times, multiselect, real sortable/reorder, file/folder picker, actions and their
  return values, and XML wrappers/subscreens/child connection states.
- Preserve preference storage types and defaults byte-for-byte where applicable; do not convert
  legacy values without a migration.
- Route all keyed settings/search/quick-tile/deep-link callers through the correct Light screen.
- Make XML-only fallback explicit, scoped, and understandable rather than allowing silent
  renderer fallthrough.

**Exit criteria**

- Every registered DSL node type has at least one meaningful persistence/callback test and a
  device visual check.
- No unsupported DSL node is silently omitted.
- Representative connect, disconnect, reconnect, pairing, settings, action, and XML-fallback
  journeys complete without losing state or sending duplicate commands.

### Phase 4 — Dashboard, widgets, charts, and health

**Work**

- Migrate dashboard modules incrementally while preserving date navigation, per-device selection,
  chart interactions, empty/missing-data distinctions, and widget configuration.
- Define summation/averaging semantics per data category and verify cross-device aggregation
  against existing dashboard calculations.
- Preserve meaningful goals, progress, legend/target settings, units, colors or monochrome
  equivalents, and data-source availability. Treat unavailable data as unavailable, not zero.
- Migrate chart drill-downs and chart configuration with Light typography, axes/markers, date
  navigation, long datasets, and touch/scroll interaction tested on device.

**Exit criteria**

- A device/data parity matrix compares each migrated metric against legacy output for zero, normal,
  large, missing, multi-device, imperial, and non-English locale cases.
- Dashboard navigation and chart detail flows have no legacy visual surprise except documented
  system boundaries.

### Phase 5 — Notifications and ordinary settings

**Work**

- Migrate notification access/status, app blacklist/whitelist, per-app filtering, work-profile and
  priority rules, and all ordinary notification preferences into Light components.
- Preserve database semantics, filter modes/submodes, word lists, package identity, bulk actions,
  search, installed-app refresh, and save/cancel behavior.
- Migrate global preferences and remaining user-facing settings by user journey, not by XML file;
  keep rare advanced/vendor-specific settings in an intentional, labelled fallback until parity is
  ready.
- Localize every migrated string with existing resources or new translated resources; format
  plurals, units, dates, times, and numbers using locale-aware APIs.

**Exit criteria**

- No migrated settings page displays stale state after a platform permission screen or legacy
  fallback returns.
- Existing notification filters round-trip and behave identically before/after migration.
- String lint inventory has no unexplained hardcoded user-facing text in migrated screens.

### Phase 6 — Workouts, integrations, app management, and utility journeys

**Work**

- Inventory then migrate user-facing workout, app management, integration, alarm/reminder,
  transfer, import/export, backup, and diagnostic screens by frequency and user importance.
- Keep protocol-specific forms and progress states usable on the small display; prefer stepwise
  Light flows over dense Android forms where appropriate.
- Preserve destructive-action confirmations, cancellation, progress, retry, and error recovery.

**Exit criteria**

- Common user journeys are end-to-end Light; rare administrative or OS-owned tooling is explicitly
  classified and never accidentally reached from ordinary navigation.

### Phase 7 — Accessibility, internationalization, and resilience

**Work**

- Audit TalkBack semantics, headings, labels, selected/checked/disabled state, progress ranges,
  actions, touch size, focus order, and meaningful content descriptions.
- Validate RTL, font scale, long translations, narrow widths, display cutouts/insets, landscape if
  supported, and process/activity recreation.
- Validate state restoration through permission dialogs, picker activities, device disconnects,
  and process pressure.
- Add UI tests or instrumentation for high-risk routes and use screenshot tests only for stable
  visual contracts; avoid brittle tests of every pixel.

**Exit criteria**

- All critical flows pass TalkBack/manual accessibility review and the supported locale/font-scale
  matrix.
- No fatal exceptions during repeated lifecycle, rotate/recreate, back/up, and permission/picker
  return tests.

### Phase 8 — Visual regression and end-user beta

**Work**

- Maintain LP3 screenshots for shell, devices (empty/connected/busy), settings, DSL screens,
  notifications, dashboard, charts, errors, and system-boundary returns.
- Perform a fresh-install setup and upgrade-existing-data pass; validate permissions, notification
  access, Bluetooth, storage/database upgrade, background service, and device reconnect.
- Run the full unit suite and appropriate instrumentation tests; investigate pre-existing failures
  separately rather than masking them.
- Recruit a small LP3 beta cohort with differing locales, font settings, device types, and data
  volume. Gather task-completion feedback, not only visual preference.
- Provide a rollback/release note describing current coverage and known limitations.

**Exit criteria / release gate**

- All P0/P1 route, data-integrity, and crash issues closed; no lost settings or health data.
- Fresh install and upgrade pass on LP3; install, launch, connection, notification, and return-path
  smoke tests documented.
- All agreed golden screens reviewed at the LP3's native resolution and default/accessibility
  font scales.
- No undocumented Android-looking in-app journey remains in the primary user path.
- Unit/build/lint checks pass or have reviewed, explicitly accepted unrelated exceptions.

## Priority rubric

- **P0 — Release blocker:** crash, data loss/corruption, silent failed device command, unreachable
  core destination, permission/service behavior broken, or inaccessible critical control.
- **P1 — Must fix before broad beta:** visible Android UI in a primary journey, wrong typography or
  clipping on LP3, incorrect metric/units, broken state/back path, important missing setting.
- **P2 — Beta polish:** uncommon configuration parity, visual rhythm inconsistencies, secondary
  accessibility issue, non-critical untranslated text.
- **P3 — Follow-up:** rare debug/admin surfaces, low-frequency integrations, minor decorative
  differences.

## Recommended execution order

1. Freeze and screenshot typography/type-role specimens on the actual LP3.
2. Complete the route/screen inventory and classify system boundaries.
3. Finish main shell and device-settings DSL parity because these establish shared interaction
   patterns and protect device behavior.
4. Complete notification settings and dashboard/chart parity, the most visible recurring app
   journeys after devices.
5. Migrate remaining settings and high-frequency workflows; then integrations and utility screens.
6. Run accessibility/localization/lifecycle matrix continuously, not as a final patch phase.
7. Gate beta/release on repeatable clean install, upgrade, device, notification, and visual checks.

## Immediate next work

1. Re-capture Devices, Dashboard, Settings, and DSL device settings on LP3 after the shared back
   and shell navigation changes; verify icon hit areas, calendar launch/return, Widgets, selected
   tabs, and font scaling.
2. Exercise first-run onboarding on a clean test profile at default and enlarged font scales;
   verify skip/continue, request-all, individually expanded permission rows, restore, add-device,
   and return paths. The current LP3 is already past first run, so this end-to-end visual pass needs
   a clean test profile rather than changing the owner's existing app data.
3. Build a screen/route inventory with explicit Light/native/legacy classifications and identify
   the highest-frequency remaining Android-styled pages (notification management/filter is first).
4. Migrate notification filters and notification management to Light-native screens while
   retaining database semantics; review native dialogs still invoked by some device-card actions.
5. Implement actual reorder controls and finish DSL summaries/dependency refresh/confirmation
   parity.
6. Decide and implement goals legend/progress semantics and restore correct multi-device sleep
   aggregation with a targeted test.
7. Migrate chart drill-downs and continue the accessibility/locale/font-scale screenshot matrix.
