package com.personal.lifeOS.features.calendar.data.datasource

import com.personal.lifeOS.features.calendar.data.repository.CalendarRepositoryImpl
import com.personal.lifeOS.features.calendar.domain.repository.CalendarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {
    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository
}
