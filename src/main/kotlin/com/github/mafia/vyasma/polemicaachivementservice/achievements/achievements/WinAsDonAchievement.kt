package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object WinAsDonAchievement : Achievement {
    override val id = "winAsDon"
    override val name = "Дон победитель"
    override val description = "Выиграйте за дона"
    override val category = AchievementCategory.BLACK
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Position): Int = boolToInt(
        game.getRole(position) == Role.DON &&
            game.result == PolemicaGameResult.BLACK_WIN
    )
}
