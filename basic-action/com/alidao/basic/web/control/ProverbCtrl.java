package com.alidao.basic.web.control;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.alidao.basic.entity.Proverb;
import com.alidao.basic.service.ProverbService;
import com.alidao.jse.util.StringUtil;
import com.alidao.jxe.control.WebCtrl;
import com.alidao.jxe.model.PageParam;

@Controller
@RequestMapping("proverb")
public class ProverbCtrl extends WebCtrl {

	@Autowired
	private ProverbService proverbService;
	
	@RequestMapping("init")
	public void init() {}

	@RequestMapping("page")
	public void page(PageParam pageParam, 
			Proverb object, HttpServletResponse response) 
					throws Exception {
		proverbService.page(pageParam, object).jsonOut(response);
	}


	@RequestMapping("input")
	public void edit(String id, Model model) {
		if (id != null) {
			Proverb object = proverbService.find(id);
			model.addAttribute("object", object);
		}
	}

	@RequestMapping("save")
	public void save(
			HttpServletResponse response, 
			Proverb object)
			throws Exception {
		if (StringUtil.isEmpty(object.getId())) {
			getResponse(
				proverbService.save(object)
			).jsonOut(response);
		} else {
			getResponse(
				proverbService.mdfy(object)
			).jsonOut(response);
		}
	}
	
	@RequestMapping("lose/{id}")
	public void lose(
			@PathVariable("id") String id, 
			HttpServletResponse response)
			throws Exception {
		getResponse(
			proverbService.lose(id)
		).jsonOut(response);
	}
}
