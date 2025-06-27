package com.github.mafia.vyasma.polemicaachivementservice.rating

import com.pocketcombats.openskill.RatingModelConfig
import com.pocketcombats.openskill.aggregate.TeamRatingAggregator
import com.pocketcombats.openskill.data.MatchMakingRating
import com.pocketcombats.openskill.data.SimpleMatchMakingRating
import kotlin.math.sqrt

class AverageTeamRatingAggregator(config: RatingModelConfig) : TeamRatingAggregator {
    private val tauSquared = config.tau * config.tau

    override fun computeTeamRating(playerRatings: Collection<MatchMakingRating>): MatchMakingRating {
        return SimpleMatchMakingRating(
            playerRatings.stream().mapToDouble { obj: MatchMakingRating -> obj.mu() }.average().asDouble,
            sqrt(playerRatings.stream().mapToDouble { playerRating: MatchMakingRating ->
                adjustedSigmaSquared(playerRating)
            }.average().asDouble)
        )
    }

    fun adjustedSigmaSquared(playerRating: MatchMakingRating): Double {
        return (playerRating.sigma() * playerRating.sigma()) + tauSquared
    }
}
