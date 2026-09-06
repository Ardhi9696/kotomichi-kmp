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
import org.koin.core.module.Module
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.property.inject

val sharedModule = module {
    single { FsrsCalculator() }
    single { BktCalculator() }
    
    factory { (vocabRepo: com.kotomichi.repository.VocabRepository, progressRepo: com.kotomichi.repository.ProgressRepository, deckRepo: com.kotomichi.repository.DeckRepository) ->
        LearnCardUseCase(vocabRepo, progressRepo, deckRepo)
    }
    
    factory { (progressRepo: com.kotomichi.repository.ProgressRepository) ->
        ReviewCardUseCase(progressRepo)
    }
    
    factory { (deckRepo: com.kotomichi.repository.DeckRepository, progressRepo: com.kotomichi.repository.ProgressRepository, vocabRepo: com.kotomichi.repository.VocabRepository) ->
        DeckProgressUseCase(deckRepo, progressRepo, vocabRepo)
    }
    
    factory { (progressRepo: com.kotomichi.repository.ProgressRepository) ->
        StatisticsUseCase(progressRepo)
    }
    
    factory { (authRepo: com.kotomichi.repository.AuthRepository) ->
        AuthUseCase(authRepo)
    }
    
    factory { (progressRepo: com.kotomichi.repository.ProgressRepository) ->
        GamificationUseCase(progressRepo)
    }
    
    factory { (syncRepo: com.kotomichi.repository.SyncRepository) ->
        SyncDataUseCase(syncRepo)
    }
}

fun startKoinShared(modules: List<Module> = listOf(sharedModule)) {
    startKoin { modules(modules) }
}

fun stopKoinShared() {
    stopKoin()
}

inline fun <reified T> get(): T = org.koin.core.koin.get()