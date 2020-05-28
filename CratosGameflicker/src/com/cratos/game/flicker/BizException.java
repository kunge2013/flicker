package com.cratos.game.flicker;

public class BizException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8367048609024947986L;

	private int code;
	
	private String message;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public BizException() {
		// TODO Auto-generated constructor stub
	}

	public BizException(int code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	
}
