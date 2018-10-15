package com.example.darling.bitmapcachetest.adapter;

import android.view.View;

import com.example.darling.bitmapcachetest.R;
import com.example.darling.bitmapcachetest.holder.UserItemHolder;
import com.example.darling.bitmapcachetest.mvp.model.entity.User;
import com.example.darling.bitmapcachetest.mvp.presenter.UserPresenter;
import com.jess.arms.base.BaseHolder;
import com.jess.arms.base.DefaultAdapter;
import com.jess.arms.mvp.BasePresenter;
import com.jess.arms.mvp.IModel;
import com.jess.arms.mvp.IView;

import java.util.List;

/**
 * 展示 {@link DefaultAdapter} 的用法
 */
public class UserAdapter extends DefaultAdapter<User> {

    public UserAdapter(List<User> infos) {
        super(infos);
    }

    UserPresenter mPresenter;
    public void setPresenter(UserPresenter presenter){

        mPresenter = presenter;
    }

    @Override
    public BaseHolder<User> getHolder(View v, int viewType) {
        return new UserItemHolder(v,mPresenter);
    }

    @Override
    public int getLayoutId(int viewType) {
        return R.layout.recycle_list;
    }
}
