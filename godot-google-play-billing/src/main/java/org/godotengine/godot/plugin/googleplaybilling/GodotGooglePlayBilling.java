/*************************************************************************/
/*  GodotGooglePlayBilling.java                                                    */
/*************************************************************************/
/*                       This file is part of:                           */
/*                           GODOT ENGINE                                */
/*                      https://godotengine.org                          */
/*************************************************************************/
/* Copyright (c) 2007-2020 Juan Linietsky, Ariel Manzur.                 */
/* Copyright (c) 2014-2020 Godot Engine contributors (cf. AUTHORS.md).   */
/*                                                                       */
/* Permission is hereby granted, free of charge, to any person obtaining */
/* a copy of this software and associated documentation files (the       */
/* "Software"), to deal in the Software without restriction, including   */
/* without limitation the rights to use, copy, modify, merge, publish,   */
/* distribute, sublicense, and/or sell copies of the Software, and to    */
/* permit persons to whom the Software is furnished to do so, subject to */
/* the following conditions:                                             */
/*                                                                       */
/* The above copyright notice and this permission notice shall be        */
/* included in all copies or substantial portions of the Software.       */
/*                                                                       */
/* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,       */
/* EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF    */
/* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.*/
/* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY  */
/* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,  */
/* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE     */
/* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                */
/*************************************************************************/

package org.godotengine.godot.plugin.googleplaybilling;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.googleplaybilling.utils.GooglePlayBillingUtils;
import org.godotengine.godot.plugin.UsedByGodot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;

