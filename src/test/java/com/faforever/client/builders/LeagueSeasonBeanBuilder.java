package com.faforever.client.builders;

import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeagueBean;
import com.faforever.client.domain.LeagueSeasonBean;

import java.time.OffsetDateTime;

public class LeagueSeasonBeanBuilder {
  public static LeagueSeasonBeanBuilder create() {
    return new LeagueSeasonBeanBuilder();
  }

  private final LeagueSeasonBean leagueSeasonBean = new LeagueSeasonBean();

  public LeagueSeasonBeanBuilder defaultValues() {
    league(LeagueBeanBuilder.create().defaultValues().get());
    leaderboard(LeaderboardBeanBuilder.create().defaultValues().get());
    nameKey("test_description");
    id(0);
    return this;
  }

  public LeagueSeasonBeanBuilder league(LeagueBean league) {
    leagueSeasonBean.setLeague(league);
    return this;
  }

  public LeagueSeasonBeanBuilder leaderboard(LeaderboardBean leaderboard) {
    leagueSeasonBean.setLeaderboard(leaderboard);
    return this;
  }

  public LeagueSeasonBeanBuilder nameKey(String nameKey) {
    leagueSeasonBean.setNameKey(nameKey);
    return this;
  }

  public LeagueSeasonBeanBuilder id(Integer id) {
    leagueSeasonBean.setId(id);
    return this;
  }

  public LeagueSeasonBeanBuilder createTime(OffsetDateTime createTime) {
    leagueSeasonBean.setCreateTime(createTime);
    return this;
  }

  public LeagueSeasonBeanBuilder updateTime(OffsetDateTime updateTime) {
    leagueSeasonBean.setUpdateTime(updateTime);
    return this;
  }

  public LeagueSeasonBean get() {
    return leagueSeasonBean;
  }

}