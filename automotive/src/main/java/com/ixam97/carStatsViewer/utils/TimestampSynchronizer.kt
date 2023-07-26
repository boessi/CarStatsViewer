package com.ixam97.carStatsViewer.utils

class TimestampSynchronizer {
    companion object {
        private var nanoTime = System.nanoTime()
        private var milliTime = System.currentTimeMillis()

        fun reset() {
            nanoTime = System.nanoTime()
            milliTime = System.currentTimeMillis()
        }

        fun getSystemTimeFromNanosTimestamp(timestamp: Long): Long {
            if (nanoTime > timestamp) {
                InAppLogger.e("nanoTime out of sync")
                reset()
            }

            val nanosDelta = (timestamp - nanoTime) / 1_000_000
            return milliTime + nanosDelta
        }
    }
}