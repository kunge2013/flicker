package com.cratos.game.flicker.bean;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.redkale.convert.json.JsonConvert;

public abstract class BaseBean implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -697149698744719031L;

	
	@Override
	public String toString() {
		return JsonConvert.root().convertTo(this);
	}
	
	protected final Logger logger = Logger.getLogger(this.getClass().getSimpleName());

    protected final boolean finer = logger.isLoggable(Level.FINER);

    protected final boolean finest = logger.isLoggable(Level.FINEST);
}
