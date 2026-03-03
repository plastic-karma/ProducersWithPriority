# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ProducersWithPriority** is a Kotlin multiplatform library (JVM + JavaScript) for prioritizing streams of data. It lets you consume data from multiple `Producer<T>` instances with configurable share distributions, penalties for null-returning producers, and two scheduling strategies.

## Build & Quality Commands

```bash
# Full build (assemble depends on check, so this runs all quality gates)
./gradlew clean build

# Run tests only
./gradlew jvmTest jsTest

# Run a single test class (JVM)
./gradlew jvmTest --tests "com.plastickarma.producerswithpriority.SchedulerTest"

# Static analysis (zero violations allowed)
./gradlew detekt

# Code formatting check / auto-fix
./gradlew ktlintCheck
./gradlew ktlintFormat

# Coverage verification (minimum 90% line coverage)
./gradlew koverVerify
```

**Quality gates are strict:** `assemble.dependsOn check`, so a build fails on any detekt violation, ktlint issue, or coverage below 90%. Forbidden comments: `TODO`, `FIXME`, `STOPSHIP`.

## Architecture

Single-module multiplatform project. All business logic lives in `src/commonMain/kotlin/com/plastickarma/producerswithpriority/`.

### Core abstractions

| File | Role |
|------|------|
| `Producer.kt` | Interface with a single `suspend fun next(): T?`. Must never throw — handle exceptions internally. |
| `PriorityConfiguration.kt` | Data class holding `shares: Double` and optional `possiblePenalty: Double`. |
| `Scheduler.kt` | Entry point. Takes `List<Pair<PriorityConfiguration, Producer<T>>>`, returns `Flow<T>`. |
| `WorkStrategy.kt` | `DISTRIBUTION` (random weighted) or `ROUND_ROBIN` (cyclic). |
| `Schedulers.kt` | Convenience helpers: `fair()` and `penalizeEmpty()`. |

### Scheduling flow

1. Each producer gets a `SemiOpenRange` mapped from its share percentage.
2. On each tick, the active strategy (`DistributionScheduler` or `RoundRobinScheduler`) picks a producer.
3. If the producer returns `null`, its `possiblePenalty` is subtracted from its current shares; ranges are recomputed. Shares restore on a non-null return.
4. An `EpochGenerator` (type alias for a suspend function) controls when the scheduling loop terminates.

### Key design constraints

- Code in `commonMain` must be compatible with both JVM and JS — no platform-specific APIs.
- `Producer.next()` must not throw; all exceptions are the producer's responsibility to handle.
- Max line length: 120. Max return statements per function: 2. Max cyclomatic complexity: 10.
