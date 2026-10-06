package com.liam.kaptalismusaufhalter.security

/**
 * Turns "what's enabled on the device right now" plus "what we remembered last time" into the
 * list of things worth warning the user about. Pure on purpose - the warning logic is the part
 * that must not be wrong, and it can be tested without a device.
 */
object GuardEvaluator {

    /**
     * @param failedKinds kinds whose scan threw an error this time. Their previous state is kept
     *   untouched instead of being treated as "everything disappeared" - otherwise one flaky
     *   system call would make every component look brand new (and alert) on the next scan.
     */
    fun evaluate(
        current: List<SecurityCandidate>,
        state: GuardState,
        ownPackage: String,
        failedKinds: Set<ComponentKind> = emptySet()
    ): GuardResult {
        val relevant = current.filter { it.packageName != ownPackage }
        val currentKeys = relevant.map { it.key }.toSet()
        val keptFromFailed = { keys: Set<String> -> keys.filter { kindOf(it) in failedKinds } }

        val alerts = mutableListOf<GuardAlert>()

        if (!state.initialized) {
            // First ever scan: whatever is enabled today is the baseline the user lives with - only
            // call out what is clearly dangerous, once.
            relevant.forEach { candidate ->
                if (candidate.key in state.trustedKeys) return@forEach
                val assessment = RiskAssessor.assess(candidate)
                if (assessment.level == RiskLevel.HIGH) {
                    alerts += GuardAlert(candidate, assessment, AlertReason.HIGH_RISK_EXISTING)
                }
            }
        } else {
            relevant.forEach { candidate ->
                if (candidate.key in state.trustedKeys) return@forEach
                val assessment = RiskAssessor.assess(candidate)
                val isNew = candidate.key !in state.knownKeys
                when {
                    isNew -> alerts += GuardAlert(candidate, assessment, AlertReason.NEW_COMPONENT)
                    assessment.level == RiskLevel.HIGH && candidate.key !in state.alertedKeys ->
                        alerts += GuardAlert(candidate, assessment, AlertReason.HIGH_RISK_EXISTING)
                }
            }
        }

        val alertedNow = alerts.map { it.candidate.key }.toSet()
        val newState = state.copy(
            initialized = true,
            knownKeys = currentKeys + keptFromFailed(state.knownKeys),
            // Forget alerts for things that are gone, so turning one off and on again alerts again.
            alertedKeys = (state.alertedKeys.filter { it in currentKeys }.toSet() +
                keptFromFailed(state.alertedKeys) + alertedNow)
        )
        return GuardResult(alerts, newState)
    }

    private fun kindOf(key: String): ComponentKind? =
        runCatching { ComponentKind.valueOf(key.substringBefore(':')) }.getOrNull()
}
