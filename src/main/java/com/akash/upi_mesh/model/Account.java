package com.akash.upi_mesh.model;

import jakarta.persistence.*;
// JPA Entity Mapping to the database accounts table
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String upiId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private long balance;

    public Account() {
    }

    public Account(String upiId, String name, long balance) {
        this.upiId = upiId;
        this.name = name;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getUpiId() {
        return upiId;
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}