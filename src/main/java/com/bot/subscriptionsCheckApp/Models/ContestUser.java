package com.bot.subscriptionsCheckApp.Models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contest_users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContestUser
{
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    @Column(name = "id")
    private Long id;

    @Column(name = "vk_id", nullable = false, unique = true)
    private String vk_id;

    @Column(name = "telegram_id", nullable = false, unique = true)
    private Long telegramId;

    @Column(name = "tg_username", nullable = false, unique = false)
    private String telegramUsername;

    @Column(name = "time_joined", nullable = false)
    private LocalDateTime time_joined;

}
