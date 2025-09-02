package org.example.entity;

// 担当者オブジェクト
public record Member(String name, String mention) {

    @Override
    public String toString() {
        return name + " (" + mention + ")";
    }
}
