package com.satwik.aimemory.data.mock

import com.satwik.aimemory.data.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Mock data provider for Phase 1 UI development.
 * Will be replaced by real data sources in later phases.
 */
object MockData {

    val recentSummary = MemorySummary(
        id = "mock-1",
        timestamp = LocalDateTime.now().minusMinutes(15),
        topics = listOf("Project architecture", "API design"),
        decisions = listOf("Use Jetpack Compose for UI"),
        tasks = listOf("Set up Gradle configuration"),
        emotionalTone = "Focused",
        rawSnippet = "Discussed the overall architecture of the AI memory engine and decided on the technology stack..."
    )

    val hourlySummaries = listOf(
        MemorySummary(
            id = "mock-2",
            timestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 0)),
            topics = listOf("Morning standup", "Sprint planning"),
            decisions = listOf("Prioritize memory timeline feature"),
            tasks = listOf("Create timeline UI", "Review API contracts"),
            emotionalTone = "Productive"
        ),
        MemorySummary(
            id = "mock-3",
            timestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(11, 0)),
            topics = listOf("Code review", "Performance optimization"),
            decisions = listOf("Optimize LazyColumn rendering"),
            tasks = listOf("Profile animation performance"),
            emotionalTone = "Analytical"
        ),
        MemorySummary(
            id = "mock-4",
            timestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0)),
            topics = listOf("Design discussion", "UX feedback"),
            decisions = listOf("Use breathing orb for idle state"),
            tasks = listOf("Implement orb animation"),
            emotionalTone = "Creative",
            isPinned = true
        )
    )

    val dailySummaries = listOf(
        DailySummary(
            date = LocalDate.now(),
            hourlySummaries = hourlySummaries
        ),
        DailySummary(
            date = LocalDate.now().minusDays(1),
            hourlySummaries = listOf(
                MemorySummary(
                    id = "mock-5",
                    timestamp = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(9, 0)),
                    topics = listOf("Project kickoff", "Requirements gathering"),
                    decisions = listOf("Build privacy-first memory engine"),
                    tasks = listOf("Draft system architecture"),
                    emotionalTone = "Excited"
                ),
                MemorySummary(
                    id = "mock-6",
                    timestamp = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(15, 0)),
                    topics = listOf("Backend planning", "Sarvam AI integration"),
                    decisions = listOf("Use OpenClaw as orchestration proxy"),
                    tasks = listOf("Define API endpoints", "Set up VPS"),
                    emotionalTone = "Determined"
                )
            )
        ),
        DailySummary(
            date = LocalDate.now().minusDays(3),
            hourlySummaries = listOf(
                MemorySummary(
                    id = "mock-7",
                    timestamp = LocalDateTime.of(LocalDate.now().minusDays(3), LocalTime.of(11, 0)),
                    topics = listOf("Brainstorming session"),
                    decisions = listOf("Focus on passive listening"),
                    tasks = listOf("Research VAD libraries"),
                    emotionalTone = "Curious",
                    isPinned = true
                )
            )
        )
    )

    val mockQueryResult = QueryResult(
        query = "What decisions did we make about the architecture?",
        answer = "Based on your conversations, the key architectural decisions were: (1) Use Jetpack Compose for the UI to handle complex state-driven animations, (2) Deploy OpenClaw as a self-hosted orchestration proxy to keep API credentials server-side, and (3) Store all persistent memory artifacts in the user's personal Google Drive for privacy.",
        sourceReferences = listOf(
            SourceReference(
                summaryId = "mock-5",
                date = LocalDate.now().minusDays(1),
                hourLabel = "9:00 AM",
                relevanceScore = 0.92f
            ),
            SourceReference(
                summaryId = "mock-6",
                date = LocalDate.now().minusDays(1),
                hourLabel = "3:00 PM",
                relevanceScore = 0.87f
            )
        )
    )
}
