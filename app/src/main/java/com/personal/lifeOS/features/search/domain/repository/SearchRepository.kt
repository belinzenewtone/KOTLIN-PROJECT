package com.personal.lifeOS.features.search.domain.repository

import com.personal.lifeOS.features.search.domain.model.SearchResult

interface SearchRepository {
    suspend fun search(
        query: String,
        limitPerSource: Int = 25,
    ): List<SearchResult>
}