import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryPurchasesParams;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.ProductDetailsResponseListener;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class GodotGooglePlayBilling extends GodotPlugin implements PurchasesUpdatedListener, BillingClientStateListener {

	private final BillingClient billingClient;
	private final HashMap<String, ProductDetails> ProductDetailsCache = new HashMap<>(); // use for iap 6.0
	private boolean calledStartConnection;
	private String obfuscatedAccountId;
	private String obfuscatedProfileId;

	public GodotGooglePlayBilling(Godot godot) {
		super(godot);

		billingClient = BillingClient
								.newBuilder(getActivity())
								.enablePendingPurchases()
								.setListener(this)
								.build();
		calledStartConnection = false;
		obfuscatedAccountId = "";
		obfuscatedProfileId = "";
	}

	public void startConnection() {
		calledStartConnection = true;
		billingClient.startConnection(this);
	}

	public void endConnection() {
		billingClient.endConnection();
	}
	@UsedByGodot
	public boolean isReady() {
		return this.billingClient.isReady();
	}
	@UsedByGodot
	public int getConnectionState() {
		return billingClient.getConnectionState();
	}

	@UsedByGodot
	public void queryPurchases(String type) { //type "inapp" or "subs"
		billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build(),
			new PurchasesResponseListener() {
			@Override
			public void onQueryPurchasesResponse(BillingResult billingResult,
					List<Purchase> purchaseList) {
				Dictionary returnValue = new Dictionary();
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					returnValue.put("status", 0); // OK = 0
					returnValue.put("purchases", GooglePlayBillingUtils.convertPurchaseListToDictionaryObjectArray(purchaseList));
				} else {
					returnValue.put("status", 1); // FAILED = 1
					returnValue.put("response_code", billingResult.getResponseCode());
					returnValue.put("debug_message", billingResult.getDebugMessage());
				}
				emitSignal("query_purchases_response", (Object)returnValue);
			}
		});
	}

	@UsedByGodot
	public void queryProductDetails(final String[] list, String type){
		BillingResult featureChecker = billingClient.isFeatureSupported(BillingClient.FeatureType.PRODUCT_DETAILS);
		if (featureChecker.getResponseCode() == BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED){
			emitSignal("feature_not_supported");
			return;
			}
		List<String> pList = Arrays.asList(list);

		ArrayList<QueryProductDetailsParams.Product> productList = new ArrayList<>();
		for(String p : pList) {
			productList.add(
				QueryProductDetailsParams.Product.newBuilder()
					.setProductId(p)
					.setProductType(type)
					.build()
			);
		}
		QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
        												.setProductList(productList)
        												.build();
														
		billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
			@Override
			public void onProductDetailsResponse(BillingResult billingResult, 
					List<ProductDetails> productDetailsList) {
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					for (ProductDetails pDetails : productDetailsList) {
						ProductDetailsCache.put(pDetails.getProductId(), pDetails);
					}
					emitSignal("product_details_query_completed", (Object)GooglePlayBillingUtils.convertProductDetailsListToDictionaryObjectArray(productDetailsList));
				} else {
					emitSignal("product_details_query_error", billingResult.getResponseCode(), billingResult.getDebugMessage(), list);
				}
			}
		});
	}

	@UsedByGodot
	public void acknowledgePurchase(final String purchaseToken) {
		AcknowledgePurchaseParams acknowledgePurchaseParams =
				AcknowledgePurchaseParams.newBuilder()
						.setPurchaseToken(purchaseToken)
						.build();
		billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
			@Override
			public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					emitSignal("purchase_acknowledged", purchaseToken);
				} else {
					emitSignal("purchase_acknowledgement_error", billingResult.getResponseCode(), billingResult.getDebugMessage(), purchaseToken);
				}
			}
		});
	}

	@UsedByGodot
	public void consumePurchase(String purchaseToken) {
		ConsumeParams consumeParams = ConsumeParams.newBuilder()
											  .setPurchaseToken(purchaseToken)
											  .build();

		billingClient.consumeAsync(consumeParams, new ConsumeResponseListener() {
			@Override
			public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
				if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
					emitSignal("purchase_consumed", purchaseToken);
				} else {
					emitSignal("purchase_consumption_error", billingResult.getResponseCode(), billingResult.getDebugMessage(), purchaseToken);
				}
			}
		});
	}

	@Override
	public void onBillingSetupFinished(BillingResult billingResult) {
		if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
			emitSignal("connected");
		} else {
			emitSignal("connect_error", billingResult.getResponseCode(), billingResult.getDebugMessage());
		}
	}

	@Override
	public void onBillingServiceDisconnected() {
		emitSignal("disconnected");
	}

	@UsedByGodot
	public Dictionary purchase(String product_id) {
		return purchaseInternal("", product_id, 
			BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE);
			//BillingFlowParams.ProrationMode.UNKNOWN_SUBSCRIPTION_UPGRADE_DOWNGRADE_POLICY);
	}
	@UsedByGodot
	public Dictionary updateSubscription(String oldToken, String product_id, Object prorationMode) {
		if (prorationMode instanceof String) {
			prorationMode = GooglePlayBillingUtils.recurrenceModeHintStringToInt((String)prorationMode);
		}
		return purchaseInternal(oldToken, product_id, (int)prorationMode);
	}

	private Dictionary purchaseInternal(String oldToken, String product_id, int prorationMode) {
		if (!ProductDetailsCache.containsKey(product_id)) {
			Dictionary returnValue = new Dictionary();
			returnValue.put("status", 1); // FAILED = 1
			returnValue.put("response_code", null); // Null since there is no ResponseCode to return but to keep the interface (status, response_code, debug_message)
			returnValue.put("debug_message", "You must query the product_id details and wait for the result before purchasing!");
			return returnValue;
		}
		ProductDetails productDetails = ProductDetailsCache.get(product_id);
		ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
				ImmutableList.of(
						BillingFlowParams.ProductDetailsParams.newBuilder()
							.setProductDetails(productDetails)
							.build()
				);

		BillingFlowParams.Builder purchaseParamsBuilder = BillingFlowParams.newBuilder();
		purchaseParamsBuilder.setProductDetailsParamsList(productDetailsParamsList);
		if (!obfuscatedAccountId.isEmpty()) {
			purchaseParamsBuilder.setObfuscatedAccountId(obfuscatedAccountId);
		}
		if (!obfuscatedProfileId.isEmpty()) {
			purchaseParamsBuilder.setObfuscatedProfileId(obfuscatedProfileId);
		}
		if (!oldToken.isEmpty() && prorationMode != BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.UNKNOWN_REPLACEMENT_MODE) {
			BillingFlowParams.SubscriptionUpdateParams updateParams =
				BillingFlowParams.SubscriptionUpdateParams.newBuilder()
					.setOldPurchaseToken(oldToken)
					.setSubscriptionReplacementMode(prorationMode)
					.build();
			purchaseParamsBuilder.setSubscriptionUpdateParams(updateParams);
		}
		BillingResult result = billingClient.launchBillingFlow(getActivity(), purchaseParamsBuilder.build());

		Dictionary returnValue = new Dictionary();
		if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
			returnValue.put("status", 0); // OK = 0
		} else {
			returnValue.put("status", 1); // FAILED = 1
			returnValue.put("response_code", result.getResponseCode());
			returnValue.put("response_code_hint", GooglePlayBillingUtils.responseCodeHint(result.getResponseCode()));
			returnValue.put("debug_message", result.getDebugMessage());
		}

		return returnValue;
	}



	@UsedByGodot
	public void setObfuscatedAccountId(String accountId) {
		obfuscatedAccountId = accountId;
	}
	@UsedByGodot
	public void setObfuscatedProfileId(String profileId) {
		obfuscatedProfileId = profileId;
	}

	@Override
	public void onPurchasesUpdated(final BillingResult billingResult, @Nullable final List<Purchase> list) {
		if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
			emitSignal("purchases_updated", (Object)GooglePlayBillingUtils.convertPurchaseListToDictionaryObjectArray(list));
		} else {
			emitSignal("purchase_error", billingResult.getResponseCode(), billingResult.getDebugMessage());
		}
	}

	@Override
	public void onMainResume() {
		if (calledStartConnection) {
			emitSignal("billing_resume");
		}
	}

	@NonNull
	@Override
	public String getPluginName() {
		return "GodotGooglePlayBilling";
	}

	@NonNull
	@Override
	public List<String> getPluginMethods() {
		// if product is not supported in user phone, will change format to querySkuDetails
		// PriceChange not longer available to developer, deleted
		// comfirmPriceChange
		return Arrays.asList("startConnection", "endConnection", "purchase", "updateSubscription", "querySkuDetails", "queryProductDetails", "isReady", "getConnectionState", "queryPurchases", "acknowledgePurchase", "consumePurchase", "setObfuscatedAccountId", "setObfuscatedProfileId");
	}

	@NonNull
	@Override
	public Set<SignalInfo> getPluginSignals() {
		Set<SignalInfo> signals = new ArraySet<>();

		signals.add(new SignalInfo("connected"));
		signals.add(new SignalInfo("disconnected"));
		signals.add(new SignalInfo("billing_resume"));
		signals.add(new SignalInfo("connect_error", Integer.class, String.class));
		signals.add(new SignalInfo("purchases_updated", Object[].class));
		signals.add(new SignalInfo("query_purchases_response", Object.class));
		signals.add(new SignalInfo("purchase_error", Integer.class, String.class));

		// convert skuDetails to productDetails
		signals.add(new SignalInfo("product_details_query_completed", Object[].class));
		signals.add(new SignalInfo("product_details_query_error", Integer.class, String.class, String[].class));
		signals.add(new SignalInfo("feature_not_supported"));

		// deprecated, "price_change_acknowledged" is not able to use
		signals.add(new SignalInfo("purchase_acknowledged", String.class));
		signals.add(new SignalInfo("purchase_acknowledgement_error", Integer.class, String.class, String.class));
		signals.add(new SignalInfo("purchase_consumed", String.class));
		signals.add(new SignalInfo("purchase_consumption_error", Integer.class, String.class, String.class));

		return signals;
	}
}
