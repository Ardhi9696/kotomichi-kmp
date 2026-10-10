package com.kotomichi.shared.core.di

import com.kotomichi.shared.core.fsrs.FsrsCalculator
import com.kotomichi.shared.core.bkt.BktCalculator
import com.kotomichi.shared.feature.belajar.domain.usecase.LearnCardUseCase
import com.kotomichi.shared.feature.belajar.domain.usecase.ReviewCardUseCase
import com.kotomichi.shared.feature.belajar.domain.usecase.DeckProgressUseCase
import com.kotomichi.shared.feature.belajar.domain.usecase.StatisticsUseCase
import com.kotomichi.shared.feature.auth.domain.usecase.AuthUseCase
import com.kotomichi.shared.feature.gamifikasi.domain.usecase.GamificationUseCase
import com.kotomichi.shared.feature.belajar.domain.usecase.SyncDataUseCase
import com.kotomichi.shared.feature.belajar.domain.usecase.BelajarQuizUseCase
import org.koin.core.module.Module
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.mp.KoinPlatformTools

val sharedModule = module {
    single { FsrsCalculator }
    single { BktCalculator }
    
    factory {
        LearnCardUseCase(
            get<com.kotomichi.shared.core.repository.VocabRepository>(),
            get<com.kotomichi.shared.core.repository.ProgressRepository>(),
            get<com.kotomichi.shared.core.repository.DeckRepository>()
        )
    }
    
    factory {
        ReviewCardUseCase(get<com.kotomichi.shared.core.repository.ProgressRepository>())
    }
    
    factory {
        DeckProgressUseCase(
            get<com.kotomichi.shared.core.repository.DeckRepository>(),
            get<com.kotomichi.shared.core.repository.ProgressRepository>(),
            get<com.kotomichi.shared.core.repository.VocabRepository>()
        )
    }
    
    factory {
        StatisticsUseCase(get<com.kotomichi.shared.core.repository.ProgressRepository>())
    }
    
    factory {
        AuthUseCase(get<com.kotomichi.shared.core.repository.AuthRepository>())
    }
    
    factory {
        GamificationUseCase(get<com.kotomichi.shared.core.repository.ProgressRepository>())
    }
    
    factory {
        SyncDataUseCase(get<com.kotomichi.shared.core.repository.SyncRepository>())
    }

    factory {
        BelajarQuizUseCase(
            get<com.kotomichi.shared.core.repository.VocabRepository>(),
            get<com.kotomichi.shared.core.repository.ProgressRepository>(),
            get<com.kotomichi.shared.core.repository.AuthRepository>(),
            get<GamificationUseCase>()
        )
    }
}

fun startKoinShared(modules: List<Module> = listOf(sharedModule)) {
    startKoin { modules(modules) }
}

fun stopKoinShared() {
    stopKoin()
}

inline fun <reified T : Any> get(): T = KoinPlatformTools.defaultContext().get().get<T>()