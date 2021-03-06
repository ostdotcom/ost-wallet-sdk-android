/*
 * Copyright 2019 OST.com Inc
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */

package com.ost.walletsdk.utils;

import com.ost.walletsdk.OstConstants;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.DynamicArray;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes3;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PricerRule {
    private static final String PAY = "pay";

    public String getPriceTxnExecutableData(String fromAddress, List<String> addressListArray, List<String> amounts, String currencyCode, BigInteger pricePoint) {
        List<Address> addressList = new ArrayList<>();
        List<Uint256> transferAmountList = new ArrayList<>();

        for (String address : addressListArray) {
            addressList.add(new Address(address));
        }
        for (String amount : amounts) {
            transferAmountList.add(new Uint256(new BigInteger(amount)));
        }
        Function function = new Function(
                PAY,  // function we're calling
                Arrays.asList(new Address(fromAddress), new DynamicArray(addressList),
                        new DynamicArray(transferAmountList), new Bytes3(currencyCode.getBytes()),
                        new Uint256(pricePoint)),
                Collections.emptyList());

        return FunctionEncoder.encode(function);
    }

    public String getPricerTransactionRawCallData(String fromAddress, List<String> tokenHolderAddresses, List<String> amounts, String currencyCode, BigInteger pricePoint) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(OstConstants.METHOD, PAY);
            CommonUtils commonUtils = new CommonUtils();
            JSONArray addressesArray = commonUtils.listToJSONArray(tokenHolderAddresses);
            JSONArray amountsArray = commonUtils.listToJSONArray(amounts);

            JSONArray parameters = new JSONArray();
            parameters.put(fromAddress);
            parameters.put(addressesArray);
            parameters.put(amountsArray);
            parameters.put(currencyCode);
            parameters.put(pricePoint.toString());

            jsonObject.put(OstConstants.PARAMETERS, parameters);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public String calDirectTransferSpendingLimit(List<String> amounts, BigDecimal fiatMultiplier) {
        BigInteger bigInteger = BigInteger.ZERO;
        for (String amount : amounts) {
            BigDecimal btFiaEquivalent = fiatMultiplier.multiply(new BigDecimal(amount));
            bigInteger = bigInteger.add(btFiaEquivalent.toBigInteger());
        }
        return bigInteger.toString();
    }
}