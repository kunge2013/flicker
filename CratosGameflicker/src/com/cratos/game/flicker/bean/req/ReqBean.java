package com.cratos.game.flicker.bean.req;

import javax.persistence.Transient;

import org.redkale.util.Comment;

import com.cratos.game.flicker.bean.BaseBean;
import com.cratos.game.flicker.bean.req.ReqBean.ReqParams;

/**
 * 请求实体
 * @author fk
 *
 */
public class ReqBean<T extends ReqParams> extends BaseBean {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1644132799956044814L;

	@Comment("http请求序号")
	public String httpseqno = "";

	@Transient
	public int currentUserid;

	public int reqType;
	
	public String params;
	
	public T paramsObj;
	
	
	public int getCurrentUserid() {
		return currentUserid;
	}

	public void setCurrentUserid(int currentUserid) {
		this.currentUserid = currentUserid;
	}
	
	public int getReqType() {
		return reqType;
	}

	public void setReqType(int reqType) {
		this.reqType = reqType;
	}

	public String getParams() {
		return params;
	}

	public void setParams(String params) {
		this.params = params;
	}

	public T getParamsObj() {
		return paramsObj;
	}

	public void setParamsObj(T paramsObj) {
		this.paramsObj = paramsObj;
	}

	public String getHttpseqno() {
		return httpseqno;
	}

	public void setHttpseqno(String httpseqno) {
		this.httpseqno = httpseqno;
	}
	
	public ReqBean() {
		// TODO Auto-generated constructor stub
	}
	
	public static class ReqParams {
		
	}
}
