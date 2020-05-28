package com.cratos.game.flicker.processors;

import java.lang.reflect.Type;
import java.util.concurrent.ThreadPoolExecutor;

import org.redkale.convert.json.JsonConvert;
import org.redkale.service.RetResult;
import org.redkale.util.Comment;

import com.cratos.game.flicker.FlickerService;
import com.cratos.game.flicker.bean.req.ReqBean;
import com.cratos.game.flicker.bean.req.ReqBean.ReqParams;

/**
 * 计算参数处理器
 * 
 * @author fk
 * @param <T>
 */
public abstract class RequestProcessor<T extends ReqParams> {

	@Comment("类型")
	public static  int TYPE = 10000_0;
	
	public static final JsonConvert CONVERT = JsonConvert.root();

	public abstract Type getReqParamsClass();

	@Comment("对象转换")
	private void convert(ReqBean<T> data) {
		data.setParamsObj(CONVERT.convertFrom(getReqParamsClass(), data.params));
	}

	@SuppressWarnings("rawtypes")
	public final RetResult validate(ReqBean<T> request) {
		convert(request); // 转换对象
		RetResult rt = doValidate(request);
		if (!rt.isSuccess()) return rt;
		return RetResult.success();
	}
	
	@SuppressWarnings("rawtypes")
	protected  RetResult doValidate(ReqBean<T> request) {
		return RetResult.success();
	}
	public final void processRequest(FlickerService service, ReqBean<T> request, ThreadPoolExecutor executor) throws RequestProcessorException {
		if (executor != null) {
			executor.execute(() -> { 
				beforeExecute(request);// 执行前
				execute(service, request); // 执行
				afterExecute(request);// 执行后
			});
		} else {
			beforeExecute(request);// 执行前
			execute(service, request); // 执行
			afterExecute(request);// 执行后
		}
	}

	@Comment("执行前")
	protected void beforeExecute(ReqBean<T> data) {
		
	}

	@Comment("执行后")
	protected void afterExecute(ReqBean<T> data) {

	}

	@Comment("执行")
	protected abstract void execute(FlickerService service, ReqBean<T> data);

	protected void shutdown() {

	}

	

}
