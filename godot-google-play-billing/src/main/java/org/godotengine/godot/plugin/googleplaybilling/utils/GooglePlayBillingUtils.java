/*************************************************************************/
/*  GooglePlayBillingUtils.java                                                    */
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

package org.godotengine.godot.plugin.googleplaybilling.utils;

import org.godotengine.godot.Dictionary;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.ProductDetails;

import java.util.ArrayList;
import java.util.List;

public class GooglePlayBillingUtils {
	public static Dictionary convertPurchaseToDictionary(Purchase purchase) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("original_json", purchase.getOriginalJson());
		dictionary.put("order_id", purchase.getOrderId());
		dictionary.put("package_name", purchase.getPackageName());
		dictionary.put("purchase_state", purchase.getPurchaseState());
		dictionary.put("purchase_time", purchase.getPurchaseTime());
		dictionary.put("purchase_token", purchase.getPurchaseToken());
		dictionary.put("quantity", purchase.getQuantity());
		dictionary.put("signature", purchase.getSignature());
		List<String> products = purchase.getProducts();
		dictionary.put("product", products.get(0));
		String[] productsArray = products.toArray(new String[0]);
		dictionary.put("products", productsArray);
		dictionary.put("is_acknowledged", purchase.isAcknowledged());
		dictionary.put("is_auto_renewing", purchase.isAutoRenewing());
		return dictionary;
	}

	public static Dictionary convertProductDetailsToDictionary(ProductDetails details) {
		Dictionary dictionary = new Dictionary();
		dictionary.put("product_id", details.getProductId()); 
		dictionary.put("product_name", details.getName());
		dictionary.put("product_title", details.getTitle());
		dictionary.put("product_description", details.getDescription());
		dictionary.put("product_type", details.getProductType());
		if (details.getSubscriptionOfferDetails() != null) { 
			List<ProductDetails.SubscriptionOfferDetails> subDetailsList = details.getSubscriptionOfferDetails();
				Dictionary subDict = new Dictionary();
				for (ProductDetails.SubscriptionOfferDetails subDetails : subDetailsList) {
					Dictionary subDetailsDict = new Dictionary();
					subDetailsDict.put("baseplan_id", subDetails.getBasePlanId());
					subDetailsDict.put("offer_id", subDetails.getOfferId());
					subDetailsDict.put("offer_token", subDetails.getOfferToken());
					subDetailsDict.put("offer_tags", subDetails.getOfferTags());
					Dictionary pricingPhasesDict = new Dictionary();
					List<ProductDetails.PricingPhase> pricingPhases = subDetails.getPricingPhases().getPricingPhaseList();
					int phase = 0;
					for(ProductDetails.PricingPhase ph : pricingPhases) {
						phase += 1;
						Dictionary phDetailsDict = new Dictionary();
						phDetailsDict.put("billing_cycle_count", ph.getBillingCycleCount());
						phDetailsDict.put("billing_period", ph.getBillingPeriod());
						phDetailsDict.put("billing_price_formatted", ph.getFormattedPrice());
						phDetailsDict.put("billing_price_amount_micros", ph.getPriceAmountMicros());
						phDetailsDict.put("billing_price_currency_codes", ph.getPriceCurrencyCode());
						phDetailsDict.put("billing_recurrence_mode", ph.getRecurrenceMode());
						phDetailsDict.put("billing_recurrence_mode_hint", GooglePlayBillingUtils.recurrenceModeHintGet(ph.getRecurrenceMode()));
						pricingPhasesDict.put("pricing_phase_" + String.valueOf(phase), phDetailsDict);
					}
					subDetailsDict.put("offer_pricing_phases", pricingPhasesDict);
					subDict.put(subDetails.getBasePlanId(), subDetailsDict);
				}
				dictionary.put("product_subs", subDict);
		}
		Dictionary otpoDict = new Dictionary();
		ProductDetails.OneTimePurchaseOfferDetails otpoDetails = details.getOneTimePurchaseOfferDetails();
		otpoDict.put("billing_price_formatted", otpoDetails.getFormattedPrice());
		otpoDict.put("billing_price_amount_micros", otpoDetails.getPriceAmountMicros());
		otpoDict.put("billing_price_currency_codes", otpoDetails.getPriceCurrencyCode());
		dictionary.put("product_one_time_purchase_offer", otpoDict);
		
		return dictionary;
	}


	public static Object[] convertPurchaseListToDictionaryObjectArray(List<Purchase> purchases) {
		Object[] purchaseDictionaries = new Object[purchases.size()];

		for (int i = 0; i < purchases.size(); i++) {
			purchaseDictionaries[i] = GooglePlayBillingUtils.convertPurchaseToDictionary(purchases.get(i));
		}

		return purchaseDictionaries;
	}

	public static Object[] convertProductDetailsListToDictionaryObjectArray(List<ProductDetails> pDetails) {
		Object[] pDetailsDictionaries = new Object[pDetails.size()];
		for (int i = 0; i < pDetails.size(); i++) {
			pDetailsDictionaries[i] = GooglePlayBillingUtils.convertProductDetailsToDictionary(pDetails.get(i));
		}
		return pDetailsDictionaries;
	}


	public static String responseCodeHint(int response_code) {
		switch (response_code){
			case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
				return "BILLING_UNAVAILABLE";
			case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
				return "DEVELOPER_ERROR";
			case BillingClient.BillingResponseCode.ERROR:
				return "ERROR";
			case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
				return "FEATURE_NOT_SUPPORTED";
			case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
				return "ITEM_ALREADY_OWNED";
			case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
				return "ITEM_NOT_OWNED";
			case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
				return "ITEM_UNAVAILABLE";
			case BillingClient.BillingResponseCode.NETWORK_ERROR:
				return "NETWORK_ERROR";
			case BillingClient.BillingResponseCode.OK:
				return "OK";
			case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
				return "SERVICE_DISCONNECTED";
			case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
				return "SERVICE_TIMEOUT";
			case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
				return "SERVICE_UNAVAILABLE";
			case BillingClient.BillingResponseCode.USER_CANCELED:
				return "USER_CANCELED";
		}
		return "Unsupported response code!";
	}

	public static int responseCodeHintToCode(String response_code_hint) {
		switch (response_code_hint){
			case "BILLING_UNAVAILABLE":
				return BillingClient.BillingResponseCode.BILLING_UNAVAILABLE;
			case "DEVELOPER_ERROR":
				return BillingClient.BillingResponseCode.DEVELOPER_ERROR;
			case "ERROR":
				return BillingClient.BillingResponseCode.ERROR;
			case "FEATURE_NOT_SUPPORTED":
				return BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED;
			case "ITEM_ALREADY_OWNED":
				return BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED;
			case "ITEM_NOT_OWNED":
				return BillingClient.BillingResponseCode.ITEM_NOT_OWNED;
			case "ITEM_UNAVAILABLE":
				return BillingClient.BillingResponseCode.ITEM_UNAVAILABLE;
			case "NETWORK_ERROR":
				return BillingClient.BillingResponseCode.NETWORK_ERROR;
			case "OK":
				return BillingClient.BillingResponseCode.OK;
			case "SERVICE_DISCONNECTED":
				return BillingClient.BillingResponseCode.SERVICE_DISCONNECTED;
			case "SERVICE_TIMEOUT":
				return BillingClient.BillingResponseCode.SERVICE_TIMEOUT;
			case "SERVICE_UNAVAILABLE":
				return BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE;
			case "USER_CANCELED":
				return BillingClient.BillingResponseCode.USER_CANCELED;
		}
		return BillingClient.BillingResponseCode.ERROR;
	}

	public static String recurrenceModeHintGet(int rm) {
		// RecurrenceMode means:
		// https://developer.android.com/reference/com/android/billingclient/api/ProductDetails.RecurrenceMode
		// FINITE_RECURRING
		// > The billing plan payment recurs for a fixed number of billing period set in billingCycleCount.
		// INFINITE_RECURRING
		// > The billing plan payment recurs for infinite billing periods unless cancelled.
		// NON_RECURRING
		// > The billing plan payment is a one time charge that does not repeat.
		if (rm == ProductDetails.RecurrenceMode.FINITE_RECURRING)
			return "FINITE_RECURRING";
		if (rm == ProductDetails.RecurrenceMode.INFINITE_RECURRING)
			return "INFINITE_RECURRING";
		if (rm == ProductDetails.RecurrenceMode.NON_RECURRING)
			return "NON_RECURRING";
		return "Not supported recurrence mode";
	}

	public static Integer recurrenceModeHintStringToInt(String hint) {
		if (hint == "FINITE_RECURRING")
			return ProductDetails.RecurrenceMode.FINITE_RECURRING;
		if (hint == "INFINITE_RECURRING")
			return ProductDetails.RecurrenceMode.INFINITE_RECURRING;
		if (hint == "NON_RECURRING")
			return ProductDetails.RecurrenceMode.NON_RECURRING;
		return ProductDetails.RecurrenceMode.NON_RECURRING;
	}

}
