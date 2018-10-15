package com.example.darling.bitmapcachetest.mvp.presenter;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.bumptech.glide.disklrucache.DiskLruCache;
import com.example.darling.bitmapcachetest.R;
import com.example.darling.bitmapcachetest.mvp.BitmapCacheContract;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.example.darling.bitmapcachetest.mvp.utils.Utils;
import com.jess.arms.integration.AppManager;
import com.jess.arms.mvp.BasePresenter;
import com.jess.arms.utils.RxLifecycleUtils;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.jessyan.rxerrorhandler.core.RxErrorHandler;
import me.jessyan.rxerrorhandler.handler.ErrorHandleSubscriber;
import me.jessyan.rxerrorhandler.handler.RetryWithDelay;

public class UserPresenter extends BasePresenter<BitmapCacheContract.Model, BitmapCacheContract.View> {

    @Inject
    RxErrorHandler mErrorHandler;

    @Inject
    AppManager mAppManager;
    @Inject
    Application mApplication;
    @Inject
    List<User> mUsers;
    @Inject
    RecyclerView.Adapter mAdapter;

    @Inject
    public UserPresenter(BitmapCacheContract.Model model, BitmapCacheContract.View rootView) {
        super(model, rootView);
    }

    @Override
    public void onStart() {
        super.onStart();

        mModel.onStart();
    }

    private int lastUserId = 1;
    private boolean isFirst = true;
    private int preEndIndex;

    /**
     * 这里定义业务方法,相应用户的交互
     *
     * @param pullToRefresh
     */
    public void requestBitmaps(final boolean pullToRefresh) {

        if (pullToRefresh) lastUserId = 1;//下拉刷新默认只请求第一页

        //关于RxCache缓存库的使用请参考 http://www.jianshu.com/p/b58ef6b0624b

        boolean isEvictCache = pullToRefresh;//是否驱逐缓存,为ture即不使用缓存,每次下拉刷新即需要最新数据,则不使用缓存

        if (pullToRefresh && isFirst) {//默认在第一次下拉刷新时使用缓存
            isFirst = false;
            isEvictCache = false;
        }
        mModel.getUsers(lastUserId, isEvictCache)
                .subscribeOn(Schedulers.io())
                .retryWhen(new RetryWithDelay(3, 2))//遇到错误时重试,第一个参数为重试几次,第二个参数为重试的间隔
                .doOnSubscribe(disposable -> {
//                    if (pullToRefresh)
//                        mRootView.showLoading();//显示下拉刷新的进度条
//                    else
//                        mRootView.startLoadMore();//显示上拉加载更多的进度条
                }).subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
//                    if (pullToRefresh)
//                        mRootView.hideLoading();//隐藏下拉刷新的进度条
//                    else
//                        mRootView.endLoadMore();//隐藏上拉加载更多的进度条
                })
                .compose(RxLifecycleUtils.bindToLifecycle(mRootView))//使用 Rxlifecycle,使 Disposable 和 Activity 一起销毁
                .subscribe(new ErrorHandleSubscriber<List<User>>(mErrorHandler) {
                    @Override
                    public void onNext(List<User> mUsers) {
                        lastUserId = mUsers.get(mUsers.size() - 1).getId();//记录最后一个id,用于下一次请求
                        // if (pullToRefresh) mUsers.clear();//如果是下拉刷新则清空列表
                        preEndIndex = mUsers.size();//更新之前列表总长度,用于确定加载更多的起始位置
                        UserPresenter.this.mUsers.addAll(mUsers);

                        Log.d("BitmapCacheTest " + TAG, mUsers.toString());

                        //    mRootView.endLoadMore(mUsers);
//                        if (pullToRefresh)
                        mAdapter.notifyDataSetChanged();
//                        else
//                            mAdapter.notifyItemRangeInserted(preEndIndex, Bitmaps.size());
                    }
                });
    }

    public Bitmap getBitmap(String iPath,SetImagefromNet setImagefromNet) {

        Bitmap bitmap = mModel.getBitmap(iPath);
        if (bitmap == null) {
            mModel.downloadBitmap(iPath)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Bitmap>() {
                        @Override
                        public void accept(Bitmap bitmap) throws Exception {
                            setImagefromNet.setImage(bitmap);
                            // Add to memory cache as before
                            String key = Utils.getKeyForDisk(iPath);
                            mModel.addBitmapToMemoryCache(key, bitmap);
                        }
                    });

        } else {
            return bitmap;
        }

        return null;
    }

    public void initDiskCache(Context context) {
        Disposable disposable = mModel.initDiskCache(context)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
//                    if (pullToRefresh)
//                        mRootView.hideLoading();//隐藏下拉刷新的进度条
//                    else
//                        mRootView.endLoadMore();//隐藏上拉加载更多的进度条
                })
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean mDiskCacheStarting) throws Exception {
                        Log.d(TAG, "initDiskCache success " + mDiskCacheStarting);
                    }
                });
    }

//    public void loadBitmap(int resId) {
//        final String imageKey = String.valueOf(resId);
//        final Bitmap bitmap = mModel.getBitmap(imageKey);
//        if (bitmap != null) {
//            Log.d(TAG,"Image from Cache");
//            mRootView.startLoadMore();
//        } else {
//            Log.d(TAG,"Image from Net");
////            mImageView.setImageResource(R.color.colorAccent);
////            mPresenter.requestBitmaps(true);
//           requestBitmaps(true);
//        }
//    }

    public interface SetImagefromNet{

        void setImage(Bitmap bitmap);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        this.mAdapter = null;
        this.mUsers = null;
        this.mErrorHandler = null;
        this.mAppManager = null;
        this.mApplication = null;
    }
}
