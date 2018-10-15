package com.example.darling.bitmapcachetest;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TabHost;

import com.bumptech.glide.disklrucache.DiskLruCache;
import com.example.darling.bitmapcachetest.adapter.UserAdapter;
import com.example.darling.bitmapcachetest.mvp.BitmapCacheContract;
import com.example.darling.bitmapcachetest.mvp.DaggerUserComponent;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.example.darling.bitmapcachetest.mvp.presenter.UserPresenter;
import com.jess.arms.base.BaseActivity;
import com.jess.arms.base.DefaultAdapter;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.di.component.DaggerAppComponent;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import timber.log.Timber;

/**
 *  应用剩下了多少可用的内存?
    多少张图片会同时呈现到屏幕上？有多少图片需要准备好以便马上显示到屏幕？
    设备的屏幕大小与密度是多少？一个具有特别高密度屏幕（xhdpi）的设备，像Galaxy Nexus会比Nexus S（hdpi）需要一个更大的缓存空间来缓存同样数量的图片。
    Bitmap的尺寸与配置是多少，会花费多少内存？
    图片被访问的频率如何？是其中一些比另外的访问更加频繁吗？如果是，那么我们可能希望在内存中保存那些最常访问的图片，或者根据访问频率给Bitmap分组，为不同的Bitmap组设置多个LruCache对象。
    是否可以在缓存图片的质量与数量之间寻找平衡点？某些时候保存大量低质量的Bitmap会非常有用，加载更高质量图片的任务可以交给另外一个后台线程
 */
public class MainActivity extends BaseActivity<UserPresenter> implements BitmapCacheContract.View{

    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;
    @Inject
    RecyclerView.LayoutManager layoutManager;
    @Inject
    RecyclerView.Adapter mAdapter;

    @Override
    public int initView(@Nullable Bundle savedInstanceState) {
        return R.layout.activity_main;
    }

    @Override
    public void setupActivityComponent(@NonNull AppComponent appComponent) {
        DaggerUserComponent
                .builder()
                .appComponent(appComponent)
                .userModule(new UserModule(this))
                .build()
                .inject(this);
    }

    @Override
    public void initData(@Nullable Bundle savedInstanceState) {

        recyclerView.setLayoutManager(layoutManager);
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        if (mAdapter instanceof UserAdapter) {
            ((UserAdapter) mAdapter).setPresenter(mPresenter);
        }
        recyclerView.setAdapter(mAdapter);

        // need context!!??
        mPresenter.initDiskCache(this);

        mPresenter.requestBitmaps(true);

    }



    @Override
    public void setAdapter(DefaultAdapter adapter) {

    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void startLoadMore() {

    }

    @Override
    public void endLoadMore(List<User> mUsers) {

    }

    @Override
    public void showLoading() {

    }

    @Override
    public void hideLoading() {

    }

    @Override
    public void showMessage(@NonNull String message) {

    }

    @Override
    public void launchActivity(@NonNull Intent intent) {

    }

    @Override
    public void killMyself() {

    }


}
