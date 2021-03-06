package piuk.blockchain.android.simplebuy

import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.models.nabu.Kyc2TierState
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Observable
import io.reactivex.Single

class SimpleBuyFlowNavigator(
    private val simpleBuyModel: SimpleBuyModel,
    private val tierService: TierService,
    private val currencyPrefs: CurrencyPrefs,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val custodialWalletManager: CustodialWalletManager
) {

    private fun stateCheck(startedFromKycResume: Boolean, startedFromDashboard: Boolean): Single<FlowScreen> =
        simpleBuyModel.state.flatMap {
            if (startedFromKycResume || it.currentScreen == FlowScreen.KYC) {
                tierService.tiers().toObservable().map { tier ->
                    when (tier.combinedState) {
                        Kyc2TierState.Tier2Approved -> FlowScreen.CHECKOUT
                        Kyc2TierState.Tier2InPending,
                        Kyc2TierState.Tier2InReview,
                        Kyc2TierState.Tier2Failed -> FlowScreen.KYC_VERIFICATION
                        else -> FlowScreen.KYC
                    }
                }
            } else {
                if (startedFromDashboard && it.currentScreen == FlowScreen.INTRO) {
                    Observable.just(FlowScreen.ENTER_AMOUNT)
                } else {
                    Observable.just(it.currentScreen)
                }
            }
        }.firstOrError()

    fun navigateTo(startedFromKycResume: Boolean, startedFromDashboard: Boolean): Single<FlowScreen> {

        val currencyCheck = (currencyPrefs.selectedFiatCurrency.takeIf { it.isNotEmpty() }?.let {
            custodialWalletManager.isCurrencySupportedForSimpleBuy(it)
        } ?: Single.just(false)).doOnSuccess {
            if (!it)
                simpleBuyPrefs.clearState()
        }

        return currencyCheck.flatMap { currencySupported ->
            if (!currencySupported) {
                if (startedFromDashboard || startedFromKycResume) {
                    Single.just(FlowScreen.CURRENCY_SELECTOR)
                } else {
                    Single.just(FlowScreen.INTRO)
                }
            } else {
                stateCheck(startedFromKycResume, startedFromDashboard)
            }
        }
    }
}