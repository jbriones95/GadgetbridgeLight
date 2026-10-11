# Gadgetbridge Light UI Migration Inventory

```yaml
phase: 0
status: inventory-and-freeze
repository: gadgetbridge
ui_target: Light SDK / Light UI
inventory_basis: app/src/main
snapshot_note: counts are a scope signal, not a migration completion metric
source_policy: documentation-only change; no application source was changed
```

## Scope Snapshot

| Surface | Inventory root | Approx. files | Migration unit |
| --- | --- | ---: | --- |
| Activities and activity-owned UI | `app/src/main/java/nodomain/freeyourgadget/gadgetbridge/activities/` | 404 files | activity/flow |
| Fragments | `app/src/main/java/nodomain/freeyourgadget/gadgetbridge/activities/` and feature packages | included above | fragment/screen |
| View layouts | `app/src/main/res/layout/`, `layout-land/` | 287 in `layout/` | layout and included sub-layouts |
| Preference XML | `app/src/main/res/xml/*preferences*.xml`, `map_settings.xml`, device settings XML | 431 XML total | preference screen/group |
| Dialogs | `*Dialog*` classes, dialog layouts, `DialogFragment` call sites | cross-cutting | dialog/confirmation |
| Bottom sheets | `*BottomSheet*` classes and bottom-sheet layouts/call sites | cross-cutting | sheet/action surface |
| Menus | `app/src/main/res/menu/` and `MenuProvider`/`onCreateOptionsMenu` call sites | 33 | menu/action set |

Counts should be refreshed when a migration phase starts. The canonical inventory is the source tree, not this snapshot.

## Entry Points And Containers

| Area | Current anchors | Migration status |
| --- | --- | --- |
| App shell and navigation | `activities/ControlCenterv2.java`, `activities/dashboard/DashboardFragment.kt`, `activities/DevicesFragment.java`, `res/menu/bottom_nav_menu.xml` | not started |
| First-run and permissions | `activities/welcome/`, `WelcomeActivity.java`, `PermissionsActivity.java`, `res/layout/fragment_welcome_*.xml` | not started |
| Settings shell | `SettingsActivity.java`, `DashboardPreferencesActivity.java`, `AbstractSettingsActivityV2.java`, `AbstractPreferenceFragment.java` | not started |
| Device discovery and pairing | `activities/discovery/`, `DiscoveryActivity` implementations, `AndroidPairingActivity.java`, `res/layout/activity_discovery.xml` | not started |
| Device detail/settings | `activities/devicesettings/`, `devices/*/ui/`, `res/xml/devicesettings_*.xml` | not started; largest family |
| Dashboard and widgets | `activities/dashboard/`, `widgets/`, `res/layout/dashboard_*.xml`, `res/layout/activity_dashboard_widgets.xml` | not started |

## Legacy UI Inventory

### Activities

- Global application activities are declared in `app/src/main/AndroidManifest.xml`; use that file as the authoritative externally reachable activity list.
- Shared bases: `AbstractGBActivity`, `AbstractListActivity`, `AppCompatPreferenceActivity`, and `GBActivity` in `app/src/main/java/nodomain/freeyourgadget/gadgetbridge/activities/`.
- Feature families: `activities/{automations,charts,debug,discovery,files,fit,internet,loyaltycards,maps,multipoint,musicmanager,preferences,quicksettings,welcome,widgets,wififtp,workouts,xiaomi}/`.
- Device-owned activities are under `activities/devicesettings/` and `devices/*/`; do not infer that a device protocol implementation is UI work.

### Fragments

- Shared bases: `AbstractGBFragment` and `AbstractPreferenceFragment`.
- Navigation/content fragments: `DashboardFragment`, `DevicesFragment`, activity summary/chart fragments, welcome fragments, and workout fragments.
- Preference fragments and nested settings are found beside their feature activity and in `activities/preferences/` and `activities/devicesettings/`.
- XML-backed fragment references must be mapped to their host activity and navigation/result contract before migration.

### Layouts

- General screen layouts: `app/src/main/res/layout/activity_*.xml` and `fragment_*.xml`.
- Reusable rows/cards: `item_*.xml`, `device_*.xml`, `preference_*.xml`, and `dashboard_*.xml`.
- Dialog-specific layouts include `dialog_*.xml` and `item_*dialog*.xml`.
- Configuration variants include `app/src/main/res/layout-land/`; preserve orientation behavior until the replacement screen is verified.

### Preference XML

- Shared roots: `res/xml/preferences.xml`, `notifications_preferences.xml`, `online_fitness_trackers_preferences.xml`, `internethelper_preferences.xml`, `health_connect_preferences.xml`, `discovery_pairing_preferences.xml`, and `map_settings.xml`.
- Integration-specific roots include `sleepasandroid_preferences.xml`, Garmin/Fossil settings, and other provider preference files.
- Device settings are the `res/xml/devicesettings_*.xml` family. Treat preference keys, defaults, dependencies, summaries, and write listeners as behavioral API.
- `res/xml/` also contains non-UI contracts such as `network_security_config.xml`, provider/widget metadata, and file sharing paths; exclude those from visual migration.

