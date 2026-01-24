package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.ProcessedTournamentId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ProcessedTournamentIdRepository : JpaRepository<ProcessedTournamentId, Long>
