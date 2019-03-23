/*
 * Copyright 2019 OST.com Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */

package com.ost.walletsdk.workflows;

import android.os.Bundle;
import android.util.Log;

import com.ost.walletsdk.OstConstants;
import com.ost.walletsdk.OstSdk;
import com.ost.walletsdk.ecKeyInteracts.OstRecoveryManager;
import com.ost.walletsdk.ecKeyInteracts.UserPassphrase;
import com.ost.walletsdk.ecKeyInteracts.structs.SignedResetRecoveryStruct;
import com.ost.walletsdk.models.entities.OstRecoveryOwner;
import com.ost.walletsdk.utils.AsyncStatus;
import com.ost.walletsdk.workflows.errors.OstError;
import com.ost.walletsdk.workflows.errors.OstErrors;
import com.ost.walletsdk.workflows.interfaces.OstWorkFlowCallback;
import com.ost.walletsdk.workflows.services.OstPollingService;
import com.ost.walletsdk.workflows.services.OstRecoveryPollingService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * It will change current passPhrase recoveryAddress with new passPhrase recoveryAddress.
 */
public class OstResetPin extends OstBaseWorkFlow {

    private static final String TAG = "OstResetPin";

    private static final String NEW_RECOVERY_OWNER_ADDRESS = "new_recovery_owner_address";
    private static final String TO = "to";
    private static final String SIGNER = "signer";
    private static final String SIGNATURE = "signature";

    private final UserPassphrase currentPassphrase;
    private final UserPassphrase newPassphrase;

    private String mNewRecoveryOwnerAddress;
    private OstResetPin.STATES mCurrentState = OstResetPin.STATES.INITIAL;
    private OstRecoveryOwner mOstRecoveryOwner;


    public OstResetPin(String userId, UserPassphrase currentPassphrase, UserPassphrase newPassphrase, OstWorkFlowCallback workFlowCallback) {
        super(userId, workFlowCallback);
        this.currentPassphrase = currentPassphrase;
        this.newPassphrase = newPassphrase;
    }

