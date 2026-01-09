package com.tradeflow.core.data.repository

import com.tradeflow.core.data.local.dao.PortfolioDao
import com.tradeflow.core.data.local.entity.PortfolioSnapshotEntity
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

class PortfolioRepositoryImpl @Inject constructor(
    private val portfolioDao: PortfolioDao
) {

    suspend fun getHighWaterMark(): BigDecimal {
        return portfolioDao.getHighWaterMark()?.toBigDecimal() ?: BigDecimal.ZERO
    }

    suspend fun updateHighWaterMark(currentEquity: BigDecimal) {
        val hwm = getHighWaterMark()
        if (currentEquity > hwm) {
            portfolioDao.insertSnapshot(
                PortfolioSnapshotEntity(
                    totalEquityUsd = currentEquity.toString(),
                    cashUsd = "0",
                    btcValue = "0",
                    highWaterMark = currentEquity.toString(),
                    drawdownPercent = 0.0,
                    regime = "UNKNOWN",
                    timestamp = Instant.now().toEpochMilli()
                )
            )
        }
    }
}
