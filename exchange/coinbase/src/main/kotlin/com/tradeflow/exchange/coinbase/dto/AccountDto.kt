package com.tradeflow.exchange.coinbase.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountsResponseDto(
    val accounts: List<AccountDto>,
    val has_next: Boolean,
    val cursor: String?,
    val size: Int
)

@Serializable
data class AccountDto(
    val uuid: String,
    val name: String,
    val currency: String,
    val available_balance: AvailableBalanceDto,
    val default: Boolean,
    val active: Boolean,
    val created_at: String,
    val updated_at: String,
    val deleted_at: String?,
    val type: String,
    val ready: Boolean,
    val hold: AvailableBalanceDto
)

@Serializable
data class AvailableBalanceDto(
    val value: String,
    val currency: String
)
