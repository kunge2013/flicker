package com.cratos.game.flicker.bean;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ConfrontationCampResult {
	/**
	 * 
	 */
	private static final long serialVersionUID = 664495252659917476L;
	
	private List<RoundRecord> roundRecords = Collections.synchronizedList(new LinkedList<RoundRecord>());

	
	public Collection<RoundRecord> getRoundRecords() {
		return roundRecords;
	}


	public void setRoundRecords(List<RoundRecord> roundRecords) {
		this.roundRecords = roundRecords;
	}


	public int addRoundRecord(RoundRecord roundRecord) {
		this.getRoundRecords().add(roundRecord);
		return this.getRoundRecords().size();
	}
}
