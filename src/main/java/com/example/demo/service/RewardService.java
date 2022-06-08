package com.example.demo.service;

import com.example.demo.entity.Reward;
import com.example.demo.repository.RewardRepository;
import com.example.demo.view.RewardsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RewardService {
    @Autowired
    private RewardRepository rewardRepository;

    //Сохранение награды
    public void saveReward(Reward reward) {
        rewardRepository.save(reward);
    }

    //Получение награды по ее id
    public Reward getReward(int rewardId) {
        return rewardRepository.findById(rewardId, Reward.class);
    }

    //Получение награды по ее id
    public RewardsView getRewardById(int rewardId) {
        return rewardRepository.findById(rewardId, RewardsView.class);
    }

    //Получение награды по ее названию
    public Reward getRewardByName(String rewardName) {
        return rewardRepository.findByName(rewardName);
    }

    //Удаление награды
    public void deleteReward(Reward reward) {
        rewardRepository.delete(reward);
    }

    //Получение списка всех наград без пустой награды
    public <T> List <T> getAllRewardWithoutBlankReward(int blankRewardId, Class<T> type) {
        return rewardRepository.findByIdNot(blankRewardId, type);
    }
}
