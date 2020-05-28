package com.cratos.game.flicker.processors;

import java.lang.reflect.Type;
import java.util.stream.Stream;

import com.cratos.game.flicker.FlickerService;
import com.cratos.game.flicker.bean.req.ReqBean;
import com.cratos.game.flicker.bean.req.ReqBean.ReqParams;
import com.cratos.game.flicker.bean.resp.RespBean;
import com.cratos.game.flicker.processors.DemoProcessor.DemoParams;

public class DemoProcessor extends RequestProcessor<DemoParams> {

	public static final RequestProcessor<DemoParams> instance = new DemoProcessor();

	public static int TYPE = 10000_1;

	@Override
	protected void execute(FlickerService service, ReqBean<DemoParams> data) {
		// 推送相关消息
		service.sendMap(Stream.of(data.getCurrentUserid()), new RespBean() {});
	}

	@Override
	public Type getReqParamsClass() {
		// TODO Auto-generated method stub
		return DemoParams.class;
	}

	public static class DemoParams extends ReqParams {

	}
}