    @Override
    protected AsyncStatus process() {
        String newRecoveryOwnerAddress = "";
        try {
            switch (mCurrentState) {
                case INITIAL:
                    Log.d(TAG, String.format("Perform workflow for userId: %s started", mUserId));

                    Log.i(TAG, "validate params");
                    if (!hasValidParams()) {
                        return postErrorInterrupt("wf_rp_pr_1", OstErrors.ErrorCode.INVALID_WORKFLOW_PARAMS);
                    }

                    Log.i(TAG, "Loading device and user entities");
                    AsyncStatus status = super.loadCurrentDevice();
                    status = status.isSuccess() ? super.loadUser() : status;

                    if (!status.isSuccess()) {
                        Log.e(TAG, String.format("Fetching of basic entities failed for user id: %s", mUserId));
                        return status;
                    }

                    if (!hasActivatedUser()) {
                        Log.e(TAG, String.format("User is not activated of user id: %s", mUserId));
                        return postErrorInterrupt("wf_rp_pr_2", OstErrors.ErrorCode.USER_NOT_ACTIVATED);
                    }


                    try {
                        mOstApiClient.getDevice(mOstUser.getCurrentDevice().getAddress());
                    } catch (IOException e) {
                        Log.e(TAG, "GetDevice api failed");
                        return postErrorInterrupt("wf_rp_pr_4", OstErrors.ErrorCode.GET_USER_API_FAILED);
                    }


                    SignedResetRecoveryStruct struct;
                    OstRecoveryManager rkm;
                    try {
                        rkm = new OstRecoveryManager(mUserId);
                        struct = rkm.getResetRecoveryOwnerSignature(currentPassphrase, newPassphrase);
                        rkm = null;
                    } catch (OstError error) {
                        return postErrorInterrupt(error.getInternalErrorCode(), error.getErrorCode());
                    }

                    newRecoveryOwnerAddress = struct.getNewRecoveryOwnerAddress();
                    mNewRecoveryOwnerAddress = newRecoveryOwnerAddress;
                    Map<String, Object> requestMap = buildApiRequest(newRecoveryOwnerAddress,
                            struct.getRecoveryOwnerAddress(), struct.getRecoveryContractAddress(), struct.getSignature());

                    JSONObject postRecoveryAddresssResponse = null;
                    try {
                        postRecoveryAddresssResponse = mOstApiClient.postRecoveryOwners(requestMap);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException in postRecoveryOwner");
                    }

                    if (!isValidResponse(postRecoveryAddresssResponse)) {
                        return postErrorInterrupt("wf_rp_pr_5", OstErrors.ErrorCode.POST_RESET_RECOVERY_API_FAILED);
                    }

                    JSONObject jsonData = postRecoveryAddresssResponse.optJSONObject(OstConstants.RESPONSE_DATA);
                    JSONObject resultTypeObject = jsonData.optJSONObject(jsonData.optString(OstConstants.RESULT_TYPE));
                    OstRecoveryOwner ostRecoveryOwner = null;
                    try {
                        ostRecoveryOwner = OstRecoveryOwner.parse(resultTypeObject);
                    } catch (JSONException e) {
                        return postErrorInterrupt("wf_rp_pr_6", OstErrors.ErrorCode.POST_RESET_RECOVERY_API_FAILED);
                    }

                    mOstRecoveryOwner = ostRecoveryOwner;
                    postRequestAcknowledge(new OstWorkflowContext(getWorkflowType()), new OstContextEntity(mOstRecoveryOwner, OstSdk.RECOVERY_OWNER));

                case POLLING:

                    Log.i(TAG, "Waiting for update");
                    Bundle bundle = OstRecoveryPollingService.startPolling(mUserId, mNewRecoveryOwnerAddress, OstRecoveryOwner.CONST_STATUS.AUTHORIZED,
                            OstRecoveryOwner.CONST_STATUS.AUTHORIZATION_FAILED);

                    if (bundle.getBoolean(OstPollingService.EXTRA_IS_POLLING_TIMEOUT, true)) {
                        Log.d(TAG, String.format("Polling time out for recovery owner Id: %s", newRecoveryOwnerAddress));
                        return postErrorInterrupt("wf_rp_pr_7", OstErrors.ErrorCode.POLLING_TIMEOUT);
                    }

                    Log.i(TAG, "Response received for RecoveryOwner");
                    postFlowComplete(
                            new OstContextEntity(mOstRecoveryOwner, OstSdk.RECOVERY_OWNER)
                    );
                    break;
                case CANCELLED:
                    Log.d(TAG, String.format("Error in Add device flow: %s", mUserId));
                    postErrorInterrupt("wf_rp_pr_8", OstErrors.ErrorCode.WORKFLOW_CANCELLED);
                    break;
            }

        } catch (Throwable th) {
            OstError error;
            if (th instanceof OstError) {
                error = (OstError) th;
            } else {
                error = new OstError("wf_rp_oudvp_5", OstErrors.ErrorCode.UNCAUGHT_EXCEPTION_HANDELED);
            }
            return postErrorInterrupt(error);
        }
        return new AsyncStatus(true);
    }

    private Map<String, Object> buildApiRequest(String newRecoveryOwnerAddress, String recoveryOwnerAddress,
                                                String recoveryContractAddress, String signature) {
        Map<String, Object> map = new HashMap<>();
        map.put(NEW_RECOVERY_OWNER_ADDRESS, newRecoveryOwnerAddress);
        map.put(TO, recoveryContractAddress);
        map.put(SIGNER, recoveryOwnerAddress);
        map.put(SIGNATURE, signature);
        return map;
    }

    @Override
    public OstWorkflowContext.WORKFLOW_TYPE getWorkflowType() {
        return OstWorkflowContext.WORKFLOW_TYPE.PIN_RESET;
    }

    private enum STATES {
        INITIAL,
        CANCELLED,
        POLLING
    }
}