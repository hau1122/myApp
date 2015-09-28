package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Const;
import com.alidao.basic.service.ConstService;
import com.alidao.jxe.control.WebCtrl;

@Controller
@RequestMapping("const")
public class ConstCtrl extends WebCtrl {

	@Autowired
	private ConstService constService;

	@RequestMapping("edit")
	public void edit(Model model) {
		model.addAttribute("object", constService.find(new Const()));
	}

	@RequestMapping("save")
	public void save(Const object, HttpServletResponse response)
			throws IOException {
		if (StringUtils.isEmpty(object.getId())) {
			getResponse(constService.save(object)).jsonOut(response);
		} else {
			getResponse(constService.mdfy(object)).jsonOut(response);
		}
	}

}
