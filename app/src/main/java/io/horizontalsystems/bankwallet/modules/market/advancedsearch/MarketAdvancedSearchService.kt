package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.Score
import io.horizontalsystems.bankwallet.modules.market.list.IMarketListFetcher
import io.horizontalsystems.core.entities.Currency
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.PublishSubject

class MarketAdvancedSearchService(private val currency: Currency, private val xRateManager: IRateManager) : Clearable, IMarketListFetcher {

    var coinCount: Int = CoinList.Top250.itemsCount
        set(value) {
            field = value

            refreshCounter()
        }
    var filterMarketCap: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterVolume: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterLiquidity: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterPeriod: TimePeriod? = null
        set(value) {
            field = value

            refreshCounter()
        }
    var filterPriceChange: Pair<Long?, Long?>? = null
        set(value) {
            field = value

            refreshCounter()
        }

    override val dataUpdatedAsync: Observable<Unit> = Observable.empty()

    var numberOfItemsAsync = PublishSubject.create<DataState<Int>>()
    private var topItemsDisposable: Disposable? = null

    private fun refreshCounter() {
        topItemsDisposable?.dispose()

        numberOfItemsAsync.onNext(DataState.Loading)
        topItemsDisposable = fetchAsync(currency)
                .map { it.size }
                .subscribeIO({
                    numberOfItemsAsync.onNext(DataState.Success(it))
                }, {
                    numberOfItemsAsync.onNext(DataState.Error(it))
                })

    }

    override fun fetchAsync(currency: Currency): Single<List<MarketItem>> {
        return xRateManager.getTopMarketList(currency.code, coinCount)
                .map {
                    it.filter {
                        filterByRange(filterMarketCap, it.marketInfo.marketCap?.toLong())
                                && filterByRange(filterVolume, it.marketInfo.volume.toLong())
                                && filterByRange(filterLiquidity, it.marketInfo.liquidity?.toLong())
                                && filterByRange(filterPriceChange, it.marketInfo.rateDiffPeriod.toLong())
                    }
                }
                .map { coinMarkets ->
                    coinMarkets.mapIndexed { index, coinMarket ->
                        MarketItem.createFromCoinMarket(coinMarket, currency, Score.Rank(index + 1))
                    }
                }
    }

    private fun filterByRange(filter: Pair<Long?, Long?>?, value: Long?): Boolean {
        if (filter == null) return true

        filter.first?.let { min ->
            if (value == null || value < min) {
                return false
            }
        }

        filter.second?.let { max ->
            if (value == null || value > max) {
                return false
            }
        }

        return true
    }

    override fun clear() {
        topItemsDisposable?.dispose()
    }
}