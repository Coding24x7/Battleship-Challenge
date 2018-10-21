package com.battleship.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum Rule {
	standard (1),
	@JsonProperty("super-charge")
	super_charge (1),
	desperation (1),
	@JsonProperty("2-shot")
	x2_shot (2),
	@JsonProperty("3-shot")
	x3_shot (3),
	@JsonProperty("4-shot")
	x4_shot (4),
	@JsonProperty("5-shot")
	x5_shot (5),
	@JsonProperty("6-shot")
	x6_shot (6),
	@JsonProperty("7-shot")
	x7_shot (7),
	@JsonProperty("8-shot")
	x8_shot (8),
	@JsonProperty("9-shot")
	x9_shot (9),
	;

	private final int shotCount;

    Rule(int shotCount) {
        this.shotCount = shotCount;
    }
    
    public int getShotCount() {
        return this.shotCount;
    }
}
