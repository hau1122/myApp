package com.alidao.basic.interceptor;

import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.help.PowerHelper;
import com.alidao.sec.invoke.Interceptor;
import com.alidao.sec.invoke.SecurityRequest;
import com.alidao.sec.invoke.SecurityResponse;

public class WebUserPowerInterceptor implements Interceptor {

	public void invoke(SecurityRequest request, SecurityResponse response)
			throws SecurityException {
		if (StringUtil.isEmpty(PowerHelper.get())) {
			response.setResultType(SecurityResponse.TYPE_PAGE);
			response.setResultData(request.getAppCtxUrl() + "/login");
			response.setIsSecurity(Boolean.FALSE);
		}
	}

}
