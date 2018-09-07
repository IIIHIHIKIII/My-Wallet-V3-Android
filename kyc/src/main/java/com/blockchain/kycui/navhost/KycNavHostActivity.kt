package com.blockchain.kycui.navhost

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.Fragment
import android.view.animation.DecelerateInterpolator
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.kycui.complete.ApplicationCompleteFragment
import com.blockchain.kycui.navhost.models.KycStep
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.kyc.R
import kotlinx.android.synthetic.main.activity_kyc_nav_host.nav_host as navHostFragment
import kotlinx.android.synthetic.main.activity_kyc_nav_host.progress_bar_kyc as progressIndicator
import kotlinx.android.synthetic.main.activity_kyc_nav_host.toolbar_kyc as toolBar

class KycNavHostActivity : BaseAuthActivity(), KycProgressListener {

    private val currentFragment: Fragment?
        get() = navHostFragment.childFragmentManager.findFragmentById(R.id.nav_host)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kyc_nav_host)
        setupToolbar(toolBar, R.string.kyc_splash_title)
    }

    override fun setHostTitle(title: Int) {
        toolBar.title = getString(title)
    }

    override fun incrementProgress(kycStep: KycStep) {
        val progress =
            100 * (
                KycStep.values()
                    .takeWhile { it != kycStep }
                    .sumBy { it.relativeValue } + kycStep.relativeValue
                ) / KycStep.values().sumBy { it.relativeValue }

        updateProgressBar(progress)
    }

    override fun decrementProgress(kycStep: KycStep) {
        val progress =
            100 * KycStep.values()
                .takeWhile { it != kycStep }
                .sumBy { it.relativeValue } / KycStep.values().sumBy { it.relativeValue }

        updateProgressBar(progress)
    }

    private fun updateProgressBar(progress: Int) {
        ObjectAnimator.ofInt(progressIndicator, "progress", progress).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        supportFragmentManager.fragments.forEach { fragment ->
            fragment.childFragmentManager.fragments.forEach {
                it.onActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        // If on final page, close host Activity on navigate up
        if (currentFragment is ApplicationCompleteFragment ||
            // If navigating up unsuccessful, close host Activity
            !findNavController(navHostFragment).navigateUp()

        ) {
            finish()
        }
    }

    override fun startLogoutTimer() = Unit

    companion object {

        fun start(context: Context) {
            Intent(context, KycNavHostActivity::class.java)
                .run { context.startActivity(this) }
        }
    }
}

interface KycProgressListener {

    fun setHostTitle(@StringRes title: Int)

    fun incrementProgress(kycStep: KycStep)

    fun decrementProgress(kycStep: KycStep)
}