/*
 * Copyright 2019 OST.com Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */

package ost.com.demoapp.util;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.ost.walletsdk.OstConstants;
import com.ost.walletsdk.OstSdk;
import com.ost.walletsdk.models.entities.OstDevice;
import com.ost.walletsdk.models.entities.OstToken;
import com.ost.walletsdk.models.entities.OstUser;
import com.ost.walletsdk.network.OstApiClient;
import com.ost.walletsdk.network.OstApiError;
import com.ost.walletsdk.workflows.OstWorkflowContext;
import com.ost.walletsdk.workflows.errors.OstError;
import com.ost.walletsdk.workflows.errors.OstErrors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.crypto.Keys;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import ost.com.demoapp.AppProvider;
import ost.com.demoapp.R;
import ost.com.demoapp.entity.CurrentEconomy;
import ost.com.demoapp.entity.LogInUser;
import ost.com.demoapp.ui.auth.OnBoardingActivity;

public class CommonUtils {
    private static final String LOG_TAG = "OstCommonUtils";
    private static final String DATA = "data";

    public CommonUtils() {
    }

    public JSONArray listToJSONArray(List<String> list) {
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < list.size(); i++) {
            jsonArray.put(list.get(i));
        }
        return jsonArray;
    }

    public List<String> jsonArrayToList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.getString(i));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "jsonArray to list exception", e);
        }
        return list;
    }

    public List<String> toCheckSumAddresses(List<String> addressList) {
        for (int i = 0; i < addressList.size(); i++) {
            String address = Keys.toChecksumAddress(addressList.get(i));
            addressList.set(i, address);
        }
        return addressList;
    }

    private static final byte[] nonSecret = ("LETS_CLEAR_BYTES" + String.valueOf((int) (System.currentTimeMillis()))).getBytes();

    public static void clearBytes(byte[] secret) {
        if (null == secret) {
            return;
        }
        for (int i = 0; i < secret.length; i++) {
            secret[i] = nonSecret[i % nonSecret.length];
        }
    }

    public String parseStringResponseForKey(JSONObject jsonObject, String key) {
        try {
            JSONObject resultType = (JSONObject) parseResponseForResultType(jsonObject);
            return resultType.getString(key);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON Exception");
        }
        return null;
    }

    public boolean isValidResponse(JSONObject jsonObject) {
        try {
            if (jsonObject.getBoolean(OstConstants.RESPONSE_SUCCESS)) {
                return true;
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON Exception");
        }
        return false;
    }

    public Object parseResponseForResultType(JSONObject jsonObject) throws JSONException {
        if (!isValidResponse(jsonObject)) {
            Log.e(LOG_TAG, "JSON response false");
            return null;
        }
        JSONObject jsonData = jsonObject.getJSONObject(OstConstants.RESPONSE_DATA);
        return jsonData.get(jsonData.getString(OstConstants.RESULT_TYPE));
    }

    public Object parseObjectResponseForKey(JSONObject jsonObject, String key) {
        try {
            JSONObject resultType = (JSONObject) parseResponseForResultType(jsonObject);
            return resultType.get(key);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "JSON Exception");
        }
        return null;
    }

    public JSONObject parseJSONData(JSONObject jsonObject) {
        try {
            return jsonObject.getJSONObject(DATA);
        } catch (JSONException e) {
            return null;
        }
    }

    public static String convertWeiToTokenCurrency(String balance) {
        if (null == balance) return "0";

        OstToken token = OstSdk.getToken(AppProvider.get().getCurrentUser().getTokenId());
        Integer decimals = Integer.parseInt(token.getBtDecimals());
        BigDecimal btWeiMultiplier = new BigDecimal(10).pow(decimals);
        BigDecimal bal = new BigDecimal(balance).divide(btWeiMultiplier);
        BigDecimal newBal = bal.setScale(2, RoundingMode.DOWN);
        return newBal.toString();
    }

    public static String convertBTWeiToUsd(String balance, JSONObject pricePointObject) {
        if (null == balance || null == pricePointObject) return null;

        try{
            OstToken token = OstSdk.getToken(AppProvider.get().getCurrentUser().getTokenId());
            Integer decimals = Integer.parseInt(token.getBtDecimals());
            BigDecimal btWeiMultiplier = new BigDecimal(10).pow(decimals);
            BigDecimal bal = new BigDecimal(balance).divide(btWeiMultiplier);
            BigDecimal baseCurrencyBal = bal.divide(new BigDecimal(token.getConversionFactor()), RoundingMode.DOWN);
            Double pricePointOSTtoUSD = pricePointObject.getJSONObject(token.getBaseToken()).getDouble("USD");
            return baseCurrencyBal.multiply(new BigDecimal(pricePointOSTtoUSD)).setScale(2, RoundingMode.DOWN).toString();
        } catch (Exception e){
            return null;
        }
    }

    public static String convertUsdWeitoUsd(String amount) {
        if (null == amount) return "";
        BigDecimal btWeiMultiplier = new BigDecimal(10).pow(18);
        BigDecimal bal = new BigDecimal(amount).divide(btWeiMultiplier);
        return bal.setScale(2, RoundingMode.DOWN).toString();
    }

    public boolean handleActivatingStateCheck(Context activityContext) {
        OstUser currentOstUser = AppProvider.get().getCurrentUser().getOstUser();
        if (currentOstUser.isActivating()) {
            Dialog dialog = DialogFactory.createSimpleOkErrorDialog(
                    activityContext,
                    activityContext.getResources().getString(R.string.wallet_being_setup),
                    activityContext.getResources().getString(R.string.wallet_setup_text));
            dialog.show();
            return true;
        }
        return false;
    }
    public boolean handleActionEligibilityCheck(Context activityContext) {
        if (handleActivatingStateCheck(activityContext)) return true;

        OstUser currentOstUser = AppProvider.get().getCurrentUser().getOstUser();
        OstDevice currentDevice = currentOstUser.getCurrentDevice();
        if (currentDevice.isRecovering()) {
            Dialog dialog = DialogFactory.createSimpleOkErrorDialog(
                    activityContext,
                    "Your Wallet is recovering",
                    "The Wallet setup process takes about 12 hours. You can continue to use the app and we’ll notify when the wallet is ready to use.");
            dialog.show();
            return true;
        }
        return false;
    }

    public String formatWorkflowSuccessToast(OstWorkflowContext.WORKFLOW_TYPE workflowType, JSONObject workflowDetails){
        if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.SETUP_DEVICE)){
            return null;
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ACTIVATE_USER)){
            return "Congratulations! Your wallet is now ready!";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ADD_SESSION)){
            return "A session has been authorized. You can now make in-app transactions seamlessly.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.GET_DEVICE_MNEMONICS)){
            return null;
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.EXECUTE_TRANSACTION)){
            if(workflowDetails != null){
                try{
                    String amount = "";
                    if(workflowDetails.getString("transferRule").equals(OstSdk.RULE_NAME_DIRECT_TRANSFER)){
                        amount = String.format("%s %s",
                                CommonUtils.convertWeiToTokenCurrency(workflowDetails.getString("amount")),
                                AppProvider.get().getCurrentEconomy().getTokenSymbol());
                    } else {
                        amount = String.format("%s USD", CommonUtils.convertUsdWeitoUsd(workflowDetails.getString("amount")));
                    }
                    return String.format("%s sent to %s successfully!",
                            amount,
                            workflowDetails.getString("userName"));
                } catch (Exception e){
                    return "Token Transfer is successful!";
                }
            } else {
                return "Token Transfer is successful!";
            }
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.AUTHORIZE_DEVICE_WITH_QR_CODE)){
            return "This device is now authorized to access your Wallet.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.AUTHORIZE_DEVICE_WITH_MNEMONICS)){
            return "This device is now authorized to access your Wallet.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.INITIATE_DEVICE_RECOVERY)){
            return "Wallet recovery has been initiated. Unless interrupted, your device will authorized in about 12 hours.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ABORT_DEVICE_RECOVERY)){
            return "Recovery has been successfully aborted. Existing authorized devices may be used.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.REVOKE_DEVICE_WITH_QR_CODE)){
            return "The chosen device has been revoked. It can no longer access your Wallet.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.RESET_PIN)){
            return "Your PIN has been reset. Please remember this new PIN.";
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.LOGOUT_ALL_SESSIONS)){
            return "All Sessions have been Logged Out!";
        }
        return null;
    }

    public String formatWorkflowFailedToast(OstWorkflowContext.WORKFLOW_TYPE workflowType, OstError ostError, JSONObject workflowDetails){
        if(OstErrors.ErrorCode.WORKFLOW_CANCELLED.equals(ostError.getErrorCode())){
            return null;
        }
        String errMsg = (ostError.getErrorCode() == OstErrors.ErrorCode.OST_PLATFORM_API_ERROR) ?
                ((OstApiError) ostError).getErrMsg() : ostError.getMessage();
        if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.SETUP_DEVICE)){
            return null;
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ACTIVATE_USER)){
            return ("User Activation Failed!\n" + errMsg);
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ADD_SESSION)){
            return ("Session could not be authorized.\n" + errMsg);
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.GET_DEVICE_MNEMONICS)){
            return ("Mnemonics cannot be fetched.\n" + errMsg);
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.EXECUTE_TRANSACTION)){
            if(workflowDetails != null){
                try{
                    String amount = "";
                    if(workflowDetails.getString("transferRule").equals(OstSdk.RULE_NAME_DIRECT_TRANSFER)){
                        amount = String.format("%s %s",
                                CommonUtils.convertWeiToTokenCurrency(workflowDetails.getString("amount")),
                                AppProvider.get().getCurrentEconomy().getTokenSymbol());
                    } else {
                        amount = String.format("%s USD", CommonUtils.convertUsdWeitoUsd(workflowDetails.getString("amount")));
                    }
                    return String.format("Token Transfer of %s to %s failed!\n%s",
                            amount,
                            workflowDetails.getString("userName"), errMsg);
                } catch (Exception e){
                    return ("Transaction Failed!\n" + errMsg);
                }
            } else {
                return ("Transaction Failed!\n" + errMsg);
            }
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.AUTHORIZE_DEVICE_WITH_QR_CODE)){
            return ("Authorization failed. Please verify the QR code.");
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.AUTHORIZE_DEVICE_WITH_MNEMONICS)){
            return ("Authorization failed. Please verify that the mnemonics are correct.");
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.INITIATE_DEVICE_RECOVERY)){
            return ("Recovery could not be initiated. Please verify PIN.");
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.ABORT_DEVICE_RECOVERY)){
            return ("Abort recovery failed.\n" + errMsg);
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.REVOKE_DEVICE_WITH_QR_CODE)){
            return ("Revokation failed. A device cannot revoke itself.\n" + errMsg);
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.RESET_PIN)){
            return ("Reset PIN failed. Please verify that you entered the correct PIN!");
        } else if(workflowType.equals(OstWorkflowContext.WORKFLOW_TYPE.LOGOUT_ALL_SESSIONS)){
            return ("Sessions Logging Out Failed!\n" + errMsg);
        }
        return null;
    }

    public String getCurrentUserViewAddress(){
        String viewEndPoint = AppProvider.get().getCurrentEconomy().getViewApiEndpoint();
        try{
            LogInUser logInUser = AppProvider.get().getCurrentUser();
            OstToken token = OstSdk.getToken(logInUser.getTokenId());
            JSONArray auxChains = token.getAuxiliaryChain();
            JSONObject jsonObject = auxChains.getJSONObject(0);
            String tokenAddr = jsonObject.getString("utility_branded_token");
            String url = viewEndPoint + "token/th-" + token.getChainId() + "-" +
                    tokenAddr + "-" +
                    logInUser.getOstUser().getTokenHolderAddress();
            return url;
        } catch (Exception e){ }
        return viewEndPoint;
    }

    public boolean isBioMetricEnrolled() {
       return isBioMetric(false);
    }

    public boolean isBioMetricHardwareAvailable() {
        return isBioMetric(true);
    }

    private boolean isBioMetric(boolean checkForHardware) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            FingerprintManager fingerprintManager = (FingerprintManager) AppProvider.get().getApplicationContext()
                    .getSystemService(Context.FINGERPRINT_SERVICE);
            if (null != fingerprintManager) {
                if (checkForHardware) {
                    return fingerprintManager.isHardwareDetected();
                } else {
                    return fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
                }
            }
        }
        return false;
    }

    public void showEnableBiometricDialog(DialogInterface.OnClickListener onCancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AppProvider.get().getCurrentActivity());
        builder.setCancelable(true);
        builder.setMessage("Enroll for Biometric to use application effectively");
        builder.setTitle("Enroll for Biometric");
        builder.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which)
            {
                AppProvider.get().getCurrentActivity().startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            }
        });
        builder.setNegativeButton("Cancel", onCancelListener);
        builder.create().show();
    }

    public boolean showEconomyChangeDialog(Intent intent, String source){
        try{
            String intentData = URLDecoder.decode(intent.getData().getEncodedQuery(), "UTF-8");
            intent.setData(null);
            String launchData = intentData.replace("ld=", "");
            // If current economy is not set and data is given in intent then set that economy without alert.
            if(null == AppProvider.get().getCurrentEconomy()){
                CurrentEconomy currentEconomy = CurrentEconomy.newInstance(launchData);
                AppProvider.get().setCurrentEconomy(currentEconomy);
                return false;
            }
            // Current Economy is present then check whether its changed or same.
            JSONObject jsonObject = new JSONObject(launchData);
            if(!jsonObject.optString("token_id").equals(AppProvider.get().getCurrentEconomy().getTokenId())){
                AlertDialog.Builder builder = new AlertDialog.Builder(AppProvider.get().getCurrentActivity());
                if(source.equals(OnBoardingActivity.LOG_TAG)){
                    builder.setTitle("Part of Other Economy");
                    builder.setMessage("You appear to be using another economy. Do you want to switch Economy?");
                    builder.setPositiveButton("Replace Economy", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try{
                                CurrentEconomy currentEconomy = CurrentEconomy.newInstance(launchData);
                                AppProvider.get().setCurrentEconomy(currentEconomy);
                            } catch (Exception e){}
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                } else {
                    builder.setTitle("Logged In Other Economy");
                    builder.setMessage("You appear to be logged in to another economy, please log out of the application and try connecting again.");
                    builder.setPositiveButton("OK", null);
                }
                builder.create().show();
                return true;
            }
        } catch (Exception e){ }
        return false;
    }
}