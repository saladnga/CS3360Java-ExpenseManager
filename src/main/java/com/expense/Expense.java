package com.expense;

public class Expense {
    private Integer id;
    private String date;
    private String name;
    private Double amount;
    private String category;
    private String description;
    private Double displayAmount;

    public Expense(Integer id, String date, String name, Double amount, String category, String description) {
        this.id = id;
        this.date = date;
        this.name = name;
        this.amount = amount;
        this.category = category;
        this.description = description;
    }

    public Expense(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    public Double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }
    
    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getDisplayAmount() {
        return displayAmount != null ? displayAmount : amount;
    }

    public void setDisplayAmount(Double value) {
        this.displayAmount = value;
    }
}


