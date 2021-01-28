package com.plastickarma.producerswithpriority

fun fixedEpochs(epochs: Int): EpochGenerator {
    var counter = 0
    return { counter++ < epochs }
}
