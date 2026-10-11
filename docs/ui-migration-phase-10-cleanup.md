# UI Migration Phase 10 Cleanup

## Scope

This cleanup removes only the app-shell drawer infrastructure proven unused after
the shell, dashboard, devices, settings, and utility migrations. The drawer was
already `gone` and locked closed in `ControlCenterv2`; the active shell is the
toolbar, pager, and bottom navigation.

## Deleted

- `app/src/main/res/layout/main_drawer_header.xml`: referenced only by the removed drawer.
- `app/src/main/res/menu/activity_main_drawer.xml`: referenced only by the removed drawer.
- The drawer wrapper and `NavigationView` from `app/src/main/res/layout/activity_main.xml`.
- Drawer setup, listener actions, and drawer-only imports from `ControlCenterv2`.
- Orphaned `drawer_header_color` and `drawer_title_color` theme attributes and values.
- The stale lint-baseline entry for the already absent `nav_header_main.xml`.

## Retained Infrastructure

- `AbstractGBActivity`: still backs activities for discovery, permissions, alarms,
  reminders, charts, workouts, device tools, and transfer/install flows.
- `AbstractSettingsActivityV2` and `AbstractPreferenceFragment`: still back
  `SettingsActivity` and feature preference activities including charts, maps,
  notifications, health connect, quick settings, and integrations.
- `AbstractListActivity`: still backs `WorkoutListActivity`.
- Preference XML, dialogs, chart layouts, device-specific screens, and transfer
  activities remain intact because their callers are active.
- `activity_main_app_bar.xml` remains the active shell content included by
  `activity_main.xml`.

## Verification Basis

Repository-wide reference search found no remaining references to the deleted
drawer resources, IDs, or drawer classes. Feature activity references were
checked before cleanup and remain outside this change.
