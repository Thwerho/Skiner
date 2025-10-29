package com.bot.subscriptionsCheckApp.Models;

import jakarta.persistence.*;

//@Entity
//@Table(name = "users")
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegram_id;

    @Column(name = "vk_id", nullable = false, unique = true)
    private String vk_id;

}
