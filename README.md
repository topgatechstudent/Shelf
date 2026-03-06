# Reddit Shelf (Android)

Reddit Shelf is a native Android companion app for Redditors who want to privately organize the posts and comments they already chose to save on Reddit.

This project is designed to support a Reddit Data API approval request for an **installed Android app**. It does **not** scrape Reddit, republish Reddit data, automate posting/commenting/voting, or train models on Reddit content. It only authenticates a single user with OAuth and accesses that user's own authorized data.

## What the app does

- Lets a Redditor sign in with Reddit OAuth as an **installed app**
- Fetches the authenticated user's profile and saved items
- Displays saved posts and comments from the user's saved listing
- Lets the user organize saved items into private folders
- Lets the user attach private tags, notes, and a local status to saved items
- Stores all organizational metadata locally on-device using DataStore

## Why this is outside Devvit

This app is an **off-platform personal utility** for private cross-device organization of a user's own saved Reddit content. It is not an in-community app experience living inside Reddit, and it is not a moderator/community interaction tool.

## OAuth / API model

This app uses Reddit's installed-app OAuth flow:

- Authorization endpoint: `https://www.reddit.com/api/v1/authorize.compact`
- Token endpoint: `https://www.reddit.com/api/v1/access_token`
- Resource base: `https://oauth.reddit.com`

The source code expects the user to provide:

- a Reddit **installed app** client ID
- a matching redirect URI
- a compliant Reddit `User-Agent`

The default redirect URI in the sample is:

`redditshelf://auth`

## Requested scopes

The app currently requests:

- `identity`
- `history`
- `save`
- `read`

These are intentionally limited to the features implemented.

## Tech stack

- Kotlin
- Jetpack Compose
- DataStore Preferences
- OkHttp
- Kotlinx Serialization
- Android Custom Tabs

## Project structure

- `MainActivity.kt` — entry point and OAuth redirect handling
- `MainViewModel.kt` — app state, OAuth flow, paging, save actions
- `RedditApi.kt` — token exchange, refresh, profile fetch, saved-item fetch
- `AppStorage.kt` — local persistence for config, tokens, folders, notes, tags
- `MainScreen.kt` — Compose UI for config, login, saved list, organization dialogs

## Setup

1. Create and obtain approval for a Reddit installed application.
2. In Reddit app settings, register the redirect URI used by this app.
3. Open the project in Android Studio.
4. Run the app on a device or emulator.
5. Enter your client ID, redirect URI, and Reddit-compliant `User-Agent` in the configuration screen.
6. Tap **Connect Reddit**.

## Important implementation notes

- Installed apps do not keep a client secret in source code.
- Token exchange uses HTTP Basic auth with `client_id` and an empty password for the installed-app flow.
- Organizational metadata (folders, tags, notes, item statuses) is private and local to the device.
- This sample is intentionally narrow and non-commercial.

## Reviewer-facing summary

This code accesses Reddit only after explicit user OAuth authorization. It reads the authenticated user's own saved content and allows the user to organize that content privately for later reference. It does not perform bulk extraction, external redistribution, ad targeting, model training, or engagement automation.
