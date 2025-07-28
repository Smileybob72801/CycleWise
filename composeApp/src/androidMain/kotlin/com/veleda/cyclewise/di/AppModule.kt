package com.veleda.cyclewise.di

import com.veleda.cyclewise.androidData.repository.InMemoryCycleRepository
import com.veleda.cyclewise.domain.repository.CycleRepository
import com.veleda.cyclewise.domain.usecases.StartNewCycleUseCase
import com.veleda.cyclewise.ui.tracker.CycleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<CycleRepository> { InMemoryCycleRepository() }
    single { StartNewCycleUseCase(get()) }

    viewModel { CycleViewModel(get(), get()) }
}
