package com.example.darling.bitmapcachetest.holder;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.darling.bitmapcachetest.MainActivity;
import com.example.darling.bitmapcachetest.R;
import com.example.darling.bitmapcachetest.UserModule;
import com.example.darling.bitmapcachetest.mvp.DaggerUserComponent;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.example.darling.bitmapcachetest.mvp.presenter.UserPresenter;
import com.jess.arms.base.BaseHolder;
import com.jess.arms.base.DefaultAdapter;
import com.jess.arms.di.component.AppComponent;
import com.jess.arms.http.imageloader.ImageLoader;
import com.jess.arms.http.imageloader.glide.ImageConfigImpl;
import com.jess.arms.mvp.BasePresenter;
import com.jess.arms.mvp.IModel;
import com.jess.arms.mvp.IView;
import com.jess.arms.utils.ArmsUtils;

import javax.inject.Inject;

import butterknife.BindView;
import io.reactivex.Observable;

/**
 * 展示 {@link BaseHolder} 的用法
 */
public class UserItemHolder extends BaseHolder<User> {

    @BindView(R.id.iv_avatar)
    ImageView mAvatar;
    @BindView(R.id.tv_name)
    TextView mName;

    protected UserPresenter mPresenter;//如果当前页面逻辑简单, Presenter 可以为 null

    private AppComponent mAppComponent;
    private ImageLoader mImageLoader;//用于加载图片的管理类,默认使用 Glide,使用策略模式,可替换框架

    public UserItemHolder(View itemView,UserPresenter presenter) {
        super(itemView);
        //可以在任何可以拿到 Context 的地方,拿到 AppComponent,从而得到用 Dagger 管理的单例对象
        mAppComponent = ArmsUtils.obtainAppComponentFromContext(itemView.getContext());
        mImageLoader = mAppComponent.imageLoader();
        mPresenter = presenter;
    }

    @Override
    public void setData(User user, int position) {
        Observable.just(user.getLogin())
                .subscribe(s -> mName.setText(s));

        //itemView 的 Context 就是 Activity, Glide 会自动处理并和该 Activity 的生命周期绑定
//        mImageLoader.loadImage(itemView.getContext(),
//                ImageConfigImpl
//                        .builder()
//                        .url(data.getAvatarUrl())
//                        .imageView(mAvatar)
//                        .build());
        String iPath = user.getAvatarUrl();
        //根据图片有关联的标识(avatarUrl)
//        mAppComponent.appManager().findActivity(MainActivity.class).
        Bitmap bitmap = mPresenter.getBitmap(iPath, new UserPresenter.SetImagefromNet() {
            @Override
            public void setImage(Bitmap bitmap) {
                Log.d(TAG, "Image from Net:" + user.getLogin());
                if (bitmap != null) {
                    mAvatar.setImageBitmap(bitmap);
                }
            }
        });
        if (bitmap != null) {
            mAvatar.setImageBitmap(bitmap);
        }
    }


    /**
     * 在 Activity 的 onDestroy 中使用 {@link DefaultAdapter#releaseAllHolder(RecyclerView)} 方法 (super.onDestroy() 之前)
     * {@link BaseHolder#onRelease()} 才会被调用, 可以在此方法中释放一些资源
     */
    @Override
    protected void onRelease() {
        //只要传入的 Context 为 Activity, Glide 就会自己做好生命周期的管理, 其实在上面的代码中传入的 Context 就是 Activity
        //所以在 onRelease 方法中不做 clear 也是可以的, 但是在这里想展示一下 clear 的用法
        mImageLoader.clear(mAppComponent.application(), ImageConfigImpl.builder()
                .imageViews(mAvatar)
                .build());
        this.mAvatar = null;
        this.mName = null;
        this.mAppComponent = null;
        this.mImageLoader = null;
    }
}
