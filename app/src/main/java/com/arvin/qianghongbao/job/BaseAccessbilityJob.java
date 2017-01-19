package com.arvin.qianghongbao.job;

import android.content.Context;

import com.arvin.qianghongbao.QiangHongBaoService;

/**
 * Created by arvin on 2017/1/19.
 */

public abstract class BaseAccessbilityJob implements AccessbilityJob {

    private QiangHongBaoService service;

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        this.service = service;
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    public QiangHongBaoService getService() {
        return service;
    }
}
