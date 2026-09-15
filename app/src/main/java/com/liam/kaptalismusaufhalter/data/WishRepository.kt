package com.liam.kaptalismusaufhalter.data

import android.content.Context
import com.liam.kaptalismusaufhalter.domain.calcWaitHours
import com.liam.kaptalismusaufhalter.work.WishUnlockWorker

class WishRepository(
    private val context: Context,
    private val wishDao: WishDao,
    private val settingsDao: SettingsDao
) {
    suspend fun createWish(name: String, price: Double, linkUrl: String? = null, imageUrl: String? = null): Long {
        val settings = settingsDao.get() ?: Settings()
        val waitHours = calcWaitHours(price, settings.waitTimeConfig.toWaitTiers(), settings.strictnessEnum.factor)
        val now = System.currentTimeMillis()
        val unlockAt = now + waitHours * 3_600_000L

        val wish = Wish(
            name = name,
            price = price,
            imageUrl = imageUrl,
            linkUrl = linkUrl,
            createdAt = now,
            unlockAt = unlockAt,
            status = WishStatus.PENDING
        )
        val id = wishDao.insert(wish)
        WishUnlockWorker.schedule(context, id, unlockAt)
        return id
    }
}
