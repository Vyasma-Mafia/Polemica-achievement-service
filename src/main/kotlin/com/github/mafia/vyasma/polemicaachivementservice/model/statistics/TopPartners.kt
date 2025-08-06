package com.github.mafia.vyasma.polemicaachivementservice.model.statistics

import java.time.LocalDateTime

data class TopPartners(
    val playerId: Long,
    val playerName: String,
    val bestPartners: List<PartnerStats>,
    val worstPartners: List<PartnerStats>,
    val calculatedAt: LocalDateTime,
    val minGamesThreshold: Int,
    val totalPartnersAnalyzed: Int
)

data class PartnerStats(
    val partnerId: Long,
    val partnerName: String,
    val partnerRating: Double?,
    val totalGames: Int,
    val wins: Int,
    val losses: Int,
    val winRate: Double,
    val lastGameTogether: LocalDateTime?,
    val averagePointsTogether: Double?
)