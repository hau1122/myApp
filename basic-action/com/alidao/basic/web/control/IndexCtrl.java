package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Manager;
import com.alidao.basic.service.ManagerService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.help.PowerHelper;

@Controller
public class IndexCtrl extends WebCtrl {

	@Autowired
	private ManagerService managerService;

	@RequestMapping("login")
	public void login() {}

	@RequestMapping("index")
	public String index(Model model) {
		String mid = PowerHelper.get();
		if (StringUtil.isNotBlank(mid)) {
			Manager object = managerService.find(mid);
			model.addAttribute("linkman", object.getLinkman());
			return "index";
		} else {
			return "login";
		}
	}

	@RequestMapping("repswd")
	public void repswd() {}
	
	@RequestMapping("left")
	public void left() {}

	@RequestMapping("right")
	public void right() {}

	@RequestMapping("power")
	public void power(HttpServletResponse response) throws IOException {
		getQueryResponse(PowerHelper.get()).jsonOut(response);
	}

}
