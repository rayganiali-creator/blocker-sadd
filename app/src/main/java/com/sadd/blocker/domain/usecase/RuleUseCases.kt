package com.sadd.blocker.domain.usecase

import com.sadd.blocker.core.KeywordMatcher
import com.sadd.blocker.core.PersianNormalizer
import com.sadd.blocker.domain.RuleValidation
import com.sadd.blocker.domain.model.BlockedApp
import com.sadd.blocker.domain.model.BlockedWebsite
import com.sadd.blocker.domain.model.KeywordRule
import com.sadd.blocker.domain.repository.BlockedAppRepository
import com.sadd.blocker.domain.repository.KeywordRepository
import com.sadd.blocker.domain.repository.WebsiteRepository

/**
 * UseCaseهای قوانین — خالص Kotlin، تست‌پذیر با Repository جعلی.
 * اعتبارسنجی و جلوگیری از تکراری اینجا انجام می‌شود، نه در UI.
 */
sealed class AddKeywordResult {
    data class Added(val rule: KeywordRule) : AddKeywordResult()
    object Duplicate : AddKeywordResult()
    data class Invalid(val reason: String) : AddKeywordResult()
}

class AddKeywordRuleUseCase(private val repo: KeywordRepository) {
    suspend fun execute(rawText: String, category: String): AddKeywordResult {
        val check = RuleValidation.validateKeyword(rawText)
        if (!check.ok) {
            return AddKeywordResult.Invalid(check.error ?: "ورودی نامعتبر است.")
        }
        val normalized = PersianNormalizer.normalize(rawText)
        if (repo.countByNormalized(normalized) > 0) {
            return AddKeywordResult.Duplicate
        }
        val rule = KeywordRule(
            rawText = rawText.trim(),
            normalizedText = normalized,
            category = category.ifBlank { "عمومی" },
            isPhrase = RuleValidation.isPhrase(normalized),
        )
        val id = repo.add(rule)
        return AddKeywordResult.Added(rule.copy(id = id))
    }
}

sealed class AddWebsiteResult {
    data class Added(val site: BlockedWebsite) : AddWebsiteResult()
    object Duplicate : AddWebsiteResult()
    data class Invalid(val reason: String) : AddWebsiteResult()
}

class AddWebsiteUseCase(private val repo: WebsiteRepository) {
    suspend fun execute(rawHost: String, matchSubdomains: Boolean): AddWebsiteResult {
        val check = RuleValidation.validateWebsite(rawHost)
        if (!check.ok) {
            return AddWebsiteResult.Invalid(check.error ?: "ورودی نامعتبر است.")
        }
        val host = KeywordMatcher.normalizeHost(rawHost)
        if (repo.findByHost(host) != null) {
            return AddWebsiteResult.Duplicate
        }
        val site = BlockedWebsite(host = host, matchSubdomains = matchSubdomains)
        val id = repo.add(site)
        return AddWebsiteResult.Added(site.copy(id = id))
    }
}

class AddBlockedAppUseCase(private val repo: BlockedAppRepository) {
    suspend fun execute(packageName: String, label: String) {
        if (packageName.isBlank()) return
        repo.add(BlockedApp(packageName = packageName, label = label.ifBlank { packageName }))
    }

    suspend fun executeAll(apps: List<Pair<String, String>>) {
        for ((pkg, label) in apps) {
            execute(pkg, label)
        }
    }
}
