# Light UI Migration Phase 11 Validation

## Passing gates

- `git diff --check`
- `cmd /c gradlew.bat --no-daemon :app:assembleMainlineDebug`
- APK installation on `LP3LHP1511300111`
- Main activity launch on `LP3LHP1511300111`
- No Gadgetbridge fatal application exception during launch

## Known unrelated unit-test failures

`cmd /c gradlew.bat --no-daemon :app:testMainlineDebugUnitTest` completed 1,972 tests
with 37 skipped and two failures:

- `CardoSupportTest.testMessages`: expected LF output but received CRLF output.
- `HexToBinaryInputStreamTest.testConversion`: expected raw CR bytes but the current
  stream path returned normalized LF bytes.

These failures are outside the Light UI migration and were not changed.

## Device limitation

The LP3 was awake enough to launch the app but remained on the first-run Welcome flow.
The app UI hierarchy was captured successfully. Full route-by-route interaction testing
requires completing the device's permission and onboarding flow interactively.
