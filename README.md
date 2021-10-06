# ProducersWithPriority
[![Kotlin version badge](https://img.shields.io/badge/kotlin-1.5-blue.svg)](https://kotlinlang.org/docs/whatsnew15.html) 
![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
[![Build Status](https://api.travis-ci.com/plastic-karma/ProducersWithPriority.svg?branch=mainline)](https://travis-ci.com/github/plastic-karma/ProducersWithPriority)
[![Maintainability](https://codeclimate.com/github/plastic-karma/ProducersWithPriority/badges/gpa.svg
)](https://codeclimate.com/github/plastic-karma/ProducersWithPriority/maintainability)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/542dbabd278e4f0d822908c18b2ebb4a)](https://app.codacy.com/gh/plastic-karma/ProducersWithPriority?utm_source=github.com&utm_medium=referral&utm_content=plastic-karma/ProducersWithPriority&utm_campaign=Badge_Grade)


Library to prioritize streams of data. If you have producers of data, that produce the same data, but you want to consume data from some producers sooner or more often than from others, this library let's you do that.

## Shares
The simplest way of prioritizing producers is to assign shares. In the following example, producer A is invoked 70% of the time, producer B 15% and producer C 5%.
```kotlin
Scheduler().schedule(
    producers = listOf(
        PriorityConfiguration(shares = 70.0) to producer("A"),
        PriorityConfiguration(shares = 15.0) to producer("B"),
        PriorityConfiguration(shares =  5.0) to producer("C")
    )
)
```

### Penalties
You can define penalties for producers, when they produce `null`. This is useful if you are reading from different queues and the queue is empty for a while. In the following example, the share distribution is initially 15% (15 shares), 15% (15 shares), 70% (70 shares). When producer C returns `null` it's shares will drop by 40 to 30 and the new distribution is 25% (15 shares), 25% (15 shares), 50% (30 shares). Once producer C returned a non-null value, the shares are restored to their original distribution.

```kotlin
Scheduler().schedule(
    producers = listOf(
        PriorityConfiguration(shares = 15.0) to producer("A"),
        PriorityConfiguration(shares = 15.0) to producer("B"),
        PriorityConfiguration(shares = 70.0, possiblePenalty = 40.0) to producer("C"),
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

## Round Robin
You can also create a round-robin by setting it as a work strategy:
```kotlin
Scheduler().schedule(
    producers = ...
    strategy = WorkStrategy.ROUND_ROBIN
)
```

## Producers
 The libraries main abstraction to retrieve data is [Producer](https://github.com/plastic-karma/ProducersWithPriority/blob/mainline/src/commonMain/kotlin/com/plastickarma/producerswithpriority/Producer.kt) interface.

### flatten
If your data source is providing data in batches, you can use the [flatten](https://github.com/plastic-karma/ProducersWithPriority/blob/mainline/src/commonMain/kotlin/com/plastickarma/producerswithpriority/ProducerExtensions.kt#L12) function to be able to treat each element of the batch as a single entity when determining priority.

```kotlin
val batchProducer: Producer<Set<MyObject>> = ...// e.g. read from queue
Scheduler().schedule(producers = batchProducer.flatten(), ...)
```





