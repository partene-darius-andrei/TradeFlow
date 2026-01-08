package com.tradeflow.exchange.coinbase.mapper

import com.tradeflow.core.domain.model.Balance
import com.tradeflow.exchange.coinbase.dto.AccountDto
import java.math.BigDecimal

fun AccountDto.toDomain(): Balance {
    return Balance(
        currency = currency,
        available = BigDecimal(available_balance.value),
        hold = BigDecimal(hold.value)
    )
}
