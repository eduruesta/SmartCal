package com.smartcal.app.di

import com.smartcal.app.repository.CalendarRepository
import com.smartcal.app.repository.CalendarRepositoryImpl
import com.smartcal.app.services.AuthService
import com.smartcal.app.services.CalendarService
import com.smartcal.app.storage.SessionStorage
import com.smartcal.app.viewmodel.AuthViewModel
import com.smartcal.app.viewmodel.CalendarViewModel
import com.smartcal.app.data.subscription.RevenueCatManager
import com.smartcal.app.data.subscription.RevenueCatManagerImpl
import com.smartcal.app.presentation.subscription.RevenueCatPaywallViewModel
import com.smartcal.app.reminders.reminderModule
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Platform Context (will be provided by platform-specific setup)
    
    // Services
    single { AuthService(get()) }
    single { CalendarService() }
    
    // Storage
    single { SessionStorage }
    
    // Repository
    single<CalendarRepository> { CalendarRepositoryImpl() }
    
    // RevenueCat
    single<RevenueCatManager> { RevenueCatManagerImpl() }
    
    // ViewModels
    viewModel<AuthViewModel> { AuthViewModel(get(), get(), get(), get()) }
    viewModel<CalendarViewModel> { CalendarViewModel(get(), get()) }
    viewModel<RevenueCatPaywallViewModel> { RevenueCatPaywallViewModel(get()) }
    
    // Include reminder module
    includes(reminderModule)
}