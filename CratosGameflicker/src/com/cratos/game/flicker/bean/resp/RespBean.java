package com.cratos.game.flicker.bean.resp;

import org.redkale.util.Comment;

import com.cratos.game.flicker.bean.BaseBean;

@Comment("响应结果")
public abstract class RespBean extends BaseBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = 9218591361933301666L;

	@Comment("http请求序号")
	public String httpseqno = "";

	public int reqType;
	
	public String getHttpseqno() {
		return httpseqno;
	}

	public void setHttpseqno(String httpseqno) {
		this.httpseqno = httpseqno;
	}

	public int getReqType() {
		return reqType;
	}

	public void setReqType(int reqType) {
		this.reqType = reqType;
	}

	

}
