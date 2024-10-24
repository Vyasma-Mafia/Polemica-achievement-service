package com.github.mafia.vyasma.polemicaachivementservice.model.game

import org.hibernate.validator.constraints.Range

data class PolemicaShot(
    val night: Int,
    @Range(min = 1, max = 10)
    val shooter: Int,
    @Range(min = 1, max = 10)
    val victim: Int
) {
}
