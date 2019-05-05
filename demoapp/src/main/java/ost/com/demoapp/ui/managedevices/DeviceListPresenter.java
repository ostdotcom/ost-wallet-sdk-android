/*
 * Copyright 2019 OST.com Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */

package ost.com.demoapp.ui.managedevices;

import android.util.Log;

import com.ost.walletsdk.models.entities.OstDevice;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ost.com.demoapp.AppProvider;
import ost.com.demoapp.network.MappyNetworkClient;
import ost.com.demoapp.ui.BasePresenter;
import ost.com.demoapp.util.CommonUtils;

class DeviceListPresenter extends BasePresenter<DeviceListView> {
    private static final String LOG_TAG = "OstDeviceListPresenter";

    private DeviceListPresenter() {}

    public static DeviceListPresenter newInstance() {
        return new DeviceListPresenter();
    }

    private JSONObject nextPayload = new JSONObject();

    private List<OstDevice> ostDeviceList = new ArrayList<>();

    @Override
    public void attachView(DeviceListView mvpView) {
        super.attachView(mvpView);
        updateDeviceList();
    }

    void updateDeviceList() {
        ostDeviceList.clear();
        AppProvider.get().getMappyClient().getCurrentUserDevices(nextPayload, new MappyNetworkClient.ResponseCallback() {
            @Override
            public void onSuccess(JSONObject jsonObject) {
                if (new CommonUtils().isValidResponse(jsonObject)) {
                    try {
                        JSONObject dataJSONObject =  new CommonUtils().parseJSONData(jsonObject);
                        nextPayload = dataJSONObject.optJSONObject("meta");

                        JSONArray deviceJSONArray = (JSONArray) new CommonUtils()
                                .parseResponseForResultType(jsonObject);


                        for (int i = 0; i < deviceJSONArray.length(); i++) {
                            JSONObject deviceJSONObject = deviceJSONArray.getJSONObject(i);
                            OstDevice ostDevice = OstDevice.parse(deviceJSONObject);
                            ostDeviceList.add(ostDevice);
                        }
                    } catch (JSONException e) {
                        //Exception not expected
                    }
                    getMvpView().notifyDataSetChanged();
                } else {
                    Log.e(LOG_TAG, String.format("Get Current User list response false: %s", jsonObject.toString()));
                    getMvpView().notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e(LOG_TAG, String.format("Get Current User list error: %s", throwable.toString()));
                getMvpView().notifyDataSetChanged();
            }
        });
    }

    void setOstDeviceList(List<OstDevice> ostDeviceList) {
        this.ostDeviceList = ostDeviceList;
    }
}