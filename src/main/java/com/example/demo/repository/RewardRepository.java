package com.example.demo.repository;

import com.example.demo.entity.Reward;
import com.example.demo.view.RewardsView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardRepository extends JpaRepository<Reward, Integer> {
    <T> T findById (int rewardId, Class<T> type);
    Reward findByName(String rewardName);
    <T> List <T> findByIdNot (int blankRewardId, Class<T> type);

    <T> List <T> findBy (Class<T> type);
}
