package com.example.matchpredictor.dto;

public class TeamPredictionRequest {

    private Integer homeTeamId;
    private Integer awayTeamId;

    public TeamPredictionRequest() {}

    public TeamPredictionRequest(Integer homeTeamId, Integer awayTeamId) {
        this.homeTeamId = homeTeamId;
        this.awayTeamId = awayTeamId;
    }

    public Integer getHomeTeamId() {
        return homeTeamId;
    }

    public void setHomeTeamId(Integer homeTeamId) {
        this.homeTeamId = homeTeamId;
    }

    public Integer getAwayTeamId() {
        return awayTeamId;
    }

    public void setAwayTeamId(Integer awayTeamId) {
        this.awayTeamId = awayTeamId;
    }
}
