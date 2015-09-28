package com.alidao.basic.api.control;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alibaba.fastjson.JSON;
import com.alidao.basic.service.SmsService;
import com.alidao.jse.util.Crypto;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.ParamException;

@Controller
@RequestMapping("api/sms")
public class SmsApiCtrl extends WebCtrl {

	@Autowired
	private SmsService smsService;
	
	@RequestMapping("send")
	public void send(HttpServletRequest request, 
			HttpServletResponse response) throws Exception {
		Map<String, String[]> params = request.getParameterMap();
		Map<String, String> map = null;
		String mobile = null;
		Integer type = null;
		if (params != null && params.size() > 0) {
			map = new HashMap<String, String>();
			for (Map.Entry<String, String[]> entry : params.entrySet()) {
				String key = entry.getKey();
				String[] values = entry.getValue();
				if (values != null && values.length > 0) {
					if ("mobile".equals(key)) {
						mobile = values[0];
					} else if ("type".equals(key)) {
						type = Integer.parseInt(values[0]);
					} else {
						map.put(key, values[0]);
					}
				}
			}
		}
		if (StringUtil.isEmpty(mobile)) {
			throw new ParamException("手机号码不能为空");
		}
		if (!mobile.matches("^1[3-8][0-9]{9}$")) {
			throw new ParamException("手机号码格式错误");
		}
		if (type == null) {
			throw new ParamException("使用场景不能为空");
		}
		System.out.println(JSON.toJSONString(map));
		getResponse(
			smsService.send(mobile, type, map)
		).jsonOut(response);
	}
	
	@RequestMapping("isok")
	public void isok(String mobile, Integer type, 
			String code, HttpServletResponse response) 
					throws Exception {
		getQueryResponse(
			smsService.isok(mobile, type, code)
		).jsonOut(response);
	}
	
	public static void main(String[] args) {
		System.out.println(Crypto.MD5("11"));
	}
	
}
