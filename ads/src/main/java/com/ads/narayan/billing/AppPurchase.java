package com.ads.narayan.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import com.ads.narayan.event.NarayanLogEventManager;
import com.ads.narayan.funtion.BillingListener;
import com.ads.narayan.funtion.PurchaseListener;
import com.ads.narayan.funtion.UpdatePurchaseListener;
import com.ads.narayan.util.AppUtil;
import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ConsumeParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryProductDetailsResult;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.common.collect.ImmutableList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppPurchase {
    private static final String LICENSE_KEY = null;
    private static final String MERCHANT_ID = null;
    private static final String TAG = "PurchaseEG";

    public static final String PRODUCT_ID_TEST = "android.test.purchased";
    @SuppressLint("StaticFieldLeak")
    private static AppPurchase instance;

    @SuppressLint("StaticFieldLeak")
    private String price = "1.49$";
    private String oldPrice = "2.99$";

    @Deprecated
    private String productId;
    private ArrayList<QueryProductDetailsParams.Product> listSubscriptionId;
    private ArrayList<QueryProductDetailsParams.Product> listINAPId;
    private PurchaseListener purchaseListener;
    private UpdatePurchaseListener updatePurchaseListener;
    private BillingListener billingListener;
    private Boolean isInitBillingFinish = false;
    private BillingClient billingClient;
    private List<ProductDetails> skuListINAPFromStore;
    private List<ProductDetails> skuListSubsFromStore;
    final private Map<String, ProductDetails> skuDetailsINAPMap = new HashMap<>();
    final private Map<String, ProductDetails> skuDetailsSubsMap = new HashMap<>();
    private boolean isAvailable;
    private boolean isListGot;
    private boolean isConsumePurchase = false;

    private int countReconnectBilling = 0;
    private int countMaxReconnectBilling = 4;
    //tracking purchase adjust
    private String idPurchaseCurrent = "";
    private int typeIap;
    // status verify purchase INAP & SUBS
    private boolean verifyFinish = false;

    private boolean isVerifyINAP = false;
    private boolean isVerifySUBS = false;
    private boolean isUpdateInapps = false;
    private boolean isUpdateSubs = false;

    private boolean isPurchase = false;//state purchase on app
    private String idPurchased = "";//id purchased
    private List<PurchaseResult> ownerIdSubs = new ArrayList<>();//id sub
    private List<String> ownerIdInapps = new ArrayList<>();//id inapp

    private Handler handlerTimeout;
    private Runnable rdTimeout;

    public void setPurchaseListener(PurchaseListener purchaseListener) {
        this.purchaseListener = purchaseListener;
    }

    public void setUpdatePurchaseListener(UpdatePurchaseListener listener) {
        this.updatePurchaseListener = listener;
    }

    public void setBillingListener(BillingListener billingListener) {
        this.billingListener = billingListener;
        if (isAvailable) {
            billingListener.onInitBillingFinished(0);
            isInitBillingFinish = true;
        }
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public Boolean getInitBillingFinish() {
        return isInitBillingFinish;
    }

    public void setEventConsumePurchaseTest(View view) {
        view.setOnClickListener(view1 -> {
            if (AppUtil.VARIANT_DEV) {
                Log.e(TAG, "setEventConsumePurchaseTest: success");
                AppPurchase.getInstance().consumePurchase(PRODUCT_ID_TEST);
            }
        });
    }

    public void setBillingListener(BillingListener billingListener, int timeout) {
        Log.e(TAG, "setBillingListener: timeout " + timeout);
        this.billingListener = billingListener;
        if (isAvailable) {
            Log.e(TAG, "setBillingListener: finish");
            billingListener.onInitBillingFinished(0);
            isInitBillingFinish = true;
            return;
        }
        handlerTimeout = new Handler();
        rdTimeout = () -> {
            Log.e(TAG, "setBillingListener: timeout run ");
            isInitBillingFinish = true;
            billingListener.onInitBillingFinished(BillingClient.BillingResponseCode.ERROR);
        };
        handlerTimeout.postDelayed(rdTimeout, timeout);
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public void setConsumePurchase(boolean consumePurchase) {
        isConsumePurchase = consumePurchase;
    }

    public void setOldPrice(String oldPrice) {
        this.oldPrice = oldPrice;
    }

    PurchasesUpdatedListener purchasesUpdatedListener = new PurchasesUpdatedListener() {
        @Override
        public void onPurchasesUpdated(@NonNull BillingResult billingResult, List<Purchase> list) {
            Log.e(TAG, "onPurchasesUpdated code: " + billingResult.getResponseCode());
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                for (Purchase purchase : list) {

                    // Try to obtain product IDs in a resilient way
                    List<String> skuList = null;
                    try {
                        skuList = purchase.getProducts(); // preferred new API
                    } catch (NoSuchMethodError | AbstractMethodError ex) {
                        try {
                            skuList = purchase.getSkus(); // deprecated fallback (safe)
                        } catch (Throwable t) {
                            skuList = null;
                        }
                    } catch (Throwable t) {
                        skuList = null;
                    }

                    handlePurchase(purchase);
                    try {
                        updatePurchaseStatus(purchase);
                    } catch (Exception e) {
                        // ignore update errors
                    }
                }
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                if (purchaseListener != null)
                    purchaseListener.onUserCancelBilling();
                Log.e(TAG, "onPurchasesUpdated:USER_CANCELED");
            } else {
                Log.e(TAG, "onPurchasesUpdated:... ");
            }
        }
    };

    BillingClientStateListener purchaseClientStateListener = new BillingClientStateListener() {
        @Override
        public void onBillingServiceDisconnected() {
            isAvailable = false;
        }

        @Override
        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
            Log.e(TAG, "onBillingSetupFinished:  " + billingResult.getResponseCode());

            if (!isInitBillingFinish) {
                verifyPurchased(true);
            }

            isInitBillingFinish = true;
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                isAvailable = true;

                if (listINAPId != null && listINAPId.size() > 0) {
                    QueryProductDetailsParams paramsINAP = QueryProductDetailsParams.newBuilder()
                            .setProductList(listINAPId)
                            .build();

                    billingClient.queryProductDetailsAsync(
                            paramsINAP,
                            new ProductDetailsResponseListener() {
                                @Override
                                public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                                    if (queryProductDetailsResult.getProductDetailsList() != null) {
                                        Log.e(TAG, "onSkuINAPDetailsResponse: " + queryProductDetailsResult.getProductDetailsList().size());
                                        skuListINAPFromStore = queryProductDetailsResult.getProductDetailsList();
                                        isListGot = true;
                                        addSkuINAPToMap(queryProductDetailsResult.getProductDetailsList());
                                    }
                                }
                            });
                }

                if (listSubscriptionId != null && listSubscriptionId.size() > 0) {
                    QueryProductDetailsParams paramsSUBS = QueryProductDetailsParams.newBuilder()
                            .setProductList(listSubscriptionId)
                            .build();

                    billingClient.queryProductDetailsAsync(
                            paramsSUBS,
                            new ProductDetailsResponseListener() {
                                @Override
                                public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                                    if (queryProductDetailsResult.getProductDetailsList() != null) {
                                        Log.e(TAG, "onSkuSubsDetailsResponse: " + queryProductDetailsResult.getProductDetailsList().size());
                                        skuListSubsFromStore = queryProductDetailsResult.getProductDetailsList();
                                        isListGot = true;
                                        addSkuSubsToMap(queryProductDetailsResult.getProductDetailsList());
                                    }
                                }
                            });
                }
            } else {
                Log.e(TAG, "onBillingSetupFinished:ERROR code=" + billingResult.getResponseCode());
            }
        }
    };

    public static AppPurchase getInstance() {
        if (instance == null) {
            instance = new AppPurchase();
        }
        return instance;
    }

    public List<PurchaseResult> getOwnerIdSubs() {
        return ownerIdSubs;
    }

    public List<String> getOwnerIdInapps() {
        return ownerIdInapps;
    }

    private AppPurchase() {

    }

    public void initBilling(final Application application, List<String> listINAPId, List<String> listSubsId) {

        if (AppUtil.VARIANT_DEV) {
            // auto add purchase test when dev
            listINAPId.add(PRODUCT_ID_TEST);
        }
        this.listSubscriptionId = listIdToListProduct(listSubsId, BillingClient.ProductType.SUBS);
        this.listINAPId = listIdToListProduct(listINAPId, BillingClient.ProductType.INAPP);

        Log.e(TAG, "initBilling: app list size=>" + listINAPId.size());
        Log.e(TAG, "initBilling: list in app size=>" + (this.listINAPId != null ? this.listINAPId.size() : 0));

        billingClient = BillingClient.newBuilder(application)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build())
                .build();

        billingClient.startConnection(purchaseClientStateListener);
    }

    private void addSkuSubsToMap(List<ProductDetails> skuList) {
        for (ProductDetails skuDetails : skuList) {
            skuDetailsSubsMap.put(skuDetails.getProductId(), skuDetails);
        }
    }

    private void addSkuINAPToMap(List<ProductDetails> skuList) {
        for (ProductDetails skuDetails : skuList) {
            skuDetailsINAPMap.put(skuDetails.getProductId(), skuDetails);
        }
    }

    public void setPurchase(boolean purchase) {
        isPurchase = purchase;
    }

    public boolean isPurchased() {
        return isPurchase;
    }

    public boolean isPurchased(Context context) {
        return isPurchase;
    }

    public String getIdPurchased() {
        return idPurchased;
    }

    private void addOrUpdateOwnerIdSub(PurchaseResult purchaseResult, String id) {
        boolean isExistId = false;
        for (PurchaseResult p : ownerIdSubs) {
            if (p.getProductId().contains(id)) {
                isExistId = true;
                ownerIdSubs.remove(p);
                ownerIdSubs.add(purchaseResult);
                break;
            }
        }
        if (!isExistId) {
            ownerIdSubs.add(purchaseResult);
        }
    }

    // verify purchase state
    public void verifyPurchased(boolean isCallback) {
        Log.e(TAG, "isPurchased : " + (listSubscriptionId != null ? listSubscriptionId.size() : 0));
        verifyFinish = false;
        if (listINAPId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    new PurchasesResponseListener() {
                        public void onQueryPurchasesResponse(
                                BillingResult billingResult,
                                List<Purchase> list) {
                            Log.e(TAG, "verifyPurchased INAPP  code:" + billingResult.getResponseCode() + " ===   size:" + (list != null ? list.size() : 0));
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    List<String> products = safeGetProducts(purchase);
                                    if (products == null) continue;
                                    for (QueryProductDetailsParams.Product id : listINAPId) {
                                        if (products.contains(id.zza())) {
                                            Log.e(TAG, "verifyPurchased INAPP: true");
                                            ownerIdInapps.add(id.zza());
                                            isPurchase = true;
                                        }
                                    }
                                }
                                isVerifyINAP = true;
                                if (isVerifySUBS) {
                                    if (billingListener != null && isCallback) {
                                        billingListener.onInitBillingFinished(billingResult.getResponseCode());
                                        if (handlerTimeout != null && rdTimeout != null) {
                                            handlerTimeout.removeCallbacks(rdTimeout);
                                        }
                                    }
                                    verifyFinish = true;
                                }
                            } else {
                                isVerifyINAP = true;
                                if (isVerifySUBS) {
                                    if (billingListener != null && isCallback) {
                                        billingListener.onInitBillingFinished(billingResult.getResponseCode());
                                        if (handlerTimeout != null && rdTimeout != null) {
                                            handlerTimeout.removeCallbacks(rdTimeout);
                                        }
                                        verifyFinish = true;
                                    }
                                }
                            }
                        }
                    }
            );
        }

        if (listSubscriptionId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    new PurchasesResponseListener() {
                        public void onQueryPurchasesResponse(
                                BillingResult billingResult,
                                List<Purchase> list) {
                            Log.e(TAG, "verifyPurchased SUBS  code:" + billingResult.getResponseCode() + " ===   size:" + (list != null ? list.size() : 0));
                            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                                for (Purchase purchase : list) {
                                    List<String> products = safeGetProducts(purchase);
                                    if (products == null) continue;
                                    for (QueryProductDetailsParams.Product id : listSubscriptionId) {
                                        if (products.contains(id.zza())) {
                                            PurchaseResult purchaseResult = new PurchaseResult(
                                                    purchase.getPackageName(),
                                                    purchase.getProducts(),
                                                    purchase.getPurchaseState(),
                                                    purchase.isAutoRenewing()
                                            );
                                            addOrUpdateOwnerIdSub(purchaseResult, id.zza());
                                            Log.e(TAG, "verifyPurchased SUBS: true");
                                            isPurchase = true;
                                        }
                                    }
                                }
                                isVerifySUBS = true;
                                if (isVerifyINAP) {
                                    if (billingListener != null && isCallback) {
                                        billingListener.onInitBillingFinished(billingResult.getResponseCode());
                                        if (handlerTimeout != null && rdTimeout != null) {
                                            handlerTimeout.removeCallbacks(rdTimeout);
                                        }
                                    }
                                    verifyFinish = true;
                                }
                            } else {
                                isVerifySUBS = true;
                                if (isVerifyINAP) {
                                    if (billingListener != null && isCallback) {
                                        billingListener.onInitBillingFinished(billingResult.getResponseCode());
                                        if (handlerTimeout != null && rdTimeout != null) {
                                            handlerTimeout.removeCallbacks(rdTimeout);
                                        }
                                        verifyFinish = true;
                                    }
                                }
                            }
                        }
                    }
            );
        }
    }

    public void updatePurchaseStatus(Purchase purchaseF) {
        if (listINAPId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                List<String> products = safeGetProducts(purchase);
                                if (products == null) continue;
                                for (QueryProductDetailsParams.Product id : listINAPId) {
                                    if (products.contains(id.zza())) {
                                        if (!ownerIdInapps.contains(id.zza())) {
                                            ownerIdInapps.add(id.zza());
                                        }
                                    }
                                }
                            }
                        }
                        isUpdateInapps = true;
                        if (isUpdateSubs) {
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished(purchaseF);
                            }
                        }
                    }
            );
        }

        if (listSubscriptionId != null) {
            billingClient.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build(),
                    (billingResult, list) -> {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                            for (Purchase purchase : list) {
                                List<String> products = safeGetProducts(purchase);
                                if (products == null) continue;
                                for (QueryProductDetailsParams.Product id : listSubscriptionId) {
                                    if (products.contains(id.zza())) {
                                        PurchaseResult purchaseResult = new PurchaseResult(
                                                purchase.getPackageName(),
                                                purchase.getProducts(),
                                                purchase.getPurchaseState(),
                                                purchase.isAutoRenewing()
                                        );
                                        addOrUpdateOwnerIdSub(purchaseResult, id.zza());
                                    }
                                }
                            }
                        }
                        isUpdateSubs = true;
                        if (isUpdateInapps) {
                            if (updatePurchaseListener != null) {
                                updatePurchaseListener.onUpdateFinished(purchaseF);
                            }
                        }
                    }
            );
        }
    }

    @Deprecated
    public void purchase(Activity activity) {
        if (productId == null) {
            Log.e(TAG, "Purchase false:productId null");
            Toast.makeText(activity, "Product id must not be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        purchase(activity, productId, false);
    }

    //AV
    public String purchase(Activity activity, String productId, Boolean isAdsRemove) {
        if (skuListINAPFromStore == null || skuDetailsINAPMap.isEmpty()) {
            if (purchaseListener != null)
                purchaseListener.displayErrorMessage("Billing error init");
            Toast.makeText(activity, "Billing error", Toast.LENGTH_SHORT).show();
            return "";
        }

        ProductDetails productDetails = skuDetailsINAPMap.get(productId);
        try {
            Log.e(TAG, "purchase: " + (productDetails != null ? productDetails.toString() : "null"));
        } catch (Exception e) {
            Log.e(TAG, "exception: " + e.getMessage());
        }

        if (AppUtil.VARIANT_DEV && isAdsRemove) {
            productId = PRODUCT_ID_TEST;
            PurchaseDevBottomSheet purchaseDevBottomSheet = new PurchaseDevBottomSheet(TYPE_IAP.PURCHASE, productDetails, activity, purchaseListener);
            purchaseDevBottomSheet.show();
            return "";
        }

        if (productDetails == null) {
            Log.e(TAG, "product ID Invalid or Null");
            return "Product ID invalid";
        }

        idPurchaseCurrent = productId;
        typeIap = TYPE_IAP.PURCHASE;

        BillingFlowParams productId1 = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                ImmutableList.of(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build())).build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, productId1);

        switch (billingResult.getResponseCode()) {

            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing not supported for type of request");
                return "Billing not supported for type of request";

            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "";

            case BillingClient.BillingResponseCode.ERROR:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Error completing request");
                return "Error completing request";

            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";

            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";

            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";

            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";

            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Network error.");
                return "Network Connection down";

            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Request Canceled");
                return "Request Canceled";

            case BillingClient.BillingResponseCode.OK:
                return "Subscribed Successfully";
        }
        return "";
    }

    public String subscribe(Activity activity, String SubsId) {

        if (skuListSubsFromStore == null) {
            if (purchaseListener != null)
                purchaseListener.displayErrorMessage("Billing error init");
            return "";
        }

        if (AppUtil.VARIANT_DEV) {
            // use test id in dev
            purchase(activity, PRODUCT_ID_TEST, false);
            return "Billing test";
        }
        ProductDetails productDetails = skuDetailsSubsMap.get(SubsId);
        if (productDetails == null) {
            return "Product ID invalid";
        }
        ProductDetails skuDetails = skuDetailsSubsMap.get(SubsId);
        List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
        String offerToken = subsDetail.get(subsDetail.size() - 1).getOfferToken();
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.of(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .setOfferToken(offerToken)
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

        BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);

        switch (billingResult.getResponseCode()) {

            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Billing not supported for type of request");
                return "Billing not supported for type of request";

            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "";

            case BillingClient.BillingResponseCode.ERROR:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Error completing request");
                return "Error completing request";

            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Error processing request.";

            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Selected item is already owned";

            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Item not available";

            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now";

            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "Timeout";

            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Network error.");
                return "Network Connection down";

            case BillingClient.BillingResponseCode.USER_CANCELED:
                if (purchaseListener != null)
                    purchaseListener.displayErrorMessage("Request Canceled");
                return "Request Canceled";

            case BillingClient.BillingResponseCode.OK:
                return "Subscribed Successfully";
        }
        return "";
    }

    public void consumePurchase() {
        if (productId == null) {
            Log.e(TAG, "Consume Purchase false:productId null ");
            return;
        }
        consumePurchase(productId);
    }

    public void consumePurchase(String productId) {
        isPurchase = false;
        billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                (billingResult, list) -> {
                    Purchase pc = null;
                    if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
                        for (Purchase purchase : list) {
                            List<String> products = safeGetProducts(purchase);
                            if (products != null && products.contains(productId)) {
                                pc = purchase;
                            }
                        }
                    }
                    if (pc == null)
                        return;
                    try {
                        ConsumeParams consumeParams =
                                ConsumeParams.newBuilder()
                                        .setPurchaseToken(pc.getPurchaseToken())
                                        .build();

                        ConsumeResponseListener listener = (billingResult1, purchaseToken) -> {
                            if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                                Log.e(TAG, "onConsumeResponse: OK");
                                verifyPurchased(false);
                            }
                        };

                        billingClient.consumeAsync(consumeParams, listener);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private List<String> getListInappId() {
        List<String> list = new ArrayList<>();
        if (listINAPId == null) return list;
        for (QueryProductDetailsParams.Product product : listINAPId) {
            list.add(product.zza());
        }
        return list;
    }

    private List<String> getListSubId() {
        List<String> list = new ArrayList<>();
        if (listSubscriptionId == null) return list;
        for (QueryProductDetailsParams.Product product : listSubscriptionId) {
            list.add(product.zza());
        }
        return list;
    }

    /**
     * Extract productId safely and notify listener(s).
     */
    private void handlePurchase(Purchase purchase) {

        // robustly extract productId (new API getProducts(), fallback getSkus(), fallback parse originalJson)
        String productIdFromPurchase = null;
        try {
            List<String> products = safeGetProducts(purchase);

            if (products != null && !products.isEmpty()) {
                productIdFromPurchase = products.get(0);
            }

            // fallback: try parse originalJson
            if (productIdFromPurchase == null) {
                String orig = purchase.getOriginalJson();
                if (orig != null && !orig.isEmpty()) {
                    try {
                        JSONObject jo = new JSONObject(orig);
                        // try common keys
                        if (jo.has("productId")) {
                            productIdFromPurchase = jo.optString("productId", null);
                        } else if (jo.has("productIds")) {
                            JSONArray arr = jo.optJSONArray("productIds");
                            if (arr != null && arr.length() > 0) {
                                productIdFromPurchase = arr.optString(0, null);
                            }
                        } else if (jo.has("products")) {
                            JSONArray arr = jo.optJSONArray("products");
                            if (arr != null && arr.length() > 0) {
                                productIdFromPurchase = arr.optString(0, null);
                            }
                        } else if (jo.has("sku")) {
                            productIdFromPurchase = jo.optString("sku", null);
                        }
                    } catch (JSONException ex) {
                        // ignore parse error
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "extractProductId error: " + t.getMessage());
            productIdFromPurchase = null;
        }

        // Prefer productId if present, else fall back to idPurchaseCurrent (last requested) for pricing lookup
        if (productIdFromPurchase != null && !productIdFromPurchase.isEmpty()) {
            idPurchaseCurrent = productIdFromPurchase;
        }

        double price = getPriceWithoutCurrency(idPurchaseCurrent, typeIap);
        String currency = getCurrency(idPurchaseCurrent, typeIap);
        try {
            NarayanLogEventManager.onTrackRevenuePurchase((float) price, currency, idPurchaseCurrent, typeIap);
        } catch (Throwable t) {
            Log.e(TAG, "onTrackRevenuePurchase error: " + t.getMessage());
        }

        if (purchaseListener != null) {
            isPurchase = true;
            // Determine type for listener: try subscription map membership -> SUBS else IN-APP
            String typeStr = "IN-APP";
            if (productIdFromPurchase != null && skuDetailsSubsMap.containsKey(productIdFromPurchase)) {
                typeStr = "SUBS";
            }
            // Backwards-compatible call (existing apps expect this)
            try {
                purchaseListener.onProductPurchased(productIdFromPurchase != null ? productIdFromPurchase : purchase.getOrderId(), purchase.getOriginalJson());
            } catch (Throwable t) {
                Log.e(TAG, "purchaseListener.onProductPurchased failed: " + t.getMessage());
            }

            // If listener also implements new signature onPurchase(type, productId, transactionDetails) call it via reflection
            try {
                Method m = purchaseListener.getClass().getMethod("onPurchase", String.class, String.class, String.class);
                if (m != null) {
                    String toSend = productIdFromPurchase != null ? productIdFromPurchase : purchase.getOrderId();
                    m.invoke(purchaseListener, typeStr, toSend, purchase.getOriginalJson());
                }
            } catch (NoSuchMethodException nsme) {
                // not implemented by listener - ignore
            } catch (Throwable t) {
                Log.e(TAG, "invoke onPurchase reflection failed: " + t.getMessage());
            }
        }

        if (isConsumePurchase) {
            ConsumeParams consumeParams =
                    ConsumeParams.newBuilder()
                            .setPurchaseToken(purchase.getPurchaseToken())
                            .build();

            ConsumeResponseListener listener = new ConsumeResponseListener() {
                @Override
                public void onConsumeResponse(BillingResult billingResult, String purchaseToken) {
                    Log.e(TAG, "onConsumeResponse: " + billingResult.getDebugMessage());
                }
            };

            billingClient.consumeAsync(consumeParams, listener);
        } else {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                try {
                    if (!purchase.isAcknowledged()) {
                        billingClient.acknowledgePurchase(acknowledgePurchaseParams, new AcknowledgePurchaseResponseListener() {
                            @Override
                            public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
                                Log.e(TAG, "onAcknowledgePurchaseResponse: " + billingResult.getDebugMessage());
                            }
                        });
                    }
                } catch (Throwable t) {
                    Log.e(TAG, "acknowledgePurchase error: " + t.getMessage());
                }
            }
        }
    }

    @Deprecated
    public String getPrice() {
        return getPrice(productId);
    }

    public String getPrice(String productId) {

        Log.e(TAG, "productId: " + productId);
        ProductDetails skuDetails = skuDetailsINAPMap.get(productId);
        Log.e(TAG, "skuDetails: " + skuDetails);
        if (skuDetails == null)
            return "";

        if (skuDetails.getOneTimePurchaseOfferDetails() == null) return "";

        Log.e(TAG, "getPrice: " + skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice());

        return skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
    }

    public String getPriceSub(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null)
            return "";

        List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
        Log.e(TAG, "getPriceSub: " + pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice());
        return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
    }

    public List<ProductDetails.PricingPhase> getPricePricingPhaseList(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null)
            return null;

        List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
        List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
        return pricingPhaseList;
    }

    public String getIntroductorySubPrice(String productId) {
        ProductDetails skuDetails = skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return "";
        }
        if (skuDetails.getOneTimePurchaseOfferDetails() != null)
            return skuDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
        else if (skuDetails.getSubscriptionOfferDetails() != null) {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(pricingPhaseList.size() - 1).getFormattedPrice();
        } else {
            return "";
        }
    }

    public String getCurrency(String productId, int typeIAP) {
        ProductDetails skuDetails = typeIAP == TYPE_IAP.PURCHASE ? skuDetailsINAPMap.get(productId) : skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return "";
        }
        if (typeIAP == TYPE_IAP.PURCHASE)
            return skuDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
        else {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(pricingPhaseList.size() - 1).getPriceCurrencyCode();
        }
    }

    public double getPriceWithoutCurrency(String productId, int typeIAP) {
        ProductDetails skuDetails = typeIAP == TYPE_IAP.PURCHASE ? skuDetailsINAPMap.get(productId) : skuDetailsSubsMap.get(productId);
        if (skuDetails == null) {
            return 0;
        }
        if (typeIAP == TYPE_IAP.PURCHASE) {
            if (skuDetails.getOneTimePurchaseOfferDetails() == null) return 0;
            // priceAmountMicros is in micros (long). Return as double so caller's code stays compatible.
            return skuDetails.getOneTimePurchaseOfferDetails().getPriceAmountMicros();
        } else {
            List<ProductDetails.SubscriptionOfferDetails> subsDetail = skuDetails.getSubscriptionOfferDetails();
            List<ProductDetails.PricingPhase> pricingPhaseList = subsDetail.get(subsDetail.size() - 1).getPricingPhases().getPricingPhaseList();
            return pricingPhaseList.get(pricingPhaseList.size() - 1).getPriceAmountMicros();
        }
    }

    private String formatCurrency(double price, String currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance();
        format.setMaximumFractionDigits(0);
        format.setCurrency(Currency.getInstance(currency));
        return format.format(price);
    }

    private double discount = 1;

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDiscount() {
        return discount;
    }

    private ArrayList<QueryProductDetailsParams.Product> listIdToListProduct(List<String> listId, String styleBilling) {
        ArrayList<QueryProductDetailsParams.Product> listProduct = new ArrayList<QueryProductDetailsParams.Product>();
        if (listId == null) return listProduct;
        for (String id : listId) {
            QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(styleBilling)
                    .build();
            listProduct.add(product);
        }
        return listProduct;
    }

    @IntDef({TYPE_IAP.PURCHASE, TYPE_IAP.SUBSCRIPTION})
    public @interface TYPE_IAP {
        int PURCHASE = 1;
        int SUBSCRIPTION = 2;
    }

    // --- Helper: safe product extraction (new API then fallback) ---
    private List<String> safeGetProducts(Purchase purchase) {
        if (purchase == null) return null;
        List<String> products = null;
        try {
            products = purchase.getProducts(); // new API
            if (products != null && !products.isEmpty()) return products;
        } catch (NoSuchMethodError | AbstractMethodError ex) {
            // fall through to try deprecated API
        } catch (Throwable t) {
            // ignore
        }

        try {
            List<String> skus = purchase.getSkus(); // deprecated fallback, safe to call inside try/catch
            if (skus != null && !skus.isEmpty()) return skus;
        } catch (Throwable t) {
            // ignore
        }

        // final fallback: parse JSON and look for arrays "productIds" or "products"
        String orig = purchase.getOriginalJson();
        if (orig != null && !orig.isEmpty()) {
            try {
                JSONObject jo = new JSONObject(orig);
                if (jo.has("productId")) {
                    String pid = jo.optString("productId", null);
                    if (pid != null) {
                        List<String> out = new ArrayList<>();
                        out.add(pid);
                        return out;
                    }
                } else if (jo.has("productIds")) {
                    JSONArray arr = jo.optJSONArray("productIds");
                    if (arr != null && arr.length() > 0) {
                        List<String> out = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
                        return out;
                    }
                } else if (jo.has("products")) {
                    JSONArray arr = jo.optJSONArray("products");
                    if (arr != null && arr.length() > 0) {
                        List<String> out = new ArrayList<>();
                        for (int i = 0; i < arr.length(); i++) out.add(arr.optString(i));
                        return out;
                    }
                } else if (jo.has("sku")) {
                    String sku = jo.optString("sku", null);
                    if (sku != null) {
                        List<String> out = new ArrayList<>();
                        out.add(sku);
                        return out;
                    }
                }
            } catch (JSONException e) {
                // ignore
            }
        }
        return null;
    }
}
