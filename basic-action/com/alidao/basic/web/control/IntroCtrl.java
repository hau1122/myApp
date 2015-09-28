package com.alidao.basic.web.control;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Intro;
import com.alidao.basic.service.IntroService;
import com.alidao.jxe.control.WebCtrl;

@Controller
@RequestMapping("intro")
public class IntroCtrl extends WebCtrl {

	@Autowired
	private IntroService introService;

	@RequestMapping("edit")
	public void edit(Model model) {
		model.addAttribute("object", introService.find(new Intro()));
	}

	@RequestMapping("save")
	public void save(Intro object, HttpServletResponse response)
			throws IOException {
		if (StringUtils.isEmpty(object.getId())) {
			getResponse(introService.save(object)).jsonOut(response);
		} else {
			getResponse(introService.mdfy(object)).jsonOut(response);
		}
	}

}
