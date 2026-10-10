# Fortun0

EMI tracker and money manager for Android. Native Kotlin + Jetpack Compose, offline-first,
with optional Firebase account and cloud backup.

## What the free tier does

- **Dashboard**: safe-to-spend until payday, this month's income / spend / remaining,
  expected month-end balance, cash vs accounts vs deposits, upcoming EMIs and bills.
- **Loans & EMIs**: personal, home, bike, car, education, gold, phone/appliance and card EMIs,
  with a full repayment schedule, interest split, outstanding principal and end date.
- **Bills**: recurring or one-time (rent, recharge, insurance, SIP...), mark as paid.
- **Wallet**: cash, bank accounts (with what each is used for), wallets, deposits;
  credit cards with limit, amount used, available limit and due date.
- **Activity**: income, expenses, transfers and card bill payments, month by month.
- **Financial health**: a 0-100 score from EMI burden, card usage, emergency buffer,
  savings margin and insurance cover, with a tip for each.
- **Scam safety**: common frauds in India and what to do, with a one-tap call to 1930.
- **App lock**: fingerprint / face / device PIN.
- **Reminders**: daily on-device check, notifies before, on and after each due date.
- **Account & backup**: email sign-up with email verification, two-way sync to Firestore.
  Or use it with no account, data on the phone only.

Premium features are listed in the app but locked; see `core/premium/Feature.kt`.

## Build and run

Needs JDK 17 and the Android SDK (platform 36). From the project root:

```
gradlew.bat :app:installDebug
gradlew.bat :app:testDebugUnitTest
```

Without Firebase configured the app builds and runs in local-only mode.

## Turning on accounts, backup and push (one-time Firebase setup)

1. Create a project at https://console.firebase.google.com (the free Spark plan is enough).
2. Add an Android app with package name `app.paisapilot`, download `google-services.json`
   and put it in the `app/` folder. It is git-ignored on purpose.
3. Authentication > Sign-in method: enable **Email/Password**.
4. Firestore Database: create a database (production mode, region `asia-south1` for India).
5. Publish the security rules in `firestore.rules`, either by pasting them into the console
   (Firestore > Rules) or with `npx firebase-tools deploy --only firestore:rules`.
6. Rebuild. The sign-in screen now appears and data syncs under `users/{uid}/...`.

Push notifications (FCM) need a device or emulator image **with Google Play services**
("Google APIs" or "Google Play" system image). Due-date reminders do not need this.

## Testing the cloud code without a Firebase project

The Firebase Emulator Suite runs Auth and Firestore locally:

```
npx firebase-tools@13.29.1 emulators:start --only auth,firestore --project demo-paisapilot --config firebase.json
gradlew.bat :app:installDebug -PfirebaseEmulator=true
```

(`firebase-tools` 14+ needs JDK 21; 13.29.1 works with JDK 17.) The debug build needs a
`google-services.json` whose `project_id` is `demo-paisapilot`. Verification emails are
not sent by the emulator; the link is listed at
`http://127.0.0.1:9099/emulator/v1/projects/demo-paisapilot/oobCodes`.

## Release build

```
gradlew.bat :app:bundleRelease     # app/build/outputs/bundle/release/app-release.aab  (upload this to Play)
gradlew.bat :app:assembleRelease   # app/build/outputs/apk/release/app-release.apk    (install directly)
```

Release builds are shrunk with R8 and signed with the upload key in `keystore/`, using the
passwords in `keystore.properties`. Both are git-ignored: **back them up somewhere safe**.
Raise `versionCode` in `app/build.gradle.kts` for every upload to Play.

A release build cannot be installed over a debug build (different signature); uninstall first.

## Web pages (privacy policy, account deletion)

The `public/` folder is a small static site for Firebase Hosting:

```
npx firebase-tools@13.29.1 deploy --only hosting --project app-paisapilot
```

The app links to `https://app-paisapilot.web.app/privacy.html`. Play Console needs that URL
and `.../delete-account.html`.

## Project layout

```
app/src/main/java/app/paisapilot/
  core/
    model/          enums: account, loan, bill, transaction types
    util/           Money (paise, Indian formatting), date helpers
    finance/        EmiCalculator, LoanMath, ForecastEngine, HealthScore  (pure Kotlin, unit-tested)
    premium/        Feature registry and Entitlements (free vs premium)
    security/       BiometricAuthenticator, AppLockManager
    notifications/  Notifier, ReminderWorker, PushMessagingService
    designsystem/   theme, cards, inputs, scaffolds, charts
  data/
    local/          Room database, entities, DAOs
    prefs/          ProfileStore (DataStore): profile and settings
    remote/         FirebaseProvider
    repository/     Auth, Wallet, Loan, Bill, Txn, Session
    sync/           SyncManager, SyncWorker, FirestoreCodec
  domain/           ObserveOverview: the combined live financial picture
  feature/          one package per screen group, each with Screen + ViewModel
  navigation/       routes, bottom bar, nav graph
  di/               Hilt module
```

Rules of thumb used throughout:

- Money is stored as whole paise in a `Long`; never floating point.
- Room is the source of truth. Firestore is a backup/sync target, so everything works offline.
- A loan's schedule is derived from its terms, never stored.
- Transactions and the balance changes they cause are written in one database transaction.
