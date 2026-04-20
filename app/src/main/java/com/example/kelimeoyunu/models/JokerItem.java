package com.example.kelimeoyunu.models;

public class JokerItem {
    public String name;
    public String description;
    public int price;
    public String icon;
    public int ownedQuantity; // YENİ: Kullanıcının elindeki miktar

    public JokerItem(String name, String description, int price, String icon, int ownedQuantity) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.icon = icon;
        this.ownedQuantity = ownedQuantity;
    }
}