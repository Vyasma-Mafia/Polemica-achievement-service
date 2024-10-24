package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.hibernate.validator.constraints.Range

@JsonIgnoreProperties(ignoreUnknown = true)
data class PolemicaPlayer(
    @Range(min = 1, max = 10)
    val position: Int,
    val username: String,
    val role: Role,
    @Range(min = 0, max = 2)
    val techs: Int,
    @Range(min = 0, max = 4)
    val fouls: Int,
    val player: Long
) {

}
