package me.jbusdriver.mvp.presenter

import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.addTo
import me.jbusdriver.common.SchedulersCompat
import me.jbusdriver.db.bean.History
import me.jbusdriver.db.bean.toPageInfo
import me.jbusdriver.db.service.HistoryService
import me.jbusdriver.mvp.HistoryContract
import me.jbusdriver.mvp.bean.PageInfo
import me.jbusdriver.mvp.model.BaseModel
import org.jsoup.nodes.Document

class HistoryPresenterImpl : AbstractRefreshLoadMorePresenterImpl<HistoryContract.HistoryView, History>(), HistoryContract.HistoryPresenter {

    private val service by lazy { HistoryService() }
    private val dbPage by lazy { service.page() }

    override fun onResume() {
        super.onResume()
        onRefresh()
    }

    override fun loadData4Page(page: Int) {
        val dbPage = dbPage.copy(currentPage = page).apply {
            pageInfo = toPageInfo
            lastPage = totalPage
        }
        service.queryPage(dbPage).toFlowable(BackpressureStrategy.DROP)
                .doOnNext {
                    pageInfo = pageInfo.copy(pageInfo.nextPage, pageInfo.nextPage + 1)
                }
                .compose(SchedulersCompat.io())
                .subscribeWith(object : ListDefaultSubscriber(page) {

                    override fun onNext(t: List<History>) {
                        super.onNext(t)
                        if (pageIndex >= dbPage.totalPage) mView?.loadMoreEnd()
                        mView?.dismissLoading()
                        (pageIndex == 1).let {
                            if (it) mView?.enableLoadMore(true) else mView?.enableRefresh(true)
                        }
                    }
                })
                .addTo(rxManager)
    }

    override fun clearHistory() {
        service.clearAll()
    }

    override fun onRefresh() {
        pageInfo = PageInfo()
        loadData4Page(1)
    }

    override fun lazyLoad() {
        onFirstLoad()
    }

    override val model: BaseModel<Int, Document>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun stringMap(str: Document): List<History> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}