### Dialogs And Bottom Sheets

- Dialog implementations are distributed across feature and device packages; locate them by `Dialog`, `DialogFragment`, `MaterialAlertDialogBuilder`, and `AlertDialog` usage.
- Dialog layouts are primarily `res/layout/dialog_*.xml`; item-picker dialogs commonly use `item_*dialog*.xml`.
- Bottom sheets are distributed rather than centralized; locate `BottomSheetDialog`, `BottomSheetDialogFragment`, `BottomSheetBehavior`, and `*BottomSheet*` names before each feature migration.
- Record for each surface: entry trigger, cancel/back behavior, result delivery, permissions, async loading, and any protocol command sent from an action.

### Menus

- XML menus: `app/src/main/res/menu/` (including `bottom_nav_menu.xml`).
- Programmatic menus: `MenuProvider`, `onCreateOptionsMenu`, `onPrepareOptionsMenu`, and `onOptionsItemSelected` call sites in activity/fragment sources.
- Menu actions must retain visibility rules, deep links, result contracts, and accessibility labels when moved to Light UI actions.

## Major Feature Families

| Family | UI roots | Behavior boundary to preserve |
| --- | --- | --- |
| App shell, dashboard, devices | `activities/ControlCenterv2.java`, `activities/dashboard/`, `DevicesFragment.java` | device state, navigation, refresh and background updates |
| Discovery, pairing, permissions | `activities/discovery/`, `AndroidPairingActivity.java`, `PermissionsActivity.java` | Bluetooth permissions, pairing state, scan lifecycle |
| Device settings | `activities/devicesettings/`, `res/xml/devicesettings_*.xml`, `devices/*/ui/` | preference keys, validation, device commands and capability gating |
| Health, activity and workouts | `activities/charts/`, `activities/workouts/`, activity summary fragments | DAO queries, time ranges, exports, workout and sample data |
| Notifications, calls and messaging | `activities/app_specific_notifications/`, `Notification*Activity`, contact/call flows | notification filters, permissions, intents and background services |
| Alarms, reminders and calendars | `AlarmDetails`, `ConfigureAlarms`, `ConfigureReminders`, `ConfigureWorldClocks`, `CalendarSelectionActivity` | scheduling, persistence and device synchronization |
| Maps, navigation and tracks | `activities/maps/`, `GpxReceiverActivity.java` | location permissions, track files, navigation intents |
| Data management and integrations | `DataManagementActivity`, `BackupRestoreProgressActivity`, `activities/internet/`, `activities/loyaltycards/`, provider settings | import/export formats, external intents and privacy boundaries |
| Device tools and media | `activities/appmanager/`, `musicmanager/`, `install/`, `audiorecordings/`, device-specific UI | file transfer, firmware/app installation and device protocol operations |

## Migration Rules

1. **Freeze new legacy UI.** Do not add new AppCompat/Material XML screens, `res/layout` screen surfaces, native preference screens, legacy menu-only navigation, dialogs, or bottom sheets. Bug fixes to existing legacy UI are allowed only when required for correctness or migration safety.
2. **Migrate behavior, not merely pixels.** Every replacement must preserve protocol commands, Bluetooth/permission sequencing, database reads and writes, preference keys/defaults/dependencies, exports, intents, service behavior, and error/retry semantics.
3. **Keep protocol and data layers out of UI migrations.** Do not rewrite device coordinators, packet handlers, services, DAOs, entities, or persistence schemas as part of a screen migration unless a separate approved change requires it.
4. **One surface at a time.** Before changing a surface, record its entry points, states, actions, results, loading/error/empty states, and legacy resource dependencies. Mark the old surface deprecated only after parity verification.
5. **Preserve user data compatibility.** Existing preference keys, serialized values, database formats, intent extras, and file formats are stable contracts. If a key must change, provide an explicit migration and test both upgrade and fresh-install behavior.
6. **Do not duplicate protocol logic.** Light UI screens call existing application/domain APIs; they must not issue raw device commands or bypass existing permission, service, or transaction boundaries.
7. **Shared components require explicit ownership.** A component used by multiple feature families is migrated only after all callers are inventoried, or it remains behind a documented compatibility boundary.

## Phase 0 Exit Criteria

- This inventory is checked in and remains the scope index for later phases.
- New legacy Material/XML UI is rejected in review unless covered by the bug-fix exception above.
- Each migration PR identifies its UI surface and feature family from this document.
- Each migration PR includes protocol/data parity evidence and explicitly lists unchanged behavior boundaries.
- No source, resource, manifest, preference, protocol, or data files were modified by Phase 0.
