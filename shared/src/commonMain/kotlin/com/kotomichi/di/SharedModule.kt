package com.kotomichi.di

import com.kotomichi.fsrs.FsrsCalculator
import com.kotomichi.bkt.BktCalculator
import com.kotomichi.usecase.LearnCardUseCase
import com.kotomichi.usecase.ReviewCardUseCase
import com.kotomichi.usecase.DeckProgressUseCase
import com.kotomichi.usecase.StatisticsUseCase
import com.kotomichi.usecase.AuthUseCase
import com.kotomichi.usecase.GamificationUseCase
import com.kotomichi.usecase.SyncDataUseCase
import com.kotomichi.usecase.BelajarQuizUseCase
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
            get<com.kotomichi.repository.VocabRepository>(),
            get<com.kotomichi.repository.ProgressRepository>(),
            get<com.kotomichi.repository.DeckRepository>()
        )
    }
    
    factory {
        ReviewCardUseCase(get<com.kotomichi.repository.ProgressRepository>())
    }
    
    factory {
        DeckProgressUseCase(
            get<com.kotomichi.repository.DeckRepository>(),
            get<com.kotomichi.repository.ProgressRepository>(),
            get<com.kotomichi.repository.VocabRepository>()
        )
    }
    
    factory {
        StatisticsUseCase(get<com.kotomichi.repository.ProgressRepository>())
    }
    
    factory {
        AuthUseCase(get<com.kotomichi.repository.AuthRepository>())
    }
    
    factory {
        GamificationUseCase(get<com.kotomichi.repository.ProgressRepository>())
    }
    
    factory {
        SyncDataUseCase(get<com.kotomichi.repository.SyncRepository>())
    }

    factory {
        BelajarQuizUseCase(
            get<com.kotomichi.repository.VocabRepository>(),
            get<com.kotomichi.repository.ProgressRepository>(),
            get<com.kotomichi.repository.AuthRepository>(),
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