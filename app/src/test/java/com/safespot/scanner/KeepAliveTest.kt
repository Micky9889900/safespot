package com.safespot.scanner

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import de.blinkt.openvpn.VpnProfile
import de.blinkt.openvpn.core.keepVPNAlive
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KeepAliveTest {
    @Test fun manifestHoldsBootPermissionAndSchedulingDoesNotCrash() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, PackageManager.GET_PERMISSIONS)
        assertTrue("RECEIVE_BOOT_COMPLETED missing", pi.requestedPermissions!!.contains(android.Manifest.permission.RECEIVE_BOOT_COMPLETED))
        keepVPNAlive.scheduleKeepVPNAliveJobService(ctx, VpnProfile("test"))
        keepVPNAlive.unscheduleKeepVPNAliveJobService(ctx)
    }
}
