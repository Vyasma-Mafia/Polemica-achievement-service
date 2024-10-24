package com.github.mafia.vyasma.polemicaachivementservice.model.game

import org.hibernate.validator.constraints.Range

data class PolemicaVote(
    val day: Int,
    val num: Int,
    @Range(min = 1, max = 10)
    val voter: Int,
    @Range(min = 1, max = 10)
    val candidate: Int
)
