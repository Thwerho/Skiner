package com.bot.subscriptionsCheckApp.Repos;

import com.bot.subscriptionsCheckApp.Models.ContestUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContestUserRepository extends JpaRepository<ContestUser, Long>
{
    Optional<ContestUser> findByTelegramId(Long telegram_id);
}
