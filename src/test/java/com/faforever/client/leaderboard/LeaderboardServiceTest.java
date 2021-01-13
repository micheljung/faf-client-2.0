package com.faforever.client.leaderboard;


import com.faforever.client.api.FafApiAccessor;
import com.faforever.client.builders.LeaderboardBeanBuilder;
import com.faforever.client.builders.LeaderboardEntryBeanBuilder;
import com.faforever.client.builders.LeagueEntryBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.SubdivisionBeanBuilder;
import com.faforever.client.domain.LeaderboardBean;
import com.faforever.client.domain.LeaderboardEntryBean;
import com.faforever.client.domain.LeagueEntryBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.SubdivisionBean;
import com.faforever.client.mapstruct.CycleAvoidingMappingContext;
import com.faforever.client.mapstruct.LeaderboardMapper;
import com.faforever.client.mapstruct.MapperSetup;
import com.faforever.client.player.PlayerService;
import com.faforever.client.test.ElideMatchers;
import com.faforever.client.test.ServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.faforever.commons.api.elide.ElideNavigator.qBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LeaderboardServiceTest extends ServiceTest {

  @Mock
  private FafApiAccessor fafApiAccessor;
  @Mock
  private PlayerService playerService;

  private LeaderboardService instance;

  private LeaderboardBean leaderboard;
  private final LeaderboardMapper leaderboardMapper = Mappers.getMapper(LeaderboardMapper.class);
  private PlayerBean player;

  @BeforeEach
  public void setUp() throws Exception {
    MapperSetup.injectMappers(leaderboardMapper);
    player = PlayerBeanBuilder.create().defaultValues().id(1).username("junit").get();
    leaderboard = LeaderboardBeanBuilder.create().defaultValues().get();

    instance = new LeaderboardService(fafApiAccessor, leaderboardMapper, playerService);
  }

  @Test
  public void testGetLeaderboards() {
    LeaderboardBean leaderboardBean = LeaderboardBeanBuilder.create().defaultValues().get();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardBean, new CycleAvoidingMappingContext())));

    List<LeaderboardBean> results = instance.getLeaderboards().toCompletableFuture().join();

    verify(fafApiAccessor).getMany(any());
    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(leaderboardBean));
  }

  @Test
  public void testGetLeaderboardEntries() {
    LeaderboardEntryBean leaderboardEntryBean = LeaderboardEntryBeanBuilder.create().defaultValues().get();

    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext())));

    List<LeaderboardEntryBean> results = instance.getEntries(leaderboard).toCompletableFuture().join();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("rating", false)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
    assertThat(results, hasSize(1));
    assertThat(results.get(0), is(leaderboardEntryBean));
  }

  @Test
  public void testGetEntriesForPlayer() {
    LeaderboardEntryBean leaderboardEntryBean = LeaderboardEntryBeanBuilder.create().defaultValues().get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leaderboardEntryBean, new CycleAvoidingMappingContext())));

    List<LeaderboardEntryBean> result = instance.getEntriesForPlayer(player).toCompletableFuture().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("player.id").eq(player.getId()))));
    Assertions.assertEquals(List.of(leaderboardEntryBean), result);
  }

  @Test
  public void testGetLeagues() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    instance.getLeagues().toCompletableFuture().join();

    verify(fafApiAccessor).getMany(any());
  }

  @Test
  public void testGetLatestSeason() {
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.empty());

    instance.getEntries(leaderboard).toCompletableFuture().join();

    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasSort("endDate", true)));
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.filterPresent()));
  }

  @Test
  public void testGetAccumulatedRank() {
    when(instance.getSizeOfDivision(any())).thenReturn(CompletableFuture.completedFuture(10));

  }

  @Test
  public void testGetTotalPlayers() {
    when(instance.getSizeOfDivision(any())).thenReturn(CompletableFuture.completedFuture(10));

  }

  @Test
  public void testGetSizeOfDivision() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().get();
    when(instance.getEntries(subdivisionBean)).thenReturn(CompletableFuture.completedFuture(List.of(leagueEntryBean)));

    int result = instance.getSizeOfDivision(subdivisionBean).toCompletableFuture().join();
    Assertions.assertEquals(1, result);
  }

  @Test
  public void testGetLeagueEntryForPlayer() {
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().username("junit").get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext())));

    LeagueEntryBean result = instance.getLeagueEntryForPlayer(player, 2).toCompletableFuture().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().intNum("loginId").eq(player.getId()).and()
        .intNum("leagueSeason.id").eq(2))));
    Assertions.assertEquals(leagueEntryBean, result);
  }

  @Test
  public void testGetLeagueEntries() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().leagueSeasonId(1).get();
    LeagueEntryBean leagueEntryBean = LeagueEntryBeanBuilder.create().defaultValues().leagueSeasonId(1).subdivision(subdivisionBean).get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(leagueEntryBean, new CycleAvoidingMappingContext())));
    when(playerService.getPlayersByIds(List.of(0))).thenReturn(
        CompletableFuture.completedFuture(List.of(PlayerBeanBuilder.create().id(0).username("junit").get())));

    List<LeagueEntryBean> result = instance.getEntries(subdivisionBean).toCompletableFuture().join();
    Assertions.assertEquals(List.of(leagueEntryBean), result);
  }

  @Test
  public void testGetAllSubdivisions() {
    SubdivisionBean subdivisionBean = SubdivisionBeanBuilder.create().defaultValues().leagueSeasonId(1).get();
    when(fafApiAccessor.getMany(any())).thenReturn(Flux.just(leaderboardMapper.map(subdivisionBean, new CycleAvoidingMappingContext())));

    List<SubdivisionBean> result = instance.getAllSubdivisions(1).toCompletableFuture().join();
    verify(fafApiAccessor).getMany(argThat(ElideMatchers.hasFilter(qBuilder().string("leagueSeasonDivision.leagueSeason.id").eq("1"))));
    Assertions.assertEquals(List.of(subdivisionBean), result);
  }
}
