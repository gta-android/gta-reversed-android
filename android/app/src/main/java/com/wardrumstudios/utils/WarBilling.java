package com.wardrumstudios.utils;

public class WarBilling extends WarBase {
    public native void changeConnection(boolean z);
    public native void notifyChange(String str, int i);

    public void AddSKU(String id) {
        System.out.println("**** AddSKU: " + id);
    }

    public boolean InitBilling() {
        System.out.println("**** InitBilling");
        return true;
    }

    public boolean RequestPurchase(String id) {
        System.out.println("**** RequestPurchase: " + id);
        return true;
    }

    public String LocalizedPrice(String id) {
        System.out.println("**** LocalizedPrice: " + id);
        return null;
    }

    public void SetBillingKey(String key) {
        System.out.println("**** SetBillingKey: " + key);
    }
}
