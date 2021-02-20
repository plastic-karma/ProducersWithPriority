# ProducersWithPriority
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.3-blue.svg)](https://kotlinlang.org/docs/reference/whatsnew13.html) 
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
[![Build Status](https://api.travis-ci.com/plastic-karma/ProducersWithPriority.svg?branch=mainline)](https://travis-ci.com/github/plastic-karma/ProducersWithPriority)
[![Maintainability](https://codeclimate.com/github/plastic-karma/ProducersWithPriority/badges/gpa.svg
)](https://codeclimate.com/github/plastic-karma/ProducersWithPriority/maintainability)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/542dbabd278e4f0d822908c18b2ebb4a)](https://app.codacy.com/gh/plastic-karma/ProducersWithPriority?utm_source=github.com&utm_medium=referral&utm_content=plastic-karma/ProducersWithPriority&utm_campaign=Badge_Grade)


Library to prioritize streams of data. If you have producers of data, that produce the same data, but you want to consume data from some producers sooner or more often than from others, this library let's you do that.

## Shares
The simplest way of prioritizing producers is to assign shares. In the following example, producer A is invoked 60% of the time, producer B 15% and producer C 5%.
```kotlin
Scheduler().schedule(
    producers = listOf(
        PriorityConfiguration(shares = 60.0) to producer("A"),
        PriorityConfiguration(shares = 15.0) to producer("B"),
        PriorityConfiguration(shares =  5.0) to producer("C")
    )
)
```

### Fair share
You can use `fair` to create an equal amount of shares between producers:
```kotlin
Schedulers.fair(
  producer("A"),
  producer("B"),
  producer("C"),
  producer("D")
)
```

