# Libraries kept in this repository

The app needs these exact library versions, and no public Maven repository serves them any more
(jcenter is closed, and they did not move to Maven Central or Google's repository; the last two were
built only on demand by jitpack). So they are kept here as a small local Maven repository, which
`build.gradle` lists first. Without it the app builds only on a machine whose Gradle cache still
has them.

- The `.jar` and `.aar` files are **unchanged** copies of the files the app has always been built
  with, taken from a Gradle cache. Their SHA-1 sums are below.
- The `.pom` files are written for this repository: the originals are not published anywhere. They
  declare the same dependencies as before (read from Gradle's cached descriptors).
- Each library's licence text sits next to it.

| library | licence | SHA-1 of the `.jar` / `.aar` |
|---|---|---|
| `ru.terrakok.cicerone:cicerone:3.0.0` | MIT | `f39081f2de6c89346f5666c8c9d4b5dd4f7caa6a` |
| `com.google.android:flexbox:1.1.0` | Apache-2.0 | `862311323db1506380ff7a8ce7e9f7685de4d9e5` |
| `com.infideap.drawerbehavior:drawer-behavior:0.0.5` | MIT | `e568a131b90095df6da614aa55898facda70b6a3` |
| `ru.superjob:kotlin-permissions:1.0.3` | Apache-2.0 | `a0760a81d5210d6c893d6793ac83fcf667a004f4` |
| `com.google.android.exoplayer:exoplayer-core:2.9.6` | Apache-2.0 | `bbb7472fb95321e19af70d1237ce03cc3e7c40e0` |
| `com.google.android.exoplayer:exoplayer-dash:2.9.6` | Apache-2.0 | `7cb4c8f6d59d727ce66904afb52a834e1a3b357a` |
| `com.google.android.exoplayer:exoplayer-hls:2.9.6` | Apache-2.0 | `74fd7ed27188bb0e05a26c2f621686d6a9b8deb4` |
| `com.google.android.exoplayer:exoplayer-smoothstreaming:2.9.6` | Apache-2.0 | `651398750b6092b6f7fc77ab356aafa42214f96b` |
| `com.google.android.exoplayer:exoplayer-ui:2.9.6` | Apache-2.0 | `d4027a7c3f3cb2882163cd3db29aeea65ae8068d` |
| `com.google.android.exoplayer:extension-mediasession:2.9.6` | Apache-2.0 | `9d9c4296ff9bcd64d3da25f34f01a2bd3df2cc9d` |
| `com.google.android.exoplayer:exoplayer:2.9.6` | Apache-2.0 | `93d5f36b2557c5a8194246f9953994557c218378` |
| `com.github.ybq:Android-SpinKit:1.1.0` | MIT | `9f57aa5fd0cc677ac43e47021a3b70a695807ce7` |
| `com.github.ome450901:SimpleRatingBar:1.4.2` | MIT | `948f0993d65ee2b857d7b5bdee92cee8d53e63b0` |

Licences and sources:

- Cicerone, https://github.com/terrakok/Cicerone - MIT (`ru/terrakok/cicerone/cicerone/LICENSE`)
- FlexboxLayout, https://github.com/google/flexbox-layout - Apache-2.0 (`com/google/android/flexbox/LICENSE`)
- Drawer-Behavior, https://github.com/shiburagi/Drawer-Behavior - MIT (`com/infideap/drawerbehavior/drawer-behavior/LICENSE`)
- kotlin-permissions, https://github.com/superjobru/kotlin-permissions - its README: "Copyright (C) 2018 ... Licensed under the Apache License, Version 2.0"; the repository has no licence file of its own - Apache-2.0 (`ru/superjob/kotlin-permissions/LICENSE`)
- ExoPlayer, https://github.com/google/ExoPlayer (all seven modules) - Apache-2.0 (`com/google/android/exoplayer/LICENSE`)
- Android-SpinKit, https://github.com/ybq/Android-SpinKit - MIT (`com/github/ybq/Android-SpinKit/LICENSE`)
- SimpleRatingBar, https://github.com/williamyyu/SimpleRatingBar (formerly ome450901/SimpleRatingBar) - MIT (`com/github/ome450901/SimpleRatingBar/LICENSE`)
