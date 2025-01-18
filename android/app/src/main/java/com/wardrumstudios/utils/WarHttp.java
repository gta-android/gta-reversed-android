package com.wardrumstudios.utils;

public class WarHttp {
    protected WarHttp(WarBase activity) {
        System.out.println("**** WarHttp");
    }

    public String HttpGet(String url) {
        System.out.println("**** HttpGet: " + url);
        return null;
    }

    public String HttpPost(String url) {
        System.out.println("**** HttpPost: " + url);
        return null;
    }

    public byte[] HttpGetData(String url) {
        System.out.println("**** HttpGetData: " + url);
        return null;
    }

    public void AddHttpGetLineFeeds(boolean value) {
        System.out.println("**** AddHttpGetLineFeeds: " + value);
    }
}
