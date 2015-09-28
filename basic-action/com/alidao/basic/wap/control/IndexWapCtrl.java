package com.alidao.basic.wap.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.wxapi.util.OpenidTracker;

@Controller
@RequestMapping("wap")
public class IndexWapCtrl {

	@RequestMapping("index")
	public String index() {
		String url = "/wap/ptype/list";
		url += "?openid=" + OpenidTracker.get();
		return "redirect:".concat(url);
	}
	
}
