package com.liam.kaptalismusaufhalter.security

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.core.app.NotificationManagerCompat

/**
 * Lists the components on this device that can see or capture what the user does: enabled
 * accessibility services (the #1 way screen-stealing trojans work), notification listeners (read
 * one-time codes), enabled keyboards (see everything typed) and device admins. Uses only public
 * APIs; package visibility comes from the `<queries>` intents in the manifest, so no
 * QUERY_ALL_PACKAGES permission is needed.
 */
object SecurityScanner {

    data class Outcome(val candidates: List<SecurityCandidate>, val failedKinds: Set<ComponentKind>)

    fun scan(context: Context): Outcome {
        val app = context.applicationContext
        val candidates = mutableListOf<SecurityCandidate>()
        val failed = mutableSetOf<ComponentKind>()

        guarded(ComponentKind.ACCESSIBILITY_SERVICE, failed) { candidates += accessibilityServices(app) }
        guarded(ComponentKind.NOTIFICATION_LISTENER, failed) { candidates += notificationListeners(app) }
        guarded(ComponentKind.INPUT_METHOD, failed) { candidates += inputMethods(app) }
        guarded(ComponentKind.DEVICE_ADMIN, failed) { candidates += deviceAdmins(app) }

        return Outcome(candidates, failed)
    }

    private inline fun guarded(kind: ComponentKind, failed: MutableSet<ComponentKind>, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            // OEM quirks / revoked access: don't let one source take the whole scan (or a worker) down.
            failed += kind
        }
    }

    private fun accessibilityServices(app: Context): List<SecurityCandidate> {
        val pm = app.packageManager
        val manager = app.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        // null means the list could not be read - that must count as a failed scan, not as "nothing enabled".
        val enabled = manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?: throw IllegalStateException("accessibility service list unavailable")
        return enabled
            .mapNotNull { info ->
                val service = info.resolveInfo?.serviceInfo ?: return@mapNotNull null
                SecurityCandidate(
                    kind = ComponentKind.ACCESSIBILITY_SERVICE,
                    componentId = ComponentName(service.packageName, service.name).flattenToShortString(),
                    packageName = service.packageName,
                    label = info.resolveInfo.loadLabel(pm).toString(),
                    isSystemApp = isSystem(service.applicationInfo),
                    installer = installerOf(pm, service.packageName),
                    capabilities = decodeAccessibilityCapabilities(info.capabilities)
                )
            }
    }

    private fun notificationListeners(app: Context): List<SecurityCandidate> {
        val pm = app.packageManager
        return NotificationManagerCompat.getEnabledListenerPackages(app).mapNotNull { pkg ->
            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            }
            SecurityCandidate(
                kind = ComponentKind.NOTIFICATION_LISTENER,
                componentId = pkg,
                packageName = pkg,
                label = appInfo.loadLabel(pm).toString(),
                isSystemApp = isSystem(appInfo),
                installer = installerOf(pm, pkg),
                capabilities = setOf(Capability.READ_NOTIFICATIONS)
            )
        }
    }

    private fun inputMethods(app: Context): List<SecurityCandidate> {
        val pm = app.packageManager
        val manager = app.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return manager.enabledInputMethodList.map { method ->
            SecurityCandidate(
                kind = ComponentKind.INPUT_METHOD,
                componentId = method.id,
                packageName = method.packageName,
                label = method.loadLabel(pm).toString(),
                isSystemApp = isSystem(method.serviceInfo.applicationInfo),
                installer = installerOf(pm, method.packageName),
                capabilities = setOf(Capability.READ_TYPING)
            )
        }
    }

    private fun deviceAdmins(app: Context): List<SecurityCandidate> {
        val pm = app.packageManager
        val manager = app.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return manager.activeAdmins.orEmpty().mapNotNull { admin ->
            val appInfo = try {
                pm.getApplicationInfo(admin.packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                return@mapNotNull null
            }
            SecurityCandidate(
                kind = ComponentKind.DEVICE_ADMIN,
                componentId = admin.flattenToShortString(),
                packageName = admin.packageName,
                label = appInfo.loadLabel(pm).toString(),
                isSystemApp = isSystem(appInfo),
                installer = installerOf(pm, admin.packageName),
                capabilities = setOf(Capability.MANAGE_DEVICE)
            )
        }
    }

    private fun isSystem(info: ApplicationInfo?): Boolean =
        info != null && (info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0

    private fun installerOf(pm: PackageManager, packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (e: Exception) {
        null
    }
}
